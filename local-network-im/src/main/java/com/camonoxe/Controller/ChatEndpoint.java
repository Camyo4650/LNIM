package com.camonoxe.Controller;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.camonoxe.Model.Message;
import com.camonoxe.Model.MessageLogs;
import com.camonoxe.Model.Packet;
import com.camonoxe.Model.PacketDecoder;
import com.camonoxe.Model.PacketEncoder;
import com.camonoxe.Model.UserTable;
import com.camonoxe.Model.UserTable.User;
import com.google.gson.Gson;


@ServerEndpoint(
    value = "/chat/{userId}", 
    encoders = PacketEncoder.class,
    decoders = PacketDecoder.class)
public class ChatEndpoint {

    private static Gson gson;

    public ChatEndpoint()
    {
        gson = new Gson();
    }

    @OnMessage
    public void OnMessage(@PathParam("userId") String userId, Session session, Packet packet)
    {
        UUID uuid = UUID.fromString(userId);
        if (uuid.compareTo(UserTable.localUser().getUserId()) != 0) return;
        switch (packet.getType()) {
            case INIT:
                @SuppressWarnings("unchecked")
                HashMap<String, String> envelope = gson.fromJson(packet.getBody(), HashMap.class);
                String username = UserTable.localUser().getUsername();
                User user = gson.fromJson(envelope.get("User"), UserTable.User.class);
                user.setIpAddress(envelope.get("IP"), Integer.parseInt(envelope.get("Port")));
                if (!UserTable.isConnectedByUserId(user.getUserId()))
                {
                    InetSocketAddress socketAddress = user.getIpAddress();
                    UserTable.Connect(user.getUserId(), socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
                }
                if (!UserTable.isUserExistsByUserId(user.getUserId()))
                {
                    try {
                        UserTable.addUser(user);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                MessageLogs.messageLogs.put(user.getUserId(), new ArrayList<Message>());
                break;
            case MESSAGE:
                Message message = gson.fromJson(packet.getBody(), Message.class);
                List<Message> p = MessageLogs.messageLogs.get(message.getSenderUUID());
                p.add(message);
                UserTable.updateMessagesDel.NewMessageHandler(message.getSenderUUID());
                break;
            default:
                break;
        }
    }

    @OnClose
    public void OnClose(Session session)
    {
        System.out.println("stupid");
    }
}
