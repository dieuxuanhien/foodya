package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.CategoryTaxonomyPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryTaxonomyRepository extends JpaRepository<CategoryTaxonomyPersistenceModel, String> {

    Optional<CategoryTaxonomyPersistenceModel> findByCode(String code);

    Optional<CategoryTaxonomyPersistenceModel> findByCodeAndActiveTrue(String code);

    List<CategoryTaxonomyPersistenceModel> findAllByOrderBySortOrderAscDisplayNameAsc();

    List<CategoryTaxonomyPersistenceModel> findAllByActiveTrueOrderBySortOrderAscDisplayNameAsc();

    @Query(value = "select exists(select 1 from menu_item_taxonomies mit where mit.taxonomy_code = :code)", nativeQuery = true)
    boolean isUsedByAnyMenuItem(@Param("code") String code);
}