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
public class HeosListener extends Thread {

    public static final int HEOS_PORT = 1255;

    private static final Logger logger = LoggerFactory.getLogger(HeosListener.class);

    private static final Integer RECONNECT_DELAY = 60000; // 1 minute

    private static final Integer TIMEOUT = 60000; // 1 minute

    private List<String> ipAddrs;

    private HeosUpdateReceivedCallback callback;

    private TelnetClient tc;

    private boolean running = true;

    private boolean connected = false;

    private Gson gson = new Gson();

    private LinkedList<String> commands = null;

    private Thread sender = null;

    public HeosListener(String addr, HeosUpdateReceivedCallback callback) {
        logger.debug("HeosListener created {}", addr);
        this.ipAddrs = new ArrayList<String>();
        this.ipAddrs.add(addr);
        this.callback = callback;
        this.commands = new LinkedList<String>();
        this.tc = createTelnetClient();
    }

    @Override
    public void run() {
        while (running) {
            if (!connected) {
                connectTelnetClient();
            }

            BufferedReader buffReader = new BufferedReader(new InputStreamReader(tc.getInputStream()));
            PrintWriter out = new PrintWriter(tc.getOutputStream(), true);

            do {
                try {
                    String line = buffReader.readLine();
                    if (!StringUtils.isBlank(line)) {
                        logger.trace("Received from {}: {}", tc.getRemoteAddress(), line);
                        HeosMessage fromJson = gson.fromJson(line, HeosMessage.class);
                        if (fromJson.getHeos().getCommand() == null) {
                            logger.warn("Uknown command in:" + line);
                        }
                        callback.updateReceived(fromJson);
                    }
                    Thread.sleep(100);
                } catch (SocketTimeoutException e) {
                    logger.trace("Socket timeout");
                    // Disconnects are not always detected unless you write to the socket.
                    sendHeartBeat(out);
                } catch (IOException e) {
                    callback.listenerDisconnected();
                    logger.error("Error in telnet connection", e);
                    connected = false;
                } catch (InterruptedException e) {
                    logger.trace("InterruptedException");
                    // Disconnects are not always detected unless you write to the socket.
                    disconnect(true);
                } catch (Exception e) {
                    logger.error("Unknown Failure", e);
                    // Disconnects are not always detected unless you write to the socket.
                    sendHeartBeat(out);
                }
            } while (running && connected);
        }
        logger.warn("HeosListener Finished {}", this);
    }

    private void sendHeartBeat(PrintWriter out) {
        if (out != null) {
            out.println("heos://system/heart_beat");
            out.flush();
        }
    }

    public void shutdown() {
        this.running = false;
        disconnect(true);
    }

    private void connectTelnetClient() {
        disconnect(false);
        int delay = 0;

        while (!tc.isConnected()) {
            try {
                for (String ipAddr : ipAddrs) {
                    Thread.sleep(delay);
                    logger.debug("Connecting to {}", ipAddr);
                    tc.connect(ipAddr, HeosListener.HEOS_PORT);
                    commands.add("heos://system/register_for_change_events?enable=on\r\n");
                    connected = true;
                    callback.listenerConnected();
                    sender = new Thread() {
                        @Override
                        public void run() {
                            PrintWriter out = new PrintWriter(tc.getOutputStream(), true);
                            while (running && connected) {
                                if (!commands.isEmpty() && connected) {
                                    out.println(commands.removeFirst());
                                    out.flush();
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    logger.error("Failed to wait", e);
                                }
                            }
                        }
                    };
                    sender.start();
                    break;
                }
            } catch (ConnectException e) {
                logger.error("Cannot connect to {}", ipAddrs, e);
                disconnect(true);
            } catch (IOException e) {
                logger.error("Cannot connect to {}", ipAddrs, e);
                disconnect(true);
            } catch (InterruptedException e) {
                logger.error("Interrupted while connecting to {}", ipAddrs, e);
            }
            delay = RECONNECT_DELAY;
        }

        logger.debug("Heos telnet client connected to {}", tc.getRemoteAddress());
    }

    private void disconnect(boolean updateListeners) {
        this.connected = false;
        if (updateListeners) {
            callback.listenerDisconnected();
        }
        if (tc != null && tc.isConnected()) {
            try {
                this.tc.disconnect();
                this.sender.interrupt();
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
        logger.debug("Heos sendCommand {}", cmd);
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
