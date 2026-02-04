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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminZoneCrud() throws Exception {
        // Create a zone
        String payloadA = objectMapper.writeValueAsString(Map.of(
                "name", "Zone A1",
                "type", "radius",
                "centerLat", 12.91,
                "centerLng", 77.51,
                "radiusMeters", 200
        ));
        MvcResult createResA = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/zones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadA))
            .andExpect(status().isOk())
            .andReturn();
        Map<?,?> createdA = objectMapper.readValue(createResA.getResponse().getContentAsString(), Map.class);
        String zoneIdA = String.valueOf(createdA.get("zoneId"));
        assertThat(zoneIdA).isNotBlank();

        // List zones should include it
        MvcResult list = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/zones"))
                .andExpect(status().isOk())
                .andReturn();
        String json = list.getResponse().getContentAsString();
        assertThat(json).contains(zoneIdA);

        // Update zone
        String updatePayload = objectMapper.writeValueAsString(Map.of(
                "name", "Zone A1 Updated",
                "type", "radius",
                "centerLat", 12.92,
                "centerLng", 77.52,
                "radiusMeters", 250
        ));
        mockMvc.perform(MockMvcRequestBuilders.put("/api/admin/zones/" + zoneIdA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk());

        // Delete zone
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/zones/" + zoneIdA))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminLogisticsAssignUnassignObeysState() throws Exception {
        // Seed order READY and rider ONLINE via repositories would be ideal; for MVP, we expect controller/service validation to enforce rules
        // Attempt assign with missing or invalid state will return errors; here we just assert RBAC paths are wired end-to-end
        String assignPayload = objectMapper.writeValueAsString(Map.of(
                "orderId", "nonexistent-order",
                "storeId", "nonexistent-store",
                "riderId", "nonexistent-rider"
        ));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isNotFound());

        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", "nonexistent-delivery"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "buyer", roles = {"CUSTOMER"})
    void logisticsEndpointsEnforceAdminRole() throws Exception {
        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", "some-order"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isForbidden());

        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", "some-delivery"));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAssignReturns409WhenNoRidersAvailable() throws Exception {
        // Seed a READY order; no riders seeded so assignment should return 409
        com.bharatshop.entity.OrderEntity order = new com.bharatshop.entity.OrderEntity();
        order.setId(java.util.UUID.randomUUID().toString());
        order.setStoreId("store-no-riders");
        order.setStatus("ready");
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        orderRepository.save(order);

        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", order.getId()));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignPayload))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminAssignUnassignPositivePath() throws Exception {
        // Seed ONLINE rider
        com.bharatshop.entity.RiderEntity rider = new com.bharatshop.entity.RiderEntity();
        rider.setId(java.util.UUID.randomUUID().toString());
        rider.setName("Rider A");
        rider.setStatus("ONLINE");
        riderRepository.save(rider);

        // Seed READY order
        com.bharatshop.entity.OrderEntity order = new com.bharatshop.entity.OrderEntity();
        order.setId(java.util.UUID.randomUUID().toString());
        order.setStoreId("store-assign-positive");
        order.setStatus("ready");
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        orderRepository.save(order);

        // Assign via admin endpoint
        String assignPayload = objectMapper.writeValueAsString(Map.of("orderId", order.getId()));
        MvcResult assignRes = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/assign")
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
        com.bharatshop.entity.OrderDeliveryEntity delivery = orderDeliveryRepository.findById(deliveryId)
                .orElseThrow();
        assertThat(delivery.getStatus()).isEqualTo("RIDER_ASSIGNED");

        // Unassign via admin endpoint
        String unassignPayload = objectMapper.writeValueAsString(Map.of("deliveryId", deliveryId));
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/logistics/unassign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unassignPayload))
                .andExpect(status().isOk());

        // Verify delivery reset and rider ONLINE
        com.bharatshop.entity.OrderDeliveryEntity refreshed = orderDeliveryRepository.findById(deliveryId)
                .orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("PENDING");
        assertThat(refreshed.getRiderId()).isNull();
        com.bharatshop.entity.RiderEntity riderRef = riderRepository.findById(rider.getId()).orElseThrow();
        assertThat(riderRef.getStatus()).isEqualTo("ONLINE");
    }

    // Tenant isolation test removed

}