package com.foodya.backend.infrastructure.adapter;

import com.foodya.backend.application.ports.out.CategoryTaxonomyPort;
import com.foodya.backend.domain.entities.CategoryTaxonomy;
import com.foodya.backend.infrastructure.mapper.CategoryTaxonomyMapper;
import com.foodya.backend.infrastructure.repository.CategoryTaxonomyRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public class CategoryTaxonomyAdapter implements CategoryTaxonomyPort {

    private final CategoryTaxonomyRepository repository;
    private final CategoryTaxonomyMapper mapper;

    public CategoryTaxonomyAdapter(CategoryTaxonomyRepository repository, CategoryTaxonomyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CategoryTaxonomy> findAnyByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.findByCode(normalized).map(mapper::toDomain);
    }

    @Override
    public Optional<CategoryTaxonomy> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.findByCodeAndActiveTrue(normalized).map(mapper::toDomain);
    }

    @Override
    public List<CategoryTaxonomy> findAll() {
        return repository.findAllByOrderBySortOrderAscDisplayNameAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CategoryTaxonomy> findActive() {
        return repository.findAllByActiveTrueOrderBySortOrderAscDisplayNameAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.existsById(Objects.requireNonNull(normalized));
    }

    @Override
    public CategoryTaxonomy save(CategoryTaxonomy taxonomy) {
        var saved = repository.save(Objects.requireNonNull(mapper.toPersistence(taxonomy)));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteByCode(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        repository.deleteById(Objects.requireNonNull(normalized));
    }

    @Override
    public boolean isUsedByAnyMenuItem(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.isUsedByAnyMenuItem(normalized);
    }
}