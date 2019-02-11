package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

public interface DeliveryTermsRepository extends PagingAndSortingRepository<DeliveryTermsType, Long> {

    @Modifying
    @Transactional
    @Query(value = "delete from delivery_terms_type where delivery_terms_party_type_hj_0 = ?1", nativeQuery = true)
    void deleteByPartyID(Long partyID);
}