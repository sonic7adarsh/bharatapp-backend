package com.bharatshop.repository;

import com.bharatshop.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {
    List<ProductEntity> findByCategoryIgnoreCase(String category);
    List<ProductEntity> findByNameContainingIgnoreCase(String search);
    List<ProductEntity> findByStoreId(String storeId);
    List<ProductEntity> findByStoreIdAndActiveTrue(String storeId);

    // Discovery API queries
    List<ProductEntity> findByStoreIdAndCategoryIdAndActiveTrue(String storeId, String categoryId);
    List<ProductEntity> findByCategoryIdAndStoreIdInAndActiveTrue(String categoryId, java.util.Collection<String> storeIds);
    List<ProductEntity> findByNameContainingIgnoreCaseAndStoreIdInAndActiveTrue(String name, java.util.Collection<String> storeIds);
}