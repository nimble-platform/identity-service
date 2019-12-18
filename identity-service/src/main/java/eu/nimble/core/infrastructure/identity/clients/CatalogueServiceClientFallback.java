package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.stereotype.Component;

@Component
public class CatalogueServiceClientFallback implements CatalogueServiceClient {
    @Override
    public void indexAllCatalogues(String partyId, String bearerToken) {
        return;
    }
}
