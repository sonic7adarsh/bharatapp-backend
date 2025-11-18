package com.bharatshop.dto;

import com.bharatshop.domain.Address;
import com.bharatshop.domain.CartItem;
import com.bharatshop.domain.Order;

import java.util.List;

public class CheckoutRequest {
    private List<CartItem> items;
    private Order.Totals totals;
    private Address address;
    private String deliverySlot;
    private String promo;
    private String deliveryInstructions;
    private String paymentMethod; // cod|online
    private Order.PaymentInfo paymentInfo;
    private String type; // order|room_booking
    private com.bharatshop.domain.BookingDetails booking;
    private String storeId;
    private String notes;

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public Order.Totals getTotals() { return totals; }
    public void setTotals(Order.Totals totals) { this.totals = totals; }
    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }
    public String getDeliverySlot() { return deliverySlot; }
    public void setDeliverySlot(String deliverySlot) { this.deliverySlot = deliverySlot; }
    public String getPromo() { return promo; }
    public void setPromo(String promo) { this.promo = promo; }
    public String getDeliveryInstructions() { return deliveryInstructions; }
    public void setDeliveryInstructions(String deliveryInstructions) { this.deliveryInstructions = deliveryInstructions; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Order.PaymentInfo getPaymentInfo() { return paymentInfo; }
    public void setPaymentInfo(Order.PaymentInfo paymentInfo) { this.paymentInfo = paymentInfo; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public com.bharatshop.domain.BookingDetails getBooking() { return booking; }
    public void setBooking(com.bharatshop.domain.BookingDetails booking) { this.booking = booking; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}