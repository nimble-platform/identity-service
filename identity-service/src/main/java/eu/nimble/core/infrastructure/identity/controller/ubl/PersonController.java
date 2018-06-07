package eu.nimble.core.infrastructure.identity.controller.ubl;

import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
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

    @ApiOperation(value = "", notes = "Get Person for Id.", response = PersonType.class, tags = {})
    @RequestMapping(value = "/person/{personId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<PersonType> getPerson(
            @ApiParam(value = "Id of person to retrieve.", required = true) @PathVariable Long personId) {

        // search for persons
        List<PersonType> foundPersons = personRepository.findByHjid(personId);

        // check if person was found
        if (foundPersons.isEmpty()) {
            logger.info("Requested person with Id {} not found", personId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PersonType person = foundPersons.get(0);

        logger.debug("Returning requested person with Id {}", person.getHjid());
        return new ResponseEntity<>(person, HttpStatus.OK);
    }
}
