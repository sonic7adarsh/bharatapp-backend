package com.bharatshop.factory.ops;

import com.bharatshop.domain.Product;
import java.util.List;

public interface ProductOps {
    List<Product> list(String category, String search);
    Product get(String id);
    List<String> categories();
    void add(Product p);
    List<Product> byStore(String storeId);
    List<Product> byStoreAndCategory(String storeId, String categoryId);
}