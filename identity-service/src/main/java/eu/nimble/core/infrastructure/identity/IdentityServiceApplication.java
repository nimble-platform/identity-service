package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.service.model.ubl.commonaggregatecomponents.*;
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
import org.springframework.context.annotation.FilterType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.net.URISyntaxException;

@ComponentScan
@Configuration
@EnableDiscoveryClient
@SpringBootApplication
//@EntityScan({"eu.nimble.service.model", "eu.nimble.core.infrastructure.identity.entity"})
@EntityScan(basePackageClasses = {UaaUser.class, PartyType.class, PersonType.class, ActivityDataLineType.class})
@EnableAutoConfiguration
public class IdentityServiceApplication extends SpringBootServletInitializer {

    @Value("${nimble.cors_enabled}")
    private String corsEnabled;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                if( corsEnabled.equals("true"))
                    registry.addMapping("/*").allowedOrigins("*");
            }
        };
    }

    public static void main(String[] args) throws URISyntaxException {
        new SpringApplicationBuilder(IdentityServiceApplication.class).web(true).run(args);
    }
}