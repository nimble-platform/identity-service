package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.entity.UaaUser;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PartyType;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import eu.nimble.service.model.ubl.commonbasiccomponents.CodeType;
import eu.nimble.utility.persistence.binary.BinaryContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.sql.DataSource;

@Configuration
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"eu.nimble.utility.persistence.binary", "eu.nimble.utility.persistence.initalizer", "eu.nimble.core.infrastructure.identity"})
@EnableJpaRepositories(basePackages = {"eu.nimble.core.infrastructure.identity"})
@EntityScan(basePackageClasses = {UaaUser.class, PartyType.class, PersonType.class, CodeType.class})
@EnableCaching
public class IdentityServiceApplication extends SpringBootServletInitializer {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceApplication.class);

    @Value("${nimble.corsEnabled}")
    private String corsEnabled;

    @Bean
    @Primary
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

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

    public static void main(String[] args) {
        new SpringApplicationBuilder(IdentityServiceApplication.class).web(true).run(args);
    }
}