package eu.nimble.core.infrastructure.identity.controller.ubl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.nimble.core.infrastructure.identity.controller.IdentityUtils;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Autowired
    private IdentityUtils identityUtils;

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "", notes = "Get Party for Id.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/{partyId}", method = RequestMethod.GET)
    ResponseEntity<PartyType> getParty(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId,
            @RequestHeader(value = "Authorization") String bearer) throws IOException {

        // search relevant parties
        List<PartyType> parties = partyRepository.findByHjid(partyId);

        // check if party was found
        if (parties.isEmpty()) {
            logger.info("Requested party with Id {} not found", partyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = parties.get(0);

        // remove person depending on access rights
        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            party.setPerson(new ArrayList<>());

        logger.debug("Returning requested party with Id {0}", party.getHjid());
        return new ResponseEntity<>(party, HttpStatus.OK);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "", notes = "Get multiple parties for Ids.", response = Iterable.class)
    @RequestMapping(value = "/parties/{partyIds}", method = RequestMethod.GET)
    ResponseEntity<?> getParty(
            @ApiParam(value = "Ids of parties to retrieve.", required = true) @PathVariable List<Long> partyIds) {

        logger.debug("Requesting parties with Ids {0}", partyIds);

        // search relevant parties
        List<PartyType> parties = new ArrayList<>();
        for (Long partyId : partyIds) {
            Optional<PartyType> party = partyRepository.findByHjid(partyId).stream().findFirst();

            // check if party was found
            if (party.isPresent() == false) {
                String message = String.format("Requested party with Id %s not found", partyId);
                logger.info(message);
                return new ResponseEntity<>(message, HttpStatus.NOT_FOUND);
            }

            parties.add(party.get());
        }

        logger.debug("Returning requested parties with Ids {0}", partyIds);
        return new ResponseEntity<>(parties, HttpStatus.OK);
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

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "", notes = "Get Party for Id in the UBL format.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/ubl/{partyId}", produces = {"text/xml"}, method = RequestMethod.GET)
    ResponseEntity<String> getPartyUbl(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId,
            @RequestHeader(value = "Authorization") String bearer) throws IOException, JAXBException {

        // search relevant parties
        List<PartyType> parties = partyRepository.findByHjid(partyId);

        // check if party was found
        if (parties.isEmpty()) {
            logger.info("Requested party with Id {} not found", partyId);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = parties.get(0);

        // remove person depending on access rights
        if (identityUtils.hasRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            party.setPerson(new ArrayList<>());

        StringWriter serializedCatalogueWriter = new StringWriter();
        String packageName = party.getClass().getPackage().getName();
        JAXBContext jc = JAXBContext.newInstance(packageName);

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        Marshaller marsh = jc.createMarshaller();
        marsh.setProperty("jaxb.formatted.output", true);
        marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        @SuppressWarnings("unchecked")
        JAXBElement element = new JAXBElement(new QName("urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2", "Party"), party.getClass(), party);
        marsh.marshal(element, serializedCatalogueWriter);

        // log the catalogue to be transformed
        String xmlParty = serializedCatalogueWriter.toString();
        xmlParty = xmlParty.replaceAll(" Hjid=\"[0-9]+\"", "");
        serializedCatalogueWriter.flush();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_XML);
        return new ResponseEntity<>(xmlParty, responseHeaders, HttpStatus.OK);
    }
}
