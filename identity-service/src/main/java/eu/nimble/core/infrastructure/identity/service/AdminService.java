package eu.nimble.core.infrastructure.identity.service;

import eu.nimble.core.infrastructure.identity.controller.ControllerUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.NegotiationSettingsRepository;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.QualifyingPartyRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin.INITIAL_REPRESENTATIVE_ROLE;
import static eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin.LEGAL_REPRESENTATIVE_ROLE;

/**
 * Created by Johannes Innerbichler on 04.12.18.
 */
@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;

    @Autowired
    private NegotiationSettingsRepository negotiationSettingsRepository;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private IdentityService identityService;

//    @Cacheable("unverifiedCompanies")
    public List<PartyType> queryUnverifiedCompanies() {
        List<PartyType> unverifiedCompanies = new ArrayList<>();
        Iterable<PartyType> allParties = partyRepository.findAll(new Sort(Sort.Direction.ASC, "name"));
        for (PartyType company : allParties) {

            // collect roles of company members
            Map<PersonType, List<String>> memberRoles = new HashMap<>();
            for (PersonType companyMember : company.getPerson()) {

                // collect roles
                try {
                    Optional<UaaUser> uaaUser = uaaUserRepository.findByUblPerson(companyMember).stream().findFirst();
                    if (uaaUser.isPresent()) {

                        // avoid invalid usernames (only emails are allowed)
                        if (uaaUser.get().getUsername().contains("@") == false)
                            continue;

                        logger.debug("Fetching roles of user {}", uaaUser.get().getUsername());
                        List<String> roles = new ArrayList<>(identityService.fetchRoles(uaaUser.get()));
                        companyMember.setRole(roles);
                        memberRoles.put(companyMember, roles);
                    }
                } catch (NotFoundException ex) {
                    // ignore
                } catch (Exception ex) {
                    logger.error("Error while fetch roles of user", ex);
                }
            }

            // check if unverified check whether at least on member has proper role
            Set<String> mergedRoles = memberRoles.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            if (mergedRoles.isEmpty() == false && mergedRoles.contains(LEGAL_REPRESENTATIVE_ROLE) == false) {
                UblUtils.removeBinaries(company);
                unverifiedCompanies.add(company);
                continue; // avoid multiple entries in list
            }
        }

        return unverifiedCompanies;
    }

    public boolean verifyCompany(Long companyId) throws Exception {
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        List<PersonType> companyMembers = company.getPerson();

        for (PersonType companyMember : companyMembers) {
            UaaUser uaaUser = uaaUserRepository.findByUblPerson(companyMember).stream().findFirst().orElseThrow(ControllerUtils.PersonNotFoundException::new);
            List<String> roles = new ArrayList<>(keycloakAdmin.getUserRoles(uaaUser.getExternalID()));
            if (roles.contains(INITIAL_REPRESENTATIVE_ROLE)) {
                keycloakAdmin.addRole(uaaUser.getExternalID(), LEGAL_REPRESENTATIVE_ROLE);

                // send email notification
                String email = companyMember.getContact().getElectronicMail();
                emailService.notifyVerifiedCompany(email, companyMember, company);

                return true;
            }
        }

        return false;
    }

    public long deleteCompany(Long companyId) throws ControllerUtils.CompanyNotFoundException {

        // query company
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        // delete associated company members
        for (PersonType member : company.getPerson()) {
            uaaUserRepository.deleteByUblPerson(member);
        }

        // delete associated qualifying party
        qualifyingPartyRepository.deleteByParty(company);

        // delete negotiation settings
        negotiationSettingsRepository.deleteByCompany(company);

        return partyRepository.deleteByHjid(companyId);
    }
}
