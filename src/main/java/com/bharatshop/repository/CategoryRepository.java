package com.bharatshop.repository;

import com.bharatshop.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {
    List<CategoryEntity> findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(String tenantId);
    List<CategoryEntity> findByNameContainingIgnoreCase(String name);
}
