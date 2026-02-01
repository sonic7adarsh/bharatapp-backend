package com.bharatshop.domain;

public class PaymentVerificationResponse {
    private String status; // ok|error
    private String message;

    public PaymentVerificationResponse() {}
    public PaymentVerificationResponse(String status, String message) { this.status = status; this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}