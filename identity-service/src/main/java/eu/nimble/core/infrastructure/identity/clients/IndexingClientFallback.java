package eu.nimble.core.infrastructure.identity.clients;

import eu.nimble.service.model.solr.Search;
import eu.nimble.service.model.solr.SearchResult;
import eu.nimble.service.model.solr.party.PartyType;
import feign.Response;
import org.springframework.stereotype.Component;

/**
 *
 * @author Dileepa Jayakody
 */
@Component
public class IndexingClientFallback implements IndexingClient {

    @Override
    public Response setParty(PartyType party, String bearerToken) {
        return null;
    }

    @Override
    public PartyType getParty(String uri,String bearerToken) {
        return null;
    }

    @Override
    public Boolean deleteParty(String uri,String bearerToken) {
        return false;
    }

    @Override
    public SearchResult searchItem( Search search,String bearerToken) {
        return null;
    }

    @Override
    public Boolean removeItem(String uri,String bearerToken) {
        return false;
    }

    @Override
    public Boolean deleteCatalogue(String uri,String bearerToken) {
        return false;
    }
}
