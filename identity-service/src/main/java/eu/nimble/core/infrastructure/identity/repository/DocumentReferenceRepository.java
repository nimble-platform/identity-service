package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.service.model.ubl.commonaggregatecomponents.DocumentReferenceType;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by Johannes Innerbichler on 16.10.18.
 */
public interface DocumentReferenceRepository extends PagingAndSortingRepository<DocumentReferenceType, Long> {
}