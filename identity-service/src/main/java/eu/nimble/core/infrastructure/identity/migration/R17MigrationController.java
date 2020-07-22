package eu.nimble.core.infrastructure.identity.migration;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.entity.UserInvitation;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.repository.UserInvitationRepository;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.service.RocketChatService;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list.ChatUser;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list.ChatUsers;
import eu.nimble.core.infrastructure.identity.system.dto.rocketchat.list.UserEmail;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

import static eu.nimble.core.infrastructure.identity.uaa.OAuthClient.Role.PLATFORM_MANAGER;

@Controller
public class R17MigrationController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private IdentityService identityService;
    @Autowired
    private UaaUserRepository uaaUserRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private KeycloakAdmin keycloakAdmin;
    @Autowired
    private UserInvitationRepository userInvitationRepository;
    @Autowired
    private RocketChatService chatService;

    @ApiOperation(value = "", notes = "Validates the data of users")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Completed the validation successfully"),
            @ApiResponse(code = 401, message = "Invalid role")
    })
    @RequestMapping(value = "/r17/migration/validate-data",
            produces = {"application/json"},
            method = RequestMethod.PATCH)
    public ResponseEntity validateData(@ApiParam(value = "The Bearer token provided by the identity service", required = true) @RequestHeader(value = "Authorization") String bearerToken
    ) throws IOException {
        logger.info("Incoming request to validate data");

        // validate role
        if (!identityService.hasAnyRole(bearerToken, PLATFORM_MANAGER))
            return new ResponseEntity<>("Only platform managers are allowed to run this migration script", HttpStatus.FORBIDDEN);

        logger.info("Validating UaaUser");
        List<UaaUser> uaaUsers = (List<UaaUser>) uaaUserRepository.findAll();
        for (UaaUser uaaUser : uaaUsers) {
            try {
                keycloakAdmin.getUserResource(uaaUser.getExternalID()).toRepresentation();
            } catch (NotFoundException exception) {
                logger.error("{} does not exist on Keycloak", uaaUser.getUsername());
                uaaUserRepository.delete(uaaUser);
            }
        }

        logger.info("Validating Person");
        List<PersonType> personTypes = (List<PersonType>) personRepository.findAll();
        for (PersonType personType : personTypes) {
            String email = personType.getContact().getElectronicMail();
            if (email != null) {
                UaaUser uaaUser = uaaUserRepository.findOneByUsername(email);
                if (uaaUser == null) {
                    logger.error("UaaUser does not exist for person:{}", email);
                    personRepository.delete(personType);
                }
            }
        }

        logger.info("Validating User Invitations");
        List<UserInvitation> userInvitations = (List<UserInvitation>) userInvitationRepository.findAll();
        for (UserInvitation userInvitation : userInvitations) {
            if (!userInvitation.getPending()) {
                UaaUser uaaUser = uaaUserRepository.findOneByUsername(userInvitation.getEmail());
                if (uaaUser == null) {
                    logger.error("UaaUser does not exist for user:{}", userInvitation.getEmail());
                    userInvitationRepository.delete(userInvitation);
                }
            }
        }

        if (chatService.isChatEnabled()) {
            logger.info("Validating chat service users");
            ChatUsers chatUsers = chatService.listUsers();
            List<ChatUser> chatUserList = chatUsers.getUsers();
            for (ChatUser chatUser : chatUserList) {
                if (chatUser.getEmails() != null) {
                    boolean userExists = false;
                    String emailAddress = null;
                    for (UserEmail userEmail : chatUser.getEmails()) {
                        emailAddress = userEmail.getAddress();
                        UaaUser uaaUser = uaaUserRepository.findOneByUsername(emailAddress);
                        if (uaaUser != null) {
                            userExists = true;
                            break;
                        }
                    }
                    if (!userExists && emailAddress != null) {
                        logger.error("UaaUser does not exist for user:{}", emailAddress);
                        chatService.deleteUser(emailAddress);
                    }
                }
            }
        }

        logger.info("Completed request to validate data");
        return ResponseEntity.ok(null);
    }

}
