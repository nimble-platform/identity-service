package eu.nimble.core.infrastructure.identity.repository;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SuppressWarnings("unused")
public interface UserInvitationRepository extends PagingAndSortingRepository<UserInvitation, Long> {
    Page<UserInvitation> findAll(Pageable pageable);

    List<UserInvitation> findByEmail(String email);

    List<UserInvitation> findByCompanyId(String companyId);

    List<UserInvitation> findBySender(UaaUser sender);

    @Transactional
    List<UserInvitation> removeByEmail(String email);
}