package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.CategoryTaxonomyData;
import com.foodya.backend.application.dto.CreateCategoryTaxonomyRequest;
import com.foodya.backend.application.dto.UpdateCategoryTaxonomyRequest;

import java.util.List;
import java.util.UUID;

public interface AdminCategoryTaxonomyUseCase {

    List<CategoryTaxonomyData> listAll();

    CategoryTaxonomyData create(CreateCategoryTaxonomyRequest request, UUID actorId);

    CategoryTaxonomyData update(String code, UpdateCategoryTaxonomyRequest request, UUID actorId);

    void delete(String code, UUID actorId);
}
