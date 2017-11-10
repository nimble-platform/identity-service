package eu.nimble.core.infrastructure.identity.controller.ubl;

import eu.nimble.core.infrastructure.identity.repository.DeliveryTermsRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.DeliveryTermsType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delivery-terms")
public class DeliveryTermsController {

    @Autowired
    private DeliveryTermsRepository deliveryRepo;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = {"application/json"})
    @ApiOperation(value = "Get delivery terms of party", notes = "Get delivery terms of party with id.", response = DeliveryTermsType.class, tags = {})
    public ResponseEntity<DeliveryTermsType> getTerm(
            @ApiParam(value = "Id of party to retrieve delivery terms from.", required = true) @PathVariable long id) {
        DeliveryTermsType terms = deliveryRepo.findOne(id);

        if (terms != null)
            return new ResponseEntity<>(terms, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}