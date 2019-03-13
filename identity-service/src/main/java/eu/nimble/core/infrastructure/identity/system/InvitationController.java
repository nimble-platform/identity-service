package eu.nimble.core.infrastructure.identity.system;

import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.mail.EmailService;
import eu.nimble.core.infrastructure.identity.repository.*;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.*;
import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.INITIAL_REPRESENTATIVE;

@Controller
public class InvitationController {

    private static final Logger logger = LoggerFactory.getLogger(InvitationController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private UserInvitationRepository userInvitationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @ApiOperation(value = "", notes = "Send invitation to user.", response = ResponseEntity.class, tags = {})
    @RequestMapping(value = "/send_invitation", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<?> sendInvitation(
            @ApiParam(value = "Invitation object.", required = true) @Valid @RequestBody UserInvitation invitation,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletRequest request) throws IOException {

        OpenIdConnectUserDetails userDetails = OpenIdConnectUserDetails.fromBearer(bearer);
        if (identityService.hasAnyRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives are allowed to invite users", HttpStatus.UNAUTHORIZED);

        String emailInvitee = invitation.getEmail();
        String companyId = invitation.getCompanyId();

        // obtain sending company and user
        Optional<PartyType> parties = partyRepository.findByHjid(Long.parseLong(companyId)).stream().findFirst();
        if (parties.isPresent() == false) {
            logger.info("Invitation: Requested party with Id {} not found", companyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PartyType company = parties.get();

        // collect information of sending user
        UaaUser sender = uaaUserRepository.findByExternalID(userDetails.getUserId());
        PersonType sendingPerson = sender.getUBLPerson();
        String senderName = sendingPerson.getFirstName() + " " + sendingPerson.getFamilyName();

        // check if user has already been invited
        if (userInvitationRepository.findByEmail(emailInvitee).isEmpty() == false) {
            logger.info("Invitation: Impossible to register user {} twice for company {}.", emailInvitee, companyId);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<String> userRoleIDs = invitation.getRoleIDs() == null ? new ArrayList() : invitation.getRoleIDs();
        List<String> prettifedRoles = KeycloakAdmin.prettfiyRoleIDs(userRoleIDs);

        // check if user is already registered
        Optional<UaaUser> potentialInvitee = uaaUserRepository.findByUsername(emailInvitee).stream().findFirst();
        if (potentialInvitee.isPresent()) {

            UaaUser invitee = potentialInvitee.get();

            // check if user is already part of a company
            List<PartyType> companiesOfInvitee = partyRepository.findByPerson(invitee.getUBLPerson());
            if (companiesOfInvitee.isEmpty() == false) {
                logger.info("Invitation: User {} is already member of another company.", emailInvitee);
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            }

            // saving invitation
            UserInvitation userInvitation = new UserInvitation(emailInvitee, companyId, userRoleIDs, sender);
            userInvitationRepository.save(userInvitation);

            // ToDo: let user accept invitation

            // send information
            String companyName = UblUtils.getName(company.getPartyName(), NimbleConfigurationProperties.LanguageID.ENGLISH);
            emailService.informInviteExistingCompany(emailInvitee, senderName, companyName, prettifedRoles);
            logger.info("Invitation: User {} is already on the platform (without company). Invite from {} ({}) sent.",
                    emailInvitee, sender.getUsername(), companyName);

            // add existing user to company
            company.getPerson().add(potentialInvitee.get().getUBLPerson());

            // set request roles to user
            keycloakAdmin.applyRoles(invitee.getExternalID(), new HashSet<>(userRoleIDs));

            // update invitation
            userInvitation.setPending(false);
            userInvitationRepository.save(userInvitation);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Existing user added to company");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }

        // send invitation
        String companyName = UblUtils.getName(company.getPartyName(), NimbleConfigurationProperties.LanguageID.ENGLISH);
        emailService.sendInvite(emailInvitee, senderName, companyName, prettifedRoles);

        logger.info("Invitation sent FROM {} ({}, {}) TO {}", senderName, companyName, companyId, emailInvitee);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "", notes = "Get list of company members.", response = UserInvitation.class, responseContainer = "List", tags = {})
    @RequestMapping(value = "/company_members/{companyID}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<?> pendingInvitations(
            @RequestHeader(value = "Authorization") String bearer,
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long companyID) throws IOException {

        if (identityService.hasAnyRole(bearer,COMPANY_ADMIN,LEGAL_REPRESENTATIVE, PLATFORM_MANAGER, INITIAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives, company admin or platform managers are allowed to retrieve company members", HttpStatus.FORBIDDEN);

        if (identityService.hasAnyRole(bearer,PLATFORM_MANAGER) == false){
            UaaUser user = identityService.getUserfromBearer(bearer);

            Optional<PartyType> userCompanyOpt = identityService.getCompanyOfUser(user);

            if (userCompanyOpt.isPresent() == true) {
                PartyType company = userCompanyOpt.get();
                if(companyID != company.getHjid())
                    return new ResponseEntity<>("Only platform managers are allowed to retrieve all company members", HttpStatus.FORBIDDEN);
            }
        }

        Optional<PartyType> companyOpt = partyRepository.findByHjid(companyID).stream().findFirst();

        if (companyOpt.isPresent() == false) {
            logger.info("Company members: Requested party for not found.");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        PartyType company = companyOpt.get();

        List<UserInvitation> members = userInvitationRepository.findByCompanyId(UblAdapter.adaptPartyIdentifier(company));

        // add initial user (i.e. initial representative)
        List<String> invitationEmails = members.stream().map(UserInvitation::getEmail).collect(Collectors.toList());
        company.getPerson().stream()
                .filter(p -> !invitationEmails.contains(p.getContact().getElectronicMail()))
                .map(m -> new UserInvitation(m.getContact().getElectronicMail(), UblAdapter.adaptPartyIdentifier(company), null, null, false))
                .forEach(members::add);;

        // update roles
        for (UserInvitation member : members) {
            if (member.getPending() == false) {
                String username = member.getEmail();
                UaaUser uaaUser = uaaUserRepository.findOneByUsername(username);
                if (uaaUser != null) {
                    Set<String> roles = keycloakAdmin.getUserRoles(uaaUser.getExternalID(), KeycloakAdmin.NON_ASSIGNABLE_ROLES);
                    member.setRoleIDs(new ArrayList<>(roles));
                }
            }
        }

        return new ResponseEntity<>(members, HttpStatus.OK);
    }


    @ApiOperation(value = "", notes = "Remove existing invitation.", response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User removed from company", response = String.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 409, message = "User not in company")})
    @RequestMapping(value = "/invitations", method = RequestMethod.DELETE)
    ResponseEntity<?> removeInvitation(@ApiParam(value = "Username", required = true) @RequestParam String username,
                                       @RequestHeader(value = "Authorization") String bearer) throws IOException {

        // check if authorized
        if (identityService.hasAnyRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>("Only legal representatives are allowed to invite users", HttpStatus.UNAUTHORIZED);

        logger.info("Requesting removal of company membership of user {}.", username);

        // delete invitation
        List<UserInvitation> deletedInvitations = userInvitationRepository.removeByEmail(username);
        String responseMessage = deletedInvitations.isEmpty() ? "" : "Removed invitation";

        if (deletedInvitations.isEmpty() == false)
            logger.info("Removed invitation of user {}.", username);

        // remove person from company
        UaaUser userToRemove = uaaUserRepository.findOneByUsername(username);
        UaaUser requester = uaaUserRepository.findOneByUsername(identityService.getUserDetails(bearer).getUsername());
        if (userToRemove != null && requester != null &&
                userToRemove.getUsername().equals(requester.getUsername()) == false) {  // user cannot remove itself
            if (identityService.inSameCompany(userToRemove, requester) == false)
                return new ResponseEntity<>(HttpStatus.CONFLICT);


            // remove from list of persons
            Optional<PartyType> companyOpt = identityService.getCompanyOfUser(requester);
            if (companyOpt.isPresent()) {

                // remove roles of user
                keycloakAdmin.applyRoles(userToRemove.getExternalID(), Collections.emptySet());

                PartyType company = companyOpt.get();
                company.getPerson().remove(userToRemove.getUBLPerson());
                partyRepository.save(company);
                responseMessage += "\nRemoved from company";

                String companyName = UblAdapter.adaptPartyIdentifier(company);
                logger.info(requester.getUsername() + " removed " + userToRemove.getUsername() + " from company " + companyName);
            }
        }

        responseMessage = responseMessage.isEmpty() ? "No changes" : responseMessage;
        return new ResponseEntity<>(responseMessage, HttpStatus.OK);
    }
}
