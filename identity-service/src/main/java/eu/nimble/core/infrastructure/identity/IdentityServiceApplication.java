package eu.nimble.core.infrastructure.identity;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

@ComponentScan
@Configuration
@EnableDiscoveryClient
@SpringBootApplication
@EntityScan({"eu.nimble.service.model", "eu.nimble.core.infrastructure.identity.entity"})
@EnableAutoConfiguration
public class IdentityServiceApplication extends SpringBootServletInitializer {
    public static void main(String[] args) throws URISyntaxException {
        new SpringApplicationBuilder(IdentityServiceApplication.class).web(true).run(args);
    }
}