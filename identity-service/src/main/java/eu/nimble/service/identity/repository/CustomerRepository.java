package eu.nimble.service.identity.repository;

import java.util.List;

import eu.nimble.service.identity.model.NimbleUser;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<NimbleUser, Long>{
    List<NimbleUser> findByLastName(String lastName);
}
