# Foodya Mobile Integration Guide

**Target audience:** Flutter developers building the Customer and Driver mobile apps.

**Base URL (local dev):** `http://localhost:8080/api/v1`  
**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`

---

## Table of Contents

1. [Authentication & Tokens](#1-authentication--tokens)
2. [Customer App Flow](#2-customer-app-flow)
3. [Driver App Flow](#3-driver-app-flow)
4. [Maps Integration (GoongMap)](#4-maps-integration-goongmap)
5. [Real-Time Tracking (STOMP WebSocket)](#5-real-time-tracking-stomp-websocket)
6. [Payment Integration (VNPAY)](#6-payment-integration-vnpay)
7. [Push Notifications (FCM)](#7-push-notifications-fcm)
8. [Error Handling](#8-error-handling)

---

## 1. Authentication & Tokens

### Registration & Login

**Register:** `POST /auth/register`
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "fullName": "Nguyen Van A",
  "phoneNumber": "+84912345678",
  "role": "CUSTOMER"
}
```
Role: `CUSTOMER`, `MERCHANT`, `DRIVER`, or `ADMIN`.

**Login:** `POST /auth/login`
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```
Response:
```json
{
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "abc123...",
    "expiresIn": 3600,
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "fullName": "Nguyen Van A",
      "role": "CUSTOMER"
    }
  }
}
```

**Refresh:** `POST /auth/refresh`
```json
{
  "refreshToken": "abc123..."
}
```

### Using Tokens

Include the access token in all authenticated requests:
```
Authorization: Bearer eyJhbGc...
```

When the access token expires (HTTP 401), call `/auth/refresh` with the refresh token to get a new access token.

---

## 2. Customer App Flow

### 2.1 Browse Restaurants

**Search/filter:** `GET /customer/catalog/restaurants?lat=10.762622&lng=106.660172&search=pizza&category=FAST_FOOD&page=0&size=20`

Response includes:
- Restaurant list with `name`, `address`, `cuisineType`, `logoUrl`, `avgRating`, `distance` (meters)
- Pagination metadata

**Restaurant detail:** `GET /customer/catalog/restaurants/{restaurantId}`

Returns menu categories and items, opening hours, ratings.

### 2.2 Cart & Checkout

**Add to cart:** `POST /customer/cart/items`
```json
{
  "menuItemId": "uuid",
  "quantity": 2,
  "specialInstructions": "No onions"
}
```

**View cart:** `GET /customer/cart`

**Update quantity:** `PATCH /customer/cart/items/{cartItemId}`
```json
{
  "quantity": 3
}
```

**Remove item:** `DELETE /customer/cart/items/{cartItemId}`

**Clear cart:** `DELETE /customer/cart`

### 2.3 Place Order

**Preview cost:** `POST /customer/orders/preview`
```json
{
  "deliveryAddress": "123 Nguyen Hue, District 1, HCMC",
  "deliveryLat": 10.762622,
  "deliveryLng": 106.660172,
  "deliveryPhone": "+84912345678",
  "paymentMethod": "COD"
}
```

Response shows:
- `subtotal`, `deliveryFee`, `taxAmount`, `commissionAmount`, `total`
- `feeBreakdown`: JSON string explaining delivery fee calculation (base + distance + weather + surge)

**Place order:** `POST /customer/orders`
```json
{
  "deliveryAddress": "123 Nguyen Hue, District 1, HCMC",
  "deliveryLat": 10.762622,
  "deliveryLng": 106.660172,
  "deliveryPhone": "+84912345678",
  "paymentMethod": "VNPAY_QR",
  "specialInstructions": "Ring the bell twice"
}
```

Response:
```json
{
  "data": {
    "order": {
      "id": "uuid",
      "orderCode": "FOOD-20261104-ABCD",
      "status": "PENDING",
      "paymentStatus": "UNPAID",
      "totalAmount": 150000,
      ...
    }
  }
}
```

Order moves from `PENDING` → `ACCEPTED` → `ASSIGNED` → `PREPARING` → `DELIVERING` → `SUCCESS`.

### 2.4 Payment

If `paymentMethod` is `VNPAY_QR`:

**Initiate payment:** `POST /customer/orders/{orderId}/pay`
```json
{
  "paymentMethod": "VNPAY_QR"
}
```

Response:
```json
{
  "data": {
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "transactionRef": "FOOD-20261104-ABCD-a1b2c3d4",
    "amount": 150000
  }
}
```

**Flutter WebView flow:**
1. Open `paymentUrl` in a WebView
2. User scans QR or enters card details on VNPAY page
3. VNPAY redirects to `returnUrl` (configured in backend `.env` as `FOODYA_VNPAY_RETURN_URL`)
4. The return URL is handled by the backend's `GET /payments/vnpay/return`, which renders an HTML page that posts a message to Flutter
5. Extract the payment result from the URL query params: `vnp_ResponseCode=00` means success
6. Close the WebView and call **check payment status**

**Check payment status:** `GET /customer/orders/{orderId}/payment-status`
```json
{
  "data": {
    "orderPaymentStatus": "PAID",
    "transactionStatus": "COMPLETED",
    "paymentMethod": "VNPAY_QR"
  }
}
```

When payment succeeds, `order.paymentStatus` becomes `PAID` and the order can proceed.

**For COD:** No payment step needed. Driver collects cash on delivery.

### 2.5 Track Order

**Order detail:** `GET /customer/orders/{orderId}`

Shows current `status`, `driverUserId`, `driverName` (when assigned), ETA, tracking points.

**Real-time tracking:** Subscribe to STOMP WebSocket (see section 5).

**Order history:** `GET /customer/orders?page=0&size=20`

**Cancel order:** `POST /customer/orders/{orderId}/cancel`
```json
{
  "reason": "Changed my mind"
}
```
Only works if `status` is `PENDING` or `ACCEPTED`.

### 2.6 Review Order

After order reaches `SUCCESS`:

**Submit review:** `POST /customer/orders/{orderId}/reviews`
```json
{
  "rating": 5,
  "comment": "Great food, fast delivery!"
}
```

---

## 3. Driver App Flow

### 3.1 Go Online/Offline

**Go online:** `POST /delivery/online`
```json
{
  "lat": 10.762622,
  "lng": 106.660172
}
```

Response:
```json
{
  "data": {
    "sessionId": "uuid",
    "status": "ONLINE",
    "startedAt": "2026-11-04T10:00:00Z"
  }
}
```

Driver is now visible to the matching algorithm.

**Update location:** `PUT /delivery/location`
```json
{
  "lat": 10.763000,
  "lng": 106.661000
}
```

Send this every 5–10 seconds while online. The backend caches it in Redis for fast matching.

**Go offline:** `POST /delivery/offline`

If the driver has active deliveries, status becomes `GOING_OFFLINE` (will auto-offline after last delivery).

**Current session:** `GET /delivery/session`

### 3.2 Accept/Reject Orders

**View assignments:** `GET /delivery/assignments`

Returns orders in `ASSIGNED`, `PREPARING`, or `DELIVERING` status assigned to this driver.

**Accept assignment:** `POST /delivery/orders/{orderId}/accept`

Driver confirms they will handle this order.

**Reject assignment:** `POST /delivery/orders/{orderId}/reject`

Order returns to `ACCEPTED` for reassignment to another driver.

### 3.3 Update Delivery Status

**Mark picked up:** `POST /delivery/orders/{orderId}/pickup`

Changes status from `PREPARING` → `DELIVERING`.

**Mark delivered:** `POST /delivery/orders/{orderId}/delivery`

Changes status from `DELIVERING` → `SUCCESS`. Order is complete.

**Order detail:** `GET /delivery/orders/{orderId}`

### 3.4 Batched Deliveries

When the backend assigns a batch, `/delivery/assignments` returns multiple orders with the same `batchId`.

**Batch detail:** `GET /delivery/batches/{batchId}`

Response:
```json
{
  "data": {
    "id": "uuid",
    "driverUserId": "uuid",
    "status": "ASSIGNED",
    "orders": [
      {"orderId": "uuid1", "sequence": 0, "restaurant": {...}, "delivery": {...}},
      {"orderId": "uuid2", "sequence": 1, "restaurant": {...}, "delivery": {...}}
    ],
    "routeSummary": {
      "totalDistance": 8500,
      "totalDuration": 1200,
      "waypoints": [...]
    }
  }
}
```

The `waypoints` array is the optimized route: `[restaurant1, restaurant2, delivery1, delivery2]`.

**Mark pickup/delivery for batch:**
- `POST /delivery/batches/{batchId}/orders/{orderId}/pickup`
- `POST /delivery/batches/{batchId}/orders/{orderId}/delivery`

### 3.5 Earnings

**View earnings:** `GET /delivery/earnings?from=2026-11-01T00:00:00Z&to=2026-11-04T23:59:59Z`

Response:
```json
{
  "data": {
    "from": "2026-11-01T00:00:00Z",
    "to": "2026-11-04T23:59:59Z",
    "totalEarnings": 450000,
    "codCashCollected": 300000,
    "deliveryCount": 15,
    "entries": [
      {
        "orderId": "uuid",
        "orderCode": "FOOD-20261104-ABCD",
        "completedAt": "2026-11-04T12:34:56Z",
        "deliveryFee": 35000,
        "platformMargin": 5000,
        "driverEarning": 30000,
        "paymentMethod": "COD"
      }
    ]
  }
}
```

**`driverEarning`** = `deliveryFee` − `platformMargin` (the platform takes a cut).

**`codCashCollected`** is the total order value the driver collected in cash for COD orders. The driver keeps their earnings and remits `codCashCollected - totalEarnings` to the platform.

---

## 4. Maps Integration (GoongMap)

**Goong Maps Flutter SDK:** https://docs.goong.io/flutter-sdk/

Get your API keys from the backend team and store them in your Flutter environment config.

### 4.1 Display Map

Add the `goong_map_flutter` package:
```dart
GoongMap(
  apiKey: 'YOUR_GOONG_API_KEY',
  initialCameraPosition: CameraPosition(
    target: LatLng(10.762622, 106.660172),
    zoom: 15,
  ),
  onMapCreated: (GoongMapController controller) {
    _controller = controller;
  },
)
```

### 4.2 Geocoding (Address → Lat/Lng)

When the customer types an address, call Goong's Geocode API:
```
GET https://rsapi.goong.io/geocode?address=123%20Nguyen%20Hue%2C%20District%201&api_key=YOUR_GOONG_API_KEY
```

Response:
```json
{
  "results": [
    {
      "geometry": {
        "location": {"lat": 10.762622, "lng": 106.660172}
      },
      "formatted_address": "123 Nguyễn Huệ, Bến Nghé, Quận 1, TP.HCM"
    }
  ]
}
```

Use the `lat`/`lng` when placing the order.

### 4.3 Driver Navigation (Turn-by-Turn)

The backend's `/delivery/orders/{orderId}` response includes a `route` object with `polyline` (encoded string) and `waypoints`.

**Decode polyline:**
```dart
import 'package:google_polyline_algorithm/google_polyline_algorithm.dart';

final points = decodePolyline(route.polyline);
final latLngs = points.map((p) => LatLng(p[0].toDouble(), p[1].toDouble())).toList();
```

**Draw route on map:**
```dart
_controller.addPolyline(PolylineOptions(
  geometry: latLngs,
  polylineColor: Colors.blue,
  polylineWidth: 5.0,
));
```

**Turn-by-turn instructions:**

The backend returns `steps` in the route:
```json
{
  "steps": [
    {
      "instruction": "Head north on Nguyễn Huệ",
      "distance": 250,
      "duration": 30
    },
    {
      "instruction": "Turn right onto Lê Lợi",
      "distance": 500,
      "duration": 60
    }
  ]
}
```

Show the current step's `instruction` in a floating panel. Update it as the driver progresses.

**Snap to route:**

As the driver moves, the backend's real-time tracking endpoint snaps their GPS position to the route and recalculates ETA. The customer sees this via WebSocket.

### 4.4 Restaurant & Delivery Markers

**Restaurant marker:**
```dart
_controller.addSymbol(SymbolOptions(
  geometry: LatLng(restaurant.lat, restaurant.lng),
  iconImage: 'restaurant-pin',
  textField: restaurant.name,
));
```

**Delivery marker:**
```dart
_controller.addSymbol(SymbolOptions(
  geometry: LatLng(order.deliveryLat, order.deliveryLng),
  iconImage: 'delivery-pin',
  textField: 'Delivery',
));
```

**Driver marker (customer view):**

Subscribe to WebSocket tracking updates (see section 5) and move the driver marker as position updates arrive:
```dart
_controller.updateSymbol(driverSymbol, SymbolOptions(
  geometry: LatLng(update.currentLat, update.currentLng),
));
```

---

## 5. Real-Time Tracking (STOMP WebSocket)

The backend publishes tracking updates to `/queue/orders/{orderId}/tracking` via STOMP over WebSocket.

### 5.1 Connect to WebSocket

Add the `stomp_dart_client` package:
```yaml
dependencies:
  stomp_dart_client: ^1.0.0
```

**Connect:**
```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';

final client = StompClient(
  config: StompConfig.sockJS(
    url: 'http://localhost:8080/ws',
    onConnect: (StompFrame frame) {
      print('Connected to WebSocket');
      _subscribeToOrder(orderId);
    },
    onWebSocketError: (dynamic error) => print('WebSocket error: $error'),
  ),
);

client.activate();
```

### 5.2 Subscribe to Order Tracking

```dart
void _subscribeToOrder(String orderId) {
  client.subscribe(
    destination: '/queue/orders/$orderId/tracking',
    callback: (StompFrame frame) {
      final update = jsonDecode(frame.body!);
      _handleTrackingUpdate(update);
    },
  );
}
```

### 5.3 Handle Tracking Updates

```dart
void _handleTrackingUpdate(Map<String, dynamic> update) {
  // update structure:
  // {
  //   "orderId": "uuid",
  //   "status": "DELIVERING",
  //   "currentLat": 10.763,
  //   "currentLng": 106.661,
  //   "distanceRemaining": 1500,
  //   "etaSeconds": 300,
  //   "timestamp": "2026-11-04T12:34:56Z"
  // }

  setState(() {
    _currentDriverPosition = LatLng(update['currentLat'], update['currentLng']);
    _eta = Duration(seconds: update['etaSeconds']);
    _distanceRemaining = update['distanceRemaining'];
  });

  // Move driver marker on map
  _controller.updateSymbol(_driverSymbol, SymbolOptions(
    geometry: _currentDriverPosition,
  ));
}
```

### 5.4 Disconnect

```dart
@override
void dispose() {
  client.deactivate();
  super.dispose();
}
```

---

## 6. Payment Integration (VNPAY)

**VNPAY Sandbox:** https://sandbox.vnpayment.vn/  
**Test card:** NCB `9704198526191432198`, name `NGUYEN VAN A`, issued `07/15`, OTP `123456`

### 6.1 Payment Flow

1. User places order with `paymentMethod: "VNPAY_QR"`
2. Call `POST /customer/orders/{orderId}/pay` to get the payment URL
3. Open the URL in a WebView
4. User completes payment on VNPAY page
5. VNPAY redirects to the backend's return URL
6. The backend's return page posts a message back to Flutter via `postMessage`
7. Flutter extracts the result and closes the WebView
8. Call `GET /customer/orders/{orderId}/payment-status` to confirm

### 6.2 Flutter WebView Code

```dart
import 'package:webview_flutter/webview_flutter.dart';

class VNPayPaymentScreen extends StatefulWidget {
  final String paymentUrl;
  final String orderId;

  const VNPayPaymentScreen({required this.paymentUrl, required this.orderId});

  @override
  _VNPayPaymentScreenState createState() => _VNPayPaymentScreenState();
}

class _VNPayPaymentScreenState extends State<VNPayPaymentScreen> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(NavigationDelegate(
        onPageFinished: (String url) {
          if (url.contains('/payments/vnpay/return')) {
            _checkPaymentResult();
          }
        },
      ))
      ..loadRequest(Uri.parse(widget.paymentUrl));
  }

  void _checkPaymentResult() async {
    // Close WebView
    Navigator.pop(context);

    // Check payment status from backend
    final response = await http.get(
      Uri.parse('http://localhost:8080/api/v1/customer/orders/${widget.orderId}/payment-status'),
      headers: {'Authorization': 'Bearer $accessToken'},
    );

    final data = jsonDecode(response.body)['data'];
    if (data['orderPaymentStatus'] == 'PAID') {
      _showSuccess();
    } else {
      _showFailure();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Payment')),
      body: WebViewWidget(controller: _controller),
    );
  }
}
```

### 6.3 Handling Return URL

The backend's `GET /payments/vnpay/return` endpoint renders an HTML page that shows the payment result. You can customize this page to post a message to Flutter if using a hybrid approach, or simply check the URL for `vnp_ResponseCode`.

**Success:** `vnp_ResponseCode=00`  
**Failed:** any other code (e.g., `02` = merchant not found, `12` = card locked)

---

## 7. Push Notifications (FCM)

### 7.1 Register Device Token

After login, register the FCM token:
```dart
import 'package:firebase_messaging/firebase_messaging.dart';

final token = await FirebaseMessaging.instance.getToken();

await http.post(
  Uri.parse('http://localhost:8080/api/v1/customer/notifications/device-token'),
  headers: {
    'Authorization': 'Bearer $accessToken',
    'Content-Type': 'application/json',
  },
  body: jsonEncode({'token': token}),
);
```

### 7.2 Handle Incoming Notifications

```dart
FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  print('Notification: ${message.notification?.title}');
  print('Data: ${message.data}');

  // Example data: {"orderId": "uuid", "status": "ASSIGNED"}
  if (message.data['orderId'] != null) {
    _navigateToOrderDetail(message.data['orderId']);
  }
});
```

### 7.3 Notification Types

- **Order status change:** `orderId`, `status` (e.g., `ASSIGNED`, `PREPARING`, `DELIVERING`, `SUCCESS`)
- **Driver assigned:** `orderId`, `driverName`, `driverPhone`
- **Payment completed:** `orderId`, `paymentStatus`
- **Order cancelled:** `orderId`, `reason`

---

## 8. Error Handling

### 8.1 HTTP Status Codes

- **200:** Success
- **201:** Created
- **400:** Bad request (validation error)
- **401:** Unauthorized (invalid/expired token)
- **403:** Forbidden (role mismatch)
- **404:** Not found
- **409:** Conflict (e.g., duplicate cart item)
- **500:** Server error

### 8.2 Error Response Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": [
      {"field": "email", "message": "Invalid email format"}
    ]
  },
  "trace": {
    "requestId": "abc123",
    "timestamp": "2026-11-04T12:34:56Z"
  }
}
```

### 8.3 Common Error Codes

- `VALIDATION_ERROR`: Input validation failed
- `AUTHENTICATION_ERROR`: Invalid credentials
- `AUTHORIZATION_ERROR`: Insufficient permissions
- `RESOURCE_NOT_FOUND`: Entity not found
- `CONFLICT`: Business rule violation (e.g., cart already has items from another restaurant)
- `RATE_LIMIT_EXCEEDED`: Too many requests

### 8.4 Retry Logic

- **Network errors:** Retry with exponential backoff (1s, 2s, 4s, 8s)
- **401 Unauthorized:** Refresh token once, then retry the original request
- **429 Rate Limit:** Wait for the duration in `Retry-After` header
- **500 Server Error:** Retry once after 2s, then show error

---

## UI Patterns & Recommendations

### Customer App

**Home Screen:**
- Search bar at top
- Category chips (Pizza, Burgers, Vietnamese, etc.)
- Restaurant cards with photo, name, rating, distance, delivery time estimate
- "Track Order" button if there's an active order

**Restaurant Detail:**
- Cover image, logo, name, rating
- Menu categories (tabs or sections)
- Menu items with photo, name, price, "Add to Cart" button
- Cart summary floating at bottom showing item count and subtotal

**Checkout:**
- Delivery address with map pin (allow editing)
- Order summary (items, subtotal, delivery fee breakdown, tax, total)
- Payment method selector (COD / VNPAY)
- "Place Order" button

**Tracking:**
- Map showing restaurant, delivery address, driver location (if assigned)
- ETA chip at top
- Order status timeline (Pending → Preparing → Out for Delivery → Delivered)
- Driver info card (name, photo, phone, vehicle) when assigned
- "Call Driver" button

**Payment:**
- Full-screen WebView for VNPAY
- Loading spinner while waiting for redirect
- Success/failure overlay after return

### Driver App

**Main Screen (Online):**
- Map centered on driver's location
- "GO" toggle button (green = online, gray = offline)
- Earnings summary card (today's earnings, deliveries count)
- Active assignments list at bottom (swipe up to see details)

**Order Detail:**
- Restaurant address, pickup time
- Customer address, delivery time
- Order items and special instructions
- "Accept" / "Reject" buttons (if not yet accepted)
- "Mark Picked Up" / "Mark Delivered" buttons
- Route polyline on map with turn-by-turn directions

**Batch View:**
- List of all orders in the batch with sequence numbers
- Optimized route showing all pickup and delivery waypoints
- Current step highlighted
- "Navigate" button opens the route in Goong Maps or Google Maps

**Earnings:**
- Date range picker
- Total earnings for period
- COD cash collected (amount to remit)
- List of completed deliveries with per-delivery breakdown

---

## Testing Checklist

### Customer App
- [ ] Register and login
- [ ] Browse restaurants, search, filter by category
- [ ] View restaurant detail and menu
- [ ] Add items to cart, update quantity, remove items
- [ ] Preview order cost (verify delivery fee breakdown)
- [ ] Place order with COD
- [ ] Place order with VNPAY, complete payment in sandbox
- [ ] Track order on map with live driver location
- [ ] Receive push notifications for status changes
- [ ] Submit review after delivery
- [ ] View order history
- [ ] Cancel order (before driver assigned)

### Driver App
- [ ] Login with driver role
- [ ] Go online, location updates sent every 10s
- [ ] Receive order assignment (push notification)
- [ ] Accept assignment
- [ ] View route to restaurant
- [ ] Mark order picked up
- [ ] Navigate to delivery address
- [ ] Mark order delivered
- [ ] View earnings summary
- [ ] Go offline
- [ ] Reject assignment and verify order is reassigned
- [ ] Handle batched deliveries (multiple pickups/deliveries in one route)

---

## Additional Resources

- **Backend Swagger:** http://localhost:8080/swagger-ui/index.html
- **Goong Maps Flutter:** https://docs.goong.io/flutter-sdk/
- **VNPAY Integration:** https://sandbox.vnpayment.vn/apis/docs/
- **STOMP Flutter Client:** https://pub.dev/packages/stomp_dart_client
- **Firebase Messaging Flutter:** https://firebase.flutter.dev/docs/messaging/overview

---

**Questions?** Check the backend `README.md` for API details, or test endpoints directly via Swagger UI.
