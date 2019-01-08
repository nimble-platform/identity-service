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
public class DataSourceFactory {

    private final Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

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
        tds.setInitialSize(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.initial-size")));
        tds.setTestWhileIdle(Boolean.valueOf(environment.getProperty("spring.datasource.tomcat.test-while-idle").toUpperCase()));
        tds.setTimeBetweenEvictionRunsMillis(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.time-between-eviction-runs-millis")));
        tds.setMinEvictableIdleTimeMillis(Integer.valueOf(environment.getProperty("spring.datasource.tomcat.min-evictable-idle-time-millis")));
        tds.setMaxActive(Integer.valueOf(environment.getProperty("spring.datasource.maxActive")));
        tds.setMaxIdle(Integer.valueOf(environment.getProperty("spring.datasource.maxIdle")));
        tds.setMinIdle(Integer.valueOf(environment.getProperty("spring.datasource.minIdle")));

        return tds;
    }
}