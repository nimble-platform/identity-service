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
    public Boolean setParty(PartyType party) {
        return false;
    }

    @Override
    public PartyType getParty(String uri) {
        return null;
    }

    @Override
    public Boolean deleteParty(PartyType party) {
        return false;
    }
}
