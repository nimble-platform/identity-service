package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by Johannes Innerbichler on 02.08.18.
 */

public interface NegotiationSettingsRepository extends PagingAndSortingRepository<NegotiationSettings, Long> {

    NegotiationSettings findOneByCompany(PartyType company);

    List<NegotiationSettings> findByCompany(PartyType company);

    @Modifying
    @Transactional
    long deleteByCompany(PartyType party);

}