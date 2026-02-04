package com.bharatshop.service;

import com.bharatshop.domain.CartItem;
import com.bharatshop.entity.ProductEntity;
import com.bharatshop.repository.ProductRepository;
import com.bharatshop.error.BadRequestException;
import com.bharatshop.error.ConflictException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CartService {
    // Per-user carts by userId
    private final Map<String, List<CartItem>> carts = new ConcurrentHashMap<>();
    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<CartItem> getCart(String userId) {
        return carts.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    public List<CartItem> addItem(String userId, CartItem item) {
        if (item.getId() == null) throw new BadRequestException("Product ID required");

        // Fetch product to validate and get storeId
        ProductEntity product = productRepository.findById(item.getId())
                .orElseThrow(() -> new BadRequestException("Product not found: " + item.getId()));

        if (!Boolean.TRUE.equals(product.getActive())) {
             throw new BadRequestException("Product is not active");
        }

        String storeId = product.getStoreId();
        List<CartItem> cart = getCart(userId);

        // Check for store mix
        if (!cart.isEmpty()) {
            // Find any item with a storeId (legacy items might not have it if we didn't migrate, but for MVP it's fine)
            String existingStoreId = cart.stream()
                    .map(CartItem::getStoreId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            
            if (existingStoreId != null && !existingStoreId.equals(storeId)) {
                throw new ConflictException("Cart contains items from another store (" + existingStoreId + "). Clear cart to add items from " + storeId);
            }
        }

        // Populate item details from source of truth
        item.setStoreId(storeId);
        item.setName(product.getName());
        item.setPrice(product.getPrice()); // Trust DB price, not client
        boolean isPharma = product.getCategory() != null && 
                           (product.getCategory().toLowerCase().contains("pharmacy") || 
                            product.getCategory().toLowerCase().contains("medicine"));
        item.setRequiresPrescription(isPharma); // Simple rule for now

        // Check if item already exists, update quantity if so
        Optional<CartItem> existing = cart.stream().filter(ci -> ci.getId().equals(item.getId())).findFirst();
        if (existing.isPresent()) {
             existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
             // Update storeId if missing (e.g. legacy)
             if (existing.get().getStoreId() == null) existing.get().setStoreId(storeId);
        } else {
             cart.add(item);
        }
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
