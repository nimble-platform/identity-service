package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by Johannes Innerbichler on 25/04/17.
 * Repository for parties.
 */
//@RepositoryRestResource(collectionResourceRel = "party", path = "party-hal")
public interface PartyRepository extends PagingAndSortingRepository<PartyType, Long>, JpaSpecificationExecutor<PartyType> {

    @Transactional
    Iterable<PartyType> findAll(Sort sort);

    @Transactional
    Iterable<PartyType> findAllByDeletedIsFalse(Sort sort);

    @Transactional
    Page<PartyType> findAll(Pageable pageable);

    List<PartyType> findByHjid(Long hijd);

    List<PartyType> findByPerson(PersonType person);

    @Modifying
    @Transactional
    long deleteByHjid(Long hijd);

    @Query(value = "select hjid from document_reference_type where document_reference_party_typ_0 = ?1 and document_type = ?2", nativeQuery = true)
    List<BigInteger> findDocumentIds(Long companyId, String documentType);

    @Query(value = "FROM PartyType WHERE stripeAccountId = ?1")
    List<PartyType> findByStripeAccountId(String accountId);

    @Query(value = "FROM PartyType WHERE productPublishSubscription is not null")
    List<PartyType> findAllWithSubscriptions();
}
