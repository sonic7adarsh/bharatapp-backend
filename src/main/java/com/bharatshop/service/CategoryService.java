package com.bharatshop.service;

import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.repository.CategoryRepository;
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

    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryDto> getCategories() {
        // Tenant context removed
        List<CategoryEntity> entities = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        return entities.stream()
            .map(e -> new CategoryDto(e.getId(), e.getName(), e.getSlug()))
            .collect(Collectors.toList());
    }

    // getTenantId removed

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
