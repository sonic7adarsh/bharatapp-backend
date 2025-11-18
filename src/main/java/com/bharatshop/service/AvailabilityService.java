package com.bharatshop.service;

import com.bharatshop.domain.AvailabilityResponse;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {
    public AvailabilityResponse check(String storeId, String roomId, String checkIn, String checkOut, int guests) {
        AvailabilityResponse resp = new AvailabilityResponse();
        resp.setAvailable(true);
        resp.setNights(1);
        resp.setBase(1000);
        resp.setSurchargeRate(0.1);
        resp.setSubtotal(1100);
        resp.setTaxes(100);
        resp.setFees(50);
        resp.setTotal(1250);
        return resp;
    }
}