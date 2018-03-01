package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@Controller
public class InvitationController {

    private static final Logger logger = LoggerFactory.getLogger(UserIdentityController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IdentityUtils identityUtils;

    @ApiOperation(value = "", notes = "Send inviation to user.", response = ResponseEntity.class, tags = {})
    @RequestMapping(value = "/send_invitation", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> sendInvitation(
            @ApiParam(value = "Invitation object.", required = true) @Valid @RequestBody UserInvitation invitation,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletRequest request) throws IOException {

        OpenIdConnectUserDetails userDetails = OpenIdConnectUserDetails.fromBearer(bearer);
        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives are allowd to invite users", HttpStatus.UNAUTHORIZED);

        String emailInvitee = invitation.getEmail();
        String companyId = invitation.getCompanyId();

        // obtain sending company and user
        Optional<PartyType> parties = partyRepository.findByHjid(Long.parseLong(companyId)).stream().findFirst();
        if (parties.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PartyType company = parties.get();


        // check if user is already registered
        Optional<UaaUser> potentialInvitee = uaaUserRepository.findByUsername(emailInvitee).stream().findFirst();
        if (potentialInvitee.isPresent()) {

            UaaUser invitee = potentialInvitee.get();

            // check if user is already part of a copmany
            List<PartyType> companiesOfInvitee = partyRepository.findByPerson(invitee.getUBLPerson());
            if (companiesOfInvitee.isEmpty() == false) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            // ToDo: ask for approval of invitee

            // add existing user to company
            company.getPerson().add(potentialInvitee.get().getUBLPerson());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Existing user added to company");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        // collect store invitation
        UaaUser sender = uaaUserRepository.findByExternalID(userDetails.getUserId());
        List<String> userRoleIDs = invitation.getRoleIDs() == null ? new ArrayList() : invitation.getRoleIDs();
        UserInvitation userInvitation = new UserInvitation(emailInvitee, companyId, userRoleIDs, sender);

        try {
            // saving invitation with duplicate check
            userInvitationRepository.save(userInvitation);
        } catch (Exception ex) {
            logger.info("Impossible to register user {} twice for company {}", emailInvitee, companyId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        PersonType sendingPerson = sender.getUBLPerson();
        String senderName = sendingPerson.getFirstName() + " " + sendingPerson.getFamilyName();

        // send invitation
        emailService.sendInvite(emailInvitee, senderName, company.getName());

        logger.info("Invitation sent FROM {} ({}, {}) TO {}", senderName, company.getName(), companyId, emailInvitee);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @ApiOperation(value = "", notes = "Get pending invitations.", response = UserInvitation.class, responseContainer = "List", tags = {})
    @RequestMapping(value = "/invitations", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> pendingInvitations(@RequestHeader(value = "Authorization") String bearer) throws IOException{
        UaaUser user = identityUtils.getUserfromBearer(bearer);

        Optional<PartyType> companyOpt = identityUtils.getCompanyOfUser(user);
        if (companyOpt.isPresent() == false) {
            logger.error("Requested party for user {} not found", user.getUsername());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PartyType company = companyOpt.get();

        List<UserInvitation> pendingInvitations = userInvitationRepository.findByCompanyId(company.getID());
        return new ResponseEntity<>(pendingInvitations, HttpStatus.OK);
    }
}
