package eu.nimble.core.infrastructure.identity.clients;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "delegate-service", url = "${nimble.delegate-service.url}", fallback = DelegateServiceClientFallback.class)
public interface DelegateServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "/eureka/app-name")
    String getFederationId();

}