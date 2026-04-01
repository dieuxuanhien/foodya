# Clean Architecture Migration Guide

**Status**: In Progress  
**Completed**: Order family (Order, OrderItem, OrderPayment, OrderReview handling shows pattern)  
**Remaining**: 13 entities to migrate

---

## Migration Pattern (Order as Template)

### Step 1: Create Persistence Model

**File**: `infrastructure/persistence/models/{Entity}PersistenceModel.java`

```java
@Entity
@Table(name = "table_name")
public class {Entity}PersistenceModel {
    @Id
    // All JPA annotations go HERE
    // Move ALL @Column, @Enumerated, @PrePersist, etc. from domain entity
}
```

**Files Created**:
- ✅ `infrastructure/persistence/models/OrderPersistenceModel.java`

**Remaining**:
- [ ] MenuItem, MenuCategory, Restaurant (Catalog domain)
- [ ] Cart, CartItem (Cart domain)
- [ ] UserAccount, RefreshToken, PasswordResetChallenge (Auth domain)
- [ ] SystemParameter, AuditLog, NotificationLog (Core domain)
- [ ] AiChatHistory (AI domain)
- [ ] OrderItem, OrderPayment, OrderReview, DeliveryTrackingPoint (Order sub-entities)

---

### Step 2: Validate & Clean Domain Entity

**File**: `domain/entities/{Entity}.java`

**Remove**:
- ALL `import jakarta.persistence.*`
- ALL `import javax.persistence.*`
- ALL JPA annotations: `@Entity`, `@Table`, `@Column`, `@Enumerated`, `@Id`, `@PrePersist`, `@PreUpdate`, etc.

**Keep**:
- All business logic and invariants
- Domain-specific value objects and enums
- Private methods for state transitions

**Steps**:
1. Delete imports
2. Remove annotations from class
3. Remove annotations from all fields
4. Remove or convert @PrePersist/@PreUpdate lifecycle methods to regular init methods
5. Verify domain still has all business logic

**Example**:
```java
// BEFORE (with JPA)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "id")
    private UUID id;
    
    @PrePersist
    void onCreate() { ... }
}

// AFTER (clean domain)
public class Order {
    private UUID id;
    
    void onCreate() { ... }  // Now just a regular method
}
```

**Completed**:
- ✅ `domain/entities/Order.java` — JPA annotations removed

**Remaining**: 16 other entities

---

### Step 3: Create Mapper

**File**: `infrastructure/persistence/mappers/{Entity}Mapper.java`

```java
@Component
public class {Entity}Mapper {
    public {Entity} toDomain({Entity}PersistenceModel model) { ... }
    public {Entity}PersistenceModel toPersistence({Entity} domain) { ... }
}
```

**Strategy**:
- 1:1 field mapping if all field names match
- Create named helper methods if transformations needed
- Always validate nulls

**Completed**:
- ✅ `infrastructure/persistence/mappers/OrderMapper.java`

**Remaining**: Create mappers for 16 other entities

---

### Step 4: Update Repository Interface

**File**: `infrastructure/repository/{Entity}Repository.java`  
**Change**: Type parameter from `{Entity}` → `{Entity}PersistenceModel`

```java
// BEFORE
public interface OrderManagementRepository extends JpaRepository<Order, UUID> { }

// AFTER
public interface OrderManagementRepository extends JpaRepository<OrderPersistenceModel, UUID> { }
```

**All method signatures must update return types**:
```java
// BEFORE: List<Order> findByCustomerUserId(...)
// AFTER:  List<OrderPersistenceModel> findByCustomerUserId(...)
```

**Completed**:
- ✅ `infrastructure/repository/OrderManagementRepository.java`
- ✅ `infrastructure/repository/OrderRepository.java`
- ✅ `infrastructure/repository/AdminOrderRepository.java`

**Remaining**: ~20 repository interfaces

---

### Step 5: Update Persistence Adapter

**File**: `infrastructure/adapter/persistence/{Entity}PersistenceAdapter.java`

```java
@Component
public class {Entity}PersistenceAdapter implements {Entity}Port {
    private final {Entity}Repository repository;
    private final {Entity}Mapper mapper;

    public {Entity}PersistenceAdapter({Entity}Repository repository, {Entity}Mapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<{Entity}> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public {Entity} save({Entity} entity) {
        var model = mapper.toPersistence(entity);
        var saved = repository.save(model);
        return mapper.toDomain(saved);
    }

    // Apply mapper.toDomain() after every repository call
}
```

**Key Pattern**: Wrap every repository result with `mapper::toDomain()`

**Completed**:
- ✅ `infrastructure/adapter/persistence/OrderManagementPersistenceAdapter.java`
- ✅ `infrastructure/adapter/persistence/OrderPersistenceAdapter.java`
- ✅ `infrastructure/adapter/persistence/AdminOrderPersistenceAdapter.java`

**Remaining**: ~26 adapter files

---

## Batch Application Strategy

### By Domain (Grouped Implementation)

#### Batch 1: Catalog (3 entities, 2 repos, 3 adapters)
- `MenuItem` / `MenuItemPersistenceModel` / `MenuItemMapper`
- `MenuCategory` / `MenuCategoryPersistenceModel` / `MenuCategoryMapper`
- `Restaurant` / `RestaurantPersistenceModel` / `RestaurantMapper`

#### Batch 2: Cart (2 entities, 2 repos, 2 adapters)
- `Cart` / `CartPersistenceModel` / `CartMapper`
- `CartItem` / `CartItemPersistenceModel` / `CartItemMapper`

#### Batch 3: Auth (3 entities, 3 repos, 3 adapters)
- `UserAccount` / `UserAccountPersistenceModel` / `UserAccountMapper`
- `RefreshToken` / `RefreshTokenPersistenceModel` / `RefreshTokenMapper`
- `PasswordResetChallenge` / `PasswordResetChallengePersistenceModel` / `PasswordResetChallengeMapper`

#### Batch 4: Core (3 entities, 3 repos, 3 adapters)
- `SystemParameter` / `SystemParameterPersistenceModel` / `SystemParameterMapper`
- `AuditLog` / `AuditLogPersistenceModel` / `AuditLogMapper`
- `NotificationLog` / `NotificationLogPersistenceModel` / `NotificationLogMapper`

#### Batch 5: Order Sub-Entities (4 entities, 4 repos, 4 adapters)
- `OrderItem` / `OrderItemPersistenceModel` / `OrderItemMapper`
- `OrderPayment` / `OrderPaymentPersistenceModel` / `OrderPaymentMapper`
- `OrderReview` / `OrderReviewPersistenceModel` / `OrderReviewMapper`
- `DeliveryTrackingPoint` / `DeliveryTrackingPointPersistenceModel` / `DeliveryTrackingPointMapper`

#### Batch 6: AI (1 entity, 1 repo, 1 adapter)
- `AiChatHistory` / `AiChatHistoryPersistenceModel` / `AiChatHistoryMapper`

---

## Verification Checklist (Per Entity)

After migration, verify each entity:

```
[ ] Domain entity has NO jakarta.persistence imports
[ ] Domain entity has NO @Entity, @Table, @Column annotations
[ ] Domain entity has NO @Id, @PrePersist, @PreUpdate, @Enumerated, etc.
[ ] Domain entity retains all business logic and methods
[ ] Persistence model has ALL JPA annotations
[ ] Persistence model is in infrastructure/persistence/models/
[ ] Mapper exists and filters both directions (toDomain, toPersistence)
[ ] Repository type param changed to PersistenceModel
[ ] Adapter constructor injects mapper
[ ] Adapter wraps all repository calls with mapper.toDomain()
[ ] Adapter converts domain to model before repository.save()
[ ] Tests pass (run test suite)
```

---

## Command Reference (Batch Processing)

### After completing each batch:
```bash
# Rebuild to check for compile errors
mvn clean compile

# Run tests for that domain
mvn test -Dtest=**{DomainName}**Test

# Check git diff
git diff --stat
```

---

## Timeline Estimate

- **Order family (Done)**: ~3 hours (1 entity + 3 adapters as template)
- **Batch 1-5**: ~12-15 hours (\~20 min per entity \* 16 entities)
  - Parallelizable: Different team members can work on different batches
- **Testing & Validation**: ~3-4 hours
- **Total**: ~18-20 hours of focused work

---

## Prevention: Linting Rules

After migration, add pre-commit hook to prevent regression:

### `.githook/pre-commit` or via `husky`:
```bash
#!/bin/bash
# Prevent JPA annotations in domain/entities/
if grep -r '@Entity\|@Table\|@Column\|jakarta.persistence' src/main/java/com/foodya/backend/domain/entities/; then
    echo "❌ FAIL: Found JPA annotations in domain layer"
    echo "   Domain entities must be framework-independent"
    exit 1
fi
```

---

## Next Steps

1. ⏳ **Batches 1-6**: Apply pattern to all entities (Estimated: ~16 hours)
2. ⏳ **Testing**: Full integration test suite (Estimated: ~2-3 hours)
3. ⏳ **Documentation**: Update architecture guide (Estimated: ~1 hour)
4. ⏳ **Code Review**: Ensure consistency across all mappers (Estimated: ~1-2 hours)

---

## Questions / Troubleshooting

### Q: Do we need Unit Tests for mappers?
**A**: Yes. Example:
```java
@Test
void testToDomainMapsAllFields() {
    OrderPersistenceModel model = new OrderPersistenceModel();
    model.setId(UUID.randomUUID());
    // ... set all fields
    
    Order domain = mapper.toDomain(model);
    
    assertEquals(model.getId(), domain.getId());
    // ... verify all fields
}
```

### Q: What about nullable fields?
**A**: Preserve nullability. If a persistence model field can be null, so can the domain entity.

### Q: Do entity IDs need special handling?
**A**: No, treat like any other field (e.g., `id` → `id`).

### Q: Can I use a library like MapStruct?
**A**: Yes, refactor after all entities are migrated. For now, manual mapping ensures clarity.

---

## Related Documentation

- Architecture Violation Report: `ARCHITECTURE_VIOLATION_REPORT.md`
- Backend System Instructions: `.github/instructions/backend-system.instructions.md`
- Clean Architecture Reference: Uncle Bob's Clean Architecture blog

