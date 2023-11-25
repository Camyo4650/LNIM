package com.camonoxe.Model;

public class Packet {
    public enum Type {
        INIT,
        MESSAGE
    }

    private Type type;
    private String body;

    public Packet(Type type, String body)
    {
        this.type = type;
        this.body = body;
    }

    public String getBody()
    {
        return body;
    }

    public Type getType() {
        return type;
    }
}
