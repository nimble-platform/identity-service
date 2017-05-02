package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by Johannes Innerbichler on 25/04/17.
 * Repository for parties.
 */
@RepositoryRestResource(collectionResourceRel = "party", path = "party-hal")
public interface PartyRepository extends PagingAndSortingRepository<PartyType, Long> {
}
