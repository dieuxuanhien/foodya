package com.foodya.backend.application.ports.out;

import com.foodya.backend.domain.entities.CategoryTaxonomy;

import java.util.List;
import java.util.Optional;

public interface CategoryTaxonomyPort {

    Optional<CategoryTaxonomy> findAnyByCode(String code);

    Optional<CategoryTaxonomy> findByCode(String code);

    List<CategoryTaxonomy> findAll();

    List<CategoryTaxonomy> findActive();

    boolean existsByCode(String code);

    CategoryTaxonomy save(CategoryTaxonomy taxonomy);

    void deleteByCode(String code);

    boolean isUsedByAnyMenuItem(String code);
}