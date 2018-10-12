package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Created by Johannes Innerbichler on 12.09.18.
 */
@Controller
@RequestMapping(path = "/admin")
@Api(value = "Admin API", description = "Administration services for managing identity on the platform.")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private static final String DEFAULT_PAGE_SIZE = "20";

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @ApiOperation(value = "Retrieve unverified companies", response = Page.class)
    @RequestMapping(value = "/unverified_companies", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Page<PartyType>> getUnverifiedCompanies(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                           @RequestParam(value = "size", required = false, defaultValue = DEFAULT_PAGE_SIZE) int size) {
        logger.info("Fetching unverified companies");

        // ToDo: verify proper access policy (e.g. admin role)

        Specification<PartyType> verifiedCompanySpecification = new Specification<PartyType>() {
            @Override
            public Predicate toPredicate(Root<PartyType> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

                return builder.equal(root.get("name"), "sdfsadf");
            }
        };

        Page<PartyType> resultPage = partyRepository.findAll(verifiedCompanySpecification, new PageRequest(page, size, Sort.Direction.ASC, "ID"));
        if (page > resultPage.getTotalPages())
            throw new ResourceNotFoundException();

        // ToDo: filter companies by verification state

        // remove binaries since they cannot be serialised to JSON
        resultPage.getContent().forEach(IdentityUtils::removeBinaries);

        return ResponseEntity.ok(resultPage);
    }
}
