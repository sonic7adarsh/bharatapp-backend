package com.bharatshop.factory.ops;

import com.bharatshop.domain.CartItem;
import java.util.List;

public interface CartOps {
    List<CartItem> getCart(String userId);
    List<CartItem> addItem(String userId, CartItem item);
    List<CartItem> removeItem(String userId, String itemId);
}