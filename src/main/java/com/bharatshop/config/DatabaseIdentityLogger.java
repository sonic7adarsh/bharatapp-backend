package com.bharatshop.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Component
public class DatabaseIdentityLogger {
    private static final Logger log = LoggerFactory.getLogger(DatabaseIdentityLogger.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment env;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void logDbIdentityAndValidate() throws Exception {
        // Log active profiles explicitly
        String[] profiles = env.getActiveProfiles();
        log.info("[PROFILES] active={}", String.join(",", profiles.length > 0 ? profiles : new String[]{"(none)"}));

        // Ensure exactly one DataSource bean exists
        String[] dsBeans = applicationContext.getBeanNamesForType(DataSource.class);
        if (dsBeans.length != 1) {
            throw new IllegalStateException("Expected exactly one DataSource bean, found=" + dsBeans.length);
        }

        // Validate DataSource is MySQL and log identity
        if (dataSource instanceof HikariDataSource hikari) {
            String jdbcUrl = hikari.getJdbcUrl();
            if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql:")) {
                throw new IllegalStateException("DataSource is not MySQL. jdbcUrl=" + jdbcUrl);
            }
        }

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String url = meta.getURL();
            String user = meta.getUserName();
            String catalog = conn.getCatalog(); // MySQL database name
            String schema = null;
            try { schema = conn.getSchema(); } catch (Throwable ignored) {}
            log.info("[DB] url={} schema={} user={} catalog={}", url, schema, user, catalog);
        } catch (Exception e) {
            // Fail startup if MySQL unreachable
            throw e;
        }
    }
}