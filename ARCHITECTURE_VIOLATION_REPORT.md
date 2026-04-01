# Clean Architecture Violation Report - Foodya Backend

**Date**: April 1, 2026  
**Scope**: All backend modules  
**Severity**: CRITICAL

---

## Executive Summary

The Foodya backend violates core Clean Architecture principles by:
1. **Domain Layer Contamination**: All 17 domain entities have JPA annotations (@Entity, @Table, @Column, etc.)
2. **Inward Dependency Violation**: Domain layer depends on Jakarta Persistence framework
3. **Framework Coupling**: Application layer tightly coupled to Spring (@Service, @Transactional)
4. **Port Semantics**: Ports return domain entities, conflating domain and boundary contracts

These violations make the codebase:
- Hard to test independently (can't use domain logic without JPA)
- Tightly coupled to persistence technology (switching DB frameworks is impossible)
- Difficult to reason about business logic (mixed with infrastructure concerns)

---

## Violation Details

### Violation 1: Domain Entities With JPA Annotations (CRITICAL)

**Affected Entities (17 total)**:
- OrderItem, Order, OrderPayment, OrderReview, DeliveryTrackingPoint
- MenuItem, MenuCategory, Restaurant
- Cart, CartItem
- UserAccount, RefreshToken, PasswordResetChallenge
- SystemParameter, AuditLog, NotificationLog
- AiChatHistory

**Example - Order.java**:
```java
@Entity                           // ← JPA framework dependency
@Table(name = "orders")          // ← JPA framework dependency
public class Order {
    @Id
    @Column(name = "id")         // ← JPA framework dependency
    private UUID id;
    
    @Column(name = "customer_user_id", nullable = false)  // ← JPA dependency
    private UUID customerUserId;
    // ... 50+ more JPA-annotated fields
}
```

**Violations**:
- ❌ Domain imports `jakarta.persistence.*`
- ❌ Domain depends on ORM framework — violates inward dependency rule
- ❌ Domain cannot be used without JPA/Hibernate
- ❌ Cannot switch persistence technologies without rewriting domain

**Impact**:
- Cannot test domain logic independently
- Cannot port business logic to event handler, CLI, or batch processor without duplicating code
- Violates Uncle Bob's Clean Architecture Rule: *"Outer layers must not know about inner layers"*

---

### Violation 2: Application Layer Spring Annotations

**Affected Services** (30+ files):
- `application/usecases/*.java` — all use `@Service`
- Multiple services use `@Transactional`, `@PostConstruct`

**Example - SystemParameterService.java**:
```java
@Service                         // ← Spring dependency
public class SystemParameterService {
    @PostConstruct              // ← Spring dependency
    @Transactional              // ← Spring dependency
    void bootstrapDefaults() {
        // ...
    }
}
```

**Risk Level**: MEDIUM (Transactional is acceptable at boundary; @Service for DI is standard practice)

**Issue**: 
- Couples application layer to Spring lifecycle
- Makes unit testing require Spring context
- Harder to port logic to other frameworks

---

### Violation 3: Ports Return Domain Entities

**Example - SystemParameterPort.java**:
```java
public interface SystemParameterPort {
    Optional<SystemParameter> findById(String key);  // ← Domain entity
    SystemParameter save(SystemParameter parameter);  // ← Domain entity
    List<SystemParameter> findAllOrderedByKey();     // ← Domain entity
}
```

**Issue**:
- Domain entities travel across port boundaries
- Doesn't establish clear API contract separate from domain model
- Makes it unclear what is "internal" vs "external" representation

---

### Violation 4: Missing Persistence Model Layer

**Current Structure**:
```
domain/entities/ ── JPA-annotated entities (WRONG)
application/ports/ ── interfaces return domain entities (UNCLEAR)
infrastructure/repository/ ── JpaRepository interfaces (EXPOSED)
infrastructure/adapter/persistence/ ── Adapters that work with domain entities
```

**Proper Structure Should Be**:
```
domain/entities/ ── Clean POJOs, no framework annotations
application/ports/ ── Interfaces in application layer
infrastructure/persistence/models/ ── @Entity classes for ORM
infrastructure/persistence/mappers/ ── Domain ↔ Persistence converters
infrastructure/repository/ ── Hidden behind adapters (already correct)
infrastructure/adapter/persistence/ ── Use mappers to convert
```

---

## Architecture Violations Summary

| Layer | Violation | Severity | Fix |
|-------|-----------|----------|-----|
| Domain | JPA annotations in 17 entities | CRITICAL | Remove ORM, create persistence models |
| Domain | Imports jakarta.persistence | CRITICAL | Remove all framework imports |
| Application | @Service, @Transactional, @PostConstruct | MEDIUM | Move lifecycle concerns to infrastructure |
| Ports | Return domain entities directly | MEDIUM | Clear boundary between domain and persistence |
| Infrastructure | Partial adapter implementation | LOW | Complete coverage of all repositories |

---

## Clean Architecture Dependency Rules (Violated)

### Rule 1: Inward Dependency Only
```
✓ Correct:  Infrastructure → Application → Domain
❌ Violated: Domain → Jakarta.Persistence (Framework)
```

### Rule 2: Domain Independence
```
✓ Correct:  Domain uses only JDK
❌ Violated: Domain has @Entity, @Column, @Table, @PrePersist, etc.
```

### Rule 3: Layer Isolation
```
✓ Correct:  Application has ports; Infrastructure implements ports
❌ Violated: Ports return domain entities (crosses boundary)
```

---

## Recommended Fix Strategy

### Phase 1: Separate Domain from Persistence (CRITICAL)
1. Create `infrastructure/persistence/models/` — JPA-annotated entities
2. Strip domain entities of all `jakarta.persistence.*` imports
3. Create mappers in `infrastructure/persistence/mappers/`
4. Update adapters to map domain ↔ persistence models

### Phase 2: Clarify Port Contracts (IMPORTANT)
1. Define whether ports work with domain entities or DTOs
2. If domain entities, add clear documentation
3. Create DTOs for API boundaries (already exists partially)

### Phase 3: Application Layer Cleanup (MEDIUM)
1. Keep `@Service` and `@Component` for Spring DI (necessary for framework)
2. Move `@PostConstruct` to Infrastructure initialization classes
3. Keep `@Transactional` but document it as application-level concern

### Phase 4: Validation & Testing
1. Verify domain entities have zero framework imports
2. Ensure infrastructure layer properly adapts domain and persistence models
3. Add integration tests for mapper correctness

---

## Implementation Priority

| Priority | Task | Effort | Impact |
|----------|------|--------|--------|
| P0 | Remove JPA from domain entities | HIGH | CRITICAL (unblocks all refactoring) |
| P0 | Create persistence models | HIGH | CRITICAL |
| P1 | Create mappers | HIGH | HIGH (enables adapter fixes) |
| P1 | Update infrastructure adapters | MEDIUM | HIGH |
| P2 | Move Spring lifecycle to infrastructure | MEDIUM | MEDIUM |
| P3 | Document port contracts | LOW | MEDIUM (clarity) |

---

## Estimated Effort

- **Phase 1 (Separation)**: ~800-1000 lines of code changes
  - Removal of annotations from 17 entities
  - Creation of 17 parallel persistence models
  - Creation of 17 mapper classes
  - Updates to 26 persistence adapters

- **Phase 2-4**: ~200-300 lines of changes

---

## References

- Uncle Bob's Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- Backend System Instructions: `.github/instructions/backend-system.instructions.md`
- Current Architecture Rules: Defined in backend system instructions (canonical structure)

---

## Next Steps

1. ✅ Document violations (THIS REPORT)
2. ⏳ Implement Phase 1: Domain/Persistence Separation
3. ⏳ Implement Phase 2-4: Refinements
4. ⏳ Validation and testing

