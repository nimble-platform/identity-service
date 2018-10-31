package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface QualifyingPartyRepository extends PagingAndSortingRepository<QualifyingPartyType, Long> {

    List<QualifyingPartyType> findByHjid(Long hijd);

    List<QualifyingPartyType> findByParty(PartyType party);
}

