package eu.nimble.core.infrastructure.identity.controller;

import com.google.common.collect.Lists;
import eu.nimble.core.infrastructure.identity.repository.PaymentMeansRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Johannes Innerbichler on 28/06/17.
 */
@RequestMapping(value = "/payment-means", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(basePath = "/payment-means", value = "Payment Means", description = "Operations with Payment Means", produces = "application/json")
@RestController
public class PaymentMeansController {

    @Autowired
    private PaymentMeansRepository paymentMeansRepository;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "Get payment means of party", notes = "Get payment means of party with id.", response = PaymentMeansType.class, tags = {})
    public ResponseEntity<PaymentMeansType> getMean(
            @ApiParam(value = "Id of party to retrieve payment means from.", required = true) @PathVariable long id) {
        PaymentMeansType means = paymentMeansRepository.findOne(id);

        if (means != null)
            return new ResponseEntity<>(means, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
