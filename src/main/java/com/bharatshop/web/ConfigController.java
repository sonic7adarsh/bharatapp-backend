package com.bharatshop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConfigController {
    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    @Value("${app.orders.acceptanceWindowMinutes:15}")
    private int acceptanceWindowMinutes;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        log.info("Config requested: tenant={} acceptanceWindowMinutes={}", tenant, acceptanceWindowMinutes);
        return ResponseEntity.ok(Map.of(
                "sellerResponseWindowMinutes", acceptanceWindowMinutes
        ));
    }
}