package eu.nimble.core.infrastructure.identity.utils;

import eu.nimble.core.infrastructure.identity.clients.IndexingClientController;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringBridge implements ApplicationContextAware {

    private static ApplicationContext applicationContext;
    @Autowired
    private IndexingClientController indexingClientController;

    public static SpringBridge getInstance() {
        return applicationContext.getBean(SpringBridge.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        SpringBridge.applicationContext = applicationContext;
    }

    public String getFederatedIndexPlatformName() {
        return indexingClientController.getFederatedIndexPlatformName();
    }

}