package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.stereotype.Component;

@Component
public class DelegateServiceClientFallback implements DelegateServiceClient {
    @Override
    public String getFederationId() {
        return null;
    }
}
