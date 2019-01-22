package eu.nimble.core.infrastructure.identity.system;


import eu.nimble.core.infrastructure.identity.entity.dto.statistics.PlatformIdentityStatistics;
import eu.nimble.core.infrastructure.identity.repository.PartyRepository;
import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(path = "/statistics")
@Api(value = "statistics", description = "Providing statistics of users and companies.")
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private PersonRepository personRepository;

    @ApiOperation(value = "Aggregate statistics of companies.", nickname = "getPlatformStats", response = PlatformIdentityStatistics.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "EPC codes registered"),
            @ApiResponse(code = 400, message = "Error while aggregating statistics.")})
    @RequestMapping(value = "/", produces = {"application/json"}, method = RequestMethod.GET)
    public ResponseEntity<?> getPlatformStatistics() {

        logger.info("Collecting platform statistics");

        // collect platform statistics
        long numParties = partyRepository.count();
        long numUsers = personRepository.count();
        PlatformIdentityStatistics statistics = new PlatformIdentityStatistics(numUsers, numParties);

        return ResponseEntity.ok(statistics);
    }

}
