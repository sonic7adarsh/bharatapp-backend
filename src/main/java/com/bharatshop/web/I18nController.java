package com.bharatshop.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class I18nController {
    private static final Logger log = LoggerFactory.getLogger(I18nController.class);

    private static final List<String> LOCALES = List.of("en-IN", "hi-IN");

    private static final Map<String, Map<String, String>> TRANSLATIONS = Map.of(
            "en-IN", Map.of(
                    "app.title", "BharatShop",
                    "home.welcome", "Welcome",
                    "checkout.pay", "Pay",
                    "orders.title", "Orders"
            ),
            "hi-IN", Map.of(
                    "app.title", "भारतशॉप",
                    "home.welcome", "स्वागत है",
                    "checkout.pay", "भुगतान",
                    "orders.title", "ऑर्डर्स"
            )
    );

    private static final Map<String, Map<String, Map<String, String>>> PAGE_TRANSLATIONS = new HashMap<>();
    private static final String DEFAULT_LOCALE = "en-IN";

    static {
        Map<String, Map<String, String>> checkout = new HashMap<>();
        checkout.put("en-IN", Map.of(
                "title", "Checkout",
                "placeOrder", "Place Order",
                "deliverySlot", "Delivery Slot",
                "welcomeUser", "Welcome, {{name}}",
                "itemsCount", "{{count}} items",
                "priceMismatchError", "Prices were updated. Please review and try again.",
                "codNotAvailable", "Cash on Delivery is not available for this order.",
                "orderPlacedToast", "Order {{orderId}} placed successfully!"
        ));
        checkout.put("hi-IN", Map.of(
                "title", "चेकआउट",
                "placeOrder", "ऑर्डर करें",
                "deliverySlot", "डिलीवरी स्लॉट",
                "welcomeUser", "स्वागत है, {{name}}",
                "itemsCount", "{{count}} आइटम",
                "priceMismatchError", "कीमतें अपडेट हुई हैं। कृपया फिर से जांचकर आगे बढ़ें।",
                "codNotAvailable", "इस ऑर्डर के लिए कैश ऑन डिलीवरी उपलब्ध नहीं है।",
                "orderPlacedToast", "ऑर्डर {{orderId}} सफलतापूर्वक प्लेस हो गया!"
        ));
        PAGE_TRANSLATIONS.put("checkout", checkout);

        Map<String, Map<String, String>> productDetail = new HashMap<>();
        productDetail.put("en-IN", Map.of(
                "addToCart", "Add to Cart",
                "requiresPrescription", "Prescription Required",
                "outOfStock", "Out of Stock"
        ));
        productDetail.put("hi-IN", Map.of(
                "addToCart", "कार्ट में जोड़ें",
                "requiresPrescription", "प्रिस्क्रिप्शन आवश्यक",
                "outOfStock", "स्टॉक खत्म"
        ));
        PAGE_TRANSLATIONS.put("product-detail", productDetail);

        Map<String, Map<String, String>> authLogin = new HashMap<>();
        authLogin.put("en-IN", Map.of(
                "loginTitle", "Sign In",
                "loginWithPhone", "Login with Phone",
                "otpSent", "OTP sent to {{phone}}"
        ));
        authLogin.put("hi-IN", Map.of(
                "loginTitle", "साइन इन",
                "loginWithPhone", "फोन से लॉगिन",
                "otpSent", "{{phone}} पर OTP भेजा गया"
        ));
        PAGE_TRANSLATIONS.put("auth-login", authLogin);
    }

    private final Map<String, Map<String, String>> preferencesStore = new ConcurrentHashMap<>();

    // GET /api/i18n/locales → list available locales
    @GetMapping("/i18n/locales")
    public ResponseEntity<Map<String, Object>> locales() {
        log.info("i18n locales requested");
        List<Map<String, String>> locales = new ArrayList<>();
        locales.add(Map.of("code", "en-IN", "name", "English (India)"));
        locales.add(Map.of("code", "hi-IN", "name", "Hindi (India)"));
        Map<String, Object> resp = new HashMap<>();
        resp.put("locales", locales);
        resp.put("defaultLocale", DEFAULT_LOCALE);
        resp.put("fallbackLocale", DEFAULT_LOCALE);
        return ResponseEntity.ok(resp);
    }

    // GET /api/i18n/translations?locale=xx → key-value translations
    @GetMapping("/i18n/translations")
    public ResponseEntity<?> translations(@RequestParam String locale,
                                          @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        log.info("i18n translations requested: locale={}", locale);
        Map<String, String> t = TRANSLATIONS.get(locale);
        if (t == null) {
            t = TRANSLATIONS.get(DEFAULT_LOCALE);
        }
        String etag = computeEtag(t);
        if (etag != null && etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).header(HttpHeaders.ETAG, etag).build();
        }
        return ResponseEntity.ok().header(HttpHeaders.ETAG, etag).body(t);
    }

    @GetMapping("/i18n/pages/{page}")
    public ResponseEntity<?> pageTranslations(@PathVariable String page,
                                              @RequestParam String locale,
                                              @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        log.info("i18n page translations requested: page={}, locale={}", page, locale);
        Map<String, Map<String, String>> pageMap = PAGE_TRANSLATIONS.get(page);
        if (pageMap == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Page not found"));
        }
        Map<String, String> t = pageMap.get(locale);
        if (t == null) {
            t = pageMap.get(DEFAULT_LOCALE);
        }
        String etag = computeEtag(t);
        if (etag != null && etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).header(HttpHeaders.ETAG, etag).build();
        }
        return ResponseEntity.ok().header(HttpHeaders.ETAG, etag).body(t);
    }

    // POST /api/storefront/i18n/preferences (optional) → persist user locale
    @PostMapping("/storefront/i18n/preferences")
    public ResponseEntity<?> setPreferences(@RequestBody Map<String, Object> body, @RequestHeader(value = "Authorization", required = false) String auth) {
        String locale = body != null ? (String) body.get("locale") : null;
        log.info("i18n preferences set: localePresent={}", StringUtils.hasText(locale));
        if (!StringUtils.hasText(locale) || !LOCALES.contains(locale)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or unsupported locale"));
        }
        String userKey = StringUtils.hasText(auth) ? auth : "guest";
        preferencesStore.put(userKey, Map.of("locale", locale));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/storefront/i18n/preferences")
    public ResponseEntity<?> getPreferences(@RequestHeader(value = "Authorization", required = false) String auth) {
        String userKey = StringUtils.hasText(auth) ? auth : "guest";
        Map<String, String> prefs = preferencesStore.getOrDefault(userKey, Map.of("locale", DEFAULT_LOCALE));
        return ResponseEntity.ok(prefs);
    }

    private String computeEtag(Map<String, String> t) {
        if (t == null || t.isEmpty()) return null;
        String joined = t.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("|"));
        int hash = joined.hashCode();
        return '"' + Integer.toHexString(hash) + '"';
    }
}