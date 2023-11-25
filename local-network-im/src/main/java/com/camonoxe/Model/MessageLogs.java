package com.camonoxe.Model;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MessageLogs {
    public static HashMap<UUID, List<Message>> messageLogs = new HashMap<>();

    public synchronized static String getMessagesByUserId(UUID uuid)
    {
        List<Message> messages = messageLogs.get(uuid);
        if (messages == null) return "";
        StringBuffer builder = new StringBuffer();

        for (Message msg : messages)
        {
            builder.append(msg + "\n");
        }

        return builder.toString();
    }
}
