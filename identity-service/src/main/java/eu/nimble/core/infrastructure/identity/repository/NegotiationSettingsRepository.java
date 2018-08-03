package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by Johannes Innerbichler on 02.08.18.
 */

public interface NegotiationSettingsRepository extends PagingAndSortingRepository<NegotiationSettings, Long> {

    NegotiationSettings findOneByCompany(PartyType company);
}