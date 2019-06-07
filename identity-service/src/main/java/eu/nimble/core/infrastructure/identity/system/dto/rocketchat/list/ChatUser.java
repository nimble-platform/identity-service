package eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list;

import java.util.ArrayList;

public class ChatUser {

    private String username;
    private ArrayList<UserEmail> emails;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ArrayList<UserEmail> getEmails() {
        return emails;
    }

    public void setEmails(ArrayList<UserEmail> emails) {
        this.emails = emails;
    }
}
