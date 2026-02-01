package com.bharatshop.devtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "devtools.tenant.inspector.enabled", havingValue = "true", matchIfMissing = true)
public class TenantDataInspector implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TenantDataInspector.class);
    private final JdbcTemplate jdbc;

    public TenantDataInspector(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<String> storeTenants = jdbc.query("SELECT DISTINCT tenant_id FROM stores", (rs, rowNum) -> rs.getString(1));
            List<String> productTenants = jdbc.query("SELECT DISTINCT tenant_id FROM products", (rs, rowNum) -> rs.getString(1));

            Integer storeCountTenantA = jdbc.queryForObject("SELECT COUNT(*) FROM stores WHERE tenant_id = ?", Integer.class, "tenantA");
            Integer productCountTenantA = jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE tenant_id = ?", Integer.class, "tenantA");

            log.info("TENANT CHECK: DISTINCT store tenants={} DISTINCT product tenants={} ", storeTenants, productTenants);
            log.info("TENANT CHECK: COUNT(stores, tenantA)={} COUNT(products, tenantA)={}", storeCountTenantA, productCountTenantA);
        } catch (Exception e) {
            log.warn("TENANT CHECK: failed to query tenant data", e);
        }
    }
}