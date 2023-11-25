package com.camonoxe.Model;

import java.util.UUID;

import com.camonoxe.Model.UserTable.User;

public interface UsersChangedDel {
    public void addUserDel(User user);
    public void remUserDel(User user);
}
