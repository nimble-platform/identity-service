package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "business-process-service", url = "${nimble.business-process-service.url}", fallback = BusinessProcessServiceClientFallback.class)
public interface BusinessProcessServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/collaboration-groups/all-finished")
    public String checkAllCollaborationsFinished(@RequestParam(value = "partyId", required = true) String partyId,
                                                 @RequestHeader(value = "federationId", required = true) String federationId,
                                                 @RequestHeader(value = "Authorization", required = true) String bearerToken);

}