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
    void domainModelMustStayFrameworkFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework..",
                        "com.foodya.backend.infrastructure..",
                        "com.foodya.backend.application.."
                );

        rule.check(classes);
    }

    @Test
    void domainPersistenceLayerMustBeFrameworkFree() {
        // IMPORTANT: domain.persistence entities should eventually move to
        // infrastructure.persistence and be replaced with pure domain models.
        // This rule enforces that IF entities are in domain.persistence,
        // they must NOT import Spring external libraries (only JPA is allowed temporarily).
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.foodya.backend.infrastructure..",
                        "com.foodya.backend.application.."
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
                .should().dependOnClassesThat().resideInAPackage("..domain.persistence..");

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
                .that().resideInAPackage("..application.service..")
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

    // ============================================================================
    // PORT-ADAPTER PATTERN RULES
    // ============================================================================

    @Test
    void portsMustBeDefinedInApplicationLayer() {
        // All port interfaces must reside in application.port modules
        ArchRule rule = classes()
                .that().haveNameMatching(".*Port")
                .should().resideInAPackage("..application.port..")
                .allowEmptyShould(true);

        rule.check(classes);
    }
}
