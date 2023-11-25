package com.camonoxe.Model;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;

import javax.websocket.DeploymentException;

import com.camonoxe.Controller.Phone;

public class UserTable {
    private static User localUser = null; // Note to self: Remember that sessionId is a relationship between the client to the websocket server (stored on the server)
    private static HashMap<UUID, UUID>  userIdToSession = new HashMap<>();  //    UserId <==> sessionId     1 to 1
    private static HashMap<UUID, Phone> sessionToPhone  = new HashMap<>();  // sessionId <==> Phone         1 to 1
    private static HashMap<UUID, User>  sessionToUser   = new HashMap<>();  // sessionId <==> User object   1 to 1

    public static UsersChangedDel usersChangedDel;
    public static UpdateMessagesDel updateMessagesDel;

    public synchronized static void setUsersChangedDel(UsersChangedDel del, UpdateMessagesDel del2)
    {
        usersChangedDel = del;
        updateMessagesDel = del2;
    }

    public synchronized static void initLocalUser(String username, UUID userId, int port)
    {
        if (localUser == null)
        {
            try {
                InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), port);
                localUser = new User(username, userId, null, address);
                System.out.println(String.format("%s || %s || %s\n", username, userId, localUser.getIpAddress()));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static User localUser()
    {
        return localUser;
    }

    public synchronized static String getUsernameByUserId(UUID userId)
    {
        User user = getUserByUserId(userId);
        return user == null ? "You" : "@" + user.username;
    }

    public synchronized static User getUserByUserId(UUID userId)
    {
        for (User u : sessionToUser.values())
        {
            if (u.getUserId().compareTo(userId) == 0) return u;
        }
        return null;
    }

    public synchronized static Phone dialupByUserId(UUID userId)
    {
        UUID sessionId = userIdToSession.get(userId);

        if (sessionId == null) return null;

        return sessionToPhone.get(sessionId);
    }

    public synchronized static boolean isConnectedByUserId(UUID userId)
    {
        return userIdToSession.containsKey(userId);
    }

    public synchronized static boolean isUserExistsByUserId(UUID userId)
    {
        UUID sessionId = userIdToSession.get(userId);

        if (sessionId == null) return false;

        return sessionToUser.containsKey(sessionId) && sessionToUser.get(sessionId) != null;
    }

    private synchronized static void addPhone(UUID userId, UUID session, Phone socket)
    {
        userIdToSession.put(userId, session); // the value can be null if the user did log on at some point, logs off, and decides to log back on
        sessionToPhone.putIfAbsent(session, socket);
    }

    public synchronized static void Connect(UUID userId, String ipAddress, int port) { // this method shou
        try {
            if (isConnectedByUserId(userId)) return;
            URI address = new URI(String.format("ws://%s:%s/chat/%s", ipAddress, port, userId.toString()));
            Phone socket = new Phone(address);
            addPhone(userId, socket.getSessionId(), socket);
            if (isUserExistsByUserId(userId)) return;
            socket.SendSessionRequest();
        } catch (DeploymentException | IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public synchronized static void addUser(User user) // must be in phonebook first
    {
        UUID session = userIdToSession.get(user.userId);
        if (session == null || !sessionToPhone.containsKey(session)) {
            return;
        }
        sessionToUser.put(session, user);
        usersChangedDel.addUserDel(user.userId);
        System.out.println(String.format("JOINED\n\tName:\t%s\n\tSessID:\t%s\n", user.username, session));
    }

    public synchronized static void remUser(UUID sessionId)
    {
        User user = sessionToUser.get(sessionId);
        UUID userId = user.userId;
        System.out.println(String.format("LEFT\n\tName:\t%s\n\tSessID:\t%s\n", user.username, sessionId));
        usersChangedDel.remUserDel(userId);
        sessionToPhone.remove(sessionId);
        sessionToUser.remove(sessionId);
    }

    public static class User implements Serializable {
        private String username;
        private UUID userId;
        private UUID sessionId;
        private transient InetSocketAddress ipAddress;
    
        protected User(String username, UUID userId, UUID sessionId, InetSocketAddress ipAddress)
        {
            this.username = username;
            this.userId = userId;
            this.sessionId = sessionId;
            this.ipAddress = ipAddress;
        }
    
        public String getUsername() {
            return username;
        }
    
        public void setUsername(String username) {
            this.username = username;
        }
    
        public UUID getUserId() {
            return userId;
        }
    
        public void setUserId(UUID userId) {
            this.userId = userId;
        }
        
        public UUID getSessionId() {
            return sessionId;
        }
    
        public void setIpAddress(String ipAddress, int port)
        {
            if (this.ipAddress == null)
            {
                this.ipAddress = new InetSocketAddress(ipAddress, port);
            }
        }

        public InetSocketAddress getIpAddress() {
            return ipAddress;
        }

        @Override
        public String toString()
        {
            return ipAddress.getAddress().getHostAddress() + "  |  " + username;
        }

        @Override
        public boolean equals(Object other)
        {
            User user = (User)other;
            return equals(user);
        }


        public boolean equals(User other)
        {
            if (other == null) return false;
            return other.username == username && other.userId == userId;
        }
    
    }
    
}
