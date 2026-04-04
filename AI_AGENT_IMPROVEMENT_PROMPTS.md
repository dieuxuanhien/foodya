# AI Agent Sequential Improvement Prompts - Foodya Backend

**Purpose:** Step-by-step prompts for AI coding agents to systematically improve the Foodya backend architecture.

**Usage:**
1. Execute prompts in sequential order (1 → 8)
2. Each prompt is independent and can be assigned to a different agent
3. Verify success criteria before moving to next prompt
4. Total estimated effort: 34-45 hours

**Context:** See ARCHITECTURE_RATING_REPORT.md for detailed analysis

---

## Prompt 1: Complete Domain Entity Migration
**Priority:** P0 (CRITICAL)
**Effort:** 8-10 hours
**Prerequisites:** Read MIGRATION_GUIDE_CLEAN_ARCHITECTURE.md, ARCHITECTURE_VIOLATION_REPORT.md

### Task Description
Complete Clean Architecture domain entity migration for the remaining 16 entities (out of 17 total). The Order entity migration is already complete and serves as the reference template.

### Objective
Ensure all domain entities are framework-independent (no JPA annotations) by creating separate persistence models in the infrastructure layer.

### Execution Steps

#### Batch 1: Catalog Domain (Priority: 1)
**Entities:** MenuItem, MenuCategory, Restaurant

For each entity:
1. Create `{Entity}PersistenceModel.java` in `infrastructure/persistence/models/`
   - Copy all fields from domain entity
   - Add all JPA annotations (@Entity, @Table, @Column, @Id, etc.)
   - Use OrderPersistenceModel.java as template

2. Clean `domain/entities/{Entity}.java`
   - Remove ALL imports: `jakarta.persistence.*`
   - Remove ALL annotations: @Entity, @Table, @Column, @Id, @Enumerated, @PrePersist, etc.
   - Keep ALL business logic methods
   - Convert @PrePersist methods to regular methods

3. Create `{Entity}Mapper.java` in `infrastructure/persistence/mappers/`
   - Implement `toDomain(PersistenceModel)` method
   - Implement `toPersistence(DomainEntity)` method
   - Use OrderMapper.java as template

4. Update repository interfaces in `infrastructure/repository/`
   - Change type parameter from `{Entity}` to `{Entity}PersistenceModel`
   - Example: `JpaRepository<MenuItem, UUID>` → `JpaRepository<MenuItemPersistenceModel, UUID>`

5. Update persistence adapters in `infrastructure/adapter/persistence/`
   - Inject mapper in constructor
   - Wrap all repository calls: `repository.findById(id).map(mapper::toDomain)`
   - Convert before save: `repository.save(mapper.toPersistence(entity))`

6. Verify
   ```bash
   mvn clean compile
   mvn test -Dtest=**Catalog**Test
   grep -r "jakarta.persistence" backend/src/main/java/com/foodya/backend/domain/entities/MenuItem.java
   # Should return: no matches
   ```

#### Batch 2: Cart Domain
**Entities:** Cart, CartItem

(Repeat steps 1-6 above for each entity)

#### Batch 3: Auth Domain
**Entities:** UserAccount, RefreshToken, PasswordResetChallenge

(Repeat steps 1-6 above for each entity)

#### Batch 4: Core Domain
**Entities:** SystemParameter, AuditLog, NotificationLog

(Repeat steps 1-6 above for each entity)

#### Batch 5: Order Sub-entities
**Entities:** OrderItem, OrderPayment, OrderReview, DeliveryTrackingPoint

(Repeat steps 1-6 above for each entity)

#### Batch 6: AI Domain
**Entities:** AiChatHistory

(Repeat steps 1-6 above)

### Success Criteria
- [ ] All 17 domain entities have ZERO `jakarta.persistence` imports
- [ ] 17 persistence models exist in `infrastructure/persistence/models/`
- [ ] 17 mappers exist in `infrastructure/persistence/mappers/`
- [ ] All repository interfaces use PersistenceModel types
- [ ] All persistence adapters inject mappers and convert types
- [ ] `mvn clean compile` succeeds
- [ ] All integration tests pass
- [ ] Architecture test validates no JPA in domain layer

### Validation Commands
```bash
# Check domain layer purity
find backend/src/main/java/com/foodya/backend/domain/entities -name "*.java" -exec grep -l "jakarta.persistence" {} \;
# Should return: empty

# Verify persistence models exist
ls backend/src/main/java/com/foodya/backend/infrastructure/persistence/models/ | wc -l
# Should return: 17

# Verify mappers exist
ls backend/src/main/java/com/foodya/backend/infrastructure/persistence/mappers/ | wc -l
# Should return: 17

# Run tests
mvn clean test
```

---

## Prompt 2: Decompose AiRecommendationService God Object
**Priority:** P0 (CRITICAL)
**Effort:** 10-12 hours
**Prerequisites:** None (independent of Prompt 1)

### Task Description
Refactor the 1,118-line AiRecommendationService into 8 focused, single-responsibility services following Clean Architecture principles.

### Objective
Eliminate the god object anti-pattern by extracting cohesive services, each handling one clear responsibility.

### Current Problem
AiRecommendationService violates Single Responsibility Principle by handling:
- NLP query parsing (60+ regex patterns)
- Embedding generation
- Vector search & RAG
- Weather context fetching + caching
- Geo-distance calculations
- Menu filtering
- Recommendation scoring
- Chat history persistence
- Response generation

### Target Architecture

Create 8 new services in `application/usecases/ai/`:

#### 1. QueryParsingService (~150-200 lines)
**Responsibility:** Parse user intent from natural language queries

**Methods:**
```java
ParsedQuery parseUserIntent(String query);
PriceConstraints extractPriceConstraints(String query);
DietaryFilters extractDietaryFilters(String query);
LocationIntent extractLocationIntent(String query);
```

**Extract from AiRecommendationService:**
- Lines containing: `NEARBY_HINTS`, `SPICY_HINTS`, `VEGETARIAN_HINTS`, etc.
- All Pattern constants: `PRICE_RANGE_PATTERN`, `RATING_MIN_PATTERN`, etc.
- Token splitting logic
- Hint matching logic

#### 2. EmbeddingService (~50-80 lines)
**Responsibility:** Generate embeddings for queries

**Methods:**
```java
float[] generateEmbedding(String text);
List<float[]> batchGenerate(List<String> texts);
```

**Extract from AiRecommendationService:**
- aiEmbeddingPort wrapper logic
- Embedding normalization if any

#### 3. VectorSearchService (~100-150 lines)
**Responsibility:** RAG retrieval and vector search

**Methods:**
```java
List<AiCatalogVectorHit> searchCatalog(float[] embedding, int topK);
void refreshVectorIndex();
boolean needsRefresh();
```

**Extract from AiRecommendationService:**
- RAG_TOP_K, RAG_REFRESH_SECONDS constants
- lastRagSnapshotAt logic
- Vector search coordination

#### 4. ContextEnrichmentService (~100-150 lines)
**Responsibility:** Enrich queries with weather, geo, and user context

**Methods:**
```java
WeatherContext getWeatherContext(BigDecimal lat, BigDecimal lon);
GeoContext getGeoContext(BigDecimal lat, BigDecimal lon);
UserContext getUserHistory(UUID userId);
```

**Extract from AiRecommendationService:**
- weatherCache logic
- H3 geo indexing
- Weather API integration
- WEATHER_CACHE_SECONDS constant

#### 5. RecommendationScoringService (~150-200 lines)
**Responsibility:** Score and rank recommendations

**Methods:**
```java
List<ScoredRecommendation> scoreRecommendations(List<MenuItem> items, ParsedQuery query);
void applyBoosts(ScoredRecommendation rec, WeatherContext weather);
List<ScoredRecommendation> rankResults(List<ScoredRecommendation> scored);
```

**Extract from AiRecommendationService:**
- Scoring algorithms
- Weather-based boosting
- Price-based boosting
- Ranking logic

#### 6. MenuFilteringService (~100-150 lines)
**Responsibility:** Filter menu items by constraints

**Methods:**
```java
List<MenuItem> filterByAvailability(List<MenuItem> items);
List<MenuItem> filterByDistance(List<MenuItem> items, GeoContext geo, int maxKm);
List<MenuItem> filterByPrice(List<MenuItem> items, PriceConstraints price);
List<MenuItem> filterByDietary(List<MenuItem> items, DietaryFilters filters);
```

**Extract from AiRecommendationService:**
- DEFAULT_MAX_SHIPPING_KM constant
- Distance filtering logic
- Price range filtering
- Dietary constraint filtering

#### 7. ChatHistoryService (~80-100 lines)
**Responsibility:** Persist and retrieve chat history

**Methods:**
```java
void saveChat(UUID userId, String query, String response);
List<AiChatHistoryView> getChatHistory(UUID userId, int limit);
```

**Extract from AiRecommendationService:**
- aiChatHistoryPort wrapper logic
- History formatting

#### 8. ResponseGenerationService (~80-100 lines)
**Responsibility:** Generate AI responses using draft port

**Methods:**
```java
String generateResponse(ParsedQuery query, List<ScoredRecommendation> recommendations, WeatherContext weather);
String formatRecommendations(List<ScoredRecommendation> recommendations);
```

**Extract from AiRecommendationService:**
- aiDraftPort wrapper logic
- Response formatting

#### 9. AiRecommendationOrchestrator (~150-200 lines)
**Responsibility:** Coordinate all services (replaces AiRecommendationService)

**Methods:**
```java
AiRecommendationResponse getRecommendation(CreateAiChatRequest request);
```

**Implementation:**
```java
@Service
public class AiRecommendationOrchestrator implements AiRecommendationUseCase {
    private final QueryParsingService queryParser;
    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearch;
    private final ContextEnrichmentService contextEnrichment;
    private final RecommendationScoringService scoring;
    private final MenuFilteringService filtering;
    private final ChatHistoryService chatHistory;
    private final ResponseGenerationService responseGen;

    public AiRecommendationOrchestrator(/* inject all 8 services */) {
        // ...
    }

    @Override
    @Transactional
    public AiRecommendationResponse getRecommendation(CreateAiChatRequest request) {
        // 1. Parse query
        var parsed = queryParser.parseUserIntent(request.getQuery());

        // 2. Generate embedding
        var embedding = embeddingService.generateEmbedding(request.getQuery());

        // 3. Vector search
        var hits = vectorSearch.searchCatalog(embedding, RAG_TOP_K);

        // 4. Enrich context
        var weather = contextEnrichment.getWeatherContext(request.getLat(), request.getLon());
        var geo = contextEnrichment.getGeoContext(request.getLat(), request.getLon());

        // 5. Filter menu items
        var filtered = filtering.filterByDistance(hits, geo, parsed.getMaxDistance());
        filtered = filtering.filterByPrice(filtered, parsed.getPriceConstraints());

        // 6. Score and rank
        var scored = scoring.scoreRecommendations(filtered, parsed);
        scored = scoring.applyBoosts(scored, weather);
        var ranked = scoring.rankResults(scored);

        // 7. Generate response
        var response = responseGen.generateResponse(parsed, ranked, weather);

        // 8. Save history
        chatHistory.saveChat(request.getUserId(), request.getQuery(), response);

        return new AiRecommendationResponse(response, ranked);
    }
}
```

### Execution Steps

1. Create package structure:
```bash
mkdir -p backend/src/main/java/com/foodya/backend/application/usecases/ai
```

2. For each service (1-8):
   - Create service class file
   - Extract relevant logic from AiRecommendationService
   - Add @Service annotation
   - Define constructor injection
   - Move relevant constants
   - Write unit tests

3. Create orchestrator:
   - Rename AiRecommendationService to AiRecommendationOrchestrator
   - Inject all 8 services
   - Implement coordination logic
   - Ensure all existing functionality preserved

4. Update tests:
   - Update existing integration tests to work with orchestrator
   - Add unit tests for each new service
   - Mock dependencies in unit tests

### Success Criteria
- [ ] No single service exceeds 250 lines
- [ ] Each service has one clear responsibility documented in Javadoc
- [ ] All 8 services + orchestrator created
- [ ] All existing integration tests pass
- [ ] New unit tests added for each service (8 test classes minimum)
- [ ] Code coverage maintained or improved
- [ ] `mvn clean compile` succeeds

### Validation Commands
```bash
# Check service line counts
wc -l backend/src/main/java/com/foodya/backend/application/usecases/ai/*.java
# No file should exceed 250 lines

# Verify all tests pass
mvn clean test

# Check unit test coverage
mvn test jacoco:report
# Review target/site/jacoco/index.html
```

---

## Prompt 3: Consolidate Redundant Persistence Adapters
**Priority:** P1 (HIGH)
**Effort:** 4-6 hours
**Prerequisites:** Prompt 1 completed

### Task Description
Reduce adapter count from 44 to ~17 by consolidating adapters around domain aggregates instead of use cases.

### Current Problem
- 44 adapters for 17 entities = 2.6 adapters per entity
- Example: 3 separate Order adapters doing similar persistence work
- Maintenance overhead and code duplication

### Target Architecture
One adapter per aggregate root (17 total):
```
OrderRepositoryAdapter (consolidates 3 adapters)
CatalogRepositoryAdapter (consolidates 3 adapters)
UserRepositoryAdapter (consolidates 3 adapters)
CartRepositoryAdapter (already consolidated)
...
```

### Execution Steps

#### Step 1: Analyze Current Adapters
List all adapters and group by domain:

```bash
find backend/src/main/java/com/foodya/backend/infrastructure/adapter/persistence -name "*Adapter.java"
```

Group them:
```
Order Domain:
- OrderManagementPersistenceAdapter
- OrderPersistenceAdapter
- AdminOrderPersistenceAdapter
→ Consolidate to: OrderRepositoryAdapter

Catalog Domain:
- MenuItemPersistenceAdapter
- MenuCategoryPersistenceAdapter
- RestaurantPersistenceAdapter
→ Keep separate (different aggregates) OR consolidate to: CatalogRepositoryAdapter

User/Auth Domain:
- UserAccountPersistenceAdapter
- RefreshTokenPersistenceAdapter
- PasswordResetChallengePersistenceAdapter
→ Consolidate to: UserRepositoryAdapter

(Continue for all domains...)
```

#### Step 2: Create Consolidated Adapters

For each domain, create one consolidated adapter:

**Example: OrderRepositoryAdapter**

```java
@Component
public class OrderRepositoryAdapter implements OrderPort {
    private final OrderManagementRepository orderMgmtRepo;
    private final OrderRepository orderRepo;
    private final AdminOrderRepository adminOrderRepo;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderPaymentMapper orderPaymentMapper;
    private final OrderReviewMapper orderReviewMapper;

    public OrderRepositoryAdapter(
        OrderManagementRepository orderMgmtRepo,
        OrderRepository orderRepo,
        AdminOrderRepository adminOrderRepo,
        OrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderPaymentMapper orderPaymentMapper,
        OrderReviewMapper orderReviewMapper
    ) {
        this.orderMgmtRepo = orderMgmtRepo;
        this.orderRepo = orderRepo;
        this.adminOrderRepo = adminOrderRepo;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderPaymentMapper = orderPaymentMapper;
        this.orderReviewMapper = orderReviewMapper;
    }

    // Consolidate methods from OrderManagementPersistenceAdapter
    @Override
    public Order saveOrder(Order order) {
        var model = orderMapper.toPersistence(order);
        var saved = orderMgmtRepo.save(model);
        return orderMapper.toDomain(saved);
    }

    // Consolidate methods from OrderPersistenceAdapter
    @Override
    public Optional<Order> findOrderById(UUID id) {
        return orderRepo.findById(id).map(orderMapper::toDomain);
    }

    // Consolidate methods from AdminOrderPersistenceAdapter
    @Override
    public List<Order> findAllOrdersForAdmin(/* filters */) {
        return adminOrderRepo.findAll(/* filters */)
            .stream()
            .map(orderMapper::toDomain)
            .toList();
    }

    // Add all other methods from 3 old adapters...
}
```

#### Step 3: Update Port Interfaces

Consolidate port interfaces to match (see Prompt 5 for full port consolidation):

```java
// application/ports/out/order/OrderPort.java
public interface OrderPort {
    // All order-related persistence methods
    Order saveOrder(Order order);
    Optional<Order> findOrderById(UUID id);
    List<Order> findAllOrdersForAdmin(/* filters */);
    // ... all methods from 3 old ports
}
```

#### Step 4: Update Use Cases

Update use case constructors to inject consolidated adapters:

```java
// BEFORE
@Service
public class OrderCheckoutService {
    private final OrderManagementPort orderMgmtPort;
    private final OrderPort orderPort;

    public OrderCheckoutService(OrderManagementPort orderMgmtPort, OrderPort orderPort) {
        // ...
    }
}

// AFTER
@Service
public class OrderCheckoutService {
    private final OrderPort orderPort; // consolidated

    public OrderCheckoutService(OrderPort orderPort) {
        // ...
    }
}
```

#### Step 5: Delete Old Adapters

After verifying tests pass:
```bash
git rm backend/src/main/java/com/foodya/backend/infrastructure/adapter/persistence/OrderManagementPersistenceAdapter.java
git rm backend/src/main/java/com/foodya/backend/infrastructure/adapter/persistence/AdminOrderPersistenceAdapter.java
# Keep OrderPersistenceAdapter and rename to OrderRepositoryAdapter
```

### Consolidation Plan

| Domain | Old Adapters | New Adapter | Methods |
|--------|--------------|-------------|---------|
| Order | 3 | OrderRepositoryAdapter | ~15-20 |
| Catalog | 3 | CatalogRepositoryAdapter | ~20-25 |
| User/Auth | 3 | UserRepositoryAdapter | ~12-15 |
| Cart | 2 | CartRepositoryAdapter | ~8-10 |
| Core | 3 | CoreRepositoryAdapter | ~10-12 |
| Notification | 1 | NotificationRepositoryAdapter | ~5-8 |
| AI | 1 | AiRepositoryAdapter | ~5-8 |

**Target:** 17 adapters total (down from 44)

### Success Criteria
- [ ] Adapter count reduced to ~17 (one per aggregate root)
- [ ] All tests pass
- [ ] No functionality loss
- [ ] Clearer adapter naming (domain-focused, not use-case-focused)
- [ ] Use cases inject consolidated adapters
- [ ] Old adapter files deleted

### Validation Commands
```bash
# Count adapters
find backend/src/main/java/com/foodya/backend/infrastructure/adapter/persistence -name "*Adapter.java" | wc -l
# Should return: ~17

# Verify tests
mvn clean test
```

---

## Prompt 4: Enrich Anemic Domain Models
**Priority:** P1 (HIGH)
**Effort:** 3-4 hours
**Prerequisites:** Prompt 1 completed

### Task Description
Add business logic methods to anemic domain entities that currently only have getters/setters.

### Objective
Move business rules from application services into domain entities where they belong, following Domain-Driven Design principles.

### Target Entities

#### 1. CartItem
**Current State:** Pure data holder (getters/setters only)

**Add Business Methods:**
```java
public class CartItem {
    // existing fields...

    /**
     * Validates if this cart item is valid for checkout
     */
    public boolean isValidForCheckout() {
        return menuItemId != null
            && quantity > 0
            && quantity <= 100 // reasonable max
            && unitPrice != null
            && unitPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Calculates subtotal for this line item
     */
    public BigDecimal calculateSubtotal() {
        if (unitPrice == null || quantity <= 0) {
            throw new IllegalStateException("cannot calculate subtotal for invalid item");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Validates and updates quantity
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 1 || newQuantity > 100) {
            throw new IllegalArgumentException("quantity must be between 1 and 100");
        }
        this.quantity = newQuantity;
    }

    /**
     * Checks if item matches a menu item
     */
    public boolean isForMenuItem(UUID menuItemId) {
        return this.menuItemId != null && this.menuItemId.equals(menuItemId);
    }
}
```

**Refactor in Application Layer:**
```java
// BEFORE (in CartService)
BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

// AFTER (in CartService)
BigDecimal subtotal = item.calculateSubtotal();
```

#### 2. OrderItem
**Current State:** Pure data holder

**Add Business Methods:**
```java
public class OrderItem {
    // existing fields...

    /**
     * Validates price matches expected amount
     */
    public void validatePrice(BigDecimal expectedPrice) {
        if (unitPrice == null || expectedPrice == null) {
            throw new IllegalArgumentException("prices cannot be null");
        }
        if (unitPrice.compareTo(expectedPrice) != 0) {
            throw new IllegalStateException(
                "price mismatch: expected " + expectedPrice + " but got " + unitPrice
            );
        }
    }

    /**
     * Calculates total for this line item
     */
    public BigDecimal calculateTotal() {
        if (unitPrice == null || quantity <= 0) {
            throw new IllegalStateException("cannot calculate total for invalid item");
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Checks if this item is refundable based on order status
     */
    public boolean isRefundable(OrderStatus orderStatus) {
        return orderStatus == OrderStatus.SUCCESS
            || orderStatus == OrderStatus.CANCELLED;
    }

    /**
     * Checks if quantity is within valid range
     */
    public boolean hasValidQuantity() {
        return quantity > 0 && quantity <= 100;
    }
}
```

#### 3. OrderPayment
**Current State:** Pure data holder

**Add Business Methods:**
```java
public class OrderPayment {
    // existing fields...

    /**
     * Marks payment as successfully completed
     */
    public void markPaid(String transactionId) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("payment already marked as paid");
        }
        this.paymentStatus = PaymentStatus.PAID;
        this.paidAt = OffsetDateTime.now();
        if (transactionId != null && !transactionId.isBlank()) {
            this.externalTransactionId = transactionId.trim();
        }
    }

    /**
     * Marks payment as failed
     */
    public void markFailed(String failureReason) {
        if (paymentStatus == PaymentStatus.PAID) {
            throw new IllegalStateException("cannot mark paid payment as failed");
        }
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }

    /**
     * Checks if payment was successful
     */
    public boolean isSuccessful() {
        return paymentStatus == PaymentStatus.PAID && paidAt != null;
    }

    /**
     * Validates payment amount matches expected total
     */
    public void validateAmount(BigDecimal expectedAmount) {
        if (amount == null || expectedAmount == null) {
            throw new IllegalArgumentException("amounts cannot be null");
        }
        if (amount.compareTo(expectedAmount) != 0) {
            throw new IllegalStateException(
                "payment amount mismatch: expected " + expectedAmount + " but got " + amount
            );
        }
    }
}
```

#### 4. OrderReview
**Current State:** Pure data holder

**Add Business Methods:**
```java
public class OrderReview {
    // existing fields...

    /**
     * Validates rating is within valid range
     */
    public void validateRating() {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
    }

    /**
     * Checks if review is editable by customer
     */
    public boolean isEditable() {
        if (createdAt == null) {
            return true; // new review
        }
        // Reviews can be edited within 24 hours
        return OffsetDateTime.now().isBefore(createdAt.plusHours(24));
    }

    /**
     * Checks if review can be deleted
     */
    public boolean canBeDeleted() {
        // Reviews can be deleted within 48 hours
        if (createdAt == null) {
            return true;
        }
        return OffsetDateTime.now().isBefore(createdAt.plusHours(48));
    }

    /**
     * Updates review content with validation
     */
    public void updateReview(Integer newRating, String newComment) {
        if (!isEditable()) {
            throw new IllegalStateException("review is no longer editable");
        }
        if (newRating != null) {
            if (newRating < 1 || newRating > 5) {
                throw new IllegalArgumentException("rating must be between 1 and 5");
            }
            this.rating = newRating;
        }
        if (newComment != null) {
            this.comment = newComment.trim();
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
```

#### 5. DeliveryTrackingPoint
**Current State:** Pure data holder

**Add Business Methods:**
```java
public class DeliveryTrackingPoint {
    // existing fields...

    /**
     * Validates coordinates are within valid ranges
     */
    public boolean hasValidCoordinates() {
        if (latitude == null || longitude == null) {
            return false;
        }
        return latitude.compareTo(new BigDecimal("-90")) >= 0
            && latitude.compareTo(new BigDecimal("90")) <= 0
            && longitude.compareTo(new BigDecimal("-180")) >= 0
            && longitude.compareTo(new BigDecimal("180")) <= 0;
    }

    /**
     * Calculates distance from another point (in kilometers)
     */
    public double distanceFrom(DeliveryTrackingPoint other) {
        if (!this.hasValidCoordinates() || !other.hasValidCoordinates()) {
            throw new IllegalStateException("both points must have valid coordinates");
        }
        // Haversine formula
        double lat1 = Math.toRadians(latitude.doubleValue());
        double lat2 = Math.toRadians(other.latitude.doubleValue());
        double lon1 = Math.toRadians(longitude.doubleValue());
        double lon2 = Math.toRadians(other.longitude.doubleValue());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return 6371 * c; // Earth radius in km
    }

    /**
     * Checks if this tracking point is recent (within last 5 minutes)
     */
    public boolean isRecent() {
        if (createdAt == null) {
            return false;
        }
        return OffsetDateTime.now().minusMinutes(5).isBefore(createdAt);
    }
}
```

### Execution Steps

1. For each entity (CartItem, OrderItem, OrderPayment, OrderReview, DeliveryTrackingPoint):
   - Add business methods as defined above
   - Write unit tests for each new method
   - Update Javadoc

2. Refactor application services:
   - Find usages of entity getters that implement business logic
   - Replace with domain method calls
   - Example: `item.getUnitPrice().multiply(...)` → `item.calculateSubtotal()`

3. Write domain-level unit tests:
   - Test each business method independently
   - No Spring context needed (pure Java tests)
   - Example:
   ```java
   @Test
   void cartItemCalculatesSubtotalCorrectly() {
       CartItem item = new CartItem();
       item.setUnitPrice(new BigDecimal("50000"));
       item.setQuantity(3);

       assertEquals(new BigDecimal("150000"), item.calculateSubtotal());
   }
   ```

### Success Criteria
- [ ] Each target entity has at least 3-5 business logic methods
- [ ] Application services use domain methods instead of direct field access
- [ ] Domain logic is testable without Spring context
- [ ] All new methods have unit tests
- [ ] All existing tests still pass
- [ ] Javadoc added for all new methods

### Validation Commands
```bash
# Check that methods were added
grep -n "public.*calculate\|public.*validate\|public.*is.*able" backend/src/main/java/com/foodya/backend/domain/entities/CartItem.java

# Run domain tests
mvn test -Dtest=**Domain**Test

# Run all tests
mvn clean test
```

---

## Prompt 5: Consolidate Port Interfaces
**Priority:** P2 (MEDIUM)
**Effort:** 2-3 hours
**Prerequisites:** Prompt 1 and Prompt 3 completed

### Task Description
Group related port interfaces into domain-focused port interfaces to reduce navigation overhead and improve cohesion.

### Current Problem
- 42 ports for 17 entities creates cognitive overhead
- Ports are too granular (one per entity)
- Difficult to understand domain boundaries

### Target Architecture
Group ports by bounded context/domain aggregate:

```
application/ports/out/
├── catalog/
│   └── CatalogPort.java
├── cart/
│   └── CartPort.java
├── order/
│   └── OrderPort.java
├── auth/
│   └── AuthPort.java
├── core/
│   └── CorePort.java
├── notification/
│   └── NotificationPort.java
└── ai/
    └── AiPort.java
```

### Execution Steps

#### Step 1: Analyze Current Ports

List all ports and group by domain:
```bash
find backend/src/main/java/com/foodya/backend/application/ports/out -name "*Port.java"
```

#### Step 2: Create Domain-Focused Port Packages

```bash
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/catalog
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/cart
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/order
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/auth
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/core
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/notification
mkdir -p backend/src/main/java/com/foodya/backend/application/ports/out/ai
```

#### Step 3: Create Consolidated Port Interfaces

**Example 1: CatalogPort.java**

```java
package com.foodya.backend.application.ports.out.catalog;

import com.foodya.backend.domain.entities.*;
import java.util.*;

/**
 * Port for catalog-related persistence operations.
 * Consolidates MenuItem, MenuCategory, and Restaurant persistence.
 */
public interface CatalogPort {

    // MenuItem operations
    Optional<MenuItem> findMenuItemById(UUID id);
    List<MenuItem> findMenuItemsByRestaurant(UUID restaurantId);
    MenuItem saveMenuItem(MenuItem menuItem);
    void deleteMenuItem(UUID id);
    List<MenuItem> searchMenuItems(String query);

    // MenuCategory operations
    Optional<MenuCategory> findMenuCategoryById(UUID id);
    List<MenuCategory> findAllMenuCategories();
    MenuCategory saveMenuCategory(MenuCategory category);
    void deleteMenuCategory(UUID id);

    // Restaurant operations
    Optional<Restaurant> findRestaurantById(UUID id);
    List<Restaurant> findAllRestaurants();
    List<Restaurant> findRestaurantsByLocation(BigDecimal lat, BigDecimal lon, int radiusKm);
    Restaurant saveRestaurant(Restaurant restaurant);
    void deleteRestaurant(UUID id);
}
```

**Example 2: OrderPort.java**

```java
package com.foodya.backend.application.ports.out.order;

import com.foodya.backend.domain.entities.*;
import java.util.*;

/**
 * Port for order-related persistence operations.
 * Consolidates Order, OrderItem, OrderPayment, OrderReview, DeliveryTrackingPoint.
 */
public interface OrderPort {

    // Order operations
    Optional<Order> findOrderById(UUID id);
    List<Order> findOrdersByCustomer(UUID customerUserId);
    List<Order> findOrdersByRestaurant(UUID restaurantId);
    Order saveOrder(Order order);
    List<Order> findAllOrdersForAdmin(/* filters */);

    // OrderItem operations
    List<OrderItem> findOrderItemsByOrder(UUID orderId);
    OrderItem saveOrderItem(OrderItem orderItem);

    // OrderPayment operations
    Optional<OrderPayment> findOrderPaymentByOrder(UUID orderId);
    OrderPayment saveOrderPayment(OrderPayment payment);

    // OrderReview operations
    Optional<OrderReview> findOrderReviewByOrder(UUID orderId);
    List<OrderReview> findOrderReviewsByRestaurant(UUID restaurantId);
    OrderReview saveOrderReview(OrderReview review);

    // DeliveryTrackingPoint operations
    List<DeliveryTrackingPoint> findTrackingPointsByOrder(UUID orderId);
    DeliveryTrackingPoint saveTrackingPoint(DeliveryTrackingPoint point);
}
```

**Example 3: AuthPort.java**

```java
package com.foodya.backend.application.ports.out.auth;

import com.foodya.backend.domain.entities.*;
import java.util.*;

/**
 * Port for authentication and user management persistence.
 * Consolidates UserAccount, RefreshToken, PasswordResetChallenge.
 */
public interface AuthPort {

    // UserAccount operations
    Optional<UserAccount> findUserById(UUID id);
    Optional<UserAccount> findUserByPhone(String phone);
    Optional<UserAccount> findUserByEmail(String email);
    UserAccount saveUser(UserAccount user);

    // RefreshToken operations
    Optional<RefreshToken> findRefreshToken(String token);
    RefreshToken saveRefreshToken(RefreshToken token);
    void deleteRefreshToken(String token);
    void deleteExpiredTokens();

    // PasswordResetChallenge operations
    Optional<PasswordResetChallenge> findResetChallenge(String token);
    PasswordResetChallenge saveResetChallenge(PasswordResetChallenge challenge);
    void deleteResetChallenge(String token);
}
```

**Create similar consolidated ports for:**
- CartPort (Cart + CartItem)
- CorePort (SystemParameter + AuditLog + NotificationLog)
- NotificationPort (notification operations)
- AiPort (AI chat history + vector operations)

#### Step 4: Update Adapters to Implement Consolidated Ports

```java
@Component
public class CatalogRepositoryAdapter implements CatalogPort {
    private final MenuItemRepository menuItemRepo;
    private final MenuCategoryRepository menuCategoryRepo;
    private final RestaurantRepository restaurantRepo;
    private final MenuItemMapper menuItemMapper;
    private final MenuCategoryMapper menuCategoryMapper;
    private final RestaurantMapper restaurantMapper;

    // Implement all CatalogPort methods...
}
```

#### Step 5: Update Use Cases to Inject Consolidated Ports

```java
// BEFORE
@Service
public class CatalogService {
    private final MenuItemPort menuItemPort;
    private final MenuCategoryPort menuCategoryPort;
    private final RestaurantPort restaurantPort;

    public CatalogService(
        MenuItemPort menuItemPort,
        MenuCategoryPort menuCategoryPort,
        RestaurantPort restaurantPort
    ) {
        this.menuItemPort = menuItemPort;
        this.menuCategoryPort = menuCategoryPort;
        this.restaurantPort = restaurantPort;
    }
}

// AFTER
@Service
public class CatalogService {
    private final CatalogPort catalogPort;

    public CatalogService(CatalogPort catalogPort) {
        this.catalogPort = catalogPort;
    }
}
```

#### Step 6: Delete Old Port Files

After verifying tests pass:
```bash
git rm backend/src/main/java/com/foodya/backend/application/ports/out/MenuItemPort.java
git rm backend/src/main/java/com/foodya/backend/application/ports/out/MenuCategoryPort.java
git rm backend/src/main/java/com/foodya/backend/application/ports/out/RestaurantPort.java
# ... etc
```

### Consolidation Plan

| Domain | Old Ports | New Port | Methods |
|--------|-----------|----------|---------|
| Catalog | MenuItemPort, MenuCategoryPort, RestaurantPort | CatalogPort | ~15 |
| Order | OrderPort, OrderItemPort, OrderPaymentPort, OrderReviewPort, DeliveryTrackingPointPort | OrderPort | ~20 |
| Cart | CartPort, CartItemPort | CartPort | ~8 |
| Auth | UserAccountPort, RefreshTokenPort, PasswordResetChallengePort | AuthPort | ~12 |
| Core | SystemParameterPort, AuditLogPort, NotificationLogPort | CorePort | ~10 |
| Notification | NotificationPort | NotificationPort | ~5 |
| AI | AiChatHistoryPort, AiCatalogVectorPort, AiEmbeddingPort, AiDraftPort | AiPort | ~8 |

**Target:** 7-8 consolidated ports (down from 42)

### Success Criteria
- [ ] Port count reduced from 42 to ~7-8
- [ ] Ports organized by domain/bounded context
- [ ] Use cases inject consolidated ports
- [ ] All tests pass
- [ ] No functionality loss
- [ ] Old port files deleted

### Validation Commands
```bash
# Count ports
find backend/src/main/java/com/foodya/backend/application/ports/out -name "*Port.java" | wc -l
# Should return: ~7-8

# Verify tests
mvn clean test
```

---

## Prompt 6: Add Architecture Fitness Functions
**Priority:** P2 (MEDIUM)
**Effort:** 2-3 hours
**Prerequisites:** None (independent)

### Task Description
Enhance `ArchitectureRulesTests.java` with comprehensive Clean Architecture validation rules using ArchUnit.

### Objective
Automated guardrails to prevent architectural violations and regressions.

### Required Architecture Rules

#### 1. Domain Layer Purity Rules

```java
@ArchTest
static final ArchRule domain_entities_must_not_depend_on_jpa =
    classes()
        .that().resideInPackage("..domain.entities..")
        .should().onlyDependOnClassesThat(
            resideInAnyPackage(
                "java..",
                "..domain..",
                "..value_objects.."
            )
        )
        .because("Domain entities must be framework-independent");

@ArchTest
static final ArchRule domain_entities_must_not_import_jakarta =
    noClasses()
        .that().resideInPackage("..domain.entities..")
        .should().dependOnClassesThat().resideInPackage("jakarta..")
        .because("Domain entities must not import JPA/Jakarta");

@ArchTest
static final ArchRule domain_entities_must_not_be_annotated_with_jpa =
    noClasses()
        .that().resideInPackage("..domain.entities..")
        .should().beAnnotatedWith("jakarta.persistence.Entity")
        .orShould().beAnnotatedWith("jakarta.persistence.Table")
        .because("JPA annotations belong in persistence models");

@ArchTest
static final ArchRule domain_layer_must_not_depend_on_spring =
    noClasses()
        .that().resideInPackage("..domain..")
        .should().dependOnClassesThat().resideInPackage("org.springframework..")
        .because("Domain layer must not depend on Spring framework");
```

#### 2. Application Layer Rules

```java
@ArchTest
static final ArchRule application_layer_must_not_depend_on_infrastructure =
    noClasses()
        .that().resideInPackage("..application..")
        .should().dependOnClassesThat().resideInPackage("..infrastructure..")
        .because("Application layer must not depend on infrastructure");

@ArchTest
static final ArchRule application_layer_must_not_depend_on_interfaces =
    noClasses()
        .that().resideInPackage("..application..")
        .should().dependOnClassesThat().resideInPackage("..interfaces..")
        .because("Application layer must not depend on interface/presentation layer");

@ArchTest
static final ArchRule ports_must_be_interfaces =
    classes()
        .that().resideInPackage("..application.ports..")
        .should().beInterfaces()
        .because("Ports should be interfaces defining contracts");
```

#### 3. Infrastructure Layer Rules

```java
@ArchTest
static final ArchRule adapters_must_implement_ports =
    classes()
        .that().resideInPackage("..infrastructure.adapter..")
        .and().haveSimpleNameEndingWith("Adapter")
        .should().implement(
            resideInPackage("..application.ports..")
        )
        .because("Adapters must implement port interfaces");

@ArchTest
static final ArchRule persistence_models_must_be_in_infrastructure =
    classes()
        .that().areAnnotatedWith("jakarta.persistence.Entity")
        .should().resideInPackage("..infrastructure.persistence.models..")
        .because("JPA entities must be in infrastructure layer");

@ArchTest
static final ArchRule mappers_must_be_in_infrastructure =
    classes()
        .that().haveSimpleNameEndingWith("Mapper")
        .and().areNotInterfaces()
        .should().resideInPackage("..infrastructure.persistence.mappers..")
        .because("Mappers belong in infrastructure layer");

@ArchTest
static final ArchRule repositories_must_use_persistence_models =
    methods()
        .that().areDeclaredInClassesThat().resideInPackage("..infrastructure.repository..")
        .and().arePublic()
        .should().haveRawReturnType(
            resideInPackage("..infrastructure.persistence.models..")
                .or(resideInPackage("java.."))
        )
        .because("Repositories should work with persistence models, not domain entities");
```

#### 4. Service Size Rules

```java
@ArchTest
static final ArchRule services_must_not_exceed_300_lines =
    classes()
        .that().resideInPackage("..application.usecases..")
        .and().areAnnotatedWith("org.springframework.stereotype.Service")
        .should(notExceedLines(300))
        .because("Services should be focused and not exceed 300 lines");

@ArchTest
static final ArchRule methods_must_not_exceed_50_lines =
    methods()
        .that().areDeclaredInClassesThat().resideInPackage("..application..")
        .should(notExceedLines(50))
        .because("Methods should be focused and not exceed 50 lines");

// Custom condition
static ArchCondition<JavaClass> notExceedLines(int maxLines) {
    return new ArchCondition<>("not exceed " + maxLines + " lines") {
        @Override
        public void check(JavaClass javaClass, ConditionEvents events) {
            int lineCount = javaClass.getSource().get().getUri().getPath();
            // Count lines logic...
            if (lineCount > maxLines) {
                events.add(SimpleConditionEvent.violated(
                    javaClass,
                    javaClass.getName() + " has " + lineCount + " lines (max: " + maxLines + ")"
                ));
            }
        }
    };
}
```

#### 5. Naming Convention Rules

```java
@ArchTest
static final ArchRule adapters_must_end_with_adapter =
    classes()
        .that().resideInPackage("..infrastructure.adapter..")
        .and().areNotInterfaces()
        .should().haveSimpleNameEndingWith("Adapter")
        .because("Adapter classes must end with 'Adapter'");

@ArchTest
static final ArchRule services_must_end_with_service =
    classes()
        .that().resideInPackage("..application.usecases..")
        .and().areAnnotatedWith("org.springframework.stereotype.Service")
        .should().haveSimpleNameEndingWith("Service")
        .orShould().haveSimpleNameEndingWith("Orchestrator")
        .because("Use case implementations should end with 'Service' or 'Orchestrator'");

@ArchTest
static final ArchRule ports_must_end_with_port =
    classes()
        .that().resideInPackage("..application.ports..")
        .and().areInterfaces()
        .should().haveSimpleNameEndingWith("Port")
        .orShould().haveSimpleNameEndingWith("UseCase")
        .because("Port interfaces should end with 'Port' or 'UseCase'");

@ArchTest
static final ArchRule persistence_models_must_end_with_persistence_model =
    classes()
        .that().areAnnotatedWith("jakarta.persistence.Entity")
        .should().haveSimpleNameEndingWith("PersistenceModel")
        .because("JPA entities should end with 'PersistenceModel' to distinguish from domain entities");
```

#### 6. Dependency Direction Rules

```java
@ArchTest
static final ArchRule layers_must_respect_onion_architecture =
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Domain").definedBy("..domain..")
        .layer("Application").definedBy("..application..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        .layer("Interface").definedBy("..interfaces..")

        .whereLayer("Interface").mayNotBeAccessedByAnyLayer()
        .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Interface")
        .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Interface")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Interface")

        .because("Clean Architecture dependency rule: dependencies must point inward");
```

### Execution Steps

1. Open `backend/src/test/java/com/foodya/backend/architecture/ArchitectureRulesTests.java`

2. Add all architecture rules defined above

3. Add dependencies if needed:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

4. Run architecture tests:
```bash
mvn test -Dtest=ArchitectureRulesTests
```

5. Fix any violations found

6. Add to CI/CD pipeline:
```yaml
# .github/workflows/ci.yml
- name: Run Architecture Tests
  run: mvn test -Dtest=ArchitectureRulesTests
```

### Success Criteria
- [ ] 15+ architecture rules enforced
- [ ] Tests fail if domain has JPA imports
- [ ] Tests fail if services exceed size limits
- [ ] Tests fail if dependency direction violated
- [ ] Tests run on every build
- [ ] CI/CD pipeline enforces rules

### Validation Commands
```bash
# Run architecture tests
mvn test -Dtest=ArchitectureRulesTests

# Should pass with all rules enforced
```

---

## Prompt 7: Optimize Integration Test Suite
**Priority:** P3 (LOW)
**Effort:** 3-4 hours
**Prerequisites:** None (independent)

### Task Description
Review and optimize the 44+ integration tests for performance, clarity, and maintainability.

### Current State
- 44+ integration test files
- Unknown execution time
- Potential redundancy
- Some tests may be slow

### Objectives
1. Reduce test execution time to <60 seconds
2. Remove redundant tests
3. Improve test clarity and naming
4. Optimize test setup/teardown

### Execution Steps

#### Step 1: Profile Test Execution

```bash
# Run tests with timing
mvn clean test | tee test-output.log

# Or use surefire report
mvn surefire-report:report
# Check target/site/surefire-report.html for timing
```

Identify:
- Total execution time
- Slowest 10 tests
- Tests that fail intermittently

#### Step 2: Analyze Slow Tests

For each slow test:
1. Check if it uses real database (Testcontainers)
2. Check if it makes external API calls
3. Check if it has excessive setup

Common bottlenecks:
- Database setup/teardown
- External API calls (weather, AI)
- Large data fixtures
- Missing test data cleanup

#### Step 3: Optimize Database Tests

**Use @Sql for efficient test data:**

```java
// BEFORE (slow)
@BeforeEach
void setup() {
    UserAccount user = new UserAccount();
    // ... set 20 fields
    userRepo.save(user);

    Restaurant restaurant = new Restaurant();
    // ... set 30 fields
    restaurantRepo.save(restaurant);

    // ... create 10 more entities
}

// AFTER (fast)
@Sql("/test-data/users.sql")
@Sql("/test-data/restaurants.sql")
@Test
void testSomething() {
    // data already loaded
}
```

**Share test containers across tests:**

```java
// BEFORE (each test starts new container)
@Testcontainers
class MyTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
}

// AFTER (shared container)
abstract class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withReuse(true);
}

class MyTest extends BaseIntegrationTest {
    // container reused
}
```

#### Step 4: Mock External Dependencies

**Mock slow external services:**

```java
// BEFORE (real API calls)
@SpringBootTest
class AiRecommendationTest {
    @Autowired
    AiRecommendationService aiService; // calls real AI API
}

// AFTER (mocked)
@SpringBootTest
@MockBean(AiEmbeddingPort.class)
@MockBean(WeatherContextPort.class)
class AiRecommendationTest {
    @MockBean
    AiEmbeddingPort embeddingPort;

    @MockBean
    WeatherContextPort weatherPort;

    @Test
    void test() {
        when(embeddingPort.generateEmbedding(any()))
            .thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        // test logic...
    }
}
```

#### Step 5: Remove Redundant Tests

Identify duplicate scenarios:

```bash
# Find tests with similar names
find backend/src/test -name "*Test.java" -exec basename {} \; | sort | uniq -d
```

**Consolidate similar tests:**

```java
// BEFORE (3 separate tests)
@Test void testCreateOrderWithCOD() { ... }
@Test void testCreateOrderWithCard() { ... }
@Test void testCreateOrderWithWallet() { ... }

// AFTER (parameterized test)
@ParameterizedTest
@EnumSource(PaymentMethod.class)
void testCreateOrderWithPaymentMethod(PaymentMethod method) {
    // test all payment methods in one test
}
```

#### Step 6: Improve Test Naming and Structure

**Use BDD-style naming:**

```java
// BEFORE
@Test
void test1() {
    // unclear intent
}

// AFTER
@Test
@DisplayName("Given valid order, when customer cancels, then order status is CANCELLED")
void givenValidOrder_whenCustomerCancels_thenOrderStatusIsCancelled() {
    // GIVEN
    Order order = createValidOrder();

    // WHEN
    order.cancelByCustomer("Changed my mind");

    // THEN
    assertEquals(OrderStatus.CANCELLED, order.getStatus());
    assertNotNull(order.getCancelReason());
}
```

**Group tests by feature:**

```java
@Nested
@DisplayName("Order Cancellation Tests")
class OrderCancellationTests {
    @Test void customerCanCancelPendingOrder() { }
    @Test void customerCannotCancelDeliveredOrder() { }
    @Test void adminCanCancelAnyOrder() { }
}

@Nested
@DisplayName("Order Payment Tests")
class OrderPaymentTests {
    @Test void codPaymentMarkedUnpaid() { }
    @Test void cardPaymentRequiresValidation() { }
}
```

#### Step 7: Optimize Test Data Creation

**Use test data builders:**

```java
// BEFORE (verbose)
@Test
void test() {
    Order order = new Order();
    order.setId(UUID.randomUUID());
    order.setCustomerUserId(UUID.randomUUID());
    order.setRestaurantId(UUID.randomUUID());
    order.setStatus(OrderStatus.PENDING);
    order.setTotalAmount(new BigDecimal("100000"));
    // ... 20 more fields
}

// AFTER (builder pattern)
@Test
void test() {
    Order order = OrderTestBuilder.validOrder()
        .withStatus(OrderStatus.PENDING)
        .withTotal("100000")
        .build();
}

// OrderTestBuilder.java
public class OrderTestBuilder {
    private Order order = new Order();

    public static OrderTestBuilder validOrder() {
        OrderTestBuilder builder = new OrderTestBuilder();
        builder.order.setId(UUID.randomUUID());
        builder.order.setStatus(OrderStatus.PENDING);
        // ... set all required fields with valid defaults
        return builder;
    }

    public OrderTestBuilder withStatus(OrderStatus status) {
        this.order.setStatus(status);
        return this;
    }

    public Order build() {
        return this.order;
    }
}
```

### Success Criteria
- [ ] Test suite runs in <60 seconds
- [ ] No redundant tests
- [ ] BDD-style test naming
- [ ] Test data builders created
- [ ] External dependencies mocked
- [ ] Test containers shared/reused
- [ ] All tests pass

### Validation Commands
```bash
# Run tests with timing
time mvn clean test

# Check test report
mvn surefire-report:report
open target/site/surefire-report.html
```

---

## Prompt 8: Documentation & Migration Guide Updates
**Priority:** P3 (LOW)
**Effort:** 2-3 hours
**Prerequisites:** Prompts 1-7 completed

### Task Description
Update all architecture documentation to reflect completed improvements.

### Files to Update

#### 1. ARCHITECTURE_VIOLATION_REPORT.md

Update sections:
```markdown
## Executive Summary

### What Was Fixed (Updated: April 2026)
- ✅ ALL 17 domain entities cleaned (JPA annotations removed)
- ✅ ALL 17 persistence models created
- ✅ ALL 17 mappers created and integrated
- ✅ AiRecommendationService decomposed into 8 focused services
- ✅ Adapters consolidated from 44 to 17
- ✅ Ports consolidated from 42 to 8
- ✅ Domain models enriched with business logic
- ✅ Architecture fitness functions added

### Metrics After Complete Fixes
| Metric | Before | After | Status |
|--------|--------|-------|--------|
| Entities with JPA annotations | 17/17 | 0/17 | ✅ |
| Persistence models | 1/17 | 17/17 | ✅ |
| Mappers | 1/17 | 17/17 | ✅ |
| Adapters | 44 | 17 | ✅ |
| Ports | 42 | 8 | ✅ |
| Largest service | 1,118 lines | <250 lines | ✅ |
| Testability: Domain independence | 6% | 100% | ✅ |
```

#### 2. CLEAN_ARCHITECTURE_FIX_SUMMARY.md

Add completion section:
```markdown
## Migration Status: COMPLETE ✅

**Completion Date**: April 2026
**Total Effort**: 35 hours
**Team Size**: 2 engineers

### Final State
- 17/17 entities clean (100% complete)
- 17/17 persistence models created
- 17/17 mappers created
- 17/17 adapters consolidated
- 8/8 domain ports consolidated
- AiRecommendationService split into 8 services
- 15+ architecture rules enforced
- Test suite optimized (<60s execution)

### Benefits Realized
1. ✅ True domain independence (testable without frameworks)
2. ✅ Reduced complexity (44→17 adapters, 42→8 ports)
3. ✅ Improved maintainability (no service >250 lines)
4. ✅ Automated architecture validation
5. ✅ Faster test execution (60s vs previous unknown)
```

#### 3. Create ARCHITECTURE_DECISION_RECORDS.md

Document key decisions:

```markdown
# Architecture Decision Records (ADR)

## ADR-001: Separate Domain Entities from Persistence Models

**Date**: April 2026
**Status**: Accepted
**Context**: Domain entities had JPA annotations, violating Clean Architecture
**Decision**: Create separate PersistenceModel classes in infrastructure layer
**Consequences**:
- Positive: Domain is framework-independent and testable
- Positive: Can swap ORM without touching domain
- Negative: More classes (17 entities + 17 models + 17 mappers)
- Mitigation: Mappers are simple 1:1 field mappings

---

## ADR-002: Decompose AiRecommendationService

**Date**: April 2026
**Status**: Accepted
**Context**: 1,118-line god object violating SRP
**Decision**: Split into 8 focused services coordinated by orchestrator
**Consequences**:
- Positive: Each service <250 lines with single responsibility
- Positive: Independently testable services
- Negative: More dependency injection
- Mitigation: Orchestrator pattern makes coordination clear

---

## ADR-003: Consolidate Adapters by Domain Aggregate

**Date**: April 2026
**Status**: Accepted
**Context**: 44 adapters created maintenance overhead
**Decision**: One adapter per aggregate root (17 total)
**Consequences**:
- Positive: Clearer domain boundaries
- Positive: Reduced code duplication
- Negative: Larger adapter classes
- Mitigation: Each adapter still cohesive around one aggregate

---

## ADR-004: Consolidate Ports by Bounded Context

**Date**: April 2026
**Status**: Accepted
**Context**: 42 granular ports created navigation overhead
**Decision**: Group ports by domain (8 total: Catalog, Order, Cart, Auth, Core, Notification, AI)
**Consequences**:
- Positive: Easier to understand domain boundaries
- Positive: Reduced import overhead
- Negative: Larger port interfaces
- Mitigation: Organized by bounded context for clarity

---

## ADR-005: Enforce Architecture with ArchUnit

**Date**: April 2026
**Status**: Accepted
**Context**: Need automated guardrails to prevent regressions
**Decision**: Add 15+ ArchUnit rules to validate Clean Architecture
**Consequences**:
- Positive: Violations caught at build time
- Positive: Self-documenting architecture
- Positive: Prevents future violations
- Negative: Build fails if rules violated
- Mitigation: Rules are clear and documented
```

#### 4. Update backend-system.instructions.md

Add examples section:

```markdown
## Examples of Correct Patterns

### Enriched Domain Model Example
```java
// domain/entities/CartItem.java
public class CartItem {
    private UUID menuItemId;
    private int quantity;
    private BigDecimal unitPrice;

    // ✅ Business logic in domain
    public BigDecimal calculateSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    // ✅ Validation in domain
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 1 || newQuantity > 100) {
            throw new IllegalArgumentException("invalid quantity");
        }
        this.quantity = newQuantity;
    }
}
```

### Consolidated Adapter Example
```java
// infrastructure/adapter/persistence/OrderRepositoryAdapter.java
@Component
public class OrderRepositoryAdapter implements OrderPort {
    private final OrderRepository orderRepo;
    private final OrderMapper orderMapper;

    // ✅ All order-related persistence in one adapter
    public Optional<Order> findOrderById(UUID id) {
        return orderRepo.findById(id).map(orderMapper::toDomain);
    }

    public List<Order> findOrdersByCustomer(UUID customerId) {
        return orderRepo.findByCustomerUserId(customerId)
            .stream()
            .map(orderMapper::toDomain)
            .toList();
    }
}
```

### Service Size Guideline
```
✅ GOOD: Services 150-250 lines (focused responsibility)
⚠️ WARNING: Services 250-400 lines (consider splitting)
❌ BAD: Services >400 lines (must split - violates SRP)
```
```

#### 5. Create DEVELOPER_ONBOARDING.md

New file:

```markdown
# Developer Onboarding Guide - Foodya Backend

## Quick Start (10 minutes)

### 1. Clone and Build
```bash
git clone https://github.com/your-org/foodya.git
cd foodya/backend
mvn clean compile
mvn test
```

### 2. Understand the Architecture

Foodya follows **Uncle Bob's Clean Architecture** with strict layer separation:

```
┌─────────────────────────────────────────┐
│  Interface Layer (REST Controllers)      │ ← User-facing
├─────────────────────────────────────────┤
│  Infrastructure Layer (Adapters)         │ ← Framework/DB
├─────────────────────────────────────────┤
│  Application Layer (Use Cases)           │ ← Business orchestration
├─────────────────────────────────────────┤
│  Domain Layer (Entities + Logic)         │ ← Pure business rules
└─────────────────────────────────────────┘

Dependency Rule: Dependencies point INWARD only
```

### 3. Key Patterns

#### Domain Entity (Framework-Independent)
```java
// domain/entities/Order.java
public class Order {
    private UUID id;
    private OrderStatus status;

    // ✅ Business logic here
    public void cancelByCustomer(String reason) {
        if (!CANCELLABLE_STATES.contains(status)) {
            throw new IllegalStateException("cannot cancel");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

#### Persistence Model (JPA in Infrastructure)
```java
// infrastructure/persistence/models/OrderPersistenceModel.java
@Entity
@Table(name = "orders")
public class OrderPersistenceModel {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) private OrderStatus status;
}
```

#### Mapper (Converts Between Layers)
```java
// infrastructure/persistence/mappers/OrderMapper.java
@Component
public class OrderMapper {
    public Order toDomain(OrderPersistenceModel model) { ... }
    public OrderPersistenceModel toPersistence(Order domain) { ... }
}
```

#### Adapter (Implements Port)
```java
// infrastructure/adapter/persistence/OrderRepositoryAdapter.java
@Component
public class OrderRepositoryAdapter implements OrderPort {
    public Order save(Order order) {
        var model = mapper.toPersistence(order);
        var saved = repo.save(model);
        return mapper.toDomain(saved);
    }
}
```

### 4. Common Tasks

**Add a new domain entity:**
1. Create domain entity in `domain/entities/` (no JPA)
2. Create persistence model in `infrastructure/persistence/models/` (with JPA)
3. Create mapper in `infrastructure/persistence/mappers/`
4. Create port in `application/ports/out/{domain}/`
5. Create adapter in `infrastructure/adapter/persistence/`

**Add a new use case:**
1. Create use case interface in `application/ports/in/`
2. Create service in `application/usecases/`
3. Inject required ports in constructor
4. Add integration test

**Add a new REST endpoint:**
1. Add method to controller in `interfaces/rest/`
2. Create DTO in `interfaces/rest/dto/`
3. Call use case from controller
4. Update OpenAPI docs

### 5. Testing Guidelines

**Domain Tests** (no Spring context):
```java
@Test
void orderCanBeCancelledInValidState() {
    Order order = new Order();
    order.setStatus(OrderStatus.PENDING);

    order.cancelByCustomer("reason");

    assertEquals(OrderStatus.CANCELLED, order.getStatus());
}
```

**Integration Tests** (with Spring context):
```java
@SpringBootTest
@Transactional
class OrderCheckoutIntegrationTests {
    @Autowired OrderCheckoutService service;

    @Test void completeCheckoutFlow() { ... }
}
```

### 6. Architecture Rules

Run architecture tests before committing:
```bash
mvn test -Dtest=ArchitectureRulesTests
```

Rules enforced:
- No JPA in domain layer
- No infrastructure imports in application layer
- Services <250 lines
- Dependency direction inward only

### 7. Resources

- **SRS**: `docs/FOODYA_SRS.md` (requirements)
- **Architecture Report**: `ARCHITECTURE_RATING_REPORT.md`
- **Migration Guide**: `MIGRATION_GUIDE_CLEAN_ARCHITECTURE.md`
- **ADRs**: `ARCHITECTURE_DECISION_RECORDS.md`

### Questions?

Ask in #backend-dev Slack channel or review existing code in:
- `Order` domain (complete reference implementation)
- `CatalogService` (use case example)
- `OrderRepositoryAdapter` (adapter example)
```

### Success Criteria
- [ ] All documentation reflects current state
- [ ] ADRs document key decisions
- [ ] Developer onboarding guide complete
- [ ] Examples provided for each pattern
- [ ] New developers can onboard in <2 hours

### Validation
```bash
# Verify documentation exists
ls -la *.md
# Should include all updated/created files
```

---

## Summary

These 8 prompts provide a complete roadmap to transform the Foodya backend from a **6.5/10** (good with issues) to an **8.5-9/10** (excellent Clean Architecture implementation).

**Execution Order:**
1. Prompt 1 (P0) - Domain migration
2. Prompt 2 (P0) - Decompose god object
3. Prompt 3 (P1) - Consolidate adapters
4. Prompt 4 (P1) - Enrich domain models
5. Prompt 5 (P2) - Consolidate ports
6. Prompt 6 (P2) - Architecture tests
7. Prompt 7 (P3) - Optimize tests
8. Prompt 8 (P3) - Update docs

**Total Effort:** 34-45 hours (6-8 days with 1 engineer, or 2-3 days with a team)

**Questions?** Refer to ARCHITECTURE_RATING_REPORT.md for detailed analysis.
