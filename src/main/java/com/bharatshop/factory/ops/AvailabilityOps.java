package com.bharatshop.factory.ops;

import com.bharatshop.domain.AvailabilityResponse;

public interface AvailabilityOps {
    AvailabilityResponse check(String storeId, String roomId, String checkIn, String checkOut, int guests);
}