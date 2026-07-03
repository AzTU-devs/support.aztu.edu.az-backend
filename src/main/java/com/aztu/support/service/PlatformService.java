package com.aztu.support.service;

import com.aztu.support.domain.Category;
import com.aztu.support.domain.Platform;
import com.aztu.support.dto.platform.CategoryRequest;
import com.aztu.support.dto.platform.CategoryResponse;
import com.aztu.support.dto.platform.PlatformRequest;
import com.aztu.support.dto.platform.PlatformResponse;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.CategoryRepository;
import com.aztu.support.repository.PlatformRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {

    private final PlatformRepository platformRepository;
    private final CategoryRepository categoryRepository;

    public PlatformService(PlatformRepository platformRepository, CategoryRepository categoryRepository) {
        this.platformRepository = platformRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── Platforms ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PlatformResponse> listPlatforms(boolean activeOnly) {
        List<Platform> platforms = activeOnly
                ? platformRepository.findByActiveTrueOrderByNameAsc()
                : platformRepository.findAllByOrderByNameAsc();
        return platforms.stream().map(PlatformResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlatformResponse getPlatform(Long id) {
        return PlatformResponse.from(loadPlatform(id));
    }

    @Transactional
    public PlatformResponse createPlatform(PlatformRequest request) {
        if (platformRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw ApiException.conflict("A platform with this name already exists.");
        }
        Platform platform = new Platform();
        platform.setName(request.name().trim());
        platform.setDescription(request.description());
        platform.setActive(request.active() == null || request.active());
        return PlatformResponse.from(platformRepository.save(platform));
    }

    @Transactional
    public PlatformResponse updatePlatform(Long id, PlatformRequest request) {
        Platform platform = loadPlatform(id);
        String newName = request.name().trim();
        if (!platform.getName().equalsIgnoreCase(newName)
                && platformRepository.existsByNameIgnoreCase(newName)) {
            throw ApiException.conflict("A platform with this name already exists.");
        }
        platform.setName(newName);
        platform.setDescription(request.description());
        if (request.active() != null) {
            platform.setActive(request.active());
        }
        return PlatformResponse.from(platformRepository.save(platform));
    }

    @Transactional
    public PlatformResponse setPlatformActive(Long id, boolean active) {
        Platform platform = loadPlatform(id);
        platform.setActive(active);
        return PlatformResponse.from(platformRepository.save(platform));
    }

    // ── Categories ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(Long platformId, boolean activeOnly) {
        loadPlatform(platformId);
        List<Category> categories = activeOnly
                ? categoryRepository.findByPlatform_IdAndActiveTrueOrderByNameAsc(platformId)
                : categoryRepository.findByPlatform_IdOrderByNameAsc(platformId);
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse createCategory(Long platformId, CategoryRequest request) {
        Platform platform = loadPlatform(platformId);
        if (categoryRepository.existsByPlatform_IdAndNameIgnoreCase(platformId, request.name().trim())) {
            throw ApiException.conflict("A category with this name already exists on this platform.");
        }
        Category category = new Category();
        category.setPlatform(platform);
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest request) {
        Category category = loadCategory(categoryId);
        String newName = request.name().trim();
        if (!category.getName().equalsIgnoreCase(newName)
                && categoryRepository.existsByPlatform_IdAndNameIgnoreCase(
                        category.getPlatform().getId(), newName)) {
            throw ApiException.conflict("A category with this name already exists on this platform.");
        }
        category.setName(newName);
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse setCategoryActive(Long categoryId, boolean active) {
        Category category = loadCategory(categoryId);
        category.setActive(active);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private Platform loadPlatform(Long id) {
        return platformRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Platform not found."));
    }

    private Category loadCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category not found."));
    }
}
