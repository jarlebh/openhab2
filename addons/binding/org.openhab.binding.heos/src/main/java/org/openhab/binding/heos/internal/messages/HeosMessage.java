package org.openhab.binding.heos.internal.messages;

import org.openhab.binding.heos.internal.HeosCommand;

import com.google.gson.JsonElement;

public class HeosMessage {

    @Override
    public String toString() {
        return "HeosMessage [heos=" + heos + ", payload=" + payload + "]";
    }

    public static class Heos {
        @Override
        public String toString() {
            return "Heos [" + (command != null ? "command=" + command + ", " : "")
                    + (message != null ? "message=" + message + ", " : "") + (result != null ? "result=" + result : "")
                    + "]";
        }

        private HeosCommand command;
        private String message;
        private String result;

        public HeosCommand getCommand() {
            return command;
        }

        public void setCommand(HeosCommand command) {
            this.command = command;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    private Heos heos;
    private JsonElement payload;

    public Heos getHeos() {
        return heos;
    }

    public void setHeos(Heos heos) {
        this.heos = heos;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }

}
