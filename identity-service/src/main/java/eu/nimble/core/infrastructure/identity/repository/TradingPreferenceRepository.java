package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.TradingPreferences;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TradingPreferenceRepository extends PagingAndSortingRepository<TradingPreferences, Long> {
}