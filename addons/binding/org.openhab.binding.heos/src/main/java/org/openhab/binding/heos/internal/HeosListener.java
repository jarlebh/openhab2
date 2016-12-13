/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.heos.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.telnet.TelnetClient;
import org.openhab.binding.heos.internal.messages.HeosMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Manage telnet connection to the Denon Receiver
 *
 * @author Jeroen Idserda
 * @since 1.7.0
 */
public class HeosListener implements Runnable, checkSquence {

    public static final int HEOS_PORT = 1255;

    private static final Logger logger = LoggerFactory.getLogger(HeosListener.class);

    private static final Integer RECONNECT_DELAY = 60000; // 1 minute

    private static final Integer TIMEOUT = 60000; // 1 minute

    private List<String> ipAddrs;

    private HeosUpdateReceivedCallback callback;

    private TelnetClient tc;

    private boolean connected = false;

    private Gson gson = new Gson();

    private LinkedList<String> commands = null;

    public HeosCommandSender sender = null;

    private long currSeq = 0;

    private boolean isSending;

    BufferedReader buffReader = null;

    PrintWriter out = null;

    public HeosListener(String addr, HeosUpdateReceivedCallback callback) {
        logger.debug("HeosListener created {}", addr);
        this.ipAddrs = new ArrayList<String>();
        this.ipAddrs.add(addr);
        this.callback = callback;
        this.commands = new LinkedList<String>();
        this.tc = createTelnetClient();
        this.sender = new HeosCommandSender();
    }

    @Override
    public void run() {
        try {
            if (!connected) {
                connectTelnetClient();
            }
            String line = buffReader.readLine();
            if (!StringUtils.isBlank(line)) {
                logger.debug("Received from {}: {}", tc.getRemoteAddress(), line);
                HeosMessage fromJson = gson.fromJson(line, HeosMessage.class);
                if (fromJson.getHeos().getCommand() == null) {
                    logger.warn("Uknown command in:" + line);
                }
                checkSequence(fromJson);
                if (fromJson.getHeos().getMessage() != null
                        && fromJson.getHeos().getMessage().contains("command under process")) {
                    // Ignore these, wait for the full update
                    logger.debug("Ignore the command under process");
                } else {
                    this.isSending = false;
                    callback.updateReceived(fromJson);
                }
            }
        } catch (SocketTimeoutException e) {
            logger.trace("Socket timeout");
            // Disconnects are not always detected unless you write to the socket.
            sendHeartBeat();
        } catch (IOException e) {
            callback.listenerDisconnected();
            logger.error("Error in telnet connection", e);
            connected = false;
        } catch (Exception e) {
            logger.error("Unknown Failure", e);
            // Disconnects are not always detected unless you write to the socket.
            sendHeartBeat();
        }
        logger.trace("HeosListener Finished {}", this);
    }

    private void checkSequence(HeosMessage fromJson) {
        if (fromJson.getHeos().getMessage() != null && fromJson.getHeos().getMessage().contains("SEQUENCE")) {
            String msg = fromJson.getHeos().getMessage();
            int index = msg.indexOf("SEQUENCE");
            logger.debug("SEQ {}", msg.substring(index));
        }
    }

    private void sendHeartBeat() {
        commands.add("heos://system/heart_beat");
    }

    public void shutdown() {
        disconnect(true);
    }

    private void connectTelnetClient() {
        disconnect(false);
        int delay = 0;

        while (!tc.isConnected()) {
            try {
                Thread.sleep(delay);
                for (String ipAddr : ipAddrs.toArray(new String[0])) {
                    logger.debug("Connecting to {}", ipAddr);
                    try {
                        tc.connect(ipAddr, HeosListener.HEOS_PORT);
                        buffReader = new BufferedReader(new InputStreamReader(tc.getInputStream()));
                        out = new PrintWriter(tc.getOutputStream(), true);
                        commands.add("heos://system/register_for_change_events?enable=off");
                        commands.add("heos://system/prettify_json_response?enable=off");
                        connected = true;
                        callback.listenerConnected();

                        break;

                    } catch (NoRouteToHostException e) {
                        logger.error("Cannot connect to {}, removing IP", ipAddrs, e);
                        disconnect(true);
                        this.ipAddrs.remove(ipAddr);
                    } catch (ConnectException e) {
                        logger.error("Cannot connect to {}", ipAddrs, e);
                        disconnect(true);
                    } catch (IOException e) {
                        logger.error("Cannot connect to {}", ipAddrs, e);
                        disconnect(true);
                    }
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while connecting to {}", ipAddrs, e);
            }

            delay = RECONNECT_DELAY;
        }

        logger.debug("Heos telnet client connected to {}", tc.getRemoteAddress());
    }

    class HeosCommandSender implements Runnable {

        @Override
        public void run() {
            if (!commands.isEmpty() && connected && !isSending) {
                String cmd = commands.removeFirst();
                if (cmd.contains("browse")) {
                    cmd = cmd + "&SEQUENCE=" + currSeq++;
                }
                logger.debug("Sending command: {}", cmd);
                out.println(cmd);
                isSending = true;

            }
        }
    }

    private void disconnect(boolean updateListeners) {
        this.connected = false;
        if (updateListeners) {
            callback.listenerDisconnected();
        }
        if (tc != null && tc.isConnected()) {
            try {
                this.tc.disconnect();
            } catch (IOException e) {
                logger.debug("Error while disconnecting telnet client", e);
            }
        }
    }

    private TelnetClient createTelnetClient() {
        TelnetClient tc = new TelnetClient();
        tc.setDefaultTimeout(TIMEOUT);
        return tc;
    }

    public void sendCommand(HeosCommand command, String param) throws IOException {
        String cmd = "heos://" + command.getGroup() + "/" + command.getCommand();
        if (param != null) {
            cmd = cmd + "?" + param;
        }
        logger.debug("Heos add sendCommand {}", cmd);
        commands.add(cmd);
    }

    /**
     * @return the ipAddr
     */
    public List<String> getIpAddrs() {
        return ipAddrs;
    }

    /**
     * @param ipAddr the ipAddr to set
     */
    public void addIpAddr(String ipAddr) {
        if (!this.ipAddrs.contains(ipAddr)) {
            this.ipAddrs.add(ipAddr);
        }

    }

}
