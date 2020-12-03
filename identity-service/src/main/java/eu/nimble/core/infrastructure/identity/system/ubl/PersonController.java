package eu.nimble.core.infrastructure.identity.system.ubl;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.system.dto.PeopleList;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import eu.nimble.common.rest.identity.model.PersonPartyTuple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Johannes Innerbichler on 02/05/17.
 * Controller for retrieving information about specific persons.
 */
@Controller
@Api(value = "person", description = "API for handling persons on the platform.")
public class PersonController {

    private static final Logger logger = LoggerFactory.getLogger(PersonController.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private PartyRepository partyRepository;

    @ApiOperation(value = "Get Person for Id.", notes = "Roles are fetch from Keycloak", response = PersonType.class, tags = {})
    @RequestMapping(value = "/person/{personId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<PersonType> getPerson(
            @ApiParam(value = "Id of person to retrieve.", required = true) @PathVariable Long personId) {

        logger.debug("Requesting person information for {}", personId);

        // search for persons
        List<PersonType> foundPersons = personRepository.findByHjid(personId);

        // check if person was found
        if (foundPersons.isEmpty()) {
            logger.info("Requested person with Id {} not found", personId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PersonType person = foundPersons.get(0);

        // fetch and set roles
        List<String> roles = new ArrayList<>(identityService.fetchRoles(person));
        person.setRole(roles);

        logger.debug("Returning requested person with Id {}", person.getHjid());
        return new ResponseEntity<>(person, HttpStatus.OK);
    }

    @ApiOperation(value = "", notes = "Returns person id-company id tuple for the given access token ", response = PersonPartyTuple.class)
    @RequestMapping(value = "/person/person-party", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<PersonPartyTuple> getPersonPartyTuple(@RequestHeader(value = "Authorization") String bearer) throws IOException {
        // get user from the bearer token
        UaaUser user = identityService.getUserfromBearer(bearer);
        // get ubl person for the user
        PersonType personType = user.getUBLPerson();
        // find the user parties
        List<PartyType> parties = partyRepository.findByPerson(personType);
        // create the response
        PersonPartyTuple personPartyTuple = new PersonPartyTuple(parties.get(0).getPartyIdentification().get(0).getID(),personType.getID());

        return new ResponseEntity<>(personPartyTuple, HttpStatus.OK);
    }

    @ApiOperation(value = "Get personal information for a list of IDs.", notes = "Roles are fetched from Keycloak", response = PersonType.class, tags = {})
    @RequestMapping(value = "/people", produces = {"application/json"}, method = RequestMethod.POST)
    ResponseEntity<List<PersonType>> getPeople(
            @ApiParam(value = "Ids of people to retrieve.", required = true) @RequestBody PeopleList peopleList) {

        List<PersonType> personTypeList = new ArrayList<>();

        for (Long personId : peopleList.getIds()) {
            logger.debug("Requesting person information for {}", personId);

            // search for persons
            List<PersonType> foundPersons = personRepository.findByHjid(personId);

            // check if person was found
            if (foundPersons.isEmpty()) {
                logger.info("Requested person with Id {} not found", personId);
            }else {
                PersonType person = foundPersons.get(0);

                // fetch and set roles
                List<String> roles = new ArrayList<>(identityService.fetchRoles(person));
                person.setRole(roles);

                logger.debug("Returning requested person with Id {}", person.getHjid());
                personTypeList.add(person);
            }
        }
        return new ResponseEntity<>(personTypeList, HttpStatus.OK);
    }

    @ApiOperation(value = "", notes = "Resolve Person for access token.", response = PersonType.class)
    @RequestMapping(value = "/person/", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<PersonType> getPerson(@RequestHeader(value = "Authorization") String bearer) throws IOException {
        UaaUser user = identityService.getUserfromBearer(bearer);
        return new ResponseEntity<>(user.getUBLPerson(), HttpStatus.OK);
    }
}
