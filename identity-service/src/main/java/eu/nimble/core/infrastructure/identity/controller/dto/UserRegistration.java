package eu.nimble.core.infrastructure.identity.controller.dto;

import eu.nimble.core.infrastructure.identity.entity.dto.Credentials;
import eu.nimble.core.infrastructure.identity.entity.dto.FrontEndUser;

@SuppressWarnings("unused")
public class UserRegistration {

    private FrontEndUser user = null;
    private Credentials credentials = null;

    public FrontEndUser getUser() {
        return user;
    }

    public void setUser(FrontEndUser user) {
        this.user = user;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public String toString() {
        return "UserRegistation{" + "user=" + user + ", credentials=" + credentials + '}';
    }
}
