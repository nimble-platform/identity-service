package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.constants.GlobalConstants;
import eu.nimble.core.infrastructure.identity.service.AdminService;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.utils.LogEvent;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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
import java.util.List;
import java.util.Map;

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
    private IdentityService identityService;

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
        adminService.verifyCompany(companyId);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Delete company")
    @RequestMapping(value = "/delete_company/{companyId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteCompany(@PathVariable(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only platform managers are allowed to delete companies", HttpStatus.UNAUTHORIZED);
        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.DELETE_COMPANY.getActivity());
        paramMap.put("companyId", String.valueOf(companyId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Deleting company with id {}", companyId);
        adminService.deleteCompany(companyId);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Delete user")
    @RequestMapping(value = "/delete_user/{userId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteUser(@PathVariable(value = "userId") long userId,
            @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasAnyRole(bearer, OAuthClient.Role.PLATFORM_MANAGER , OAuthClient.Role.COMPANY_ADMIN) == false)
            return new ResponseEntity<>("Only platform managers and company admins are allowed to delete users", HttpStatus.UNAUTHORIZED);

        Map<String,String> paramMap = new HashMap<String, String>();
        paramMap.put("activity", LogEvent.DELETE_USER.getActivity());
        paramMap.put("userId", String.valueOf(userId));
        LoggerUtils.logWithMDC(logger, paramMap, LoggerUtils.LogLevel.INFO, "Deleting user with id {}", userId);
        adminService.deleteCompany(userId);

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Page<PartyType>> makePage(@RequestParam(value = "page", required = false, defaultValue = "1") int pageNumber, @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize, List<PartyType> unverifiedCompanies) {
        int start = (pageNumber - 1) * pageSize;
        int end = (start + pageSize) > unverifiedCompanies.size() ? unverifiedCompanies.size() : (start + pageSize);
        Page<PartyType> companyPage = new PageImpl<>(unverifiedCompanies.subList(start, end), new PageRequest(pageNumber - 1, pageSize), unverifiedCompanies.size());

        return ResponseEntity.ok(companyPage);
    }
}
