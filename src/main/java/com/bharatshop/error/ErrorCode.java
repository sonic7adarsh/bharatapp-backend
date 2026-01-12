package com.bharatshop.error;

/**
 * User-friendly error codes for customer-facing API responses
 */
public enum ErrorCode {
    // Store availability errors
    STORE_NOT_FOUND("Store not found"),
    STORE_CLOSED("Store is currently closed"),
    STORE_TEMPORARILY_CLOSED("Store is temporarily closed"),
    STORE_ORDERING_DISABLED("Store is not accepting orders at the moment"),
    STORE_OUT_OF_SERVICE_AREA("Store does not deliver to your location"),
    
    // Inventory errors
    INSUFFICIENT_INVENTORY("Some items are no longer available"),
    PRODUCT_NOT_AVAILABLE("Product is not available"),
    PRODUCT_OUT_OF_STOCK("Product is out of stock"),
    
    // Cart errors
    EMPTY_CART("Your cart is empty"),
    CART_ITEM_INVALID("Cart item is invalid"),
    
    // Order errors
    ORDER_NOT_FOUND("Order not found"),
    ORDER_CANNOT_BE_CANCELLED("Order cannot be cancelled"),
    ORDER_ALREADY_CANCELLED("Order has already been cancelled"),
    ORDER_EXPIRED("Order has expired"),
    
    // Payment errors
    PAYMENT_FAILED("Payment processing failed"),
    PAYMENT_METHOD_NOT_SUPPORTED("Payment method not supported"),
    INVALID_PAYMENT_INFO("Payment information is invalid"),
    
    // Delivery errors
    DELIVERY_SLOT_UNAVAILABLE("Selected delivery slot is not available"),
    DELIVERY_ADDRESS_INVALID("Delivery address is invalid"),
    DELIVERY_NOT_AVAILABLE("Delivery is not available for your location"),
    
    // Authentication errors
    UNAUTHORIZED("Authentication required"),
    FORBIDDEN("Access denied"),
    INVALID_CREDENTIALS("Invalid credentials"),
    
    // Validation errors
    VALIDATION_ERROR("Validation failed"),
    INVALID_REQUEST("Invalid request"),
    MISSING_REQUIRED_FIELD("Missing required field"),
    
    // System errors
    INTERNAL_ERROR("Something went wrong"),
    SERVICE_UNAVAILABLE("Service temporarily unavailable"),
    RATE_LIMIT_EXCEEDED("Too many requests");
    
    private final String defaultMessage;
    
    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }
    
    public String getDefaultMessage() {
        return defaultMessage;
    }
}