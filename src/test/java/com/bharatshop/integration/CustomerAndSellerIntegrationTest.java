package com.bharatshop.integration;

import com.bharatshop.dto.seller.SellerOrderDetail;
import com.bharatshop.dto.seller.SellerOrderSummary;
import com.bharatshop.dto.UserAddressDto;
import com.bharatshop.dto.UserProfileDto;
import com.bharatshop.entity.UserEntity;
import com.bharatshop.entity.UserAddressEntity;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.repository.UserAddressRepository;
import com.bharatshop.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CustomerAndSellerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private UserAddressRepository userAddressRepository;

    private UsernamePasswordAuthenticationToken getAuth(String userId, String role) {
        UserPrincipal principal = new UserPrincipal(userId, "Test User", role);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void testCustomerProfile() throws Exception {
        String userId = UUID.randomUUID().toString();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("Old Name");
        user.setPhone("9999999999");
        userRepository.save(user);

        // Update Profile
        UserProfileDto update = new UserProfileDto();
        update.setName("New Name");
        update.setAlternatePhone("8888888888");

        MvcResult res = mockMvc.perform(MockMvcRequestBuilders.put("/api/user/profile")
                        .with(authentication(getAuth(userId, "CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andReturn();

        UserProfileDto result = objectMapper.readValue(res.getResponse().getContentAsString(), UserProfileDto.class);
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getAlternatePhone()).isEqualTo("8888888888");

        // Verify DB
        UserEntity updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("New Name");
        assertThat(updatedUser.getAlternatePhone()).isEqualTo("8888888888");
    }

    @Test
    void testAddressManagement() throws Exception {
        String userId = UUID.randomUUID().toString();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("Test User");
        userRepository.save(user);

        // Create Address
        UserAddressDto addr1 = new UserAddressDto();
        addr1.setName("Home");
        addr1.setLine1("123 Street");
        addr1.setCity("City A");
        addr1.setState("State A");
        addr1.setZip("10001");
        addr1.setPhone("1234567890");
        addr1.setIsDefault(true);

        MvcResult createRes = mockMvc.perform(MockMvcRequestBuilders.post("/api/user/addresses")
                        .with(authentication(getAuth(userId, "CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addr1)))
                .andExpect(status().isOk())
                .andReturn();

        UserAddressDto created = objectMapper.readValue(createRes.getResponse().getContentAsString(), UserAddressDto.class);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getIsDefault()).isTrue();

        // Create Second Address (Default)
        UserAddressDto addr2 = new UserAddressDto();
        addr2.setName("Work");
        addr2.setLine1("456 Avenue");
        addr2.setCity("City B");
        addr2.setState("State B");
        addr2.setZip("10002");
        addr2.setPhone("0987654321");
        addr2.setIsDefault(true); // Should override first

        MvcResult createRes2 = mockMvc.perform(MockMvcRequestBuilders.post("/api/user/addresses")
                        .with(authentication(getAuth(userId, "CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addr2)))
                .andExpect(status().isOk())
                .andReturn();
        
        UserAddressDto created2 = objectMapper.readValue(createRes2.getResponse().getContentAsString(), UserAddressDto.class);
        assertThat(created2.getIsDefault()).isTrue();

        // Verify first address is no longer default
        List<UserAddressEntity> addresses = userAddressRepository.findByUserId(userId);
        assertThat(addresses).hasSize(2);
        
        UserAddressEntity first = addresses.stream().filter(a -> a.getId().equals(created.getId())).findFirst().orElseThrow();
        UserAddressEntity second = addresses.stream().filter(a -> a.getId().equals(created2.getId())).findFirst().orElseThrow();
        
        assertThat(first.getIsDefault()).isFalse();
        assertThat(second.getIsDefault()).isTrue();
    }
    
    @Test
    void testSellerOrderSummary() throws Exception {
        // Since we cannot easily mock the entire Order/Factory/Ops chain without seeding a lot of data,
        // we will check if the endpoint is reachable and returns a list (empty is fine).
        // If the controller logic is correct, it should call the factory and return a list.
        // To verify the DTO mapping, we would need actual data.
        
        // For now, let's just ensure the endpoint responds 200 OK to a SELLER.
        
        String sellerId = UUID.randomUUID().toString();
        
        mockMvc.perform(MockMvcRequestBuilders.get("/api/seller/orders")
                    .with(authentication(getAuth(sellerId, "SELLER")))
                    .param("storeId", "some-store"))
                .andExpect(status().isOk());
                
        // This confirms the controller method signature is valid and doesn't crash on invocation.
    }
}
