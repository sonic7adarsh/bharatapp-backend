package com.bharatshop.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminEndpointsIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private com.bharatshop.repository.OrderRepository orderRepository;
    @Autowired private com.bharatshop.repository.RiderRepository riderRepository;
    @Autowired private com.bharatshop.repository.OrderDeliveryRepository orderDeliveryRepository;

    private final String tenantA = "tenant-a";
    private final String tenantB = "tenant-b";

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void tenantHeaderIsRequired() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "name", "Zone X",
                "type", "radius",
                "centerLat", 12.9,
                "centerLng", 77.5,
                "radiusMeters", 200
        ));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "buyer", roles = {"CUSTOMER"})
    void rbacEnforcedForAdminZones() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "name", "Zone Y",
                "type", "radius",
                "centerLat", 13.0,
                "centerLng", 77.6,
                "radiusMeters", 250
        ));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/zones")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminZoneCrudTenantIsolation() throws Exception {
        // Create a zone in tenant A
        String payloadA = objectMapper.writeValueAsString(Map.of(
                "name", "Zone A1",
                "type", "radius",
                "centerLat", 12.91,
                "centerLng", 77.51,
                "radiusMeters", 200
        ));
        MvcResult createResA = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/zones")
                .header("X-Tenant-Domain", tenantA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadA))
            .andExpect(status().isOk())
            .andReturn();
        Map<?,?> createdA = objectMapper.readValue(createResA.getResponse().getContentAsString(), Map.class);
        String zoneIdA = String.valueOf(createdA.get("zoneId"));
        assertThat(zoneIdA).isNotBlank();

        // List zones in tenant B should not include zone from tenant A
        MvcResult listB = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/zones")
                        .header("X-Tenant-Domain", tenantB))
                .andExpect(status().isOk())
                .andReturn();
        String jsonB = listB.getResponse().getContentAsString();
        assertThat(jsonB).doesNotContain(zoneIdA);

        // Update zone in tenant A
        String updatePayload = objectMapper.writeValueAsString(Map.of(
                "name", "Zone A1 Updated",
                "type", "radius",
                "centerLat", 12.92,
                "centerLng", 77.52,
                "radiusMeters", 250
        ));
        mockMvc.perform(MockMvcRequestBuilders.put("/api/admin/zones/" + zoneIdA)
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk());

        // Delete zone in tenant A
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/zones/" + zoneIdA)
                        .header("X-Tenant-Domain", tenantA))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminLogisticsAssignUnassignObeysStateAndCrossTenant() throws Exception {
        // Seed order READY and rider ONLINE via repositories would be ideal; for MVP, we expect controller/service validation to enforce rules
        // Attempt assign with missing or invalid state will return errors; here we just assert RBAC + tenant header paths are wired end-to-end
        String assignPayload = objectMapper.writeValueAsString(Map.of(
                "orderId", "nonexistent-order",
                "storeId", "nonexistent-store",
                "riderId", "nonexistent-rider"
        ));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isNotFound());

        // Cross-tenant check: with a different tenant header, payload still subject to scoping; using tenantB for unassign
        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", "nonexistent-delivery"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .header("X-Tenant-Domain", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "buyer", roles = {"CUSTOMER"})
    void logisticsEndpointsEnforceAdminRole() throws Exception {
        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", "some-order"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isForbidden());

        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", "some-delivery"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .header("X-Tenant-Domain", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAssignReturns409WhenNoRidersAvailable() throws Exception {
        // Seed a READY order in tenantA; no riders seeded so assignment should return 409
        com.bharatshop.entity.OrderEntity order = new com.bharatshop.entity.OrderEntity();
        order.setId(java.util.UUID.randomUUID().toString());
        order.setTenantId(tenantA);
        order.setStoreId("store-no-riders");
        order.setStatus("ready");
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        orderRepository.save(order);

        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", order.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAssignUnassignPositivePath() throws Exception {
        // Seed ONLINE rider in tenantA
        com.bharatshop.entity.RiderEntity rider = new com.bharatshop.entity.RiderEntity();
        rider.setId(java.util.UUID.randomUUID().toString());
        rider.setTenantId(tenantA);
        rider.setName("Rider A");
        rider.setStatus("ONLINE");
        riderRepository.save(rider);

        // Seed READY order in tenantA
        com.bharatshop.entity.OrderEntity order = new com.bharatshop.entity.OrderEntity();
        order.setId(java.util.UUID.randomUUID().toString());
        order.setTenantId(tenantA);
        order.setStoreId("store-assign-positive");
        order.setStatus("ready");
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        orderRepository.save(order);

        // Assign via admin endpoint
        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", order.getId()));
        MvcResult assignRes = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isOk())
                .andReturn();
        Map<?,?> assignBody = objectMapper.readValue(assignRes.getResponse().getContentAsString(), Map.class);
        String deliveryId = String.valueOf(assignBody.get("deliveryId"));
        String riderIdAssigned = String.valueOf(assignBody.get("riderId"));
        assertThat(deliveryId).isNotBlank();
        assertThat(riderIdAssigned).isEqualTo(rider.getId());

        // Verify delivery status
        com.bharatshop.entity.OrderDeliveryEntity delivery = orderDeliveryRepository.findByTenantIdAndId(tenantA, deliveryId)
                .orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("RIDER_ASSIGNED");

        // Unassign via admin endpoint
        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", deliveryId));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isOk());

        // Verify delivery reset and rider ONLINE
        com.bharatshop.entity.OrderDeliveryEntity refreshed = orderDeliveryRepository.findByTenantIdAndId(tenantA, deliveryId)
                .orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("PENDING");
        assertThat(refreshed.getRiderId()).isNull();
        com.bharatshop.entity.RiderEntity riderRef = riderRepository.findById(rider.getId()).orElseThrow();
        assertThat(riderRef.getStatus()).isEqualTo("ONLINE");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void tenantCrossingAssignUnassignRejected() throws Exception {
        // Seed ONLINE rider and READY order in tenantA
        com.bharatshop.entity.RiderEntity rider = new com.bharatshop.entity.RiderEntity();
        rider.setId(java.util.UUID.randomUUID().toString());
        rider.setTenantId(tenantA);
        rider.setName("Rider A2");
        rider.setStatus("ONLINE");
        riderRepository.save(rider);

        com.bharatshop.entity.OrderEntity order = new com.bharatshop.entity.OrderEntity();
        order.setId(java.util.UUID.randomUUID().toString());
        order.setTenantId(tenantA);
        order.setStoreId("store-assign-tenantA");
        order.setStatus("ready");
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        orderRepository.save(order);

        // Attempt assign with tenantB header should 404 (ORDER_NOT_FOUND in tenantB)
        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", order.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isNotFound());

        // Assign correctly under tenantA to produce a delivery
        MvcResult assignRes = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .header("X-Tenant-Domain", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isOk())
                .andReturn();
        Map<?,?> assignBody = objectMapper.readValue(assignRes.getResponse().getContentAsString(), Map.class);
        String deliveryId = String.valueOf(assignBody.get("deliveryId"));
        assertThat(deliveryId).isNotBlank();

        // Attempt unassign with tenantB header should 404 (DELIVERY_NOT_FOUND in tenantB)
        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", deliveryId));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .header("X-Tenant-Domain", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isNotFound());
    }
}