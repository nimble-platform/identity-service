package eu.nimble.core.infrastructure.identity.clients;

import eu.nimble.service.model.solr.party.PartyType;
import org.springframework.stereotype.Component;

/**
 *
 * @author Dileepa Jayakody
 */
@Component
public class IndexingClientFallback implements IndexingClient {

    @Override
    public Boolean setParty(PartyType party,String bearerToken) {
        return false;
    }

    @Override
    public PartyType getParty(String uri,String bearerToken) {
        return null;
    }

    @Override
    public Boolean deleteParty(PartyType party,String bearerToken) {
        return false;
    }
}
