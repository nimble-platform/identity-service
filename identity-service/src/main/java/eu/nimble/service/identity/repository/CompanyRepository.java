package eu.nimble.service.identity.repository;

import eu.nimble.service.identity.model.Company;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by jinnerbi on 22/03/17.
 */
public interface CompanyRepository extends CrudRepository<Company, Long> {
}