package eu.nimble.core.infrastructure.identity.messaging;

import eu.nimble.core.infrastructure.identity.config.KafkaConfig.AuthorizedCompanyUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by Johannes Innerbichler on 27.09.18.
 */

@Component
public class KafkaSender {

    @Value("${nimble.kafka.topics.companyUpdates}")
    private String companyUpdatesTopic;

    @Value("${nimble.kafka.topics.ratingsUpdates}")
    private String ratingsUpdatesTopic;

    @Autowired
    private KafkaTemplate<String, AuthorizedCompanyUpdate> kafkaTemplate;

    public void broadcastCompanyUpdate(String companyID, String accessToken) {
        accessToken = accessToken.replace("Bearer ", "");
        AuthorizedCompanyUpdate update = new AuthorizedCompanyUpdate(companyID, accessToken);
        kafkaTemplate.send(companyUpdatesTopic, update);
        System.out.println("Message: " + update + " sent to topic: " + companyUpdatesTopic);
    }

    public void broadcastRatingsUpdate(String companyId, String accessToken) {
        accessToken = accessToken.replace("Bearer ", "");
        AuthorizedCompanyUpdate update = new AuthorizedCompanyUpdate(companyId, accessToken);
        kafkaTemplate.send(ratingsUpdatesTopic, update);
        System.out.println("Message: " + update + " sent to topic: " + ratingsUpdatesTopic);
    }
}
