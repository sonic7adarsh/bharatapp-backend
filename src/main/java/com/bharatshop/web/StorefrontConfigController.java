package com.bharatshop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
public class StorefrontConfigController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontConfigController.class);

    // Payments config
    @Value("${payments.gateways:razorpay}")
    private String gateways;

    @Value("${payments.methods.online:true}")
    private boolean onlineEnabled;
    @Value("${payments.methods.cod:true}")
    private boolean codEnabled;
    @Value("${payments.methods.upi:true}")
    private boolean upiEnabled;
    @Value("${payments.methods.card:true}")
    private boolean cardEnabled;
    @Value("${payments.methods.wallet:false}")
    private boolean walletEnabled;

    @Value("${checkout.cod.minTotal:0}")
    private int codMinTotal;
    @Value("${checkout.cod.maxTotal:0}")
    private int codMaxTotal;



    // Checkout config
    @Value("${checkout.delivery.enabled:true}")
    private boolean deliverySlotsEnabled;
    @Value("${checkout.delivery.slotLengthMin:30}")
    private int slotLengthMin;
    @Value("${checkout.delivery.start:08:00}")
    private String deliveryStart;
    @Value("${checkout.delivery.end:20:00}")
    private String deliveryEnd;

    @Value("${checkout.tips.enabled:false}")
    private boolean tipsEnabled;
    @Value("${checkout.tips.ranges:10,20,50}")
    private String tipsRanges;

    @Value("${checkout.prescription.enabled:false}")
    private boolean prescriptionEnabled;
    @Value("${checkout.prescription.categories:pharmacy}")
    private String prescriptionCategories;

    @Value("${checkout.address.required:name,phone,line1,pincode}")
    private String addressRequiredFields;

    @GetMapping("/payments/config")
    public ResponseEntity<?> paymentsConfig(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        log.info("Payments config requested: tenant={}", tenant);
        Map<String, Object> methods = Map.of(
                "online", onlineEnabled,
                "cod", codEnabled,
                "upi", upiEnabled,
                "card", cardEnabled,
                "wallet", walletEnabled
        );
        Map<String, Object> codPolicy = Map.of(
                "enabled", codEnabled,
                "minTotal", codMinTotal,
                "maxTotal", codMaxTotal
        );
        return ResponseEntity.ok(Map.of(
                "gateways", gateways.split(","),
                "methodsAllowed", methods,
                "codPolicy", codPolicy
        ));
    }



    @GetMapping("/checkout/config")
    public ResponseEntity<?> checkoutConfig(@RequestHeader(value = "X-Tenant-Domain", required = false) String tenant) {
        log.info("Checkout config requested: tenant={}", tenant);
        Map<String, Object> deliverySlots = Map.of(
                "enabled", deliverySlotsEnabled,
                "slotLengthMin", slotLengthMin,
                "start", deliveryStart,
                "end", deliveryEnd
        );
        Map<String, Object> tips = Map.of(
                "enabled", tipsEnabled,
                "ranges", tipsRanges.split(",")
        );
        Map<String, Object> prescriptionFlow = Map.of(
                "enabled", prescriptionEnabled,
                "categories", prescriptionCategories.split(",")
        );
        Map<String, Object> addressFields = Map.of(
                "required", addressRequiredFields.split(",")
        );
        return ResponseEntity.ok(Map.of(
                "deliverySlots", deliverySlots,
                "tips", tips,
                "prescriptionFlow", prescriptionFlow,
                "addressFields", addressFields
        ));
    }


}