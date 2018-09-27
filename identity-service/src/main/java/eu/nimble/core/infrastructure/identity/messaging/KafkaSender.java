package eu.nimble.core.infrastructure.identity.messaging;

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

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void broadcastCompanyUpdate(String companyID) {
        publish(companyUpdatesTopic, companyID);
    }

    private void publish(String topic, String payload) {
        kafkaTemplate.send(topic, payload);
        System.out.println("Message: " + payload + " sent to topic: " + topic);
    }
}
