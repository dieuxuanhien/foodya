package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.CategoryTaxonomyData;
import com.foodya.backend.application.dto.CreateCategoryTaxonomyRequest;
import com.foodya.backend.application.dto.UpdateCategoryTaxonomyRequest;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.AdminCategoryTaxonomyUseCase;
import com.foodya.backend.application.ports.out.CategoryTaxonomyPort;
import com.foodya.backend.domain.entities.CategoryTaxonomy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AdminCategoryTaxonomyService implements AdminCategoryTaxonomyUseCase {

    private final CategoryTaxonomyPort categoryTaxonomyPort;
    private final AuditLogService auditLogService;

    public AdminCategoryTaxonomyService(CategoryTaxonomyPort categoryTaxonomyPort,
                                        AuditLogService auditLogService) {
        this.categoryTaxonomyPort = categoryTaxonomyPort;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<CategoryTaxonomyData> listAll() {
        return categoryTaxonomyPort.findAll().stream().map(this::toData).toList();
    }

    @Override
    public CategoryTaxonomyData create(CreateCategoryTaxonomyRequest request, UUID actorId) {
        String code = normalizeCode(request.code());
        if (categoryTaxonomyPort.existsByCode(code)) {
            throw new ConflictException("taxonomy code already exists");
        }

        CategoryTaxonomy taxonomy = new CategoryTaxonomy();
        taxonomy.setCode(code);
        taxonomy.setDisplayName(request.displayName().trim());
        taxonomy.setSortOrder(request.sortOrder());
        taxonomy.setActive(request.isActive());

        CategoryTaxonomy saved = categoryTaxonomyPort.save(taxonomy);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_TAXONOMY_CREATE", "CATEGORY_TAXONOMY", saved.getCode(), null, saved.getDisplayName());
        return toData(saved);
    }

    @Override
    public CategoryTaxonomyData update(String code, UpdateCategoryTaxonomyRequest request, UUID actorId) {
        String normalizedCode = normalizeCode(code);
        CategoryTaxonomy taxonomy = categoryTaxonomyPort.findAnyByCode(normalizedCode)
                .orElseThrow(() -> new NotFoundException("category taxonomy not found"));

        String oldValue = taxonomy.getDisplayName() + "|" + taxonomy.getSortOrder() + "|" + taxonomy.isActive();
        taxonomy.setDisplayName(request.displayName().trim());
        taxonomy.setSortOrder(request.sortOrder());
        taxonomy.setActive(request.isActive());

        CategoryTaxonomy saved = categoryTaxonomyPort.save(taxonomy);
        String newValue = saved.getDisplayName() + "|" + saved.getSortOrder() + "|" + saved.isActive();
        auditLogService.securityEvent(actorId.toString(), "ADMIN_TAXONOMY_UPDATE", "CATEGORY_TAXONOMY", saved.getCode(), oldValue, newValue);
        return toData(saved);
    }

    @Override
    public void delete(String code, UUID actorId) {
        String normalizedCode = normalizeCode(code);
        CategoryTaxonomy taxonomy = categoryTaxonomyPort.findAnyByCode(normalizedCode)
                .orElseThrow(() -> new NotFoundException("category taxonomy not found"));
        if (categoryTaxonomyPort.isUsedByAnyMenuItem(normalizedCode)) {
            throw new ConflictException("taxonomy is in use by menu items");
        }
        categoryTaxonomyPort.deleteByCode(normalizedCode);
        auditLogService.securityEvent(actorId.toString(), "ADMIN_TAXONOMY_DELETE", "CATEGORY_TAXONOMY", taxonomy.getCode(), taxonomy.getDisplayName(), null);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("invalid code", Map.of("code", "must not be blank"));
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private CategoryTaxonomyData toData(CategoryTaxonomy taxonomy) {
        CategoryTaxonomyData data = new CategoryTaxonomyData();
        data.setCode(taxonomy.getCode());
        data.setDisplayName(taxonomy.getDisplayName());
        data.setSortOrder(taxonomy.getSortOrder());
        data.setActive(taxonomy.isActive());
        data.setCreatedAt(taxonomy.getCreatedAt());
        data.setUpdatedAt(taxonomy.getUpdatedAt());
        return data;
    }
}
