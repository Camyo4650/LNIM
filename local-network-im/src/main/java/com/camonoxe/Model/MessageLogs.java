package com.camonoxe.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class MessageLogs {
    public static HashMap<UUID, List<Message>> messageLogs = new HashMap<>();

    public synchronized static Pair<Boolean, String> getNewestMessageByUserId(UUID uuid)
    {
        List<Message> messages = messageLogs.get(uuid);
        if (messages == null || (messages != null && messages.size() == 0)) return null;
        Message message = messages.get(messages.size() - 1);
        return new ImmutablePair<Boolean,String>(message.getSenderUUID().equals(UserTable.localUser().getUserId()), message.toString());
    }

    public synchronized static Iterator<Pair<Boolean, String>> getMessagesByUserId(UUID uuid)
    {
        List<Message> messages = messageLogs.get(uuid);
        if (messages == null) return null;
        List<Pair<Boolean, String>> messageList = new ArrayList<>();

        for (Message msg : messages)
        {
            messageList.add(new ImmutablePair<Boolean,String>(msg.getSenderUUID().equals(UserTable.localUser().getUserId()), msg.toString()+"\n"));
        }

        return messageList.iterator();
    }
}
