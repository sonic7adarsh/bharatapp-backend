package com.bharatshop.service;

import com.bharatshop.domain.CartItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {
    // Per-user carts by userId
    private final Map<String, List<CartItem>> carts = new ConcurrentHashMap<>();

    public List<CartItem> getCart(String userId) {
        return carts.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    public List<CartItem> addItem(String userId, CartItem item) {
        List<CartItem> cart = getCart(userId);
        cart.add(item);
        return cart;
    }

    public List<CartItem> removeItem(String userId, String itemId) {
        List<CartItem> cart = getCart(userId);
        cart.removeIf(ci -> Objects.equals(ci.getId(), itemId));
        return cart;
    }

    public List<CartItem> clearCart(String userId) {
        // Remove existing cart and return a fresh empty list
        carts.remove(userId);
        return getCart(userId);
    }
}