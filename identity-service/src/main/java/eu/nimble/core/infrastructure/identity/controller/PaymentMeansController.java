package eu.nimble.core.infrastructure.identity.controller;

import com.google.common.collect.Lists;
import eu.nimble.core.infrastructure.identity.repository.PaymentMeansRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PaymentMeansType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * Created by Johannes Innerbichler on 28/06/17.
 */
@RestController
@RequestMapping("/payment-means")
public class PaymentMeansController {

    @Autowired
    private PaymentMeansRepository paymentMeansRepository;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<PaymentMeansType> getMean(@PathVariable long id) {
        PaymentMeansType means = paymentMeansRepository.findOne(id);

        if (means != null)
            return new ResponseEntity<>(means, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<?> updateMeans(@RequestBody PaymentMeansType means) {

        if (paymentMeansRepository.exists(means.getHjid()) == false)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(paymentMeansRepository.save(means), HttpStatus.ACCEPTED);
    }
}
