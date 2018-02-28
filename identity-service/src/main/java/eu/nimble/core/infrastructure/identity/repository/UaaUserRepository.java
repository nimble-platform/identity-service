package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * Created by Johannes Innerbichler on 29/05/17.
 * Repository for Uaa Users.
 */
//@RepositoryRestResource(collectionResourceRel = "uaa-user", path = "uaa-hal")
public interface UaaUserRepository extends PagingAndSortingRepository<UaaUser, Long> {
    Page<UaaUser> findAll(Pageable pageable);

    List<UaaUser> findByUsername(String username);

    UaaUser findByExternalID(String externalId);

    List<UaaUser> findByUblPerson(PersonType ublPerson);
}
