package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.clients.StripeClient;
import eu.nimble.core.infrastructure.identity.entity.stripe.AccountLink;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.*;

@Controller
public class StripeController {

    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    @Autowired
    private StripeClient stripeClient;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private IdentityService identityService;

    @ApiOperation(value = "", notes = "Connects the given party to Stripe")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Connected the party to Stripe successfully"),
            @ApiResponse(code = 403, message = "Invalid role"),
            @ApiResponse(code = 500, message = "Unexpected error while connecting the party to Stripe")
    })
    @RequestMapping(value = "/account",
            method = RequestMethod.POST)
    public ResponseEntity connectStripe(@ApiParam(value = "Id of account if exists") @RequestParam(value = "id", required = false) String accountId,
                                        @ApiParam(value = "Id of company for which the account is to be created") @RequestParam(value = "partyId", required = false) Long partyId,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearer) {
        try {
            if (!identityService.hasAnyRole(bearer, COMPANY_ADMIN, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE, PUBLISHER))
                return new ResponseEntity<>("You are not allowed to connect stripe", HttpStatus.FORBIDDEN);

            AccountLink accountLink;
            // when we have the account id, it is enough to generate a link for the party
            // so that it can complete its registration to Stripe
            if (accountId != null) {
                accountLink = this.stripeClient.getAccountLink(accountId);
            }
            // otherwise, we need to create a Stripe account for the party
            else {
                accountLink = this.stripeClient.createAccount();
                List<PartyType> partyTypes = partyRepository.findByHjid(partyId);
                for (PartyType partyType : partyTypes) {
                    partyType.setStripeAccountId(accountLink.getAccountId());
                    partyRepository.save(partyType);
                }
            }
            return ResponseEntity.ok(accountLink);
        } catch (Exception e) {
            logger.error("Unexpected error while connecting the party to Stripe:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while connecting the party to Stripe");
        }
    }

    @ApiOperation(value = "", notes = "Retrieves the login link for the given account")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Retrieved the login link successfully"),
            @ApiResponse(code = 403, message = "Invalid role."),
            @ApiResponse(code = 500, message = "Unexpected error while retrieving the login link")
    })
    @RequestMapping(value = "/account/login-link",
            method = RequestMethod.GET)
    public ResponseEntity getAccountLoginLink(@ApiParam(value = "Id of account for which the login link is created.", required = true) @RequestParam(value = "id", required = true) String id,
                                              @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearer) {
        try {
            if (!identityService.hasAnyRole(bearer, COMPANY_ADMIN, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE, PUBLISHER))
                return new ResponseEntity<>("You are not allowed to retrieve a login link", HttpStatus.FORBIDDEN);

            return ResponseEntity.ok(this.stripeClient.getAccountLoginLink(id));
        } catch (Exception e) {
            logger.error("Unexpected error while retrieving the login link:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while retrieving the login link");
        }
    }

    @ApiOperation(value = "", notes = "Deletes the stripe account")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Deleted the stripe account successfully"),
            @ApiResponse(code = 403, message = "Invalid role"),
            @ApiResponse(code = 500, message = "Unexpected error while deleting Stripe account")
    })
    @RequestMapping(value = "/account",
            method = RequestMethod.DELETE)
    public ResponseEntity deleteAccount(@ApiParam(value = "Id of account to be deleted.", required = true) @RequestParam(value = "id", required = true) String id,
                                        @ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearer) {
        try {
            if (!identityService.hasAnyRole(bearer, COMPANY_ADMIN, LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE, PUBLISHER))
                return new ResponseEntity<>("You are not allowed to delete an account", HttpStatus.FORBIDDEN);

            boolean deleted = this.stripeClient.deleteAccount(id);

            if (deleted) {
                List<PartyType> partyTypeList = partyRepository.findByStripeAccountId(id);
                for (PartyType partyType : partyTypeList) {
                    partyType.setStripeAccountId(null);
                    partyRepository.save(partyType);
                }
            }

            return ResponseEntity.ok(deleted);
        } catch (Exception e) {
            logger.error("Unexpected error while deleting Stripe account:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error while deleting Stripe account");
        }
    }

}
