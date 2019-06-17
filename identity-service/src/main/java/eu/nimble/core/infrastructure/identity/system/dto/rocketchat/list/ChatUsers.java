package eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list;

import java.util.ArrayList;

public class ChatUsers {

    private ArrayList<ChatUser> users;

    public ArrayList<ChatUser> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<ChatUser> users) {
        this.users = users;
    }
}
