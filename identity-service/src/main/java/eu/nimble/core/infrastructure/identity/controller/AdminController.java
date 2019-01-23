package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.service.AdminService;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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

import java.util.List;

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
                                                           @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int pageSize) {
        // ToDo: verify proper access policy (e.g. admin role)

        logger.info("Fetching unverified companies");
        List<PartyType> unverifiedCompanies = adminService.queryUnverifiedCompanies();

        // paginate results
        int start = (pageNumber - 1) * pageSize;
        int end = (start + pageSize) > unverifiedCompanies.size() ? unverifiedCompanies.size() : (start + pageSize);
        Page<PartyType> companyPage = new PageImpl<>(unverifiedCompanies.subList(start, end), new PageRequest(pageNumber - 1, pageSize), unverifiedCompanies.size());

        return ResponseEntity.ok(companyPage);
    }

    @ApiOperation(value = "Verify company")
    @RequestMapping(value = "/verify_company", method = RequestMethod.POST)
    ResponseEntity<?> verifyCompany(@RequestParam(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only legal platform managers are allowed to verify companies", HttpStatus.UNAUTHORIZED);

        logger.info("Verifying company with id {}", companyId);
        adminService.verifyCompany(companyId);

        return ResponseEntity.ok().build();
    }

    @ApiOperation(value = "Delete company")
    @RequestMapping(value = "/delete_company/{companyId}", method = RequestMethod.DELETE)
    ResponseEntity<?> deleteCompany(@PathVariable(value = "companyId") long companyId,
                                    @RequestHeader(value = "Authorization") String bearer) throws Exception {

        if (identityService.hasRole(bearer, OAuthClient.Role.PLATFORM_MANAGER) == false)
            return new ResponseEntity<>("Only legal platform managers are allowed to delete companies", HttpStatus.UNAUTHORIZED);

        logger.info("Deleting company with id {}", companyId);
        adminService.deleteCompany(companyId);

        return ResponseEntity.ok().build();
    }
}
