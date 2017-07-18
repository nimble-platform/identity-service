package eu.nimble.core.infrastructure.identity.controller.ubl;

import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Johannes Innerbichler on 26/04/17.
 * Controller for retrieving party data.
 */
@Controller
@Api(value = "party", description = "API for handling parties on the platform.")
public class PartyController {

    private static final Logger logger = LoggerFactory.getLogger(PartyController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PersonRepository personRepository;

    @ApiOperation(value = "", notes = "Get Party for Id.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/{partyId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<PartyType> getParty(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId) {

        // search relevant parties
        List<PartyType> parties = partyRepository.findByHjid(partyId);

        // check if party was found
        if( parties.isEmpty() ) {
            logger.info("Requested party with Id {} not found", partyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = parties.get(0);

        logger.debug("Returning requested party with Id {0}", party.getHjid());
        return new ResponseEntity<>(party, HttpStatus.FOUND);
    }

    @ApiOperation(value = "", notes = "Get Party for person ID.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party_by_person/{personId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<List<PartyType>> getPartyByPersonID(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long personId) {

        // search for persons
        List<PersonType> foundPersons = personRepository.findByHjid(personId);

        // check if person was found
        if (foundPersons.isEmpty()) {
            logger.info("Requested person with Id {} not found", personId);
            return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
        }

        PersonType person = foundPersons.get(0);
        List<PartyType> parties = partyRepository.findByPerson(person);

        return new ResponseEntity<>(parties, HttpStatus.OK);
    }
}
