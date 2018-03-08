package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by Johannes Innerbichler on 27/06/17.
 */
//@RepositoryRestResource(collectionResourceRel = "payment-means", path = "payment-hal")
public interface PaymentMeansRepository extends PagingAndSortingRepository<PaymentMeansType, Long> {
}