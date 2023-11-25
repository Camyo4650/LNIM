package com.camonoxe.Model;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;

public class PacketEncoder implements Encoder.Text<Packet> {

    private static Gson gson;

    @Override
    public void destroy() {
    }

    @Override
    public void init(EndpointConfig arg0) {
        gson = new Gson();
    }

    @Override
    public String encode(Packet arg0) throws EncodeException {
        return gson.toJson(arg0);
    }
    
}
