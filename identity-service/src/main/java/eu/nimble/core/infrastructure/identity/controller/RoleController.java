package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.UaaUserRepository;
import eu.nimble.core.infrastructure.identity.uaa.KeycloakAdmin;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.uaa.OpenIdConnectUserDetails;
import io.swagger.annotations.*;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(path = "/roles")
@Api(value = "user roles", description = "Services for managing roles on the platform.")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private KeycloakAdmin keycloakAdmin;

    @Autowired
    private UaaUserRepository uaaUserRepository;

    @Autowired
    private IdentityUtils identityUtils;

    @ApiOperation(value = "List of user roles on the platform.", response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Roles found", response = Iterable.class),
            @ApiResponse(code = 400, message = "Error while fetching roles")})
    @RequestMapping(value = "", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Map<String, String>> roles(HttpServletResponse response) {

        logger.info("Fetching list of user roles");

        Map<String, String> roles = keycloakAdmin.getRoles();

        // prettify names
        roles = roles.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getValue,
                e -> WordUtils.capitalize(e.getValue().replace("_", " "))
        ));

        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "List of roles of a specific user", response = Iterable.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Roles found", response = Iterable.class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "User not found")})
    @RequestMapping(value = "/user", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<Set<String>> getUserRoles(
            @ApiParam(value = "Username", required = true) @RequestParam String username,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletResponse response) throws IOException {

        logger.info("Requesting roles of user {}", username);

        // Check if requesting user is legal representative
        OpenIdConnectUserDetails userDetails = OpenIdConnectUserDetails.fromBearer(bearer);
        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        // Check if users are in the same company
        UaaUser requestingUser = uaaUserRepository.findByExternalID(userDetails.getUserId());
        UaaUser targetUser = uaaUserRepository.findOneByUsername(username);
        if (identityUtils.inSameCompany(requestingUser, targetUser) == false) {
            logger.info("Requested user {} not found in company", username);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // fetch roles of user
        Set<String> roles = keycloakAdmin.getUserRoles(targetUser.getExternalID());

        logger.info("Returning roles of user {}: {}", username, roles);

        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "Apply roles to a specific user",
            notes = "After calling this operation the list of roles is applied to the user, which might result in removing certain roles.",
            response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Roles successfully applied", response = String[].class),
            @ApiResponse(code = 401, message = "Not authorized"),
            @ApiResponse(code = 404, message = "User not found"),
            @ApiResponse(code = 400, message = "Error while applying roles")})
    @RequestMapping(value = "/user", consumes = {"application/json"}, produces = {"application/text"}, method = RequestMethod.POST)
    ResponseEntity<String> setUserRoles(
            @ApiParam(value = "Username", required = true) @RequestParam String username,
            @ApiParam(value = "Set of roles to apply.", required = true) @RequestBody Set<String> rolesToApply,
            @RequestHeader(value = "Authorization") String bearer,
            HttpServletResponse response) throws IOException {

        logger.info("Setting roles {} of user {}", rolesToApply, username);

        // Check if requesting user is legal representative
        OpenIdConnectUserDetails userDetails = OpenIdConnectUserDetails.fromBearer(bearer);
        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

        // Check if users are in the same company
        UaaUser requestingUser = uaaUserRepository.findByExternalID(userDetails.getUserId());
        UaaUser targetUser = uaaUserRepository.findOneByUsername(username);
        if (identityUtils.inSameCompany(requestingUser, targetUser) == false) {
            logger.info("Requested user {} not found in company", username);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            // setting proper set of roles
            int numChangeRoles = keycloakAdmin.applyRoles(targetUser.getExternalID(), rolesToApply);
            return new ResponseEntity<>("Changed " + numChangeRoles + " roles", HttpStatus.OK);
        } catch (NotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
