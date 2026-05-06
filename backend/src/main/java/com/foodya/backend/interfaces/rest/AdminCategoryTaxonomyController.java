package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.CategoryTaxonomyData;
import com.foodya.backend.application.dto.CreateCategoryTaxonomyRequest;
import com.foodya.backend.application.dto.UpdateCategoryTaxonomyRequest;
import com.foodya.backend.application.ports.in.AdminCategoryTaxonomyUseCase;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.CategoryTaxonomyResponse;
import com.foodya.backend.interfaces.rest.mapper.CommonApiMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/category-taxonomies")
@Tag(name = "Admin Category Taxonomies", description = "Admin taxonomy management")
@Validated
public class AdminCategoryTaxonomyController {

    private final AdminCategoryTaxonomyUseCase adminCategoryTaxonomyService;

    public AdminCategoryTaxonomyController(AdminCategoryTaxonomyUseCase adminCategoryTaxonomyService) {
        this.adminCategoryTaxonomyService = adminCategoryTaxonomyService;
    }

    @GetMapping
    @Operation(summary = "List all category taxonomies")
    public ResponseEntity<ApiSuccessResponse<List<CategoryTaxonomyResponse>>> list(HttpServletRequest httpServletRequest) {
        List<CategoryTaxonomyResponse> data = adminCategoryTaxonomyService.listAll().stream()
                .map(CommonApiMapper::toCategoryTaxonomyResponse)
                .toList();
        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest)));
    }

    @PostMapping
    @Operation(summary = "Create category taxonomy")
    public ResponseEntity<ApiSuccessResponse<CategoryTaxonomyResponse>> create(Authentication authentication,
                                                                               @Valid @RequestBody CreateCategoryTaxonomyRequest request,
                                                                               HttpServletRequest httpServletRequest) {
        CategoryTaxonomyData data = adminCategoryTaxonomyService.create(request, CurrentUser.userId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.of(CommonApiMapper.toCategoryTaxonomyResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update category taxonomy")
    public ResponseEntity<ApiSuccessResponse<CategoryTaxonomyResponse>> update(Authentication authentication,
                                                                               @PathVariable String code,
                                                                               @Valid @RequestBody UpdateCategoryTaxonomyRequest request,
                                                                               HttpServletRequest httpServletRequest) {
        CategoryTaxonomyData data = adminCategoryTaxonomyService.update(code, request, CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(CommonApiMapper.toCategoryTaxonomyResponse(data), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete category taxonomy")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String code) {
        adminCategoryTaxonomyService.delete(code, CurrentUser.userId(authentication));
        return ResponseEntity.noContent().build();
    }
}
