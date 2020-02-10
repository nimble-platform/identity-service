package eu.nimble.core.infrastructure.identity.migration;

import eu.nimble.core.infrastructure.identity.clients.DelegateServiceClient;
import eu.nimble.core.infrastructure.identity.config.FederationConfig;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.*;

@ApiIgnore
@Controller
public class R15MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private DelegateServiceClient delegateServiceClient;
    @Autowired
    private FederationConfig federationConfig;

    @ApiOperation(value = "", notes = "Sets the federation id of parties")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Sets federation id of parties successfully"),
            @ApiResponse(code = 401, message = "Invalid token. No user was found for the provided token"),
            @ApiResponse(code = 500, message = "Unexpected error while setting federation id of parties")
    })
    @RequestMapping(value = "/r15/migration/federate-parties",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity federateParties(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization", required = true) String bearerToken
    ) throws IOException {
        logger.info("Incoming request to federate parties");

        // validate role
        if (identityService.hasAnyRole(bearerToken,COMPANY_ADMIN,LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE, PUBLISHER) == false)
            return new ResponseEntity<>("Only legal representatives, company admin or platform managers are allowed to run this migration script", HttpStatus.FORBIDDEN);

        // federation id
        String federationId = federationConfig.getFederationInstanceId();
        if(federationId == null){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("This instance does not have a federation id");
        }

        for (PartyType partyType : partyRepository.findAll()) {
            partyType.setFederationInstanceID(federationId);
            partyRepository.save(partyType);
        }

        logger.info("Completed request to federate parties");
        return ResponseEntity.ok(null);
    }

}
