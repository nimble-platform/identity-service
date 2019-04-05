package eu.nimble.core.infrastructure.identity.entity.dto;

/**
 * Created by Nirojan Selvanathan on 04/04/19.
 */
public class ResetCredentials {

    private String username = null;

    private String key = null;

    private String newPassword = null;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "Credentials{" + "username='" + username + '\'' + ", password='*******" + '}';
    }
}
