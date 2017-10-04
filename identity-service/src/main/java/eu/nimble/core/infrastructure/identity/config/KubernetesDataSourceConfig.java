package eu.nimble.core.infrastructure.identity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Configuration
@Profile("kubernetes")
@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
public class KubernetesDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesDataSourceConfig.class);

    @Value("${nimble.db-credentials-json}")
    private String dbCredentialsJson;

    @Bean
    @Primary
    public DataSource getDataSource() {

        // parse JSON
        BasicJsonParser parser = new BasicJsonParser();
        String originalUrl = (String) parser.parseMap(this.dbCredentialsJson).get("uri");

        // construct data from 'postgres://username:password@host:port/database'
        Matcher matcher = Pattern.compile("^postgres://(.*?):(.*?)@").matcher(originalUrl);
        matcher.find();
        String username = matcher.group(1);
        String password = matcher.group(2);
        String url = "jdbc:postgresql://" + matcher.replaceAll("");
        String driverClass = "org.postgresql.Driver";

        logger.info("Setting datasource to {} (user: {})", url, username);
        return DataSourceBuilder.create().url(url).username(username).password(password).driverClassName(driverClass).build();
    }
}