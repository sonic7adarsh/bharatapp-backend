package com.bharatshop.web;

import com.bharatshop.entity.CategoryEntity;
import com.bharatshop.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/global")
    public ResponseEntity<?> getGlobalCategories() {
        List<CategoryEntity> categories = categoryRepository.findByIsGlobalTrueOrderByPriorityAsc();
        return ResponseEntity.ok(Map.of("categories", categories));
    }
}
