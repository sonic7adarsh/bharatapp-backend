package com.bharatshop.policy;

import com.bharatshop.domain.Store;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class StoreAvailabilityPolicy {
    public Map<String, Object> availabilityError(Store store) {
        boolean disabled = Boolean.TRUE.equals(store.getOrderingDisabled());
        boolean closed = store.getStatus() != null && store.getStatus().equalsIgnoreCase("closed");
        boolean untilClosed = store.getClosedUntil() != null && Instant.now().isBefore(store.getClosedUntil());

        if (disabled || closed || untilClosed) {
            String reason = store.getClosedReason() != null ? store.getClosedReason() : "Store is closed. Please try later.";
            return Map.of(
                    "code", "STORE_CLOSED",
                    "message", reason,
                    "storeId", store.getId(),
                    "closedUntil", store.getClosedUntil()
            );
        }
        return null;
    }
}