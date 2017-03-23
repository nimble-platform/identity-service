package eu.nimble.service.identity.repository;

import eu.nimble.service.identity.model.NimbleUser;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "nimbleuser", path = "nimbleuser")
public interface UserRepository extends PagingAndSortingRepository<NimbleUser, Long> {
}
