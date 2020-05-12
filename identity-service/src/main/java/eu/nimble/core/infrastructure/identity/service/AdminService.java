package eu.nimble.core.infrastructure.identity.service;

import eu.nimble.core.infrastructure.identity.clients.IndexingClient;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.system.ControllerUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import eu.nimble.utility.persistence.binary.BinaryContentService;
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
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private PaymentMeansRepository paymentMeansRepository;

    @Autowired
    private DeliveryTermsRepository deliveryTermsRepository;

    @Autowired
    private DocumentReferenceRepository documentReferenceRepository;
    @Autowired
    private CertificateRepository certificateRepository;
    private BinaryContentService binaryContentService = new BinaryContentService();
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

    @Autowired
    private IndexingClient indexingClient;

    //    @Cacheable("unverifiedCompanies")
    public List<PartyType> queryCompanies(CompanyState companyState) {
        List<PartyType> resultingCompanies = new ArrayList<>();
        Iterable<PartyType> allParties = partyRepository.findAllByDeletedIsFalse(new Sort(Sort.Direction.ASC, "hjid"));
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

    public boolean verifyCompany(Long companyId, String bearer) {
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

                //indexing the verified status of the company
                eu.nimble.service.model.solr.party.PartyType party =  indexingClient.getParty(company.getHjid().toString(),bearer);
                party.setVerified(true);
                indexingClient.setParty(party,bearer);
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

    public boolean revertCompany(Long companyId, String bearer) throws Exception {

        // query company
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == true){
            // revert associated company members
            for (PersonType member : company.getPerson()) {
                Long memberHjid = member.getHjid();
                revertPerson(memberHjid,bearer,true);
            }

            //set deleted flag fr the party
            company.setDeleted(false);

            //update the party
            partyRepository.save(company);
            //update the index by removing the company
            indexingClient.deleteParty(company.getHjid().toString(), bearer);
            return true;
        }

        else {
            return false;
        }

    }

    public boolean deleteCompany(Long companyId, String bearer, Long userId) throws Exception {

        // query company
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        boolean isUserInCompany = false;

        for (PersonType member : company.getPerson()) {
            Long memberHjid = member.getHjid();
            if(memberHjid.equals(userId)){
                isUserInCompany = true;
                break;
            }
        }

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == true){
            isUserInCompany = true;
        }

        if(isUserInCompany){
            // delete associated company members
            for (PersonType member : company.getPerson()) {
                Long memberHjid = member.getHjid();
                deletePerson(memberHjid,bearer,true);
            }

            //set deleted flag fr the party
            company.setDeleted(true);

            //update the party
            partyRepository.save(company);

            //update the index by removing the company
            indexingClient.deleteParty(company.getHjid().toString(), bearer);
            return true;
        }else {
            return false;
        }

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

    }

    // this method deletes the party represented by the given id
    public void deleteCompanyPermanently(Long companyId) {
        // query company
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        // retrieve qualifying party
        QualifyingPartyType qualifyingParty = qualifyingPartyRepository.findByParty(company).stream().findFirst().get();
        // retrieve negotiation settings
        NegotiationSettings negotiationSettings = negotiationSettingsRepository.findOneByCompany(company);
        if(negotiationSettings != null){
            // delete negotiation settings
            negotiationSettingsRepository.delete(negotiationSettings);
        }

        // delete qualifying party
        qualifyingPartyRepository.delete(qualifyingParty);

        // delete trading preferences
        if (company.getPurchaseTerms() != null) {
            deliveryTermsRepository.delete(company.getPurchaseTerms().getDeliveryTerms());
            paymentMeansRepository.delete(company.getPurchaseTerms().getPaymentMeans());
        }
        if (company.getSalesTerms() != null) {
            deliveryTermsRepository.delete(company.getSalesTerms().getDeliveryTerms());
            paymentMeansRepository.delete(company.getSalesTerms().getPaymentMeans());
        }

        // delete document references
        if(company.getDocumentReference() != null){
            for (DocumentReferenceType documentReference : company.getDocumentReference()) {
                if(documentReference.getAttachment() != null && documentReference.getAttachment().getEmbeddedDocumentBinaryObject() != null && documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri() != null){
                    String uri = documentReference.getAttachment().getEmbeddedDocumentBinaryObject().getUri();
                    binaryContentService.deleteContentIdentity(uri);
                }

                documentReferenceRepository.delete(documentReference);
            }
        }

        // delete company certificates
        if(company.getCertificate() != null){
            for (CertificateType certificate : company.getCertificate()) {

                if(certificate.getDocumentReference() != null && certificate.getDocumentReference().get(0).getAttachment() != null && certificate.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject() != null && certificate.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri() != null){
                    String uri = certificate.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getUri();
                    binaryContentService.deleteContentIdentity(uri);
                }

                certificateRepository.delete(certificate);
            }

        }

        // delete party
        partyRepository.delete(company);
    }

    public boolean deletePerson(Long personHjid, String bearer , boolean isCompanyDelete) throws Exception {

        OpenIdConnectUserDetails oidUser = identityService.getUserDetails(bearer);
        String keyCloackuid = oidUser.getUserId();

        // query person
        PersonType person = personRepository.findByHjid(personHjid).stream().findFirst().orElseThrow(ControllerUtils.PersonNotFoundException::new);
        List<UaaUser> potentialUser = uaaUserRepository.findByUblPerson(person);
        UaaUser uaaUser = potentialUser.stream().findFirst().orElseThrow(() -> new Exception("Invalid user mapping"));
        String keyCloakId = uaaUser.getExternalID();

        if(keyCloakId.equals(keyCloackuid) || identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == true
                || isCompanyDelete){
            //set delete flag for the person
            person.setDeleted(true);
            keycloakAdmin.addRole(keyCloakId, KeycloakAdmin.NIMBLE_DELETED_USER);
            //save deleted person
            personRepository.save(person);
            return true;
        }else{
            return false;
        }
    }

    public boolean revertPerson(Long personHjid, String bearer , boolean isCompanyReverted) throws Exception {

        OpenIdConnectUserDetails oidUser = identityService.getUserDetails(bearer);
        String keyCloackuid = oidUser.getUserId();

        // query person
        PersonType person = personRepository.findByHjid(personHjid).stream().findFirst().orElseThrow(ControllerUtils.PersonNotFoundException::new);
        List<UaaUser> potentialUser = uaaUserRepository.findByUblPerson(person);
        UaaUser uaaUser = potentialUser.stream().findFirst().orElseThrow(() -> new Exception("Invalid user mapping"));
        String keyCloakId = uaaUser.getExternalID();

        if(keyCloakId.equals(keyCloackuid) || identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == true
                || isCompanyReverted){
            //set delete flag for the person
            person.setDeleted(false);
            keycloakAdmin.removeRole(keyCloakId, KeycloakAdmin.NIMBLE_DELETED_USER);
            //save person
            personRepository.save(person);
            return true;
        }else{
            return false;
        }
    }

    public enum CompanyState {
        VERIFIED, UNVERIFIED
    }
}
