package eu.nimble.core.infrastructure.identity.controller.frontend;

import eu.nimble.core.infrastructure.identity.entity.dto.CompanySettings;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.utils.UblAdapter;
import eu.nimble.core.infrastructure.identity.utils.UblUtils;
import eu.nimble.service.model.ubl.commonaggregatecomponents.AddressType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Created by Johannes Innerbichler on 04/07/17.
 */
@RestController
@RequestMapping("/company-settings")
@Api(value = "company-settings", description = "API for handling settings of companies.")
public class CompanySettingsController {

    private static final Logger logger = LoggerFactory.getLogger(CompanySettingsController.class);

    @Autowired
    private PartyRepository partyRepository;

    @ApiOperation(value = "Retrieve company settings", response = CompanySettings.class)
    @RequestMapping(value = "/{companyID}", produces = {"application/json"}, method = RequestMethod.GET)
    ResponseEntity<CompanySettings> getSettings(
            @ApiParam(value = "Id of company to retrieve settings from.", required = true) @PathVariable Long companyID) {

        // search relevant parties
        Optional<PartyType> party = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (party.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        logger.debug("Returning requested settings for party with Id {}", party.get().getHjid());

        CompanySettings settings = UblAdapter.adaptCompanySettings(party.get());
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @ApiOperation(value = "Change company settings")
    @RequestMapping(value = "/{companyID}", consumes = {"application/json"}, method = RequestMethod.PUT)
    ResponseEntity<CompanySettings> setSettings(
            @ApiParam(value = "Id of company to change settings from.", required = true) @PathVariable Long companyID,
            @ApiParam(value = "Settings to update.", required = true) @RequestBody CompanySettings newSettings) {

        // search relevant parties
        Optional<PartyType> partyOptional = partyRepository.findByHjid(companyID).stream().findFirst();

        // check if party was found
        if (partyOptional.isPresent() == false) {
            logger.info("Requested party with Id {} not found", companyID);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        PartyType party = partyOptional.get();
        logger.debug("Changing settings for party with Id {}", party.getHjid());

        // set delivery terms
        DeliveryTermsType deliveryTerms = UblAdapter.adaptDeliveryTerms(newSettings.getDeliveryTerms());
        party.setDeliveryTerms(UblUtils.toModifyableList(deliveryTerms));

        // set payment means
        PaymentMeansType paymentMeansType = UblAdapter.adaptPaymentMeans(newSettings.getPaymentMeans());
        party.setPaymentMeans(UblUtils.toModifyableList(paymentMeansType));

        // set address
        AddressType companyAddress = UblAdapter.adaptAddress(newSettings.getAddress());
        party.setPostalAddress(companyAddress);

        partyRepository.save(party);

        return new ResponseEntity<>(newSettings, HttpStatus.ACCEPTED);
    }
}
