package com.camonoxe.Model;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Message implements Serializable {
    private UUID senderUUID;
    private String message;
    private Date timestamp;

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy h:mm:ss a");

    public Message(UUID senderUUID, String message, Date timestamp)
    {
        this.senderUUID = senderUUID;
        this.message = message;
        this.timestamp = timestamp;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }
    
    public void setSenderUUID(UUID senderUUID) {
        this.senderUUID = senderUUID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString()
    {
        return String.format("%s\n%s\n%s\n", dateFormat.format(timestamp), UserTable.getUsernameByUserId(senderUUID), message);
    }

}
