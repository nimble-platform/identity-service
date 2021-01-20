package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.clients.BusinessProcessServiceClient;
import eu.nimble.core.infrastructure.identity.clients.CatalogueServiceClient;
import eu.nimble.core.infrastructure.identity.clients.IndexingClient;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.entity.NegotiationSettings;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.messaging.KafkaSender;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.service.AdminService;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.service.RocketChatService;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.utils.DataModelUtils;
import eu.nimble.core.infrastructure.identity.clients.IndexingClientController;
import eu.nimble.core.infrastructure.identity.utils.LogEvent;
import eu.nimble.service.model.solr.Search;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.QualifyingPartyType;
import eu.nimble.utility.ExecutionContext;
import eu.nimble.utility.LoggerUtils;
import eu.nimble.utility.bp.BusinessWorkflowUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.PLATFORM_MANAGER;

/**
 * Created by Johannes Innerbichler on 12.09.18.
 */
@Controller
@RequestMapping(path = "/admin")
@Api(value = "Admin API", description = "Administration services for managing identity on the platform.")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private static final String DEFAULT_PAGE_SIZE = "10";

    @Autowired
    private AdminService adminService;

    @Autowired
    private KeycloakAdmin keycloakAdmin;
    @Autowired
    private CompanySettingsController companySettingsController;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private UaaUserRepository uaaUserRepository;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private BusinessProcessServiceClient businessProcessServiceClient;

    @Autowired
    private IndexingClientController indexingController;

    @Autowired
    private CatalogueServiceClient catalogueServiceClient;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private QualifyingPartyRepository qualifyingPartyRepository;
    @Autowired
    private RocketChatService chatService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NegotiationSettingsRepository negotiationSettingsRepository;

    @ApiOperation(value = "Retrieve unverified companies", response = Page.class)
    @RequestMapping(value = "/unverified_companies", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Page<PartyType>> getUnverifiedCompanies(@RequestParam(value = "page", required = false, defaultValue = "1") int pageNumber,
                                                           @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                           @RequestParam(value = "sortBy", required = false, defaultValue = GlobalConstants.PARTY_NAME_STRING) String sortBy,
                                                           @RequestParam(value = "orderBy", required = false, defaultValue = GlobalConstants.ASCENDING_STRING) String orderBy){

        // ToDo: verify proper access policy (e.g. admin role)

        logger.info("Fetching unverified companies");
        List<PartyType> unverifiedCompanies = adminService.queryCompanies(AdminService.CompanyState.UNVERIFIED);

        if(!orderBy.isEmpty())
            adminService.sortCompanies(unverifiedCompanies, sortBy, orderBy);


        // paginate results
        return makePage(pageNumber, pageSize, unverifiedCompanies);
    }

    @ApiOperation(value = "Retrieve verified companies", response = Page.class)
    @RequestMapping(value = "/verified_companies", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Page<PartyType>> getVerifiedCompanies(@RequestParam(value = "page", required = false, defaultValue = "1") int pageNumber,
                                                         @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                         @RequestParam(value = "sortBy", required = false, defaultValue = GlobalConstants.PARTY_NAME_STRING) String sortBy,
                                                         @RequestParam(value = "orderBy", required = false, defaultValue = GlobalConstants.ASCENDING_STRING) String orderBy){
        // ToDo: verify proper access policy (e.g. admin role)

        logger.info("Fetching unverified companies");
        List<PartyType> verifiedCompanies = adminService.queryCompanies(AdminService.CompanyState.VERIFIED);

        if(!orderBy.isEmpty())
            adminService.sortCompanies(verifiedCompanies, sortBy, orderBy);
        // paginate results
        return makePage(pageNumber, pageSize, verifiedCompanies);
    }

    @ApiOperation(value = "Verify company")
    @RequestMapping(value = "/verify_company", method = RequestMethod.POST)
    ResponseEntity<?> verifyCompany(@RequestParam(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only legal platform managers are allowed to verify companies", HttpStatus.UNAUTHORIZED);

        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.VERIFY_COMPANY.getActivity());
        paramMap.put("companyId", String.valueOf(companyId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Verifying company with id {}", companyId);
        adminService.verifyCompany(companyId, bearer);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Revert a deleted company back")
    @RequestMapping(value = "/revert_company/{companyId}", method = RequestMethod.POST)
    ResponseEntity<?> revertCompany(@PathVariable(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER,OAuthClient.Role.COMPANY_ADMIN,
                OAuthClient.Role.INITIAL_REPRESENTATIVE,OAuthClient.Role.LEGAL_REPRESENTATIVE,OAuthClient.Role.EXTERNAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only platform managers,company_admin, external_representative, "
                    + "initial_representative, legal_representative are allowed to revert companies back", HttpStatus.UNAUTHORIZED);
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.REVERT_COMPANY.getActivity());
        paramMap.put("companyId", String.valueOf(companyId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Reverting company with id {}", companyId);
        boolean isCompanyReverted = adminService.revertCompany(companyId,bearer);
        if(isCompanyReverted){
            //index catalogues
            catalogueServiceClient.indexAllCatalogues(Long.toString(companyId),bearer);

            //index party
            PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
            // retrieve qualifying party
            QualifyingPartyType qualifyingParty = qualifyingPartyRepository.findByParty(company).stream().findFirst().get();
            eu.nimble.service.model.solr.party.PartyType newParty = DataModelUtils.toIndexParty(company,qualifyingParty);
            // the deleted companies are the ones which are already verified previously
            newParty.setVerified(true);
            List<IndexingClient> indexingClients = indexingController.getClients();
            for (IndexingClient indexingClient : indexingClients) {
                indexingClient.setParty(newParty, bearer);
            }
            return ResponseEntity.ok().build();
        }else{
            return new ResponseEntity<>("Only company_admin, external_representative, "
                    + "initial_representative, legal_representative of company are allowed to revert companies back",
                    HttpStatus.UNAUTHORIZED);
        }


    }

    @ApiOperation(value = "Reject company")
    @RequestMapping(value = "/reject_company/{companyId}", method = RequestMethod.DELETE)
    ResponseEntity<?> rejectCompany(@PathVariable(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER,OAuthClient.Role.COMPANY_ADMIN,
                OAuthClient.Role.INITIAL_REPRESENTATIVE,OAuthClient.Role.LEGAL_REPRESENTATIVE,OAuthClient.Role.EXTERNAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only platform managers,company_admin, external_representative, "
                    + "initial_representative, legal_representative are allowed to reject companies", HttpStatus.UNAUTHORIZED);
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.REJECT_COMPANY.getActivity());
        paramMap.put("companyId", String.valueOf(companyId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Rejecting company with id {}", companyId);

        // retrieve party
        PartyType company = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
        // some companies might not have any associated person
        if(company.getPerson() != null && !company.getPerson().isEmpty()){
            // retrieve person
            PersonType person = company.getPerson().get(0);
            String emailAddress = person.getContact().getElectronicMail();
            // retrieve uaa user and delete the user from keycloak
            try{
                // retrieve uaa user
                UaaUser uaaUser = uaaUserRepository.findByUblPerson(person).stream().findFirst().orElseThrow(ControllerUtils.PersonNotFoundException::new);
                // delete the user from keycloak
                keycloakAdmin.deleteUser(uaaUser.getExternalID());
            }catch (ControllerUtils.PersonNotFoundException exception){
                logger.error("No UaaUser is found for person with id: {}",person.getID(),exception);
            }
            // delete the user from UaaUser
            uaaUserRepository.deleteByUblPerson(person);
            // delete person
            personRepository.delete(person);
            // remove the user from RocketChat if enabled
            if(chatService.isChatEnabled()){
                chatService.deleteUser(emailAddress);
            }
        }
        // delete company permanently
        adminService.deleteCompanyPermanently(companyId);
        // remove party from the solr indexes
        for (IndexingClient indexingClient : indexingController.getClients()) {
            indexingClient.deleteParty(String.valueOf(companyId), bearer);
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Delete company")
    @RequestMapping(value = "/delete_company/{companyId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteCompany(@PathVariable(value = "companyId") long companyId,
                                    @RequestParam(value = "userId") long userId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER,OAuthClient.Role.COMPANY_ADMIN,
                OAuthClient.Role.INITIAL_REPRESENTATIVE,OAuthClient.Role.LEGAL_REPRESENTATIVE,OAuthClient.Role.EXTERNAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only platform managers,company_admin, external_representative, "
                    + "initial_representative, legal_representative are allowed to delete companies", HttpStatus.UNAUTHORIZED);
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.DELETE_COMPANY.getActivity());
        paramMap.put("companyId", String.valueOf(companyId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Deleting company with id {}", companyId);
        boolean isCompanyDeleted = adminService.deleteCompany(companyId,bearer,userId);
        if(isCompanyDeleted){
                    //find items idexed by the manufaturer
                    eu.nimble.service.model.solr.Search search = new Search();
                    search.setQuery("manufacturerId:"+companyId);
            eu.nimble.service.model.solr.SearchResult sr = indexingController.getNimbleIndexClient().searchItem(search, bearer);

                    List<Object> result  = sr.getResult();
                    Set<String> catIds = new HashSet<String>();
                    for(Object ob : result){
                        LinkedHashMap<String,String> lmap = (LinkedHashMap<String, String>) ob;
                        String catLineId = lmap.get("uri");
                        String catalogueId = lmap.get("catalogueId");
                        if(catalogueId != null){
                            catIds.add(catalogueId);
                        }
                        //remove items from indexing
                        for (IndexingClient indexingClient : indexingController.getClients()) {
                            indexingClient.removeItem(catLineId, bearer);
                        }
                    }

                    Iterator iterate = catIds.iterator();

                    while (iterate.hasNext()){
                        //remove catalogue from the index
                        for (IndexingClient indexingClient : indexingController.getClients()) {
                            indexingClient.deleteCatalogue(iterate.next().toString(), bearer);
                        }
                    }
            //delete party from the indexes
            for (IndexingClient indexingClient : indexingController.getClients()) {
                indexingClient.deleteParty(String.valueOf(companyId), bearer);
            }

            // send email to Legal Representatives of the company
            PartyType party = partyRepository.findByHjid(companyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);
            // enrich persons with roles
            identityService.enrichWithRoles(party);
            List<PersonType> legalRepresentatives = party.getPerson().stream().filter(personType -> personType.getRole().contains(OAuthClient.Role.LEGAL_REPRESENTATIVE.toString())).collect(Collectors.toList());
            emailService.notifyDeletedCompany(legalRepresentatives,party,executionContext.getLanguageId());

            return ResponseEntity.ok().build();
        }else{
            return new ResponseEntity<>("Only platform managers or company members are allowed to delete the company.",
                    HttpStatus.UNAUTHORIZED);
        }


    }

    @ApiOperation(value = "Delete user (marks user as deleted)")
    @RequestMapping(value = "/delete_user/{userId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteUser(@PathVariable(value = "userId") long userId,
            @RequestHeader(value = "Authorization") String bearer) throws Exception {

        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.DELETE_USER.getActivity());
        paramMap.put("userId", String.valueOf(userId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Deleting user with id {}", userId);
        boolean status = adminService.deletePerson(userId,bearer,false);

        if(status){
            return ResponseEntity.ok().build();

        }else{
            return new ResponseEntity<>("Only platform managers and users by them are allowed to delete users", HttpStatus.UNAUTHORIZED);
        }

    }

    @ApiOperation(value = "Delete user from the platform permanently")
    @RequestMapping(value = "/user", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteUserPermanently(@RequestParam(value = "username") String username,
                                 @RequestHeader(value = "Authorization") String bearer) throws Exception {
        // validate role
        if (!identityService.hasAnyRole(bearer, PLATFORM_MANAGER))
            return new ResponseEntity<>("Only platform managers are allowed to delete a user from the platform permanently", HttpStatus.FORBIDDEN);
        // delete the user from keycloak
        keycloakAdmin.deleteUserByUsername(username);
        // retrieve uaa user
        UaaUser uaaUser = uaaUserRepository.findOneByUsername(username);
        if(uaaUser != null){
            // retrieve person
            PersonType person = uaaUser.getUBLPerson();
            // delete the user from UaaUser
            uaaUserRepository.deleteByUblPerson(person);
            // delete person
            personRepository.delete(person);
            // delete user invitations
            List<UserInvitation> userInvitations = userInvitationRepository.findByEmail(person.getContact().getElectronicMail());
            userInvitations.forEach(userInvitation -> userInvitationRepository.delete(userInvitation));
            // remove the user from RocketChat if enabled
            if(chatService.isChatEnabled()){
                chatService.deleteUser(person.getContact().getElectronicMail());
            }
        }
        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "",notes = "Updates the business workflow of companies.The business workflow of companies which have unfinished collaborations can not be updated." +
            "Therefore, the service returns the identifiers of companies which need to finish their collaborations.")
    @RequestMapping(value = "/business-workflow", method = RequestMethod.PUT)
    ResponseEntity<?> updateCompanyBusinessWorkflow(@ApiParam(value = "List of business process ids.<br>Example:[\"Negotiation\",\"Order\"]", required = true) @RequestBody List<String> workflow,
                                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        // role check
        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only platform managers are allowed to update companies' business workflow", HttpStatus.UNAUTHORIZED);

        logger.info("Incoming request to update companies' business workflow to {}",workflow);

        // company ids with unfinished collaborations
        List<String> companyIdsWithUnfinishedCollaborations = new ArrayList<>();
        // check whether the workflow is valid or not
        BusinessWorkflowUtil.validateBusinessWorkflow(workflow);
        // get all parties
        List<PartyType> partyTypes = (List<PartyType>) partyRepository.findAll();
        // update the negotiation settings for each party
        try {
            for (PartyType partyType : partyTypes) {
                // check whether the party has any unfinished collaborations
                String isAllCollaborationsFinished = businessProcessServiceClient.checkAllCollaborationsFinished(partyType.getPartyIdentification().get(0).getID(),partyType.getFederationInstanceID(),bearer);
                if(isAllCollaborationsFinished.contentEquals("true")){
                    // get negotiation settings for the party
                    NegotiationSettings negotiationSetting = negotiationSettingsRepository.findOneByCompany(partyType);
                    if(negotiationSetting != null){
                        // set process ids for the company
                        negotiationSetting.getCompany().getProcessIDItems().clear();
                        negotiationSetting.getCompany().setProcessID(workflow);
                        // update negotiation settings
                        negotiationSettingsRepository.save(negotiationSetting);

                        // when the available process id list is updated for the company,
                        // we need to recalculate the company rating since the available sub-ratings depend on the selected process ids
                        // broadcast the change on ratings
                        kafkaSender.broadcastRatingsUpdate(partyType.getPartyIdentification().get(0).getID(),bearer);
                    }
                } else{
                    companyIdsWithUnfinishedCollaborations.add(partyType.getPartyIdentification().get(0).getID());
                }
            }
        }catch (Exception e){
            String msg = String.format("Unexpected error while updating companies' business workflow to %s",workflow);
            logger.error(msg,e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(msg);
        }

        logger.info("Completed the request to update companies' business workflow to {}",workflow);
        if(companyIdsWithUnfinishedCollaborations.size() > 0){
            // returns the party ids which need to finish their collaboration
            String msg = String.format("The following companies have unfinished collaborations:%s",companyIdsWithUnfinishedCollaborations);
            logger.info(msg);
            return ResponseEntity.ok().body(msg);
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Page<PartyType>> makePage(@RequestParam(value = "page", required = false, defaultValue = "1") int pageNumber, @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize, List<PartyType> unverifiedCompanies) {
        int start = (pageNumber - 1) * pageSize;
        int end = (start + pageSize) > unverifiedCompanies.size() ? unverifiedCompanies.size() : (start + pageSize);
        Page<PartyType> companyPage = new PageImpl<>(unverifiedCompanies.subList(start, end), new PageRequest(pageNumber - 1, pageSize), unverifiedCompanies.size());

        return ResponseEntity.ok(companyPage);
    }
}
