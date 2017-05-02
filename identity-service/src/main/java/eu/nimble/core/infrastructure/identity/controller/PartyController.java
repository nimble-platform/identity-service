package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
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

        logger.debug("Returning reqeusted party with Id {0}", party.getHjid());
        return new ResponseEntity<>(party, HttpStatus.FOUND);


    }
}
