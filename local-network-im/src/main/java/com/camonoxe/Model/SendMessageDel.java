package com.camonoxe.Model;

import java.util.UUID;

public interface SendMessageDel {
    public void MessageHandler(UUID recipientId, String message);
}
