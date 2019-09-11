package eu.nimble.core.infrastructure.identity.service;

import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.system.ControllerUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
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
    private PaymentMeansRepository paymentMeansRepository;

    @Autowired
    private DeliveryTermsRepository deliveryTermsRepository;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private PersonRepository personRepository;


    @Autowired
    private IdentityService identityService;

    //    @Cacheable("unverifiedCompanies")
    public List<PartyType> queryCompanies(CompanyState companyState) {
        List<PartyType> resultingCompanies = new ArrayList<>();
        Iterable<PartyType> allParties = partyRepository.findAll(new Sort(Sort.Direction.ASC, "hjid"));
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
            if (mergedRoles.isEmpty() == false && mergedRoles.contains(LEGAL_REPRESENTATIVE_ROLE) == false && companyState.equals(CompanyState.UNVERIFIED)) {
                resultingCompanies.add(company);
                continue; // avoid multiple entries in list
            } else if (mergedRoles.contains(LEGAL_REPRESENTATIVE_ROLE) && companyState.equals(CompanyState.VERIFIED)) {
                resultingCompanies.add(company);
                continue; // avoid multiple entries in list
            }
        }

        return resultingCompanies;
    }

    public boolean verifyCompany(Long companyId) {
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

    public List<PartyType> sortCompanies(List<PartyType> partyList, String sortBy, String orderBy) {
        Comparator<PartyType> p;

        if (sortBy.equals(GlobalConstants.PARTY_NAME_STRING)) {
            if(GlobalConstants.DESCENDING_STRING.equals(orderBy)){
                p = (p1, p2) ->
                        p2.getPartyName().get(0).getName().getValue().toLowerCase().compareTo(p1.getPartyName().get(0).getName().getValue().toLowerCase());
            }else {
                p = (p1, p2) ->
                        p1.getPartyName().get(0).getName().getValue().toLowerCase().compareTo(p2.getPartyName().get(0).getName().getValue().toLowerCase());
            }
            partyList.sort(p);
        }
        return partyList;
    }

    public boolean deleteCompany(Long companyId) throws ControllerUtils.CompanyNotFoundException {

        // query company
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        // delete associated company members
        for (PersonType member : company.getPerson()) {
            Long memberHjid = member.getHjid();
            deletePerson(memberHjid);
        }

        //set deleted flag fr the party
        company.setDeleted(true);

        //update the party
        partyRepository.save(company);

//        // delete negotiation settings
//        negotiationSettingsRepository.deleteByCompany(company);
//
//        // delete trading preferences
//        if (company.getPurchaseTerms() != null) {
//            deliveryTermsRepository.delete(company.getPurchaseTerms().getDeliveryTerms());
//            paymentMeansRepository.delete(company.getPurchaseTerms().getPaymentMeans());
//        }
//        if (company.getSalesTerms() != null) {
//            deliveryTermsRepository.delete(company.getSalesTerms().getDeliveryTerms());
//            paymentMeansRepository.delete(company.getSalesTerms().getPaymentMeans());
//        }
//
//        try {
//            // delete for legacy schema
//            deliveryTermsRepository.deleteByPartyID(companyId);
//            paymentMeansRepository.deleteByPartyID(companyId);
//        } catch (InvalidDataAccessResourceUsageException ex) {
//            // ignored
//        }
//
//        return partyRepository.deleteByHjid(companyId);

        return true;
    }


    public boolean deletePerson(Long personHjid) throws ControllerUtils.CompanyNotFoundException {
        // query person
        PersonType person = personRepository.findByHjid(personHjid).stream().findFirst().orElseThrow(ControllerUtils.PersonNotFoundException::new);

        //set delete flag for the person
        person.setDeleted(true);

        //save deleted person
        personRepository.save(person);

        return true;
    }

    public enum CompanyState {
        VERIFIED, UNVERIFIED
    }
}
