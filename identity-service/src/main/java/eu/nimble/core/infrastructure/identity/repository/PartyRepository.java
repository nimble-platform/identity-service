package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * Created by Johannes Innerbichler on 25/04/17.
 * Repository for parties.
 */
//@RepositoryRestResource(collectionResourceRel = "party", path = "party-hal")
public interface PartyRepository extends PagingAndSortingRepository<PartyType, Long> {

    List<PartyType> findByHjid(Long hijd);

    List<PartyType> findByPerson(PersonType person);
}
