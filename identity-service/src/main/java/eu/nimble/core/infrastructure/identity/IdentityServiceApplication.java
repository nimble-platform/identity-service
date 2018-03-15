package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.service.model.ubl.commonaggregatecomponents.ActivityDataLineType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.URISyntaxException;

@ComponentScan
@Configuration
@EnableDiscoveryClient
@SpringBootApplication
@EntityScan(basePackageClasses = {UaaUser.class, PartyType.class, PersonType.class, ActivityDataLineType.class, CodeType.class})
@EnableAutoConfiguration
public class IdentityServiceApplication extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceApplication.class);

    @Value("${nimble.corsEnabled}")
    private String corsEnabled;

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if (corsEnabled.equals("true")) {
                    logger.info("Enabling CORS...");
                    registry.addMapping("/**").allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH");
                }
            }
        };
    }

    public static void main(String[] args) throws URISyntaxException {
        new SpringApplicationBuilder(IdentityServiceApplication.class).web(true).run(args);
    }
}