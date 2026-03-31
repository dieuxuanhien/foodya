package com.foodya.backend.infrastructure.adapter.mapper;

/**
 * TEMPLATE: Entity Mapper Implementation
 *
 * <p>
 * This is a code generation template for Phase 2.1 refactoring.
 * Use this pattern when splitting domain.persistence entities into pure domain models.
 * </p>
 *
 * <p>
 * File structure after Phase 2.1 refactoring:
 * <pre>
 * domain/core/UserAccountDomainModel.java
 *   ↓ (mapper converts to/from)
 * infrastructure/adapter/mapper/UserAccountMapper.java (THIS FILE)
 *   ↓ (uses)
 * infrastructure/persistence/entity/UserAccountJpaEntity.java
 * </pre>
 * </p>
 *
 * <p>
 * To create a new mapper:
 * 1. Copy this template
 * 2. Replace DomainModel with your domain class name (e.g., UserAccountDomainModel)
 * 3. Replace JpaEntity with your JPA class name (e.g., UserAccountJpaEntity)
 * 4. Update the conversion methods
 * 5. Delete this comment block
 * 6. Run: mvn test -Dtest=ArchitectureRulesTests
 * </p>
 */
@Deprecated(forRemoval = true, since = "Phase 2.1")
public class EntityMapperTemplate {

    // Template code (DELETE THIS AFTER USING):

    /*
    package com.foodya.backend.infrastructure.adapter.mapper;

    import com.foodya.backend.domain.core.DomainModel;
    import com.foodya.backend.infrastructure.persistence.entity.JpaEntity;
    import org.springframework.stereotype.Component;

    @Component
    public class DomainModelMapper extends EntityMapper<DomainModel, JpaEntity> {

        @Override
        public DomainModel toDomain(JpaEntity jpaEntity) {
            if (jpaEntity == null) {
                return null;
            }

            return new DomainModel(
                    jpaEntity.getId(),
                    jpaEntity.getField1(),
                    jpaEntity.getField2(),
                    jpaEntity.getField3()
            );
        }

        @Override
        public JpaEntity toJpa(DomainModel domainModel) {
            if (domainModel == null) {
                return null;
            }

            JpaEntity entity = new JpaEntity();
            entity.setId(domainModel.getId());
            entity.setField1(domainModel.getField1());
            entity.setField2(domainModel.getField2());
            entity.setField3(domainModel.getField3());
            return entity;
        }

        @Override
        public DomainModel mergeFromJpa(JpaEntity jpaEntity, DomainModel existing) {
            // Custom merge logic (optional)
            // Example: preserve some state from existing during update
            return toDomain(jpaEntity);
        }
    }
    */
}
