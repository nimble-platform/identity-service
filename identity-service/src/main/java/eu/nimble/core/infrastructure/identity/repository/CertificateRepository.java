package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CertificateRepository extends PagingAndSortingRepository<CertificateType, Long> {
}