package com.camonoxe.Controller;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.core.HandshakeException;

import com.camonoxe.Model.Message;
import com.camonoxe.Model.MessageLogs;
import com.camonoxe.Model.Packet;
import com.camonoxe.Model.Packet.Type;
import com.camonoxe.Model.PacketDecoder;
import com.camonoxe.Model.PacketEncoder;
import com.camonoxe.Model.UserTable;
import com.google.gson.Gson;

@ClientEndpoint(
    encoders = PacketEncoder.class,
    decoders = PacketDecoder.class
)
public class Phone {
    private Session session;
    private URI uri;

    private static Gson gson;
    public static final long PORT = 8080;

    public Phone(URI uri) throws DeploymentException, IOException
    {
        this.uri = uri;
        boolean serverRunning = false;
        do {
            try {
                session = newSession();
                serverRunning = true;
            } catch (DeploymentException e)
            {
                System.out.println("Failed to connect. Retrying...");
            }
        } while (!serverRunning);
        gson = new Gson();
    }

    private Session newSession() throws DeploymentException, IOException
    {
        ClientManager clientManager = ClientManager.createClient(ContainerProvider.getWebSocketContainer());
        return clientManager.connectToServer(this, this.uri);
    }

    @OnOpen
    public void OnOpen(Session session, EndpointConfig config)
    {
        
    }

    @OnClose
    public void OnClose(Session session, CloseReason closeReason)
    {
        UserTable.remUser(UUID.fromString(session.getId()));
    }

    public UUID getSessionId()
    {
        return UUID.fromString(session.getId());
    }

    public void SendSessionRequest()
    {
        HashMap<String, Object> envelope = new HashMap<>();
        envelope.put("User", gson.toJson(UserTable.localUser()));
        envelope.put("IP",   UserTable.localUser().getIpAddress().getAddress().getHostAddress());
        envelope.put("Port", Integer.toString(UserTable.localUser().getIpAddress().getPort()));
        Packet packet = new Packet(Type.INIT, gson.toJson(envelope));
        try {
            session.getBasicRemote().sendObject(packet);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }

    public void SendMessage(UUID recipientId, String message)
    {
        Message messageObj = new Message(UserTable.localUser().getUserId(), message, new Date());
        MessageLogs.messageLogs.get(recipientId).add(messageObj);
        UserTable.updateMessagesDel.NewMessageHandler(recipientId);
        Packet packet = new Packet(Type.MESSAGE, gson.toJson(messageObj));
        try {
            session.getBasicRemote().sendObject(packet);
        } catch (IOException | EncodeException e) {
            e.printStackTrace();
        }
    }
}
