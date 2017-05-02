package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by Johannes Innerbichler on 26/04/17.
 * Controller for retrieving party data.
 */
@Controller
@Api(value = "party", description = "API for handling parties on the platform.")
public class PartyController {

    @Autowired
    PartyRepository partyRepository;

    @ApiOperation(value = "", notes = "Get Party for Id.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/{partyId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<PartyType> getParty(
            @ApiParam(value = "Id of party to retrieve." ,required=true )@PathVariable String partyId) {

//        partyRepository.findOne()

        return new ResponseEntity<>(new PartyType(), HttpStatus.OK);
    }
}
