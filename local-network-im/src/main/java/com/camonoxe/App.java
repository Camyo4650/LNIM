package com.camonoxe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.websocket.DeploymentException;
import org.glassfish.tyrus.server.Server;
import com.camonoxe.Controller.ChatEndpoint;
import com.camonoxe.Controller.MulticastListener;
import com.camonoxe.Model.SendMessageDel;
import com.camonoxe.Model.SyncDel;
import com.camonoxe.Model.UserTable;
import com.camonoxe.View.GUI;

public class App implements Runnable, SendMessageDel, SyncDel {
    private GUI gui;

    private Server server;
    private int port;

    public App()
    {
        port = findAvailablePort(8080, 20);
        String name = "Session" + (port - 8080 + 1);
        gui = new GUI(name, this, this);
        String tempName = JOptionPane.showInputDialog("Enter desired username");
        if (tempName != null && !tempName.isBlank()) 
        {
            name = tempName;
            gui.setTitle(tempName);
        }
        UserTable.setUsersChangedDel(gui, gui);
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            UserTable.initLocalUser(name, UUID.randomUUID(), socket.getLocalAddress(), port);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        (new App()).run();
    }

    @Override
    public void run() {
        runServer();
        System.out.println("Starting server...");
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
        try {
            server = new Server("0.0.0.0", port, null, null, ChatEndpoint.class);
            server.start();
            new Thread(new MulticastListener()).start();
            multicastSend();
        } catch (DeploymentException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
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

    @Override
    public void refresh() {
        try {
            multicastSend();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}