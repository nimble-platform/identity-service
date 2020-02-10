package eu.nimble.core.infrastructure.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:bootstrap.yml")
public class FederationConfig {

    @Value("${nimble.federation-instance-id}")
    private String federationInstanceId;

    public String getFederationInstanceId() {
        return federationInstanceId;
    }

    public void setFederationInstanceId(String federationInstanceId) {
        this.federationInstanceId = federationInstanceId;
    }
}
