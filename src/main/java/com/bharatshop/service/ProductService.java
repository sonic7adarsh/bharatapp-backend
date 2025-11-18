package com.bharatshop.service;

import com.bharatshop.domain.Product;
import com.bharatshop.entity.ProductEntity;
import com.bharatshop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAll(String category, String search) {
        List<ProductEntity> list;
        if (category != null && !category.isBlank()) {
            list = productRepository.findByCategoryIgnoreCase(category);
        } else if (search != null && !search.isBlank()) {
            list = productRepository.findByNameContainingIgnoreCase(search);
        } else {
            list = productRepository.findAll();
        }
        return list.stream().map(this::toDto)
                .sorted(Comparator.comparing(Product::getName))
                .collect(Collectors.toList());
    }

    public Product getById(String id) {
        return productRepository.findById(id).map(this::toDto).orElse(null);
    }

    public List<Product> getByStore(String storeId) {
        return productRepository.findByStoreId(storeId).stream().map(this::toDto).collect(Collectors.toList());
    }

    public void add(Product p) {
        ProductEntity e = new ProductEntity();
        e.setId(p.getId());
        e.setName(p.getName());
        e.setPrice(p.getPrice());
        e.setDescription(p.getDescription());
        e.setImage(p.getImage());
        e.setCategory(p.getCategory());
        e.setStoreId(p.getStoreId());
        e.setCurrency(p.getCurrency());
        e.setSku(p.getSku());
        e.setStock(p.getStock());
        e.setActive(p.getActive() == null ? Boolean.TRUE : p.getActive());
        productRepository.save(e);
    }

    public List<String> categories() {
        return productRepository.findAll().stream().map(ProductEntity::getCategory)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
    }

    private Product toDto(ProductEntity e) {
        Product p = new Product(e.getId(), e.getName(), e.getPrice(), e.getCategory(), e.getStoreId());
        p.setDescription(e.getDescription());
        p.setImage(e.getImage());
        p.setCurrency(e.getCurrency());
        p.setSku(e.getSku());
        p.setStock(e.getStock());
        p.setActive(e.getActive());
        return p;
    }

    public Product updatePartial(String id, Map<String, Object> changes) {
        Optional<ProductEntity> opt = productRepository.findById(id);
        if (!opt.isPresent()) return null;
        ProductEntity e = opt.get();
        if (changes.containsKey("name")) e.setName((String) changes.get("name"));
        if (changes.containsKey("description")) e.setDescription((String) changes.get("description"));
        if (changes.containsKey("price")) {
            Object pv = changes.get("price");
            if (pv instanceof Number) e.setPrice(((Number) pv).doubleValue());
        }
        if (changes.containsKey("image")) e.setImage((String) changes.get("image"));
        if (changes.containsKey("category")) e.setCategory((String) changes.get("category"));
        if (changes.containsKey("storeId")) e.setStoreId((String) changes.get("storeId"));
        if (changes.containsKey("currency")) e.setCurrency((String) changes.get("currency"));
        if (changes.containsKey("sku")) e.setSku((String) changes.get("sku"));
        if (changes.containsKey("stock")) {
            Object sv = changes.get("stock");
            if (sv instanceof Number) e.setStock(((Number) sv).intValue());
        }
        if (changes.containsKey("active")) {
            Object av = changes.get("active");
            if (av instanceof Boolean) e.setActive((Boolean) av);
        }
        productRepository.save(e);
        return toDto(e);
    }

    public Product adjustInventory(String id, Integer stockDelta, Integer stockSet, Double price) {
        Optional<ProductEntity> opt = productRepository.findById(id);
        if (!opt.isPresent()) return null;
        ProductEntity e = opt.get();
        if (stockSet != null) e.setStock(stockSet);
        else if (stockDelta != null) e.setStock(e.getStock() == null ? stockDelta : e.getStock() + stockDelta);
        if (price != null) e.setPrice(price);
        productRepository.save(e);
        return toDto(e);
    }

    public Product updateImage(String id, String imageName) {
        Optional<ProductEntity> opt = productRepository.findById(id);
        if (!opt.isPresent()) return null;
        ProductEntity e = opt.get();
        e.setImage(imageName);
        productRepository.save(e);
        return toDto(e);
    }
}