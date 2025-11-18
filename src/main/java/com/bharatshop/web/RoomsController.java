package com.bharatshop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bharatshop.repository.RoomRepository;
import com.bharatshop.entity.RoomEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront/rooms")
public class RoomsController {
    private static final Logger log = LoggerFactory.getLogger(RoomsController.class);
    private final RoomRepository roomRepository;

    public RoomsController(RoomRepository roomRepository) { this.roomRepository = roomRepository; }

    @PostMapping("/availability")
    public ResponseEntity<?> availability(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant,
                                          @RequestBody Map<String, Object> body) {
        try {
            String roomId = body != null ? String.valueOf(body.get("roomId")) : null;
            String checkIn = body != null ? String.valueOf(body.get("checkIn")) : null;
            String checkOut = body != null ? String.valueOf(body.get("checkOut")) : null;
            Integer guests = body != null && body.get("guests") instanceof Number ? ((Number) body.get("guests")).intValue() : null;

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate inDate = LocalDate.parse(checkIn, fmt);
            LocalDate outDate = LocalDate.parse(checkOut, fmt);
            if (!inDate.isBefore(outDate)) {
                return ResponseEntity.ok(Map.of("available", false, "reason", "INVALID_DATES"));
            }

            Integer perRoomMax = null;
            if (roomId != null && !roomId.isBlank()) {
                try {
                    RoomEntity room = roomRepository.findById(roomId).orElse(null);
                    if (room != null) perRoomMax = room.getPerRoomMax();
                } catch (Exception ignored) {}
            }
            if (perRoomMax == null) perRoomMax = 3;
            if (guests != null && guests > perRoomMax + 1) { // simple rule: allow 1 extra mattress
                return ResponseEntity.ok(Map.of("available", false, "reason", "CAPACITY_EXCEEDED"));
            }

            log.info("Rooms availability check: tenant={} roomId={} in={} out={} guests={}", tenant, roomId, checkIn, checkOut, guests);
            return ResponseEntity.ok(Map.of("available", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("available", false, "reason", "BAD_REQUEST"));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String storeId) {
        if (storeId == null || storeId.isBlank()) return ResponseEntity.badRequest().body(Map.of("message", "storeId is required"));
        List<RoomEntity> rooms = roomRepository.findByStoreIdAndActiveIsTrueOrderByCreatedAtDesc(storeId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return roomRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Room not found")));
    }
}