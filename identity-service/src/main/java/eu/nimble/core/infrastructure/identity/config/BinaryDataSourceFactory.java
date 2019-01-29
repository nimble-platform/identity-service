package eu.nimble.core.infrastructure.identity.config;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Created by suat on 22-Nov-18.
 */
@Component
public class BinaryDataSourceFactory {

    private final Logger logger = LoggerFactory.getLogger(BinaryDataSourceFactory.class);

    @Autowired
    private Environment environment;

    public DataSource createDatasource(String dataSourceName) {
        logger.info("Creating datasource: url={}, user={}",
                environment.getProperty("spring.datasource." + dataSourceName + ".url"),
                environment.getProperty("spring.datasource." + dataSourceName + ".username"));

        javax.sql.DataSource ds = DataSourceBuilder.create()
                .url(environment.getProperty("spring.datasource." + dataSourceName + ".url"))
                .username(environment.getProperty("spring.datasource." + dataSourceName + ".username"))
                .password(environment.getProperty("spring.datasource." + dataSourceName + ".password"))
                .driverClassName(environment.getProperty("spring.datasource." + dataSourceName + ".driverClassName"))
                .build();

        // Assume we make use of Apache Tomcat connection pooling (default in Spring Boot)
        org.apache.tomcat.jdbc.pool.DataSource tds = (org.apache.tomcat.jdbc.pool.DataSource) ds;
        tds.setInitialSize(Integer.valueOf(environment.getProperty("spring.datasource.initial-size")));
        tds.setTestWhileIdle(Boolean.valueOf(environment.getProperty("spring.datasource.test-while-idle").toUpperCase()));
        tds.setTimeBetweenEvictionRunsMillis(Integer.valueOf(environment.getProperty("spring.datasource.time-between-eviction-runs-millis")));
        tds.setMinEvictableIdleTimeMillis(Integer.valueOf(environment.getProperty("spring.datasource.min-evictable-idle-time-millis")));
        tds.setMaxActive(Integer.valueOf(environment.getProperty("spring.datasource.max-active")));
        tds.setMaxIdle(Integer.valueOf(environment.getProperty("spring.datasource.max-idle")));
        tds.setMinIdle(Integer.valueOf(environment.getProperty("spring.datasource.min-idle")));

        return tds;
    }
}
