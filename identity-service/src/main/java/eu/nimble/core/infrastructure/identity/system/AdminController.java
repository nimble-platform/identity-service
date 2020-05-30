package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.clients.CatalogueServiceClient;
import eu.nimble.core.infrastructure.identity.clients.IndexingClient;
import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
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
import eu.nimble.utility.LoggerUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private PersonRepository personRepository;
    @Autowired
    private UaaUserRepository uaaUserRepository;
    @Autowired
    private IdentityService identityService;

    @Autowired
    private IndexingClientController indexingController;

    @Autowired
    private CatalogueServiceClient catalogueServiceClient;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private RocketChatService chatService;

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
            eu.nimble.service.model.solr.party.PartyType newParty = DataModelUtils.toIndexParty(company);
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
            return ResponseEntity.ok().build();
        }else{
            return new ResponseEntity<>("Only company_admin, external_representative, "
                    + "initial_representative, legal_representative of company are allowed to delete companies",
                    HttpStatus.UNAUTHORIZED);
        }


    }

    @ApiOperation(value = "Delete user")
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

    private ResponseEntity<Page<PartyType>> makePage(@RequestParam(value = "page", required = false, defaultValue = "1") int pageNumber, @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize, List<PartyType> unverifiedCompanies) {
        int start = (pageNumber - 1) * pageSize;
        int end = (start + pageSize) > unverifiedCompanies.size() ? unverifiedCompanies.size() : (start + pageSize);
        Page<PartyType> companyPage = new PageImpl<>(unverifiedCompanies.subList(start, end), new PageRequest(pageNumber - 1, pageSize), unverifiedCompanies.size());

        return ResponseEntity.ok(companyPage);
    }
}
