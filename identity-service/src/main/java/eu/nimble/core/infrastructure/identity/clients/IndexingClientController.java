package eu.nimble.core.infrastructure.identity.clients;

import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@PropertySource("classpath:bootstrap.yml")
public class IndexingClientController {

    private IndexingClient nimbleIndexClient;

    private IndexingClient federatedIndexClient;

    private List<IndexingClient> clients;

    @Value("${nimble.indexing.url}")
    private String nimbleIndexUrl;

    @Value("${federated-index-enabled}")
    private boolean federatedIndexEnabled;

    @Value("${nimble.indexing.federated-index-url}")
    private String federatedIndexUrl;

    public IndexingClientController() {

    }

    public IndexingClient getNimbleIndexClient() {
        if (nimbleIndexClient == null) {
            nimbleIndexClient = createIndexingClient(nimbleIndexUrl);
        }
        return nimbleIndexClient;
    }


    public IndexingClient getFederatedIndexClient() {
        if (federatedIndexEnabled && federatedIndexClient == null) {
            federatedIndexClient = createIndexingClient(federatedIndexUrl);
        }
        return federatedIndexClient;
    }

    public List<IndexingClient> getClients() {
        if (clients == null) {
            clients = new ArrayList<IndexingClient>();
            clients.add(getNimbleIndexClient());
            if (federatedIndexEnabled) {
                clients.add(getFederatedIndexClient());
            }
        }
        return clients;
    }

    private IndexingClient createIndexingClient(String url) {
        return Feign.builder().contract(new SpringMvcContract())
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .target(IndexingClient.class, url);
    }

}
