package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface CertificateRepository extends PagingAndSortingRepository<CertificateType, Long> {
}