package com.camonoxe.Controller;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

import com.camonoxe.Model.UserTable;

public class MulticastListener extends Thread {
    protected MulticastSocket socket = null;

    public void run()
    {
        try {
            byte[] buf = new byte[24];
            socket = new MulticastSocket(4446);
            InetAddress group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);
            while (isAlive()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                ByteBuffer bb = ByteBuffer.wrap(packet.getData());
                long high = bb.getLong(), low = bb.getLong();
                int port = bb.getInt();
                UUID userId = new UUID(high, low);
                if (userId == UserTable.localUser().getUserId()) continue;
                UserTable.Connect(userId, packet.getAddress().getHostAddress(), port);
            }
            socket.leaveGroup(group);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
