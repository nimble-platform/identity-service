package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

//@RepositoryRestResource(collectionResourceRel = "person", path = "person-hal")
public interface PersonRepository extends PagingAndSortingRepository<PersonType, Long> {
    List<PersonType> findByHjid(Long hijd);
}
