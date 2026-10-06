# Order Batching Feature — Full Specification

> **Replaces**: Phase 5 (Multi-Order Batching) in `docs/implementation_plan.md`
>
> **Model**: Dynamic Multi-Vehicle Pickup-and-Delivery with Time Windows (VRPPD-TW)
>
> **Solver**: Google OR-Tools (RoutingModel)
>
> **Distance Matrix Source**: Goong Maps API v2 (`/v2/distancematrix`)

---

## 1. Executive Summary

### 1.1 Problem Statement

Foodya hiện tại gán từng đơn hàng riêng lẻ cho tài xế (1 đơn : 1 tài xế) bằng thuật toán scoring đơn giản. Cách tiếp cận này không tối ưu trong khu vực đô thị mật độ cao (TP.HCM, Hà Nội) khi nhiều đơn hàng từ các nhà hàng gần nhau giao cho khách hàng gần nhau. Kết quả:

- Tài xế phải chạy nhiều chuyến ngắn, thời gian chờ giữa các chuyến cao
- Tổng quãng đường di chuyển toàn hệ thống lớn hơn cần thiết
- Thu nhập tài xế/giờ thấp hơn tiềm năng
- Chi phí giao hàng cho nền tảng cao

### 1.2 Proposed Solution

Triển khai hệ thống **Order Batching** — ghép nhiều đơn hàng (2-4 đơn) thành một batch cho cùng một tài xế — sử dụng mô hình **VRPPD-TW** (Vehicle Routing Problem with Pickup and Delivery and Time Windows) được giải bằng **Google OR-Tools**.

**Điểm khác biệt quan trọng so với Phase 5 cũ**:

| Aspect | Phase 5 cũ (Greedy) | Phase 5 mới (VRPPD-TW) |
|--------|---------------------|------------------------|
| Algorithm | Greedy nearest-neighbor | Google OR-Tools RoutingModel |
| Optimality | ~85% optimal | ~95-98% optimal |
| Distance Data | Haversine (đường chim bay) | Goong Distance Matrix v2 (đường thực tế) |
| Route Planning | Chỉ tính detour | Tối ưu toàn bộ thứ tự pickup + delivery |
| Time Windows | Không có | Có (30p từ khi đặt) |
| Scalability | O(n²) per candidate | Chạy một lần solver cho toàn bộ eligible pool |
| Pickup ↔ Delivery Pairing | Thủ công insertion | OR-Tools `AddPickupAndDelivery` constraint |
| Batch Timing | Real-time per order | Batch window (mỗi 30-60s collect rồi solve) |

### 1.3 Key Design Decision: Pre-computed Batching

> **Đơn hàng được ghép batch HOÀN TOÀN TRƯỚC khi hiện lên tài xế.**
>
> Không có trường hợp tài xế đã nhận đơn mà có đơn mới ghép thêm vào đơn hiện tại. Khi tài xế nhận batch, batch đã finalized — biết trước:
> - Tất cả đơn hàng trong batch
> - Thứ tự pickup tại các nhà hàng
> - Thứ tự giao cho khách hàng
> - Thời gian giao dự kiến cho mỗi khách

---

## 2. Current System Analysis

### 2.1 Current Order Flow

```
Customer đặt đơn → Order PENDING
    → Merchant accept → Order ACCEPTED
        → System auto-assign driver (DriverMatchingAdapter) → Order ASSIGNED
            → Merchant preparing → Order PREPARING
                → Driver pick up → Order DELIVERING
                    → Driver giao → Order SUCCESS/FAILED
```

**Current assignment logic** (in `OrderLifecycleService.tryAutoAssignDriver()`):
1. Khi merchant accept → trigger `DriverMatchingPort.findBestDriver()`
2. H3 k-ring expansion: 1km → 3km → 6km từ nhà hàng
3. Score: proximity(50%) + quality(20%) + capacity(20%) + freshness(10%)
4. Assign driver cao điểm nhất → Order ACCEPTED → ASSIGNED

**Vấn đề**: Mỗi đơn được xử lý riêng lẻ tại thời điểm merchant accept. Không có cơ chế "chờ" để ghép với đơn khác.

### 2.2 Current Data Model (Relevant Entities)

| Entity | Table | Key Fields |
|--------|-------|------------|
| `Order` | `orders` | `id`, `restaurant_id`, `customer_user_id`, `driver_user_id`, `status`, `delivery_latitude`, `delivery_longitude`, `delivery_address`, `delivery_fee`, `total_amount`, `placed_at` |
| `OrderItem` | `order_items` | `order_id`, `menu_item_id`, `quantity`, `line_total` |
| `Restaurant` | `restaurants` | `id`, `latitude`, `longitude`, `h3_index_res9`, `max_delivery_km` |
| `DriverOnlineSession` | `driver_online_sessions` | `driver_user_id`, `status`, `h3_index_res9`, `current_lat`, `current_lng`, `active_order_count`, `max_concurrent_orders` |
| `OrderPayment` | `order_payments` | `order_id`, `payment_method`, `payment_status` |

### 2.3 Current Goong Maps Integration

- `GoongMapsClient` (`infrastructure/integration/GoongMapsClient.java`):
  - `routeDistanceRaw()` → `/v2/direction` (A→B routing)
  - `reverseGeocodeRaw()` → `/v2/geocode`
  - `tripRouteRaw()` → `/v2/trip` (multi-stop routing)
- `GoongRouteDistanceAdapter` → implements `RouteDistancePort.routeDistanceKm()`
- **Chưa có**: Distance Matrix API integration (`/v2/distancematrix`)

---

## 3. VRPPD-TW Model Design

### 3.1 Problem Formulation

**Cho**:
- Tập đơn hàng eligible `O = {o₁, o₂, ..., oₙ}` — các đơn ở status `ACCEPTED` chưa assign
- Tập tài xế available `D = {d₁, d₂, ..., dₘ}` — driver online, `canAcceptOrder() = true`
- Mỗi đơn `oᵢ` có:
  - Pickup node `pᵢ` = vị trí nhà hàng (`restaurant.latitude`, `restaurant.longitude`)
  - Delivery node `dᵢ` = vị trí khách (`order.delivery_latitude`, `order.delivery_longitude`)
  - Time window `[earliest_pickup, latest_delivery]` = `[placed_at, placed_at + MAX_DELIVERY_TIME]`
  - Service time tại pickup (thời gian chờ lấy đồ ăn): `PICKUP_SERVICE_TIME` (default 5 phút)
  - Service time tại delivery (thời gian giao đồ): `DELIVERY_SERVICE_TIME` (default 3 phút)
- Distance/time matrix `T[i][j]` — thời gian di chuyển thực tế từ node `i` đến node `j` (từ Goong Distance Matrix v2)

**Tìm**:
- Assignment: đơn hàng nào → tài xế nào
- Pickup sequence: thứ tự lấy hàng tại các nhà hàng
- Delivery sequence: thứ tự giao cho khách
- Estimated time cho mỗi node

**Tối ưu** (objective):
- Minimize tổng thời gian/quãng đường di chuyển của tất cả tài xế

**Ràng buộc**:
1. **Pickup trước Delivery**: Với mỗi đơn `oᵢ`, tài xế phải visit `pᵢ` trước `dᵢ`
2. **Same vehicle**: `pᵢ` và `dᵢ` phải cùng tài xế
3. **Time windows**: Mỗi node phải được visit trong khoảng time window
4. **Capacity**: Mỗi tài xế chở tối đa `max_concurrent_orders` đơn cùng lúc
5. **Max detour**: Thời gian giao cho bất kỳ khách nào ≤ `MAX_DELIVERY_TIME` (configurable, default 45 phút từ khi đặt)
6. **Max batch size**: Mỗi batch tối đa `MAX_BATCH_SIZE` đơn (configurable, default 4)

### 3.2 Node Graph Construction

```
Nodes = [depot₁, depot₂, ..., depotₘ,     ← m driver start positions
         p₁, p₂, ..., pₙ,                  ← n pickup nodes  
         d₁, d₂, ..., dₙ]                  ← n delivery nodes

Total nodes = m + 2n
```

Mỗi driver `dⱼ` có depot node = vị trí hiện tại (`current_lat`, `current_lng`).

### 3.3 Distance Matrix từ Goong v2

**API Call**: `GET https://rsapi.goong.io/v2/distancematrix`
- `origins`: tất cả nodes (lat,lng)
- `destinations`: tất cả nodes (lat,lng)
- `vehicle`: `bike`
- `api_key`: Goong API key

**Batching API calls**: Goong giới hạn số origins × destinations per request. Cần chia request thành multiple calls nếu vượt limit.

**Cache strategy**: Distance matrix cache theo H3 res8 cell pairs, TTL 15 phút. Cùng cell pair thì distance tương đương.

### 3.4 Google OR-Tools Model Setup

```java
// 1. Create RoutingIndexManager
RoutingIndexManager manager = new RoutingIndexManager(
    numNodes,           // total nodes (depots + pickups + deliveries)
    numVehicles,        // number of available drivers
    depotIndices        // start indices for each driver (their current location)
);

// 2. Create RoutingModel
RoutingModel routing = new RoutingModel(manager);

// 3. Register transit callback (uses Goong distance matrix)
int transitCallbackIndex = routing.registerTransitCallback(
    (fromIndex, toIndex) -> {
        int fromNode = manager.indexToNode(fromIndex);
        int toNode = manager.indexToNode(toIndex);
        return distanceMatrix[fromNode][toNode]; // seconds
    }
);

// 4. Set arc cost evaluator
routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

// 5. Add Time dimension
routing.addDimension(transitCallbackIndex,
    30 * 60,       // max wait time at each node (30 min)
    90 * 60,       // max total time per route (90 min)
    false,         // don't force start cumul to zero
    "Time"
);

RoutingDimension timeDimension = routing.getMutableDimension("Time");

// 6. Add pickup/delivery constraints
for (int i = 0; i < orders.size(); i++) {
    int pickupIndex = manager.nodeToIndex(pickupNodes[i]);
    int deliveryIndex = manager.nodeToIndex(deliveryNodes[i]);
    
    routing.addPickupAndDelivery(pickupIndex, deliveryIndex);
    
    // Same vehicle constraint
    routing.solver().addConstraint(routing.solver().makeEquality(
        routing.vehicleVar(pickupIndex),
        routing.vehicleVar(deliveryIndex)
    ));
    
    // Precedence: pickup before delivery
    timeDimension.cumulVar(pickupIndex)
        .isLessOrEqual(timeDimension.cumulVar(deliveryIndex));
}

// 7. Set time windows
for (int i = 0; i < numNodes; i++) {
    int index = manager.nodeToIndex(i);
    timeDimension.cumulVar(index).setRange(
        timeWindows[i][0],  // earliest arrival
        timeWindows[i][1]   // latest arrival
    );
}

// 8. Add capacity dimension
int demandCallbackIndex = routing.registerUnaryTransitCallback(
    fromIndex -> demands[manager.indexToNode(fromIndex)]  
    // +1 at pickup, -1 at delivery, 0 at depot
);
routing.addDimensionWithVehicleCapacity(demandCallbackIndex,
    0, vehicleCapacities, true, "Capacity"
);

// 9. Search parameters
RoutingSearchParameters searchParams = main.defaultRoutingSearchParameters();
searchParams.setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC);
searchParams.setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH);
searchParams.setTimeLimitMs(3000);  // max 3 seconds

// 10. Solve
Assignment solution = routing.solveWithParameters(searchParams);
```

### 3.5 Batching Flow (New)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BATCHING SCHEDULER (Cron job)                   │
│                   Runs every BATCH_INTERVAL (30-60s)               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. COLLECT eligible orders                                         │
│     - Status = ACCEPTED                                             │
│     - Not yet assigned to any driver                                │
│     - Placed at least BATCH_COOLDOWN seconds ago                    │
│                                                                     │
│  2. COLLECT available drivers                                       │
│     - DriverOnlineSession.canAcceptOrder() = true                   │
│     - Within MAX_SEARCH_RADIUS (10km) of any eligible order        │
│                                                                     │
│  3. BUILD distance matrix                                           │
│     - All unique locations: driver positions + restaurants + custs  │
│     - Call Goong Distance Matrix v2 API                             │
│     - Cache results (H3 res8 pair key, 15min TTL)                  │
│                                                                     │
│  4. SOLVE VRPPD-TW                                                  │
│     - Google OR-Tools RoutingModel                                  │
│     - Constraints: pickup↔delivery pairing, time windows, capacity │
│     - Objective: minimize total travel time                         │
│     - Time limit: 3 seconds                                        │
│                                                                     │
│  5. EXTRACT solution → OrderBatch entities                          │
│     - Group orders by assigned vehicle → create OrderBatch          │
│     - Extract pickup_sequence, delivery_sequence                    │
│     - Calculate ETA for each node                                   │
│                                                                     │
│  6. PERSIST and NOTIFY                                              │
│     - Save OrderBatch with all orders                               │
│     - Transition orders: ACCEPTED → ASSIGNED                        │
│     - Update DriverOnlineSession: startDelivery()                   │
│     - Push notification to driver with batch details                │
│     - Push notification to each customer with ETA                   │
│                                                                     │
│  7. SINGLE orders fallback                                          │
│     - Orders not batched by solver → single-driver assign           │
│     - Uses existing DriverMatchingPort.findBestDriver()             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.6 When NOT to Batch

Chỉ batch khi có lợi. Nguyên tắc:
- Nếu chỉ có 1 eligible order → assign single driver (dùng existing flow)
- Nếu batch solution cho detour > `MAX_DETOUR_MINUTES` (15 phút) cho bất kỳ customer nào → tách ra single
- Nếu tất cả nhà hàng cùng cluster nhưng khách ở hướng ngược nhau → không batch
- Solver tự xử lý: nếu batch không optimal, nó sẽ assign mỗi driver 1 đơn

### 3.7 Dynamic vs Static

Mô hình là **"semi-dynamic"**:
- Mỗi batch interval (30-60s), solver chạy với snapshot hiện tại
- Đơn mới đến sau snapshot sẽ chờ batch interval tiếp theo
- Đơn đã assign (batch đã finalized) KHÔNG bao giờ bị thay đổi
- Nếu đơn urgent (chờ quá lâu), cooldown threshold giảm → ưu tiên assign ngay

---

## 4. Architecture & Integration

### 4.1 Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     interfaces/rest                          │
│  ┌──────────────────────┐  ┌─────────────────────────────┐  │
│  │ DeliveryBatchCtrl    │  │ AdminBatchMonitorCtrl       │  │
│  │ GET /batches/active  │  │ GET /admin/batches          │  │
│  │ GET /batches/{id}    │  │ GET /admin/batches/{id}     │  │
│  │ POST /{id}/start     │  │ POST /admin/batches/trigger │  │
│  └──────────────────────┘  └─────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    application layer                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ OrderBatchingUseCase (port/in)                       │   │
│  │  - triggerBatchCycle()                               │   │
│  │  - getActiveBatchForDriver(driverId)                 │   │
│  │  - getBatchDetail(batchId)                           │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ OrderBatchingService (usecases/)                     │   │
│  │  - collectEligibleOrders()                           │   │
│  │  - collectAvailableDrivers()                         │   │
│  │  - buildDistanceMatrix()                             │   │
│  │  - solveVrppdTw()                                    │   │
│  │  - persistAndNotify()                                │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Ports (ports/out)                                    │   │
│  │  - DistanceMatrixPort                                │   │
│  │  - VrpSolverPort                                     │   │
│  │  - OrderBatchPort                                    │   │
│  │  - (existing: OrderManagementPort,                   │   │
│  │     DriverOnlineSessionPort, DriverMatchingPort,     │   │
│  │     OrderEventPublisherPort)                         │   │
│  └──────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                   infrastructure layer                       │
│  ┌───────────────────┐ ┌────────────────────────────────┐   │
│  │ GoongDistMatrix   │ │ OrToolsVrpSolverAdapter        │   │
│  │ Adapter           │ │ (implements VrpSolverPort)     │   │
│  │ (implements       │ │                                │   │
│  │  DistanceMatrix   │ │ Uses: RoutingModel,            │   │
│  │  Port)            │ │ AddPickupAndDelivery,          │   │
│  │                   │ │ Time/Capacity dims             │   │
│  │ Calls: Goong v2   │ │                                │   │
│  │ /distancematrix   │ │ Returns: VrpSolution           │   │
│  └───────────────────┘ └────────────────────────────────┘   │
│  ┌───────────────────┐ ┌────────────────────────────────┐   │
│  │ OrderBatchPersist │ │ BatchSchedulerJob              │   │
│  │ Adapter           │ │ (@Scheduled cron)              │   │
│  │ (JPA persistence) │ │ triggers batchCycle()          │   │
│  └───────────────────┘ └────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                      domain layer                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ OrderBatch entity                                      │ │
│  │ VrpSolution value object                               │ │
│  │ BatchAssignment value object                           │ │
│  │ DistanceMatrix value object                            │ │
│  │ BatchNode value object                                 │ │
│  │ BatchStatus enum                                       │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Order Status Flow (Updated)

```
PENDING → ACCEPTED → [Batching Scheduler collects] → ASSIGNED (with batch_id)
                                                        → PREPARING
                                                          → DELIVERING
                                                            → SUCCESS/FAILED

Fallback (single-assign, no batch):
PENDING → ACCEPTED → [Single driver assign] → ASSIGNED (batch_id = NULL)
```

**Thay đổi**: `tryAutoAssignDriver()` trong `OrderLifecycleService` sẽ **KHÔNG** trigger ngay khi merchant accept. Thay vào đó, order ở ACCEPTED chờ batch scheduler.

**Exception**: Nếu đơn chờ > `MAX_WAIT_FOR_BATCH` (configurable, default 2 phút), fallback assign single driver.

### 4.3 Integration with Existing Order Lifecycle

Thay đổi `OrderLifecycleService.merchantUpdateStatus()`:

```diff
 if (targetStatus == OrderStatus.ACCEPTED) {
     order.merchantAccept();
     Order saved = orderManagementPort.save(order);
     publishOrderEvent(saved, "ORDER_ACCEPTED", "...");
-    // Auto-assign a driver after merchant acceptance
-    tryAutoAssignDriver(saved);
+    // Order stays ACCEPTED — batch scheduler will assign driver
+    log.info("Order {} accepted, waiting for batch scheduler", saved.getOrderCode());
     return toDetail(saved);
 }
```

---

## 5. Database Changes

### 5.1 New Table: `order_batches`

```sql
CREATE TABLE order_batches (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_user_id           UUID NOT NULL REFERENCES user_accounts(id),
    session_id               UUID NOT NULL REFERENCES driver_online_sessions(id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING: batch created, not yet accepted by driver
    -- ACTIVE: driver has accepted/started the batch
    -- COMPLETED: all orders in batch delivered
    -- CANCELLED: batch cancelled (e.g., driver went offline)
    -- PARTIAL: some orders completed, some failed
    
    pickup_sequence          JSONB NOT NULL,
    -- [{orderId, restaurantId, lat, lng, estimatedArrivalSec, serviceTimeSec}]
    
    delivery_sequence        JSONB NOT NULL,
    -- [{orderId, customerId, lat, lng, estimatedArrivalSec, serviceTimeSec}]
    
    total_distance_meters    INTEGER,
    total_duration_seconds   INTEGER,
    
    optimized_route_polyline TEXT,  -- encoded polyline from Goong Trip API
    
    solver_metadata          JSONB,
    -- {solver_time_ms, objective_value, num_orders, batch_interval_id}
    
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at               TIMESTAMPTZ,
    completed_at             TIMESTAMPTZ,
    
    CONSTRAINT chk_batch_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'PARTIAL')
    )
);

CREATE INDEX idx_batch_driver_active 
    ON order_batches (driver_user_id, status) 
    WHERE status IN ('PENDING', 'ACTIVE');

CREATE INDEX idx_batch_created 
    ON order_batches (created_at) 
    WHERE status = 'PENDING';
```

### 5.2 New Table: `batch_order_items`

```sql
CREATE TABLE batch_order_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id        UUID NOT NULL REFERENCES order_batches(id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders(id),
    
    pickup_index    INTEGER NOT NULL,   -- position in pickup_sequence (0-based)
    delivery_index  INTEGER NOT NULL,   -- position in delivery_sequence (0-based)
    
    estimated_pickup_at     TIMESTAMPTZ,
    estimated_delivery_at   TIMESTAMPTZ,
    actual_pickup_at        TIMESTAMPTZ,
    actual_delivery_at      TIMESTAMPTZ,
    
    item_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING, PICKED_UP, DELIVERING, DELIVERED, FAILED
    
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_batch_order UNIQUE (batch_id, order_id),
    CONSTRAINT chk_item_status CHECK (
        item_status IN ('PENDING', 'PICKED_UP', 'DELIVERING', 'DELIVERED', 'FAILED')
    )
);

CREATE INDEX idx_batch_items_batch ON batch_order_items (batch_id);
CREATE INDEX idx_batch_items_order ON batch_order_items (order_id);
```

### 5.3 Alter Table: `orders`

```sql
ALTER TABLE orders ADD COLUMN batch_id UUID REFERENCES order_batches(id);
ALTER TABLE orders ADD COLUMN batch_pickup_index INTEGER;
ALTER TABLE orders ADD COLUMN batch_delivery_index INTEGER;
ALTER TABLE orders ADD COLUMN estimated_delivery_at TIMESTAMPTZ;

CREATE INDEX idx_orders_batch ON orders (batch_id) WHERE batch_id IS NOT NULL;
CREATE INDEX idx_orders_accepted_unbatched 
    ON orders (status, placed_at) 
    WHERE status = 'ACCEPTED' AND batch_id IS NULL AND driver_user_id IS NULL;
```

### 5.4 New Table: `distance_matrix_cache`

```sql
CREATE TABLE distance_matrix_cache (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin_h3_res8  VARCHAR(20) NOT NULL,
    dest_h3_res8    VARCHAR(20) NOT NULL,
    distance_meters INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    vehicle_type    VARCHAR(10) NOT NULL DEFAULT 'bike',
    fetched_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_dm_cache UNIQUE (origin_h3_res8, dest_h3_res8, vehicle_type)
);

CREATE INDEX idx_dm_cache_lookup 
    ON distance_matrix_cache (origin_h3_res8, dest_h3_res8, vehicle_type);
```

> **Note**: Nếu Redis đã sẵn sàng (Phase 9), dùng Redis thay vì table này. Schema trên là fallback cho khi chưa có Redis.

---

## 6. Goong Distance Matrix v2 Integration

### 6.1 New Port: `DistanceMatrixPort`

```java
// application/ports/out/DistanceMatrixPort.java
public interface DistanceMatrixPort {
    
    /**
     * Returns a time matrix (seconds) between all pairs of locations.
     * Matrix[i][j] = travel time in seconds from locations[i] to locations[j].
     * Diagonal = 0.
     */
    long[][] getTimeMatrix(List<LatLng> locations, String vehicleType);
    
    /**
     * Returns a distance matrix (meters) between all pairs of locations.
     */
    long[][] getDistanceMatrix(List<LatLng> locations, String vehicleType);
    
    /**
     * Returns both time and distance matrices.
     */
    DistanceMatrixResult getFullMatrix(List<LatLng> locations, String vehicleType);
}
```

### 6.2 GoongDistanceMatrixAdapter

```java
// infrastructure/adapter/integration/GoongDistanceMatrixAdapter.java
public class GoongDistanceMatrixAdapter implements DistanceMatrixPort {
    
    // Goong limit: max 25 origins × 25 destinations per request
    private static final int GOONG_MAX_ELEMENTS = 25;
    
    @Override
    public DistanceMatrixResult getFullMatrix(List<LatLng> locations, String vehicleType) {
        int n = locations.size();
        long[][] timeMatrix = new long[n][n];
        long[][] distMatrix = new long[n][n];
        
        // Chunk locations into batches of GOONG_MAX_ELEMENTS
        // For n > 25, split into multiple API calls
        for (int originStart = 0; originStart < n; originStart += GOONG_MAX_ELEMENTS) {
            for (int destStart = 0; destStart < n; destStart += GOONG_MAX_ELEMENTS) {
                // Build origin/dest strings
                // Call Goong API
                // Parse response and fill matrices
            }
        }
        
        return new DistanceMatrixResult(timeMatrix, distMatrix);
    }
}
```

### 6.3 New GoongMapsClient Method

```java
// Add to GoongMapsClient.java
public String distanceMatrixRaw(String origins, String destinations, String vehicle) {
    String apiKey = apiSecretsProvider.get(IntegrationKeyCatalog.GOONG_API_KEY)
            .orElseThrow(() -> new IllegalStateException("Missing Goong API key"));

    return executeWithRetry(() -> restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/v2/distancematrix")
                    .queryParam("origins", origins)
                    .queryParam("destinations", destinations)
                    .queryParam("vehicle", vehicle == null ? "bike" : vehicle)
                    .queryParam("api_key", apiKey)
                    .build())
            .retrieve()
            .body(String.class));
}
```

---

## 7. Google OR-Tools Integration

### 7.1 Maven Dependency

```xml
<dependency>
    <groupId>com.google.ortools</groupId>
    <artifactId>ortools-java</artifactId>
    <version>9.11.4210</version>
</dependency>
```

> **Note**: OR-Tools requires platform-specific native libraries. The `ortools-java` artifact bundles them for Linux/macOS/Windows.

### 7.2 VrpSolverPort (Application Layer)

```java
// application/ports/out/VrpSolverPort.java
public interface VrpSolverPort {
    
    /**
     * Solves the VRPPD-TW problem and returns the optimal batching solution.
     *
     * @param request Contains: distance/time matrices, orders with pickup/delivery nodes,
     *                drivers with depot locations and capacities, time windows, service times
     * @return Solution with assignments, sequences, and ETAs; or empty if infeasible
     */
    Optional<VrpSolution> solve(VrpProblem request);
}
```

### 7.3 Value Objects

```java
// domain/value_objects/VrpProblem.java
public record VrpProblem(
    long[][] timeMatrix,                    // seconds between all node pairs
    long[][] distanceMatrix,                // meters between all node pairs
    List<VrpOrder> orders,                  // orders with pickup/delivery node indices
    List<VrpDriver> drivers,                // drivers with depot node index and capacity
    Map<Integer, long[]> timeWindows,       // nodeIndex → [earliest, latest] in seconds
    Map<Integer, Long> serviceTimes,        // nodeIndex → service time in seconds
    long maxRouteTime,                      // max total route time per driver (seconds)
    long solverTimeLimitMs                  // solver time limit
) {}

// domain/value_objects/VrpOrder.java
public record VrpOrder(
    UUID orderId,
    int pickupNodeIndex,
    int deliveryNodeIndex,
    long timeWindowStart,                   // earliest pickup (seconds from epoch)
    long timeWindowEnd,                     // latest delivery (seconds from epoch)
    long pickupServiceTime,                 // seconds at restaurant
    long deliveryServiceTime                // seconds at customer
) {}

// domain/value_objects/VrpDriver.java
public record VrpDriver(
    UUID driverUserId,
    UUID sessionId,
    int depotNodeIndex,
    int capacity                            // max concurrent orders
) {}

// domain/value_objects/VrpSolution.java
public record VrpSolution(
    List<VrpRoute> routes,                  // one route per driver that has assignments
    long totalDistance,                      // total distance in meters
    long totalTime,                         // total time in seconds
    long objectiveValue,                    // OR-Tools objective
    long solverTimeMs                       // actual solver time
) {}

// domain/value_objects/VrpRoute.java
public record VrpRoute(
    UUID driverUserId,
    UUID sessionId,
    List<VrpRouteStop> stops,               // ordered list of stops
    long routeDistanceMeters,
    long routeDurationSeconds
) {}

// domain/value_objects/VrpRouteStop.java
public record VrpRouteStop(
    UUID orderId,
    StopType type,                          // PICKUP or DELIVERY
    int nodeIndex,
    double lat,
    double lng,
    long estimatedArrivalSeconds,           // from route start
    long serviceDurationSeconds
) {
    public enum StopType { PICKUP, DELIVERY }
}
```

### 7.4 OrToolsVrpSolverAdapter

```java
// infrastructure/adapter/OrToolsVrpSolverAdapter.java
public class OrToolsVrpSolverAdapter implements VrpSolverPort {
    
    static {
        Loader.loadNativeLibraries(); // Load OR-Tools native libs
    }
    
    @Override
    public Optional<VrpSolution> solve(VrpProblem problem) {
        long startMs = System.currentTimeMillis();
        
        int numNodes = problem.timeMatrix().length;
        int numVehicles = problem.drivers().size();
        int[] depots = problem.drivers().stream()
            .mapToInt(VrpDriver::depotNodeIndex).toArray();
        
        RoutingIndexManager manager = new RoutingIndexManager(
            numNodes, numVehicles, depots, depots  // start=end=depot
        );
        RoutingModel routing = new RoutingModel(manager);
        
        // 1. Transit callback (time-based)
        int transitCbIndex = routing.registerTransitCallback((from, to) -> {
            int fromNode = manager.indexToNode((int) from);
            int toNode = manager.indexToNode((int) to);
            return problem.timeMatrix()[fromNode][toNode];
        });
        routing.setArcCostEvaluatorOfAllVehicles(transitCbIndex);
        
        // 2. Time dimension
        routing.addDimension(transitCbIndex,
            1800,    // max slack (wait time): 30 min
            problem.maxRouteTime(),
            false,
            "Time"
        );
        RoutingDimension timeDim = routing.getMutableDimension("Time");
        
        // 3. Pickup & Delivery constraints
        for (VrpOrder order : problem.orders()) {
            long pickupIdx = manager.nodeToIndex(order.pickupNodeIndex());
            long deliveryIdx = manager.nodeToIndex(order.deliveryNodeIndex());
            
            routing.addPickupAndDelivery(pickupIdx, deliveryIdx);
            
            // Same vehicle
            routing.solver().addConstraint(
                routing.solver().makeEquality(
                    routing.vehicleVar(pickupIdx),
                    routing.vehicleVar(deliveryIdx)
                )
            );
            
            // Pickup before delivery
            timeDim.cumulVar(pickupIdx)
                .isLessOrEqual(timeDim.cumulVar(deliveryIdx));
        }
        
        // 4. Time windows
        for (var entry : problem.timeWindows().entrySet()) {
            long index = manager.nodeToIndex(entry.getKey());
            long[] tw = entry.getValue();
            timeDim.cumulVar(index).setRange(tw[0], tw[1]);
        }
        
        // 5. Capacity dimension
        int[] demands = new int[numNodes]; // +1 pickup, -1 delivery, 0 depot
        for (VrpOrder order : problem.orders()) {
            demands[order.pickupNodeIndex()] = 1;
            demands[order.deliveryNodeIndex()] = -1;
        }
        int demandCbIndex = routing.registerUnaryTransitCallback(
            from -> demands[manager.indexToNode((int) from)]
        );
        long[] capacities = problem.drivers().stream()
            .mapToLong(VrpDriver::capacity).toArray();
        routing.addDimensionWithVehicleCapacity(
            demandCbIndex, 0, capacities, true, "Capacity"
        );
        
        // 6. Allow dropping nodes (if infeasible, skip some orders)
        long penalty = 100_000;  // high penalty for dropping
        for (VrpOrder order : problem.orders()) {
            routing.addDisjunction(
                new long[]{
                    manager.nodeToIndex(order.pickupNodeIndex()),
                    manager.nodeToIndex(order.deliveryNodeIndex())
                },
                penalty
            );
        }
        
        // 7. Search parameters
        RoutingSearchParameters params = RoutingSearchParameters.newBuilder()
            .mergeFrom(main.defaultRoutingSearchParameters())
            .setFirstSolutionStrategy(
                FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
            .setLocalSearchMetaheuristic(
                LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
            .setTimeLimitMs(problem.solverTimeLimitMs())
            .build();
        
        // 8. Solve
        Assignment solution = routing.solveWithParameters(params);
        if (solution == null) {
            return Optional.empty();
        }
        
        // 9. Extract solution
        return Optional.of(extractSolution(
            routing, manager, solution, problem, 
            System.currentTimeMillis() - startMs
        ));
    }
}
```

---

## 8. Service Layer Design

### 8.1 OrderBatchingService

Core orchestration logic:

```java
// application/usecases/OrderBatchingService.java
public class OrderBatchingService implements OrderBatchingUseCase {

    // Dependencies (injected via constructor)
    private final OrderManagementPort orderManagementPort;
    private final DriverOnlineSessionPort driverOnlineSessionPort;
    private final DistanceMatrixPort distanceMatrixPort;
    private final VrpSolverPort vrpSolverPort;
    private final OrderBatchPort orderBatchPort;
    private final GeoPort geoPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    private final SystemParameterPort systemParameterPort;
    private final DriverMatchingPort driverMatchingPort;  // fallback

    @Override
    public BatchCycleResult triggerBatchCycle() {
        // 1. Collect eligible orders (ACCEPTED, no driver, no batch)
        List<Order> eligibleOrders = orderManagementPort
            .findEligibleForBatching(/* status=ACCEPTED, batch_id=NULL */);
        
        if (eligibleOrders.isEmpty()) {
            return BatchCycleResult.empty();
        }
        
        // 2. Collect available drivers
        Set<String> orderH3Cells = collectH3CellsForOrders(eligibleOrders);
        Set<String> searchCells = expandH3Cells(orderH3Cells, searchRadiusKm);
        List<DriverOnlineSession> drivers = driverOnlineSessionPort
            .findOnlineDriversInH3Cells(searchCells)
            .stream()
            .filter(DriverOnlineSession::canAcceptOrder)
            .toList();
        
        if (drivers.isEmpty()) {
            return BatchCycleResult.noDrivers();
        }
        
        // 3. Build location list & distance matrix
        List<LatLng> locations = buildLocationList(drivers, eligibleOrders);
        DistanceMatrixResult matrix = distanceMatrixPort
            .getFullMatrix(locations, "bike");
        
        // 4. Build VRP problem
        VrpProblem problem = buildVrpProblem(
            matrix, eligibleOrders, drivers, locations);
        
        // 5. Solve
        Optional<VrpSolution> solution = vrpSolverPort.solve(problem);
        
        if (solution.isEmpty()) {
            // Fallback: single-assign each order
            return fallbackSingleAssign(eligibleOrders);
        }
        
        // 6. Persist batches and notify
        return persistAndNotify(solution.get(), eligibleOrders, drivers);
    }
}
```

### 8.2 Batch Scheduler Job

```java
// infrastructure/scheduler/BatchSchedulerJob.java
@Component
public class BatchSchedulerJob {
    
    private final OrderBatchingUseCase orderBatchingUseCase;
    
    @Scheduled(fixedDelayString = "${foodya.batching.interval-ms:30000}")
    public void runBatchCycle() {
        try {
            BatchCycleResult result = orderBatchingUseCase.triggerBatchCycle();
            log.info("Batch cycle complete: {} batches created, {} orders assigned, {} single-assigned",
                result.batchesCreated(), result.ordersAssigned(), result.singleAssigned());
        } catch (Exception ex) {
            log.error("Batch cycle failed", ex);
        }
    }
}
```

---

## 9. REST API Design

### 9.1 Driver Batch Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/delivery/batches/active` | Lấy batch đang active của driver |
| `GET` | `/api/v1/delivery/batches/{batchId}` | Chi tiết batch (pickup/delivery sequence, ETA) |
| `POST` | `/api/v1/delivery/batches/{batchId}/pickup/{orderId}` | Mark pickup complete cho 1 order |
| `POST` | `/api/v1/delivery/batches/{batchId}/deliver/{orderId}` | Mark delivery complete cho 1 order |

### 9.2 Customer Batch Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/customer/orders/{orderId}/batch-info` | Thông tin batch (ETA, thứ tự giao, tổng đơn trong batch) |

### 9.3 Admin Batch Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/admin/batches` | List all batches (paginated, filterable) |
| `GET` | `/api/v1/admin/batches/{batchId}` | Chi tiết batch |
| `POST` | `/api/v1/admin/batches/trigger` | Manual trigger batch cycle |
| `GET` | `/api/v1/admin/batches/stats` | Batch performance statistics |

### 9.4 Response DTOs

```java
// Driver batch response
record BatchDetailResponse(
    UUID batchId,
    String status,
    List<BatchPickupStop> pickupStops,
    List<BatchDeliveryStop> deliveryStops,
    int totalDistanceMeters,
    int totalDurationSeconds,
    String routePolyline,
    Instant createdAt
) {}

record BatchPickupStop(
    UUID orderId,
    String orderCode,
    String restaurantName,
    double lat,
    double lng,
    String address,
    int estimatedArrivalSeconds,
    boolean completed
) {}

record BatchDeliveryStop(
    UUID orderId,
    String orderCode,
    String customerName,
    double lat,
    double lng,
    String address,
    int estimatedArrivalSeconds,
    Instant estimatedDeliveryAt,
    boolean completed
) {}

// Customer batch info response
record CustomerBatchInfoResponse(
    UUID batchId,
    int totalOrdersInBatch,
    int myDeliveryPosition,     // e.g., "2 of 3"
    Instant estimatedDeliveryAt,
    int estimatedMinutesRemaining
) {}
```

---

## 10. Configuration Parameters

Thêm vào `system_parameters`:

| Key | Default | Description |
|-----|---------|-------------|
| `batching.enabled` | `true` | Master switch cho batching |
| `batching.interval_seconds` | `30` | Tần suất chạy batch cycle |
| `batching.max_batch_size` | `4` | Max đơn per batch |
| `batching.max_delivery_time_minutes` | `45` | Max time window (phút từ placed_at) |
| `batching.max_detour_minutes` | `15` | Max thời gian chậm hơn so với giao riêng |
| `batching.max_wait_for_batch_seconds` | `120` | Max thời gian chờ batch trước khi fallback single |
| `batching.search_radius_km` | `10` | Bán kính tìm driver |
| `batching.pickup_service_time_seconds` | `300` | Thời gian chờ tại nhà hàng (5 phút) |
| `batching.delivery_service_time_seconds` | `180` | Thời gian giao tại nhà khách (3 phút) |
| `batching.solver_time_limit_ms` | `3000` | OR-Tools solver time limit |
| `batching.cooldown_seconds` | `10` | Thời gian tối thiểu sau khi đơn ACCEPTED trước khi eligible for batch |

---

## 11. Requirements (SEI Format)

### 11.1 Functional Requirements

#### FR-BATCH-001: Batch Collection

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-001 |
| **Title** | Eligible Order Collection |
| **Priority** | MUST HAVE |
| **Description** | Hệ thống phải tự động collect các đơn hàng eligible (status = ACCEPTED, chưa assign driver, chưa có batch_id) theo chu kỳ `batching.interval_seconds`. |
| **Rationale** | Thu thập đủ đơn trong 1 window cho phép solver tối ưu hóa toàn diện. |
| **Acceptance Criteria** | <ul><li>AC1: Chỉ collect đơn status `ACCEPTED` AND `driver_user_id IS NULL` AND `batch_id IS NULL`</li><li>AC2: Đơn phải chờ ít nhất `batching.cooldown_seconds` sau khi ACCEPTED</li><li>AC3: Đơn quá `batching.max_wait_for_batch_seconds` sẽ được ưu tiên assign single</li></ul> |

#### FR-BATCH-002: Distance Matrix Retrieval

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-002 |
| **Title** | Goong v2 Distance Matrix Integration |
| **Priority** | MUST HAVE |
| **Description** | Hệ thống phải gọi Goong Distance Matrix v2 API để lấy ma trận thời gian/khoảng cách thực tế giữa tất cả các điểm (driver positions, restaurants, customer locations). |
| **Rationale** | Haversine (đường chim bay) sai lệch 20-50% so với đường thực tế trong đô thị. Distance matrix cho kết quả chính xác. |
| **Acceptance Criteria** | <ul><li>AC1: Gọi `GET /v2/distancematrix` với `vehicle=bike`</li><li>AC2: Handle chunking khi vượt quá 25 origins × 25 destinations</li><li>AC3: Cache kết quả theo H3 res8 pair, TTL = 15 phút</li><li>AC4: Retry 3 lần với exponential backoff nếu API lỗi</li><li>AC5: Fallback sang Haversine estimate nếu Goong hoàn toàn không phản hồi</li></ul> |

#### FR-BATCH-003: VRPPD-TW Solver

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-003 |
| **Title** | OR-Tools VRPPD-TW Solver |
| **Priority** | MUST HAVE |
| **Description** | Hệ thống phải sử dụng Google OR-Tools RoutingModel để giải bài toán VRPPD-TW, xác định: (a) đơn nào ghép batch với nhau, (b) tài xế nào nhận batch, (c) thứ tự pickup, (d) thứ tự delivery, (e) ETA cho mỗi stop. |
| **Rationale** | OR-Tools cho kết quả ~95-98% optimal cho bài toán NP-hard trong thời gian bounded. |
| **Acceptance Criteria** | <ul><li>AC1: Sử dụng `AddPickupAndDelivery` constraint cho mỗi cặp (pickup, delivery)</li><li>AC2: Time windows cho mỗi node: `[0, placed_at + max_delivery_time]`</li><li>AC3: Vehicle capacity = `DriverOnlineSession.maxConcurrentOrders`</li><li>AC4: Solver time limit ≤ `batching.solver_time_limit_ms` (default 3s)</li><li>AC5: Hỗ trợ "drop" nodes (penalty-based) nếu infeasible — đơn bị drop sẽ fallback single assign</li><li>AC6: First solution strategy: `PATH_CHEAPEST_ARC`</li><li>AC7: Metaheuristic: `GUIDED_LOCAL_SEARCH`</li></ul> |

#### FR-BATCH-004: Pre-computed Batch Finalization

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-004 |
| **Title** | Batch Finalization Before Driver Notification |
| **Priority** | MUST HAVE |
| **Description** | Batch phải được hoàn toàn xác định (tất cả đơn, thứ tự, ETA) TRƯỚC KHI gửi notification cho tài xế. Không có trường hợp thêm/bớt đơn sau khi batch đã gửi cho tài xế. |
| **Rationale** | Yêu cầu nghiệp vụ: tài xế nhận thông tin batch đầy đủ, tránh confuse và thay đổi lộ trình giữa chừng. |
| **Acceptance Criteria** | <ul><li>AC1: Batch status = `PENDING` khi mới tạo (chưa gửi driver)</li><li>AC2: Tất cả orders trong batch chuyển `ACCEPTED → ASSIGNED` cùng lúc (trong 1 transaction)</li><li>AC3: Driver nhận notification với đầy đủ thông tin: danh sách đơn, thứ tự pickup, thứ tự delivery, ETA mỗi stop</li><li>AC4: Sau khi batch gửi driver, KHÔNG thêm order mới vào batch</li><li>AC5: Nếu driver reject batch, tất cả orders quay về ACCEPTED để batch scheduler xử lý lại</li></ul> |

#### FR-BATCH-005: Pickup Sequence

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-005 |
| **Title** | Optimized Pickup Sequence |
| **Priority** | MUST HAVE |
| **Description** | Thứ tự lấy hàng tại các nhà hàng phải được tối ưu bởi solver, tính đến khoảng cách thực tế và thời gian chờ. |
| **Acceptance Criteria** | <ul><li>AC1: Pickup sequence xác định bởi OR-Tools solution</li><li>AC2: Nếu nhiều đơn cùng nhà hàng, gom thành 1 stop (1 lần dừng)</li><li>AC3: Mỗi pickup stop có `estimated_arrival_at` tính từ `batch.created_at`</li><li>AC4: Service time tại pickup = `batching.pickup_service_time_seconds` per stop</li></ul> |

#### FR-BATCH-006: Delivery Sequence

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-006 |
| **Title** | Optimized Delivery Sequence |
| **Priority** | MUST HAVE |
| **Description** | Thứ tự giao cho khách phải được tối ưu bởi solver, đảm bảo tất cả pickup hoàn thành trước delivery cho cùng đơn. |
| **Acceptance Criteria** | <ul><li>AC1: Delivery sequence xác định bởi OR-Tools solution</li><li>AC2: Với mỗi đơn, pickup PHẢI trước delivery (enforced by solver constraint)</li><li>AC3: Mỗi delivery stop có `estimated_delivery_at`</li><li>AC4: Service time tại delivery = `batching.delivery_service_time_seconds` per stop</li><li>AC5: Khách hàng nhận được ETA ngay khi batch được assign</li></ul> |

#### FR-BATCH-007: ETA Calculation

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-007 |
| **Title** | Estimated Time of Arrival |
| **Priority** | MUST HAVE |
| **Description** | Hệ thống phải tính toán và cung cấp ETA cho mỗi stop (pickup và delivery) dựa trên distance matrix và service times. |
| **Acceptance Criteria** | <ul><li>AC1: ETA cho mỗi customer = `batch.created_at` + solver cumulative time</li><li>AC2: ETA được trả về trong customer order detail API</li><li>AC3: ETA cập nhật khi driver complete mỗi stop (re-estimate based on actual progress)</li><li>AC4: Customer batch info API trả về `estimated_delivery_at` và `estimated_minutes_remaining`</li></ul> |

#### FR-BATCH-008: Total Route Metrics

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-008 |
| **Title** | Total Route Distance and Duration |
| **Priority** | MUST HAVE |
| **Description** | Hệ thống phải tính và lưu tổng quãng đường (meters) và tổng thời gian (seconds) cho mỗi batch route. |
| **Acceptance Criteria** | <ul><li>AC1: `total_distance_meters` = tổng distance giữa tất cả consecutive stops trong route</li><li>AC2: `total_duration_seconds` = tổng travel time + service times</li><li>AC3: Metrics được lưu vào `order_batches` table</li><li>AC4: Admin API trả về metrics cho monitoring</li></ul> |

#### FR-BATCH-009: Driver Assignment

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-009 |
| **Title** | Driver Assignment for Batch |
| **Priority** | MUST HAVE |
| **Description** | OR-Tools solver xác định tài xế nào nhận batch nào dựa trên vị trí hiện tại, capacity, và tối ưu tổng thể. |
| **Acceptance Criteria** | <ul><li>AC1: Solver chọn driver tối ưu dựa trên vị trí (depot) gần cluster pickup nhất</li><li>AC2: Driver chỉ nhận batch nếu `canAcceptOrder() = true`</li><li>AC3: `DriverOnlineSession.startDelivery()` được gọi cho mỗi order trong batch</li><li>AC4: `activeOrderCount` increment đúng số orders trong batch</li></ul> |

#### FR-BATCH-010: Fallback Single Assignment

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-010 |
| **Title** | Fallback to Single-Order Assignment |
| **Priority** | MUST HAVE |
| **Description** | Đơn hàng không được ghép batch (solver drop hoặc chỉ 1 eligible order) phải fallback về single-driver assignment flow. |
| **Acceptance Criteria** | <ul><li>AC1: Đơn bị solver "drop" → sử dụng `DriverMatchingPort.findBestDriver()` như hiện tại</li><li>AC2: Đơn chờ > `max_wait_for_batch_seconds` → immediate single assign</li><li>AC3: Nếu chỉ 1 eligible order → skip solver, direct single assign</li><li>AC4: Single-assigned orders có `batch_id = NULL`</li></ul> |

#### FR-BATCH-011: Batch Progress Tracking

| Attribute | Value |
|-----------|-------|
| **ID** | FR-BATCH-011 |
| **Title** | Batch Progress Tracking |
| **Priority** | SHOULD HAVE |
| **Description** | Driver có thể mark từng stop hoàn thành (pickup complete, delivery complete), cập nhật ETA cho các stop còn lại. |
| **Acceptance Criteria** | <ul><li>AC1: API `POST /batches/{batchId}/pickup/{orderId}` marks pickup complete</li><li>AC2: API `POST /batches/{batchId}/deliver/{orderId}` marks delivery complete</li><li>AC3: Khi pickup complete, `batch_order_items.actual_pickup_at` được set</li><li>AC4: Khi delivery complete, order chuyển `DELIVERING → SUCCESS`</li><li>AC5: Khi tất cả orders trong batch SUCCESS/FAILED, batch.status → COMPLETED/PARTIAL</li></ul> |

### 11.2 Non-Functional Requirements

#### NFR-BATCH-001: Performance

| Attribute | Value |
|-----------|-------|
| **ID** | NFR-BATCH-001 |
| **Title** | Batch Cycle Performance |
| **Priority** | MUST HAVE |
| **Description** | Toàn bộ batch cycle (collect + distance matrix + solve + persist) phải hoàn thành trong thời gian hợp lý. |
| **Acceptance Criteria** | <ul><li>AC1: Solver time ≤ 3 seconds cho ≤ 20 orders, ≤ 10 drivers</li><li>AC2: Distance matrix fetch ≤ 5 seconds (with caching)</li><li>AC3: Tổng batch cycle ≤ 15 seconds</li><li>AC4: Không block main thread (chạy trên scheduled thread pool)</li></ul> |

#### NFR-BATCH-002: Reliability

| Attribute | Value |
|-----------|-------|
| **ID** | NFR-BATCH-002 |
| **Title** | Fault Tolerance |
| **Priority** | MUST HAVE |
| **Description** | Batch cycle failures không được ảnh hưởng đến các đơn hàng. Đơn phải luôn có đường fallback. |
| **Acceptance Criteria** | <ul><li>AC1: Nếu OR-Tools crash/timeout → fallback single assign cho tất cả eligible orders</li><li>AC2: Nếu Goong API lỗi → fallback Haversine distance estimate</li><li>AC3: Nếu batch cycle exception → log error, orders stay ACCEPTED, next cycle retry</li><li>AC4: Không có đơn bị "stuck" ở ACCEPTED forever — max wait triggers single assign</li></ul> |

#### NFR-BATCH-003: Observability

| Attribute | Value |
|-----------|-------|
| **ID** | NFR-BATCH-003 |
| **Title** | Monitoring and Logging |
| **Priority** | SHOULD HAVE |
| **Description** | Batch system phải có đủ logging và metrics để monitor performance và debug issues. |
| **Acceptance Criteria** | <ul><li>AC1: Log mỗi batch cycle: eligible count, driver count, solver time, batches created, fallbacks</li><li>AC2: Lưu `solver_metadata` JSONB chứa solver_time_ms, objective_value, num_nodes</li><li>AC3: Admin stats API trả về: avg batch size, avg solver time, batch/single ratio, avg delivery time</li></ul> |

#### NFR-BATCH-004: Configurability

| Attribute | Value |
|-----------|-------|
| **ID** | NFR-BATCH-004 |
| **Title** | Runtime Configuration |
| **Priority** | MUST HAVE |
| **Description** | Tất cả tham số batching phải configurable qua `system_parameters` (runtime, không cần restart). |
| **Acceptance Criteria** | <ul><li>AC1: `batching.enabled` toggle on/off toàn bộ feature</li><li>AC2: Khi disabled, revert về per-order assignment (existing `tryAutoAssignDriver`)</li><li>AC3: Tất cả parameters trong Section 10 configurable via existing admin API</li></ul> |

---

## 12. Definition of Done (DoD)

### 12.1 Code Completion

- [ ] `DistanceMatrixPort` interface + `GoongDistanceMatrixAdapter` implementation
- [ ] `distanceMatrixRaw()` method added to `GoongMapsClient`
- [ ] `VrpSolverPort` interface + `OrToolsVrpSolverAdapter` implementation
- [ ] `OrderBatchPort` interface + `OrderBatchPersistenceAdapter` implementation
- [ ] `OrderBatchingService` implementing `OrderBatchingUseCase`
- [ ] `BatchSchedulerJob` with `@Scheduled` cron
- [ ] Domain entities: `OrderBatch`, value objects: `VrpProblem`, `VrpSolution`, `VrpRoute`, etc.
- [ ] Persistence: `OrderBatchPersistenceModel`, `BatchOrderItemPersistenceModel`, `OrderBatchMapper`
- [ ] REST controllers: `DeliveryBatchController`, `CustomerBatchInfoController`, `AdminBatchController`
- [ ] Flyway migrations for new tables (`order_batches`, `batch_order_items`, alter `orders`)
- [ ] `OrderLifecycleService` updated: remove `tryAutoAssignDriver()` on merchant accept
- [ ] All services wired in `AppConfig.java`

### 12.2 Architecture Compliance

- [ ] All new code passes existing ArchUnit tests
- [ ] Application layer has NO framework annotations (`@Service`, `@Transactional`)
- [ ] VRP solver behind port (application layer doesn't depend on OR-Tools directly)
- [ ] Distance matrix behind port (application layer doesn't depend on Goong client directly)
- [ ] Domain entities are pure (no JPA annotations)

### 12.3 Testing

- [ ] Unit tests for `OrderBatchingService`:
  - Batch cycle with eligible orders → creates batches
  - Batch cycle with no eligible orders → no-op
  - Batch cycle with single eligible order → fallback single assign
  - Batch cycle with solver failure → fallback single assign
  - Order exceeds max wait → immediate single assign
- [ ] Unit tests for `OrToolsVrpSolverAdapter`:
  - Simple 2-order, 1-driver case → correct sequence
  - 4-order, 2-driver case → correct assignment and sequencing
  - Infeasible problem → returns empty or drops nodes
  - Time window violation → drops violating orders
- [ ] Integration tests for `GoongDistanceMatrixAdapter`:
  - Mock Goong API response → correct matrix parsing
  - Chunking for > 25 locations
  - Retry on failure
- [ ] Integration tests for batch REST APIs:
  - Driver get active batch
  - Driver mark pickup/delivery complete
  - Customer get batch info
  - Admin list batches
  - Admin trigger batch cycle
- [ ] Existing tests still pass:
  - `OrderLifecycleIntegrationTests` (merchant accept no longer auto-assigns)
  - `ArchitectureRulesTests`
  - `OpenApiLiveRouteSmokeIntegrationTests`

### 12.4 Documentation

- [ ] This spec document finalized in `docs/ORDER_BATCHING_SPEC.md`
- [ ] API endpoints documented in Swagger/OpenAPI annotations
- [ ] `implementation_plan.md` Phase 5 updated to reference this document
- [ ] `backend/docs/SEED_ACCOUNTS.md` updated if new seed data needed

### 12.5 Deployment

- [ ] `google-ortools` dependency added to `pom.xml` and resolves correctly
- [ ] Flyway migrations run successfully on PostgreSQL
- [ ] Docker image builds with OR-Tools native libraries
- [ ] System parameters seeded with default values
- [ ] Feature toggle `batching.enabled` works correctly (on/off)

---

## 13. Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| OR-Tools native library compatibility (Linux/macOS/ARM) | HIGH | MEDIUM | Test on target deployment platform early. Use `ortools-java` which bundles natives. |
| Goong Distance Matrix API rate limits | MEDIUM | LOW | Implement caching, batch API calls efficiently. Monitor usage. |
| Solver timeout with large problem size (> 30 orders) | MEDIUM | LOW | Cap eligible orders per cycle, use time limit, partition by geographic region if needed. |
| Batch cycle takes too long, delaying assignment | HIGH | LOW | Hard time limit on solver + fallback. Monitor cycle duration. |
| Customer upset about delayed delivery in batch | MEDIUM | MEDIUM | Max detour constraint (15 min). Show transparent ETA. Allow opt-out via config. |
| Merchant food gets cold waiting for batch | MEDIUM | MEDIUM | Pickup service time accounts for prep. Batch scheduler runs frequently (30s). |

---

## 14. Glossary

| Term | Definition |
|------|------------|
| **Batch** | Nhóm 2-4 đơn hàng được giao bởi cùng 1 tài xế trong 1 chuyến |
| **Batch Cycle** | Một lần chạy scheduler: collect → solve → assign |
| **VRPPD-TW** | Vehicle Routing Problem with Pickup and Delivery and Time Windows |
| **Distance Matrix** | Ma trận N×N chứa khoảng cách/thời gian giữa tất cả các cặp điểm |
| **Pickup Node** | Vị trí nhà hàng (nơi tài xế lấy đồ ăn) |
| **Delivery Node** | Vị trí khách hàng (nơi tài xế giao đồ ăn) |
| **Depot** | Vị trí hiện tại của tài xế (điểm xuất phát) |
| **Time Window** | Khoảng thời gian cho phép visit 1 node |
| **Service Time** | Thời gian dừng tại 1 node (lấy đồ / giao đồ) |
| **Solver** | Google OR-Tools RoutingModel engine |
| **Fallback** | Cơ chế dự phòng: gán single driver khi batch không khả thi |
