package eu.nimble.core.infrastructure.identity.controller;

import eu.nimble.core.infrastructure.identity.repository.DeliveryTermsRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery-terms")
public class DeliveryTermsController {

    @Autowired
    private DeliveryTermsRepository deliveryRepo;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<DeliveryTermsType> getTerm(@PathVariable long id) {
        DeliveryTermsType terms = deliveryRepo.findOne(id);

        if (terms != null)
            return new ResponseEntity<>(terms, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<?> updateTerms(@RequestBody DeliveryTermsType terms) {

        if( terms.getID().getValue().equals(terms.getHjid().toString()) == false)
            return new ResponseEntity<>("Ids of terms do not match", HttpStatus.CONFLICT);

        if (deliveryRepo.exists(terms.getHjid()) == false)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(deliveryRepo.save(terms), HttpStatus.ACCEPTED);
    }
}