package eu.nimble.core.infrastructure.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@ComponentScan
@Configuration
@EnableCircuitBreaker
@EnableAutoConfiguration
@EnableEurekaClient
@EnableFeignClients
@RestController
public class Application {

    @Autowired
    private UserRegistrationClient userClient;

    @Value("${spring.datasource.password}")
    private String password;

    @RequestMapping("/")
    public String home() {
        return userClient.getUser("0");
    }

    @RequestMapping("/user/{userId}")
    public String getUser(@PathVariable("userId") String userId) {
        return userClient.getUser(userId) + "SecretPW: " + password;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }
}

@FeignClient("user-registration")
interface UserRegistrationClient {
    @RequestMapping(method = RequestMethod.GET, value = "/user/{userId}")
    String getUser(@PathVariable("userId") String userId);
}