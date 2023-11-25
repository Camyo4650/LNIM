package com.camonoxe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;
import com.camonoxe.Controller.ChatEndpoint;
import com.camonoxe.Controller.MulticastListener;
import com.camonoxe.Model.SendMessageDel;
import com.camonoxe.Model.UserTable;
import com.camonoxe.View.GUI;

public class App implements Runnable, SendMessageDel {
    private GUI gui;

    private Server server;
    private int port;

    public App()
    {
        port = findAvailablePort(8080, 20);
        String name = "Session" + (port - 8080 + 1);
        gui = new GUI(name, this);
        UserTable.setUsersChangedDel(gui, gui);
        UserTable.initLocalUser(name, UUID.randomUUID(), port);
    }

    public static void main(String[] args) throws Exception {
        (new App()).run();
    }

    @Override
    public void run() {
        try {
            multicastSend();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(new MulticastListener()).start();
        System.out.println("Starting server...");
        runServer();
    }

    public void multicastSend() throws IOException
    {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName("230.0.0.0");
        UUID id = UserTable.localUser().getUserId();
        ByteBuffer bb = ByteBuffer.wrap(new byte[24]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        bb.putInt(port);

        DatagramPacket packet = new DatagramPacket(bb.array(), bb.array().length, group, 4446);
        socket.send(packet);
        socket.close();
    }

    public void runServer() {
        server = new Server("0.0.0.0", port, null, null, ChatEndpoint.class);
        try {
            server.start();
        } catch (DeploymentException e) {
            e.printStackTrace();
            return;
        }
        while (gui.isAlive()) { // TODO make this do something
        }
        System.out.println("Closing server...");
        server.stop();
    }

    private static int findAvailablePort(int startingPort, int maxAttempts) {
        for (int port = startingPort; port < startingPort + maxAttempts; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                return port; // Port is available
            } catch (IOException e) {
                // Port is not available, try the next one
            }
        }
        return -1; // No available port found
    }

    @Override
    public void MessageHandler(UUID recipientId, String message) {
        UserTable.dialupByUserId(recipientId).SendMessage(recipientId, message);
    }

}