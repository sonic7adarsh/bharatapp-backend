package com.bharatshop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AvailabilityController {
    private static final Logger log = LoggerFactory.getLogger(AvailabilityController.class);

    @GetMapping("/availability")
    public ResponseEntity<?> availability(
            @RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
            @RequestParam(value = "storeId", required = false) String storeId,
            @RequestParam(value = "roomId", required = false) String roomId,
            @RequestParam("checkIn") String checkIn,
            @RequestParam("checkOut") String checkOut,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "roomsGuests", required = false) String roomsGuestsParam
    ) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate inDate, outDate;
        try {
            inDate = LocalDate.parse(checkIn, fmt);
            outDate = LocalDate.parse(checkOut, fmt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "code", "VALIDATION_ERROR",
                    "message", "Invalid date format. Use YYYY-MM-DD for checkIn/checkOut"
            ));
        }

        long nights = ChronoUnit.DAYS.between(inDate, outDate);
        if (nights <= 0) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "reason", "INVALID_DATES",
                    "nights", 0
            ));
        }

        // Parse roomsGuests as comma-separated counts per room; fallback to single room using guests
        List<Integer> roomsGuests = new ArrayList<>();
        if (roomsGuestsParam != null && !roomsGuestsParam.isBlank()) {
            for (String s : roomsGuestsParam.split(",")) {
                try { roomsGuests.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            }
        }
        if (roomsGuests.isEmpty()) {
            int g = guests != null ? guests : 1;
            roomsGuests.add(Math.max(g, 1));
        }

        int rooms = roomsGuests.size();
        int perRoomMax = 3; // default capacity per room
        boolean extraMattressAllowed = true;
        int mattressFeePerNight = 300;

        int extraMattressCount = 0;
        for (int g : roomsGuests) {
            if (g > perRoomMax) extraMattressCount += (g - perRoomMax);
        }

        // Simple base pricing model; can be replaced with real rate-plans later
        int basePricePerRoomPerNight = 1500;
        double surchargeRate = includesWeekend(inDate, outDate) ? 0.20 : 0.0;

        int baseSubtotal = (int) (rooms * nights * basePricePerRoomPerNight);
        int surcharge = (int) Math.round(baseSubtotal * surchargeRate);
        int mattressSubtotal = (int) (extraMattressCount * nights * mattressFeePerNight);
        int subtotal = baseSubtotal + surcharge + mattressSubtotal;
        int taxes = (int) Math.round(subtotal * 0.12); // 12% tax
        int fees = 50; // flat platform fees
        int total = subtotal + taxes + fees;

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("available", true);
        resp.put("nights", (int) nights);
        resp.put("rooms", rooms);
        resp.put("perRoomMax", perRoomMax);
        resp.put("extraMattressAllowed", extraMattressAllowed);
        resp.put("extraMattressCount", extraMattressCount);
        resp.put("subtotal", subtotal);
        resp.put("taxes", taxes);
        resp.put("fees", fees);
        resp.put("total", total);
        resp.put("surchargeRate", surchargeRate);
        resp.put("mattressFeePerNight", mattressFeePerNight);

        log.info("Availability: tenant={} storeId={} roomId={} in={} out={} roomsGuests={} respTotal={}",
                tenant, storeId, roomId, checkIn, checkOut, roomsGuests, total);
        return ResponseEntity.ok(resp);
    }

    private boolean includesWeekend(LocalDate start, LocalDate end) {
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) return true;
        }
        return false;
    }
}