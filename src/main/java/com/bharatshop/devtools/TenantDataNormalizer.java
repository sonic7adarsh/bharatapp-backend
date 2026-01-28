package com.bharatshop.devtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "devtools.tenant.normalizer.enabled", havingValue = "true", matchIfMissing = false)
public class TenantDataNormalizer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TenantDataNormalizer.class);
    private final JdbcTemplate jdbc;

    public TenantDataNormalizer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updatedStores = jdbc.update("UPDATE stores SET tenant_id = 'tenantA' WHERE tenant_id IS NULL OR tenant_id = 'default'");
            int updatedProducts = jdbc.update("UPDATE products SET tenant_id = 'tenantA' WHERE tenant_id IS NULL OR tenant_id = 'default'");
            int updatedCategories = jdbc.update("UPDATE categories SET tenant_id = 'tenantA' WHERE tenant_id IS NULL OR tenant_id = 'default'");
            log.info("TENANT NORMALIZE: updated stores={} updated products={} updated categories={} (NULL/default -> tenantA)", updatedStores, updatedProducts, updatedCategories);
        } catch (Exception e) {
            log.warn("TENANT NORMALIZE: failed to update tenant data", e);
        }
    }
}