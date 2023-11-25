package com.camonoxe.Model;

import java.util.Dictionary;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import com.camonoxe.Model.Packet.Type;
import com.google.gson.Gson;

public class PacketDecoder implements Decoder.Text<Packet> {

    private static Gson gson;

    @Override
    public void destroy() {
    }

    @Override
    public void init(EndpointConfig arg0) {
        gson = new Gson();
    }

    @Override
    public Packet decode(String arg0) throws DecodeException {
        return gson.fromJson(arg0, Packet.class);
    }

    @Override
    public boolean willDecode(String arg0) {
        return arg0 != null;
    }
    
}
