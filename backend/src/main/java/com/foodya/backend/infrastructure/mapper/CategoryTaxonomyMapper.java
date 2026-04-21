package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.CategoryTaxonomy;
import com.foodya.backend.infrastructure.persistence.models.CategoryTaxonomyPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class CategoryTaxonomyMapper {

    public CategoryTaxonomy toDomain(CategoryTaxonomyPersistenceModel model) {
        if (model == null) {
            return null;
        }

        CategoryTaxonomy domain = new CategoryTaxonomy();
        domain.setCode(model.getCode());
        domain.setDisplayName(model.getDisplayName());
        domain.setSortOrder(model.getSortOrder());
        domain.setActive(model.isActive());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public CategoryTaxonomyPersistenceModel toPersistence(CategoryTaxonomy domain) {
        if (domain == null) {
            return null;
        }

        CategoryTaxonomyPersistenceModel model = new CategoryTaxonomyPersistenceModel();
        model.setCode(domain.getCode());
        model.setDisplayName(domain.getDisplayName());
        model.setSortOrder(domain.getSortOrder());
        model.setActive(domain.isActive());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}