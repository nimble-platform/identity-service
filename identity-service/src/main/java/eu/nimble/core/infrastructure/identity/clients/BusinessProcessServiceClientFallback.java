package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.stereotype.Component;

@Component
public class BusinessProcessServiceClientFallback implements BusinessProcessServiceClient {
    @Override
    public String checkAllCollaborationsFinished(String partyId, String federationId, String bearerToken) {
        return null;
    }
}
