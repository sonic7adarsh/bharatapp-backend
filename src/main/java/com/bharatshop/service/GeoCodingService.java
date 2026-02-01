package com.bharatshop.service;

import com.bharatshop.dto.location.Address;
import com.bharatshop.dto.location.LocationDto;
import com.bharatshop.dto.location.NominatimResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class GeoCodingService {

    private final RestTemplate restTemplate;

    public GeoCodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LocationDto reverseGeocode(double lat, double lng) {
        // Updated URL with User-Agent requirement handled via headers or just standard call
        // Nominatim requires a User-Agent identifying the application
        String url = String.format(
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&zoom=18&addressdetails=1",
            lat, lng
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "bharatshop/1.0 (contact@bharatshop.com)");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NominatimResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, NominatimResponse.class);

            NominatimResponse body = response.getBody();
            if (body == null || body.getAddress() == null) return null;

            Address a = body.getAddress();

            return new LocationDto(
                firstNonNull(a.getSuburb(), a.getNeighbourhood(), a.getResidential()),
                a.getCity() != null ? a.getCity() : a.getTown(),
                a.getState(),
                a.getPostcode()
            );
        } catch (Exception e) {
            // Log error or return null/empty
            e.printStackTrace();
            return null;
        }
    }

    public List<?> search(String q) {
        String url = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=5&q="
            + URLEncoder.encode(q, StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "bharatshop/1.0 (contact@bharatshop.com)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Using List.class as per user instruction (returns List of LinkedHashMap usually)
        return restTemplate.exchange(url, HttpMethod.GET, entity, List.class).getBody();
    }

    private String firstNonNull(String... values) {
        for (String v : values) if (v != null) return v;
        return null;
    }
}
