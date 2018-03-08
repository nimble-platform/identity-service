package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

//@RepositoryRestResource(collectionResourceRel = "delivery-terms", path = "delivery-hal")
public interface DeliveryTermsRepository extends PagingAndSortingRepository<DeliveryTermsType, Long> {
}