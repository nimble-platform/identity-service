package eu.nimble.core.infrastructure.identity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URISyntaxException;

@ComponentScan
@Configuration
@EnableAutoConfiguration
@EnableDiscoveryClient
@SpringBootApplication
@EntityScan({"eu.nimble.service.model"})
//@Import({springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration.class})
public class Application {
    public static void main(String[] args) throws URISyntaxException {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }
}