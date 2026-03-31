package com.foodya.backend.infrastructure.adapter.mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic contract for mapping between domain models and JPA entities.
 *
 * <p>
 * Used during Phase 2.1 refactoring when splitting domain.persistence entities
 * into: pure domain models (domain/core) + JPA entities (infrastructure/persistence/entity)
 * </p>
 *
 * <p>
 * Implementations: Extend for each entity pair, e.g.:
 * <pre>
 * public class UserAccountMapper extends EntityMapper<UserAccountDomainModel, UserAccountJpaEntity> {
 *     @Override
 *     public UserAccountDomainModel toDomain(UserAccountJpaEntity jpaEntity) {
 *         // Convert JPA entity → domain model
 *     }
 *
 *     @Override
 *     public UserAccountJpaEntity toJpa(UserAccountDomainModel domainModel) {
 *         // Convert domain model → JPA entity
 *     }
 * }
 * </pre>
 * </p>
 *
 * @param <D> Domain model type (pure business object)
 * @param <J> JPA entity type (persistence concern)
 */
public abstract class EntityMapper<D, J> {

    /**
     * Convert JPA entity to domain model (database → memory).
     *
     * @param jpaEntity the JPA entity from database
     * @return pure domain model for business logic
     */
    public abstract D toDomain(J jpaEntity);

    /**
     * Convert domain model to JPA entity (memory → database).
     *
     * @param domainModel pure business model
     * @return JPA entity ready for persistence
     */
    public abstract J toJpa(D domainModel);

    /**
     * Convert list of JPA entities to domain models.
     *
     * @param jpaEntities list of JPA entities
     * @return list of domain models
     */
    public List<D> toDomainList(List<J> jpaEntities) {
        if (jpaEntities == null)  {
            return List.of();
        }
        return jpaEntities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of domain models to JPA entities.
     *
     * @param domainModels list of domain models
     * @return list of JPA entities
     */
    public List<J> toJpaList(List<D> domainModels) {
        if (domainModels == null) {
            return List.of();
        }
        return domainModels.stream()
                .map(this::toJpa)
                .collect(Collectors.toList());
    }

    /**
     * Update existing domain model from JPA entity (used for refresh/reload).
     *
     * <p>
     * Override for custom merge logic. Default implementation:
     * converts JPA to domain (loses previous state).
     * </p>
     *
     * @param jpaEntity updated JPA entity from database
     * @param existingDomain existing domain model to update
     * @return updated domain model
     */
    public D mergeFromJpa(J jpaEntity, D existingDomain) {
        // Default: re-create from JPA
        return toDomain(jpaEntity);
    }
}
