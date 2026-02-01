package com.bharatshop.integration;

import com.bharatshop.entity.StoreEntity;
import com.bharatshop.entity.ProductEntity;
import com.bharatshop.repository.StoreRepository;
import com.bharatshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    public void setup() {
        productRepository.deleteAll();
        storeRepository.deleteAll();

        // Seed tenant A store and product
        StoreEntity storeA = new StoreEntity();
        storeA.setId("storeA");
        storeA.setName("Alpha Grocer");
        storeA.setArea("Sector 1");
        storeA.setCategory("grocery");
        storeA.setTenantId("tenantA");
        storeRepository.save(storeA);

        ProductEntity pA = new ProductEntity();
        pA.setId("prodA");
        pA.setName("Milk");
        pA.setPrice(50.0);
        pA.setCategory("dairy");
        pA.setStoreId("storeA");
        pA.setActive(true);
        pA.setTenantId("tenantA");
        productRepository.save(pA);

        // Seed tenant B store and product
        StoreEntity storeB = new StoreEntity();
        storeB.setId("storeB");
        storeB.setName("Beta Bazaar");
        storeB.setArea("Sector 2");
        storeB.setCategory("grocery");
        storeB.setTenantId("tenantB");
        storeRepository.save(storeB);

        ProductEntity pB = new ProductEntity();
        pB.setId("prodB");
        pB.setName("Bread");
        pB.setPrice(30.0);
        pB.setCategory("bakery");
        pB.setStoreId("storeB");
        pB.setActive(true);
        pB.setTenantId("tenantB");
        productRepository.save(pB);
    }

    @Test
    public void storesIsolationByTenant() throws Exception {
        // Tenant A sees only storeA
        mockMvc.perform(get("/api/stores")
                        .header("X-Tenant-Domain", "tenantA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("storeA"))
                .andExpect(jsonPath("$[0].name").value("Alpha Grocer"));

        // Tenant B sees only storeB
        mockMvc.perform(get("/api/stores")
                        .header("X-Tenant-Domain", "tenantB")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("storeB"))
                .andExpect(jsonPath("$[0].name").value("Beta Bazaar"));
    }

    @Test
    public void productsIsolationByTenant() throws Exception {
        // Tenant A fetches products for storeA, sees only prodA
        mockMvc.perform(get("/api/stores/storeA/products")
                        .header("X-Tenant-Domain", "tenantA")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prodA"))
                .andExpect(jsonPath("$[0].storeId").value("storeA"));

        // Tenant B cannot see prodA via storeA
        mockMvc.perform(get("/api/stores/storeA/products")
                        .header("X-Tenant-Domain", "tenantB")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // Tenant B fetches products for storeB, sees only prodB
        mockMvc.perform(get("/api/stores/storeB/products")
                        .header("X-Tenant-Domain", "tenantB")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("prodB"))
                .andExpect(jsonPath("$[0].storeId").value("storeB"));
    }
}