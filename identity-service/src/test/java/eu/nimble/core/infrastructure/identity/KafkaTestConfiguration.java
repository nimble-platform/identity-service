package eu.nimble.core.infrastructure.identity;

import eu.nimble.core.infrastructure.identity.messaging.KafkaReceiver;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * Created by Johannes Innerbichler on 09.10.18.
 */
@Profile("test")
@TestConfiguration
public class KafkaTestConfiguration {
    @Bean
    @Primary
    public KafkaReceiver receiverResolver() {
        return Mockito.mock(KafkaReceiver.class);
    }
}
