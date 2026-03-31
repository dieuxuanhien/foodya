package com.foodya.backend.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces Clean Architecture layering rules for Foodya backend.
 * 
 * Violations detected here indicate architecture decay—fix immediately.
 * 
 * Reference: Uncle Bob's Clean Architecture + backend-system.instructions.md
 */
class ArchitectureRulesTests {

    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("com.foodya.backend");

    // ============================================================================
    // LAYER ISOLATION RULES (Critical)
    // ============================================================================

    @Test
    void applicationMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    void infrastructureMustNotDependOnInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..interfaces..");

        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnApplicationLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..");

        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    // ============================================================================
    // DOMAIN LAYER PURITY RULES (High Priority)
    // ============================================================================

    @Test
    void domainValueObjectsMustStayFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.value_objects..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework..",
                        "com.foodya.backend.infrastructure..",
                        "com.foodya.backend.application.."
                );

        rule.check(classes);
    }

    @Test
    void domainLayerMustNotContainLegacyPersistencePackage() {
        ArchRule rule = classes()
                .that().resideInAPackage("..domain.persistence..")
                .should().haveFullyQualifiedName("com.foodya.backend.__legacy__.ShouldNotExist")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    void domainEntitiesMustNotDependOnSpringOrOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.entities..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.foodya.backend.infrastructure..",
                        "com.foodya.backend.application..",
                        "com.foodya.backend.interfaces.."
                );

        rule.check(classes);
    }

    // ============================================================================
    // REST LAYER BOUNDARY RULES
    // ============================================================================

    @Test
    void restDtosMustNotDependOnPersistenceEntities() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..interfaces.rest.dto..")
                                .should().dependOnClassesThat().resideInAPackage("..domain.entities..");

        rule.check(classes);
    }

    @Test
    void restControllersMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..interfaces.rest..")
                .and().haveNameMatching(".*Controller")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    // ============================================================================
    // APPLICATION LAYER PURITY RULES
    // ============================================================================

    @Test
    void applicationServicesMustDependOnlyOnPorts() {
        // Services can depend on:
        //   - domain.* (business models)
        //   - application.* (use-case DTO, exceptions, other ports)
        //   - standard Java/Jakarta libraries
        // They MUST NOT directly depend on:
        //   - infrastructure.* (except through ports)
        //   - interfaces.* (which is outside layer)
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application.usecases..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.foodya.backend.infrastructure.repository..",
                        "com.foodya.backend.infrastructure.persistence..",
                        "com.foodya.backend.interfaces.."
                );

        rule.check(classes);
    }

    @Test
    void applicationMustNotDependOnRepositoryDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure.repository..");

        rule.check(classes);
    }

        @Test
        void applicationUseCasesMustNotDependOnFrameworkPasswordOrEventApis() {
                ArchRule rule = noClasses()
                                .that().resideInAPackage("..application.usecases..")
                                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.security.crypto.password.PasswordEncoder")
                                .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.context.ApplicationEventPublisher");

                rule.check(classes);
        }

    // ============================================================================
    // PORT-ADAPTER PATTERN RULES
    // ============================================================================

    @Test
    void portsMustBeDefinedInApplicationLayer() {
        // All port interfaces must reside in application.ports modules
        ArchRule rule = classes()
                .that().haveNameMatching(".*Port")
                .should().resideInAPackage("..application.ports..")
                .allowEmptyShould(true);

        rule.check(classes);
    }

        @Test
        void legacyApplicationServicePackageMustBeEmpty() {
                ArchRule rule = classes()
                                .that().resideInAPackage("..application.service..")
                                .should().haveFullyQualifiedName("com.foodya.backend.__legacy__.ShouldNotExist")
                                .allowEmptyShould(true);

                rule.check(classes);
        }

        @Test
        void legacyApplicationPortPackageMustBeEmpty() {
                ArchRule rule = classes()
                                .that().resideInAPackage("..application.port..")
                                .should().haveFullyQualifiedName("com.foodya.backend.__legacy__.ShouldNotExist")
                                .allowEmptyShould(true);

                rule.check(classes);
        }

        @Test
        void legacyDomainModelPackageMustBeEmpty() {
                ArchRule rule = classes()
                                .that().resideInAPackage("..domain.model..")
                                .should().haveFullyQualifiedName("com.foodya.backend.__legacy__.ShouldNotExist")
                                .allowEmptyShould(true);

                rule.check(classes);
        }
}
