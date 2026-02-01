package com.bharatshop.web;

import com.bharatshop.dto.location.LocationDto;
import com.bharatshop.service.GeoCodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    private final GeoCodingService geoCodingService;

    public LocationController(GeoCodingService geoCodingService) {
        this.geoCodingService = geoCodingService;
    }

    @GetMapping("/reverse")
    public ResponseEntity<LocationDto> reverseGeocode(@RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(geoCodingService.reverseGeocode(lat, lng));
    }

    @GetMapping("/search")
    public ResponseEntity<List<?>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(geoCodingService.search(query));
    }
}
