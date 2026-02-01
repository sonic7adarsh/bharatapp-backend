package com.bharatshop.service;

import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.repository.CategoryRepository;
import com.bharatshop.tenant.TenantContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = "categories", key = "#root.target.getTenantId()")
    public List<CategoryDto> getCategories() {
        String tenantId = getTenantId();
        List<CategoryEntity> entities = categoryRepository.findByTenantIdAndIsActiveTrueOrderByDisplayOrderAsc(tenantId);
        
        return entities.stream()
            .map(e -> new CategoryDto(e.getId(), e.getName(), e.getSlug()))
            .collect(Collectors.toList());
    }

    public String getTenantId() {
        String tenant = TenantContext.getTenant();
        if (tenant == null) {
            throw new IllegalStateException("Tenant context missing");
        }
        return tenant;
    }

    public static class CategoryDto {
        private String id;
        private String name;
        private String slug;

        public CategoryDto(String id, String name, String slug) {
            this.id = id;
            this.name = name;
            this.slug = slug;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSlug() { return slug; }
    }
}
