package eu.nimble.core.infrastructure.identity.system.ubl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import eu.nimble.core.infrastructure.identity.config.NimbleConfigurationProperties;
import eu.nimble.core.infrastructure.identity.system.ControllerUtils;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.core.infrastructure.identity.repository.QualifyingPartyRepository;
import eu.nimble.core.infrastructure.identity.service.IdentityService;
import eu.nimble.core.infrastructure.identity.uaa.OAuthClient;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    private QualifyingPartyRepository qualifyingPartyRepository;

    @Autowired
    private IdentityService identityService;

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "", notes = "Get Party for Id.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/{partyId}", method = RequestMethod.GET)
    ResponseEntity<PartyType> getParty(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId,
            @ApiParam(value = "Switch for including roles of persons in response (slower)") @RequestParam(required = false) boolean includeRoles,
            @RequestHeader(value = "Authorization") String bearer) throws IOException {

        // search relevant parties
        PartyType party = partyRepository.findByHjid(partyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        // remove person depending on access rights
        if (identityService.hasAnyRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            party.setPerson(new ArrayList<>());

        if (includeRoles)
            // enrich persons with roles
            identityService.enrichWithRoles(party);

        logger.debug("Returning requested party with Id {}", party.getHjid());
        return new ResponseEntity<>(party, HttpStatus.OK);
    }


    @ApiOperation(value = "getAllParties", notes = "Get all parties in a paginated manner", response = Page.class)
    @RequestMapping(value = "/parties/all", method = RequestMethod.GET)
    ResponseEntity<Page<PartyType>> getAllParties(@RequestParam(value = "page", required = false, defaultValue = "0") int pageNumber,
                                                  @ApiParam(value = "Switch for including roles of persons in response (slower)") @RequestParam(required = false) boolean includeRoles,
                                                  @RequestParam(value = "size", required = false, defaultValue = "10") int pageSize) {

        logger.debug("Requesting all parties page {}", pageNumber);

        Page<PartyType> partyPage = partyRepository.findAll(new PageRequest(pageNumber, pageSize, new Sort(Sort.Direction.ASC, "partyName")));

        // fetch and include roles
        if (includeRoles)
            partyPage.getContent().forEach(identityService::enrichWithRoles);

        return new ResponseEntity<>(partyPage, HttpStatus.OK);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "getParties", notes = "Get multiple parties for Ids.", response = Iterable.class)
    @RequestMapping(value = "/parties/{partyIds}", method = RequestMethod.GET)
    ResponseEntity<?> getParty(
            @ApiParam(value = "Switch for including roles of persons in response (slower)") @RequestParam(required = false) boolean includeRoles,
            @ApiParam(value = "Ids of parties to retrieve.", required = true) @PathVariable List<Long> partyIds) {

        logger.debug("Requesting parties with Ids {}", partyIds);

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

        if (includeRoles)
            // fetch and include roles
            parties.forEach(identityService::enrichWithRoles);

        logger.debug("Returning requested parties with Ids {}", partyIds);
        return new ResponseEntity<>(parties, HttpStatus.OK);
    }

    @ApiOperation(value = "", notes = "Get Party for person ID.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/party_by_person/{personId}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<List<PartyType>> getPartyByPersonID(
            @ApiParam(value = "Switch for including roles of persons in response (slower)") @RequestParam(required = false) boolean includeRoles,
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

        if (includeRoles)
            // fetch and include roles
            parties.forEach(identityService::enrichWithRoles);

        return new ResponseEntity<>(parties, HttpStatus.OK);
    }

    @SuppressWarnings("PointlessBooleanExpression")
    @ApiOperation(value = "Get Party for Id in the UBL format.",
            response = PartyType.class, tags = {})
    @RequestMapping(value = "/party/ubl/{partyId}", produces = {"text/xml"}, method = RequestMethod.GET)
    ResponseEntity<String> getPartyUbl(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId,
            @ApiParam(value = "Switch for including roles of persons in response (slower)") @RequestParam(required = false) boolean includeRoles,
            @RequestHeader(value = "Authorization") String bearer) throws IOException, JAXBException {

        // search relevant parties
        PartyType party = partyRepository.findByHjid(partyId).stream().findFirst().orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        // remove person depending on access rights
        if (identityService.hasAnyRole(bearer, OAuthClient.Role.LEGAL_REPRESENTATIVE) == false)
            party.setPerson(new ArrayList<>());

        if (includeRoles)
            identityService.enrichWithRoles(party);

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

    @ApiOperation(value = "Get all party ids and name. Returns id-name tuples.",
            notes = "Roles for persons are not set. Please use /person/{personId} for fetching roles of users",
            response = PartyTuple.class, responseContainer = "List")
    @RequestMapping(value = "/party/all", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<List<PartyTuple>> getAllPartyIds(
            @ApiParam(value = "Excluded ids") @RequestParam(value = "exclude", required = false) List<String> exclude) {

        List<PartyTuple> partyIds = StreamSupport.stream(partyRepository.findAll().spliterator(), false)
                .map(p -> new PartyTuple(UblAdapter.adaptPartyIdentifier(p), UblAdapter.adaptPartyNames(p.getPartyName())))
                .collect(Collectors.toList());

        if (exclude != null)
            partyIds = partyIds.stream().filter(p -> !exclude.contains(p.getCompanyID())).collect(Collectors.toList());

        return ResponseEntity.ok(partyIds);
    }

    @SuppressWarnings("ConstantConditions")
    @ApiOperation(value = "", notes = "Get QualifyingParty for Id.", response = PartyType.class, tags = {})
    @RequestMapping(value = "/qualifying/{partyId}", method = RequestMethod.GET)
    ResponseEntity<QualifyingPartyType> getQualifyingParty(
            @ApiParam(value = "Id of party to retrieve.", required = true) @PathVariable Long partyId,
            @RequestHeader(value = "Authorization") String bearer) {

        PartyType party = partyRepository.findByHjid(partyId).stream()
                .findFirst()
                .orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        QualifyingPartyType qualifyingParty = qualifyingPartyRepository.findByParty(party).stream()
                .findFirst()
                .orElseThrow(ControllerUtils.CompanyNotFoundException::new);

        logger.debug("Returning requested QualifyingParty with Id {}", partyId);
        return new ResponseEntity<>(qualifyingParty, HttpStatus.OK);
    }

    private static class PartyTuple {
        private String identifier;
        private Map<NimbleConfigurationProperties.LanguageID, String> name;

        public PartyTuple(String identifier, Map<NimbleConfigurationProperties.LanguageID, String> name) {
            this.identifier = identifier;
            this.name = name;
        }

        public String getCompanyID() {
            return identifier;
        }

        public Map<NimbleConfigurationProperties.LanguageID, String> getName() {
            return name;
        }
    }
}
