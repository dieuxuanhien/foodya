# Foodya Backend – Architecture Guidelines
## Enforcing Uncle Bob Clean Architecture

**Date:** March 31, 2026  
**Status:** ACTIVE ENFORCEMENT  
**Validation:** ArchUnit (11 tests, all passing)

---

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Layer Responsibilities](#layer-responsibilities)
3. [Dependency Rules](#dependency-rules)
4. [Implementation Patterns](#implementation-patterns)
5. [Code Review Checklist](#code-review-checklist)
6. [Common Mistakes & Fixes](#common-mistakes--fixes)

---

## Quick Reference

### Layer Structure

```
com.foodya.backend/
├── domain/
│   ├── model/                 ← Pure business logic, ZERO framework imports
│   ├── persistence/           ← Entities WITH JPA (temp, migrate to infrastructure)
│   └── service/               ← Shared domain services (optional, rare)
├── application/
│   ├── service/               ← Use-case orchestration (17 services)
│   ├── port/
│   │   └── out/              ← Output ports (adapters implement these)
│   ├── dto/                  ← Application boundary contracts
│   ├── exception/            ← Application exceptions
│   ├── constant/             ← Constants
│   └── security/             ← Auth DTOs, constants (not filters)
├── infrastructure/
│   ├── adapter/
│   │   ├── persistence/      ← Persistence adapters (implement ports)
│   │   ├── integration/      ← External API adapters
│   │   └── config/           ← Config adapters
│   ├── repository/           ← Spring Data JPA repositories (private to adapters)
│   ├── persistence/          ← JPA entities, converters
│   ├── config/               ← Spring configuration
│   ├── integration/          ← External API clients
│   └── security/             ← Security filters, providers
└── interfaces/
    └── rest/
        ├── controller/       ← HTTP entrypoints (thin layer)
        ├── dto/             ← REST DTOs (request/response)
        ├── mapper/          ← DTO mappers
        └── exception/       ← HTTP exception handlers
```

### Dependency Direction (Strict)

```
Interfaces (REST)
     ↓ (depends on)
Application (Use Cases + Ports)
     ↓ (depends on)
Domain (Business Rules)

Infrastructure (Adapters)
     ↓ (implements)
Application Ports
```

**What this means:**
- ✅ Domain has NO dependencies
- ✅ Application only depends on domain + application.port
- ✅ Infrastructure depends on application.port + domain
- ✅ Interfaces (REST) depends on application
- ❌ Nothing crosses back up

---

## Layer Responsibilities

### 1) Domain Layer
**Purpose:** Contain all business rules independent of framework

#### ✅ Implementation Rules

```java
// domain/model/UserRole.java ✅ CORRECT
package com.foodya.backend.domain.model;

public enum UserRole {
    CUSTOMER, MERCHANT, ADMIN
}
// ZERO imports except java.*, no framework
```

```java
// domain/model/OrderStatus.java ✅ CORRECT
package com.foodya.backend.domain.model;

public enum OrderStatus {
    PENDING, CONFIRMED, CANCELLED
    // Pure enum, no Spring, no JPA
}
```

#### ❌ What NOT to do

```java
// ❌ WRONG: Importing Spring
@Entity
@Table(name = "users")
public class User {
    // Don't put @Entity here!
}

// ❌ WRONG: Importing application layer
public class UserDomainModel {
    private final UserAccountPort port; // NO! Domain can't know about ports
}

// ❌ WRONG: Importing infrastructure
public class Order {
    private final OrderRepository repo; // NO! Domain can't know about repos
}
```

#### 📋 Checklist

- [ ] No `@Entity`, `@Table`, `@Column` in domain/model
- [ ] No `import org.springframework`
- [ ] No `import jakarta.persistence`
- [ ] No imports from `application` or `infrastructure`
- [ ] Only imports from `domain.model` and `java.*`

---

### 2) Application Layer
**Purpose:** Orchestrate use cases using ports (dependency inversion)

#### ✅ Implementation  Pattern

```java
// application/service/CartService.java ✅ CORRECT
package com.foodya.backend.application.service;

import com.foodya.backend.application.port.out.CartPort;
import com.foodya.backend.application.port.out.MenuItemPort;
import com.foodya.backend.application.dto.ActiveCartView;
import com.foodya.backend.domain.model.CartStatus;
import com.foodya.backend.domain.persistence.Cart;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    
    private final CartPort cartPort;              // ✅ Depends on port
    private final MenuItemPort menuItemPort;      // ✅ Depends on port
    
    public CartService(CartPort cartPort, MenuItemPort menuItemPort) {
        this.cartPort = cartPort;
        this.menuItemPort = menuItemPort;
    }
    
    public ActiveCartView getActiveCart(UUID customerId) {
        // Use-case logic here
        Cart cart = cartPort.findByCustomerId(customerId);
        return new ActiveCartView(cart);
    }
}
```

#### ✅ Rules for Services

```java
// Services MAY import:
import com.foodya.backend.domain.*;                    // ✅ Domain models
import com.foodya.backend.application.port.out.*;     // ✅ Ports
import com.foodya.backend.application.dto.*;          // ✅ DTOs
import com.foodya.backend.application.exception.*;    // ✅ Exceptions
import org.springframework.stereotype.Service;         // ✅ Stereotype only
import org.springframework.transaction.*;              // ✅ Transactions only

// Services MUST NOT import:
import com.foodya.backend.infrastructure.repository.*;     // ❌
import com.foodya.backend.infrastructure.persistence.*;    // ❌
import com.foodya.backend.interfaces.rest.*;              // ❌
```

#### ❌ Common Mistakes

```java
// ❌ WRONG: Injecting repository directly
@Service
public class CartService {
    private final CartRepository cartRepository;  // ❌ Direct dependency
    // Should use CartPort interface instead
}

// ❌ WRONG: Importing REST DTOs
@Service
public class CartService {
    private final CartRequest request;  // ❌ Mixing layers
    // Use application DTOs instead
}

// ❌ WRONG: Calling other services for cross-domain logic
@Service
public class OrderService {
    private final CartService cartService;  // Consider if this violates boundaries
    private final PaymentService paymentService;
    // If services grow, consider event bus
}
```

#### 📋 Checklist

- [ ] Service depends only on ports (`application.port.out.*`)
- [ ] Service imports domain models (`domain.model.*`, `domain.persistence.*`)
- [ ] Service does NOT import repositories
- [ ] Service dependencies are injected via constructor
- [ ] Service is annotated `@Service`
- [ ] Service is transactional if needed (`@Transactional`)

---

### 3) Infrastructure Layer
**Purpose:** Implement application ports using concrete technologies

#### ✅ Adapter Pattern

```java
// infrastructure/adapter/persistence/CartPersistenceAdapter.java ✅ CORRECT
package com.foodya.backend.infrastructure.adapter.persistence;

import com.foodya.backend.application.port.out.CartPort;  // ✅ Implements port
import com.foodya.backend.domain.persistence.Cart;
import com.foodya.backend.infrastructure.repository.CartRepository;
import org.springframework.stereotype.Component;

@Component
public class CartPersistenceAdapter implements CartPort {
    
    private final CartRepository repository;  // ✅ Repository is private to adapter
    
    public CartPersistenceAdapter(CartRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Cart findByCustomerId(UUID customerId) {
        // Adapter converts persistence to domain
        return repository.findByCustomerUserId(customerId);
    }
    
    @Override
    public void save(Cart cart) {
        repository.save(cart);
    }
}
```

#### ✅ Rules for Adapters

```java
// Adapters MAY import:
import com.foodya.backend.application.port.out.*;     // ✅ Ports to implement
import com.foodya.backend.domain.*;                    // ✅ Domain models
import com.foodya.backend.infrastructure.*;            // ✅ Other infrastructure
import org.springframework.stereotype.Component;       // ✅ Only Component
import org.springframework.data.jpa.repository.*;      // ✅ JPA/Spring Data
import jakarta.persistence.*;                          // ✅ JPA annotations

// Adapters MUST NOT import:
import com.foodya.backend.application.service.*;      // ❌
import com.foodya.backend.interfaces.rest.*;          // ❌
```

#### 📋 Checklist

- [ ] Adapter implements port interface
- [ ] Adapter is annotated `@Component`
- [ ] Adapter has no public constructor, Spring injects
- [ ] Repository is private to adapter
- [ ] Adapter converts between domain and persistence models (if needed)

---

### 4) Interface (REST) Layer
**Purpose:** Thin HTTP entrypoint layer

#### ✅ Thin Controller Pattern

```java
// interfaces/rest/controller/CartController.java ✅ CORRECT
package com.foodya.backend.interfaces.rest.controller;

import com.foodya.backend.application.service.CartService;           // ✅ Service
import com.foodya.backend.application.dto.ActiveCartView;            // ✅ App DTOs
import com.foodya.backend.interfaces.rest.dto.CartItemRequest;       // ✅ REST DTOs
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {
    
    private final CartService cartService;  // ✅ Only depends on service
    
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    
    @GetMapping
    public ResponseEntity<?> getCart(Authentication auth) {
        // 1. Extract context (minimal)
        UUID customerId = (UUID) auth.getPrincipal();
        
        // 2. Call service
        ActiveCartView cart = cartService.getActiveCart(customerId);
        
        // 3. Map to REST DTO
        CartResponse response = CartMapper.toResponse(cart);
        
        // 4. Return HTTP response
        return ResponseEntity.ok(response);
    }
}
```

#### ✅ Rules for Controllers

```java
// Controllers MAY import:
import com.foodya.backend.application.service.*;      // ✅ Services
import com.foodya.backend.application.dto.*;          // ✅ Application DTOs
import com.foodya.backend.interfaces.rest.dto.*;      // ✅ REST DTOs
import org.springframework.web.bind.annotation.*;      // ✅ Web annotations
import org.springframework.security.core.*;            // ✅ Security context

// Controllers MUST NOT import:
import com.foodya.backend.domain.persistence.*;       // ❌
import com.foodya.backend.infrastructure.*;           // ❌
```

#### 📋 Checklist

- [ ] Controller is annotated `@RestController`
- [ ] Controller only calls services, never repositories
- [ ] Controller has minimal logic (parsing, mapping, returning)
- [ ] Controller maps request DTOs → service DTOs → response DTOs
- [ ] No business logic in controller

---

## Dependency Rules

### The Core Rule

**No layer can depend on anything outside the rules below:**

| From \ To | Domain | Application | Infrastructure | Interfaces |
|-----------|--------|-------------|-----------------|------------|
| **Domain** | ✅ | ❌ | ❌ | ❌ |
| **Application** | ✅ | ✅ | ❌ (ports ok) | ❌ |
| **Infrastructure** | ✅ | ✅ (ports) | ✅ | ❌ |
| **Interfaces** | ❌ | ✅ | ❌ | ✅ |

### Rules Enforced by ArchUnit (11 Tests)

1. ✅ **Application must not depend on infrastructure** – Prevents coupling
2. ✅ **Infrastructure must not depend on interfaces** – Prevents web leakage
3. ✅ **Domain must not depend on application** – Ensures isolation
4. ✅ **Domain must not depend on infrastructure** – Ensures portability
5. ✅ **Domain/model must stay framework-free** – Ensures purity
6. ✅ **Domain/persistence must not use Spring** – Prevents Spring spread
7. ✅ **REST DTOs must not depend on persistence entities** – Ensures mapping
8. ✅ **REST controllers must not depend on infrastructure** – Ensures boundaries
9. ✅ **Application services must not depend on repositories** – Ensures inversion
10. ✅ **Application must not depend on repositories directly** – Ensures ports
11. ✅ **Ports must be defined in application.port** – Ensures organization

**Run tests before every commit:**
```bash
mvn test -Dtest=ArchitectureRulesTests
```

---

## Implementation Patterns

### Pattern 1: Adding a New Use Case

**Example:** Create "Review Order Status" use case

#### Step 1: Create Port (application/port/out)
```java
// application/port/out/OrderPort.java – ADD METHOD
public interface OrderPort {
    Order findById(UUID orderId);  // ✅ NEW
    // existing methods...
}
```

#### Step 2: Create DTO (application/dto)
```java
// application/dto/OrderStatusView.java – NEW FILE
package com.foodya.backend.application.dto;

public class OrderStatusView {
    private UUID orderId;
    private String status;
    private OffsetDateTime createdAt;
    // getters, constructor
}
```

#### Step 3: Create Service (application/service)
```java
// application/service/OrderStatusService.java – NEW FILE
@Service
public class OrderStatusService {
    private final OrderPort orderPort;
    
    public OrderStatusService(OrderPort orderPort) {
        this.orderPort = orderPort;
    }
    
    public OrderStatusView getOrderStatus(UUID orderId) {
        Order order = orderPort.findById(orderId);
        if (order == null) {
            throw new NotFoundException("Order not found");
        }
        return new OrderStatusView(...);
    }
}
```

#### Step 4: Implement Adapter (infrastructure/adapter/persistence)
```java
// infrastructure/adapter/persistence/OrderPersistenceAdapter.java – MODIFY
@Component
public class OrderPersistenceAdapter implements OrderPort {
    private final OrderRepository repository;
    
    @Override
    public Order findById(UUID orderId) {  // ✅ NEW
        return repository.findById(orderId).orElse(null);
    }
}
```

#### Step 5: Add Controller (interfaces/rest/controller)
```java
// interfaces/rest/controller/OrderStatusController.java – NEW FILE
@RestController
@RequestMapping("/api/v1/order-status")
public class OrderStatusController {
    private final OrderStatusService orderStatusService;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getStatus(
        @PathVariable UUID orderId,
        Authentication auth) {
        OrderStatusView status = orderStatusService.getOrderStatus(orderId);
        return ResponseEntity.ok(OrderStatusMapper.toResponse(status));
    }
}
```

#### ✅ Verification

```bash
# Run architecture tests
mvn test -Dtest=ArchitectureRulesTests
# Should pass all 11 tests

# Run integration tests
mvn test -Dtest=OrderIntegrationTests
```

---

### Pattern 2: Adding a Domain Value Object

**Example:** Add `OrderAddress` value object

#### ✅ What TO do

```java
// domain/model/OrderAddress.java – NEW FILE
package com.foodya.backend.domain.model;

public class OrderAddress {  // ✅ NO JPA, NO Spring
    private final String street;
    private final String city;
    private final String zipCode;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    
    public Order Address(String street, String city, String zip, double lat, double lng) {
        // Validation here
        if (street == null || street.isBlank()) {
            throw new IllegalArgumentException("Street cannot be blank");
        }
        this.street = street;
        this.city = city;
        this.zipCode = zip;
        this.latitude = new BigDecimal(lat);
        this.longitude = new BigDecimal(lng);
    }
    
    public double distanceTo(OrderAddress other) {
        // Business logic
        return calculateDistance(this.latitude, this.longitude, other.latitude, other.longitude);
    }
}
```

#### ❌ What NOT to do

```java
// ❌ WRONG: Putting JPA in domain
@Entity  // ❌ NO!
@Embeddable  // ❌ NO!
public class OrderAddress {
    @Column  // ❌ NO!
}

// ❌ WRONG: Importing Spring
public class OrderAddress {
    @Autowired  // ❌ NO!
    private GeoService geoService;
}
```

---

### Pattern 3: Migrating Entity to New Structure
### ⏰ Future (Phase 2.1)

When you migrate an entity from `domain/persistence` to split into pure domain + infrastructure entity:

```
BEFORE:
domain/persistence/Order.java (with @Entity)

AFTER:
domain/core/Order.java (pure business object)
infrastructure/persistence/entity/OrderJpaEntity.java (with @Entity)
infrastructure/adapter/mapper/OrderMapper.java (convert between them)
```

**This is documented in Phase 2.1 of the refactor blueprint.**

---

## Code Review Checklist

When reviewing a PR with backend changes, check:

### ✅ Layer Isolation
- [ ] No file imports from "higher" layers (Interfaces ← Application ← Domain)
- [ ] `application/*`  only imports `domain/*` and `application.*`
- [ ] `infrastructure/*` only imports `application.port.*` and `infrastructure.*`
- [ ] Domain only imports other domain modules

### ✅ Dependency Inversion
- [ ] Services depend on ports, not repositories
- [ ] Services injected via constructor (`@Autowired` on constructor, not field)
- [ ] Adapters implement ports from `application.port.out.*`

### ✅ Framework Boundaries
- [ ] Domain models have ZERO Spring/JPA imports
- [ ] `@Entity` only in `infrastructure/persistence`
- [ ] `@RestController` only in `interfaces/rest/controller`
- [ ] `@Service` in `application/service` only

### ✅ DTO Mapping
- [ ] REST controllers use REST DTOs (`interfaces/rest/dto`)
- [ ] Services use application DTOs (`application/dto`)
- [ ] Controllers map REST DTO → Application DTO → Service → Application DTO → REST DTO
- [ ] Entities are never exposed in REST responses

### ✅ Test Coverage
- [ ] Architecture tests pass: `mvn test -Dtest=ArchitectureRulesTests`
- [ ] Integration tests for affected flows pass
- [ ] New domains have at least basic unit test coverage

### ✅ Documentation
- [ ] New ports are documented (what they do, why)
- [ ] Services have clear responsibility (one reason to change)
- [ ] Complex adapters have inline comments

---

## Common Mistakes & Fixes

### Mistake 1: Service Importing Repository

```java
// ❌ WRONG
@Service
public class OrderService {
    private final OrderRepository repo;  // ❌
}

// ✅ CORRECT
@Service
public class OrderService {
    private final OrderPort port;  // ✅ Depends on port
}
```

**Why:** Breaks dependency inversion. If you change database, every service breaks.  
**Fix:** Define port in `application/port/out`, implement in adapter.

---

### Mistake 2: Domain Importing Framework

```java
// ❌ WRONG
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private UUID id;
}

// ✅ CORRECT
public class Order {  // NO @Entity here
    private UUID id;
    // business logic
}
```

**Why:** Domain shouldn't know about persistence details.  
**Fix:** Split entity into pure domain (`domain/core/Order`) and JPA entity (`infrastructure/persistence/entity/OrderJpaEntity`). This is Phase 2.1 work.

---

### Mistake 3: REST Controller with Business Logic

```java
// ❌ WRONG
@RestController
public class OrderController {
    @PostMapping("/checkout")
    public void checkout(@RequestBody Order order) {
        // Business logic here
        if (order.getTotalPrice() > 1000) {
            order.applyDiscount(0.1);  // ❌
        }
        // ...
    }
}

// ✅ CORRECT
@RestController
public class OrderController {
    private final OrderCheckoutService service;
    
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequest request) {
        // Mapping only
        CheckoutCommand cmd = CheckoutMapper.toCommand(request);
        
        // Call service (business logic here)
        OrderCheckoutResult result = service.checkout(cmd);
        
        // Map response
        CheckoutResponse response = CheckoutMapper.toResponse(result);
        return ResponseEntity.ok(response);
    }
}
```

**Why:** Business logic in controllers is untestable and breaks SRP.  
**Fix:** Move logic to services, keep controller thin.

---

### Mistake 4: Controller Using Domain/Persistence DTOs

```java
// ❌ WRONG
@RestController
public class CartController {
    @GetMapping
    public Cart getCart() {  // ❌ Returns domain entity
        Cart cart = cartService.getCart();
        return cart;  // NO! Exposes internals
    }
}

// ✅ CORRECT
@RestController
public class CartController {
    @GetMapping
    public ResponseEntity<?> getCart() {
        ActiveCartView cart = cartService.getCart();  // Application DTO
        CartResponse response = CartMapper.toResponse(cart);  // REST DTO
        return ResponseEntity.ok(response);
    }
}
```

**Why:** Leaks domain details to API consumer. Couples API to implementation.  
**Fix:** Always use REST DTOs in responses. Use mappers.

---

### Mistake 5: Adapter Importing Application Service

```java
// ❌ WRONG
@Component
public class CartPersistenceAdapter implements CartPort {
    private final CartService service;  // ❌
    
    @Override
    public void sync() {
        service.validateCart();  // ❌
    }
}

// ✅ CORRECT
@Component
public class CartPersistenceAdapter implements CartPort {
    private final CartRepository repository;  // ✅
    
    @Override
    public void sync() {
        // Adapter logic only
        repository.flush();
    }
}
```

**Why:** Circular dependency. Adapters shouldn't know about services.  
**Fix:** Adapters are simple wrappers. Business logic belongs in services.

---

## Running Architecture Validation

### Before Every Commit

```bash
# Run all architecture rules
mvn test -Dtest=ArchitectureRulesTests

# Expected output:
# Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

### If Architecture Test Fails

1. **Read the error message carefully** – It tells you exactly which rule was violated
2. **Identify the violation** – Usually an import statement crossing layers
3. **Fix it** – Don't add exemptions, fix the code
4. **Re-run** – Verify it passes

Example:
```
VIOLATION: application.service.CartService imports
infrastructure.repository.CartRepository

FIX: Change CartRepository dependency to CartPort
```

### Rebuilding Index (GitNexus)

After significant refactoring, rebuild the architecture index:

```bash
npx gitnexus analyze
```

This updates the code intelligence graph so better refactoring tools work correctly.

---

## Further Reading

- **Clean Architecture (Uncle Bob):** Robert C. Martin, 2017
- **Backend System Instructions:** `.github/instructions/backend-system.instructions.md`
- **Architecture Refactor Blueprint:** `docs/CLEAN_ARCHITECTURE_REFACTOR_BLUEPRINT.md`
- **ArchUnit Documentation:** https://www.archunit.org/

---

## Contact & Questions

For architecture questions or clarifications:
- Review existing code in similar domains
- Check the architecture tests (`ArchitectureRulesTests`)
- Ask during code review (don't merge violations)

---

**Last Updated:** March 31, 2026  
**Status:** ACTIVE – All 11 ArchUnit tests passing  
**Next Phase:** Phase 2.1 (Domain/Persistence entity split)
