package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "catalogue-service", url = "${nimble.catalogue-service.url}", fallback = CatalogueServiceClientFallback.class)
public interface CatalogueServiceClient {

    @RequestMapping(method = RequestMethod.POST, value = "/admin/index-catalogues")
    void indexAllCatalogues(@RequestParam(value = "partyId") String partyId,
                            @RequestHeader(value = "Authorization", required = true) String bearerToken);

}
