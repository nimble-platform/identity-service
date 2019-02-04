package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Johannes Innerbichler on 27/06/17.
 */
//@RepositoryRestResource(collectionResourceRel = "payment-means", path = "payment-hal")
public interface PaymentMeansRepository extends PagingAndSortingRepository<PaymentMeansType, Long> {

    @Modifying
    @Transactional
    @Query(value = "delete from payment_means_type where payment_means_party_type_hjid = ?1", nativeQuery = true)
    void deleteByPartyID(Long partyID);

}