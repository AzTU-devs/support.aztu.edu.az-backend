package com.aztu.support.controller;

import com.aztu.support.dto.platform.CategoryRequest;
import com.aztu.support.dto.platform.CategoryResponse;
import com.aztu.support.dto.platform.PlatformRequest;
import com.aztu.support.dto.platform.PlatformResponse;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.PlatformService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platforms")
@PreAuthorize(Authorities.AUTHENTICATED)
public class PlatformController {

    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    // ── Read (any authenticated user, e.g. the Open Ticket form) ─────────────

    @GetMapping
    public List<PlatformResponse> listPlatforms(
            @RequestParam(name = "all", defaultValue = "false") boolean includeInactive) {
        return platformService.listPlatforms(!includeInactive);
    }

    @GetMapping("/{id}/categories")
    public List<CategoryResponse> listCategories(
            @PathVariable Long id,
            @RequestParam(name = "all", defaultValue = "false") boolean includeInactive) {
        return platformService.listCategories(id, !includeInactive);
    }

    // ── Management (ADMIN / SUPER_ADMIN) ─────────────────────────────────────

    @PostMapping
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<PlatformResponse> createPlatform(@Valid @RequestBody PlatformRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformService.createPlatform(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public PlatformResponse updatePlatform(@PathVariable Long id, @Valid @RequestBody PlatformRequest request) {
        return platformService.updatePlatform(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize(Authorities.ADMIN)
    public PlatformResponse setPlatformActive(@PathVariable Long id, @RequestParam boolean active) {
        return platformService.setPlatformActive(id, active);
    }

    @PostMapping("/{id}/categories")
    @PreAuthorize(Authorities.ADMIN)
    public ResponseEntity<CategoryResponse> createCategory(@PathVariable Long id,
                                                           @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(platformService.createCategory(id, request));
    }

    @PutMapping("/categories/{categoryId}")
    @PreAuthorize(Authorities.ADMIN)
    public CategoryResponse updateCategory(@PathVariable Long categoryId,
                                           @Valid @RequestBody CategoryRequest request) {
        return platformService.updateCategory(categoryId, request);
    }

    @PatchMapping("/categories/{categoryId}/active")
    @PreAuthorize(Authorities.ADMIN)
    public CategoryResponse setCategoryActive(@PathVariable Long categoryId, @RequestParam boolean active) {
        return platformService.setCategoryActive(categoryId, active);
    }
}
