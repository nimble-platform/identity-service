package eu.nimble.core.infrastructure.identity.controller;

import com.google.common.collect.Lists;
import eu.nimble.core.infrastructure.identity.repository.DeliveryTermsRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/delivery-terms")
public class DeliveryTermsController {

    @Autowired
    private DeliveryTermsRepository deliveryRepo;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Collection<DeliveryTermsType>> getTerms() {
        Collection<DeliveryTermsType> terms = Lists.newArrayList(deliveryRepo.findAll().iterator());
        return new ResponseEntity<>(terms, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<DeliveryTermsType> getTerm(@PathVariable long id) {
        DeliveryTermsType terms = deliveryRepo.findOne(id);

        if (terms != null)
            return new ResponseEntity<>(terms, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> addTerms(@RequestBody DeliveryTermsType terms) {
        return new ResponseEntity<>(deliveryRepo.save(terms), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<?> updateTerms(@RequestBody DeliveryTermsType terms) {
        return new ResponseEntity<>(deliveryRepo.save(terms), HttpStatus.ACCEPTED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteTerms(@PathVariable long id) {
        deliveryRepo.delete(id);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}