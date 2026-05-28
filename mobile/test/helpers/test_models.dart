import 'package:foodya_mobile/core/auth/auth_tokens.dart';
import 'package:foodya_mobile/core/auth/user_role.dart';
import 'package:foodya_mobile/features/auth/domain/models/auth_session.dart';
import 'package:foodya_mobile/features/customer/domain/models/active_cart.dart';
import 'package:foodya_mobile/features/customer/domain/models/ai_chat.dart';
import 'package:foodya_mobile/features/customer/domain/models/cart_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/category_taxonomy.dart';
import 'package:foodya_mobile/features/customer/domain/models/customer_notification.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_cost_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_created.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_review.dart';
import 'package:foodya_mobile/features/customer/domain/models/order_summary.dart';
import 'package:foodya_mobile/features/customer/domain/models/paged_result.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_detail.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_menu_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/restaurant_search_item.dart';
import 'package:foodya_mobile/features/customer/domain/models/user_profile.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_restaurant_request.dart';
import 'package:foodya_mobile/features/merchant/domain/models/merchant_revenue_report.dart';

AuthSession authSession({UserRole role = UserRole.customer}) {
  return AuthSession(
    role: role,
    tokens: const AuthTokens(
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
    ),
  );
}

CategoryTaxonomy categoryTaxonomy({
  String code = 'vietnamese',
  String displayName = 'Vietnamese',
}) {
  return CategoryTaxonomy(
    code: code,
    displayName: displayName,
    sortOrder: 1,
    isActive: true,
  );
}

CartItem cartItem({String id = 'item-1', int quantity = 1}) {
  return CartItem(
    menuItemId: id,
    menuItemName: 'Pho Bo',
    unitPrice: 50000,
    quantity: quantity,
    lineTotal: 50000.0 * quantity,
    note: null,
  );
}

ActiveCart activeCart({bool empty = false}) {
  final items = empty ? const <CartItem>[] : [cartItem()];
  return ActiveCart(
    cartId: 'cart-1',
    restaurantId: 'restaurant-1',
    restaurantName: 'Pho House',
    subtotal: empty ? 0 : 50000,
    itemCount: items.length,
    items: items,
  );
}

OrderCostReview orderCostReview() {
  return const OrderCostReview(
    subtotalAmount: 50000,
    deliveryFee: 10000,
    totalAmount: 60000,
    commissionAmount: 5000,
    shippingFeeMarginAmount: 1000,
    platformProfitAmount: 6000,
    currencyCode: 'VND',
  );
}

OrderCreated orderCreated() {
  return const OrderCreated(
    orderId: 'order-1',
    orderCode: 'FDY-001',
    status: 'PENDING',
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    subtotalAmount: 50000,
    deliveryFee: 10000,
    totalAmount: 60000,
    commissionAmount: 5000,
    shippingFeeMarginAmount: 1000,
    platformProfitAmount: 6000,
    currencyCode: 'VND',
  );
}

OrderSummary orderSummary({String status = 'PENDING'}) {
  return OrderSummary(
    orderId: 'order-1',
    orderCode: 'FDY-001',
    customerName: 'Alice',
    restaurantName: 'Pho House',
    status: status,
    paymentStatus: 'UNPAID',
    totalAmount: 60000,
  );
}

OrderDetail orderDetail({String status = 'PENDING'}) {
  return OrderDetail(
    orderId: 'order-1',
    orderCode: 'FDY-001',
    restaurantId: 'restaurant-1',
    restaurantName: 'Pho House',
    customerUserId: 'customer-1',
    customerName: 'Alice',
    status: status,
    paymentMethod: 'COD',
    paymentStatus: 'UNPAID',
    subtotalAmount: 50000,
    deliveryFee: 10000,
    totalAmount: 60000,
    deliveryAddress: '1 Nguyen Hue',
  );
}

OrderReview orderReview() {
  return OrderReview(
    reviewId: 'review-1',
    orderId: 'order-1',
    restaurantId: 'restaurant-1',
    customerUserId: 'customer-1',
    stars: 5,
    comment: 'Great food',
    createdAt: DateTime(2026, 1, 1),
  );
}

RestaurantSearchItem restaurantSearchItem({String id = 'restaurant-1'}) {
  return RestaurantSearchItem(
    restaurantId: id,
    restaurantName: 'Pho House',
    cuisine: 'Vietnamese',
    backgroundImageUrl: null,
    avatarImageUrl: null,
    rating: 4.7,
    openStatus: true,
    maxDeliveryKm: 5,
    distanceKm: 1.2,
    matchedItems: const [],
  );
}

PagedResult<RestaurantSearchItem> restaurantSearchPage({
  List<RestaurantSearchItem>? items,
  int page = 0,
  int totalPages = 1,
}) {
  final resolved = items ?? [restaurantSearchItem()];
  return PagedResult(
    items: resolved,
    page: page,
    size: 10,
    totalElements: resolved.length,
    totalPages: totalPages,
  );
}

RestaurantDetail restaurantDetail() {
  return const RestaurantDetail(
    id: 'restaurant-1',
    name: 'Pho House',
    cuisineType: 'Vietnamese',
    description: 'Warm bowls',
    backgroundImageUrl: null,
    avatarImageUrl: null,
    addressLine: '1 Nguyen Hue',
    latitude: 10.77,
    longitude: 106.7,
    avgRating: 4.7,
    reviewCount: 12,
    open: true,
    maxDeliveryKm: 5,
  );
}

RestaurantMenuItem restaurantMenuItem({String id = 'item-1'}) {
  return RestaurantMenuItem(
    id: id,
    restaurantId: 'restaurant-1',
    categoryId: 'category-1',
    taxonomyCodes: const ['vietnamese'],
    name: 'Pho Bo',
    description: 'Beef noodle soup',
    imageUrl: null,
    price: 50000,
    active: true,
    available: true,
  );
}

PagedResult<RestaurantMenuItem> menuPage({
  List<RestaurantMenuItem>? items,
  int page = 0,
  int totalPages = 1,
}) {
  final resolved = items ?? [restaurantMenuItem()];
  return PagedResult(
    items: resolved,
    page: page,
    size: 20,
    totalElements: resolved.length,
    totalPages: totalPages,
  );
}

CustomerNotification customerNotification({String status = 'UNREAD'}) {
  return CustomerNotification(
    id: 'notification-1',
    receiverUserId: 'customer-1',
    receiverType: 'CUSTOMER',
    eventType: 'ORDER',
    title: 'New update',
    message: 'Your order changed.',
    status: status,
    orderId: 'order-1',
    sentAt: DateTime(2026, 1, 1),
    readAt: status == 'READ' ? DateTime(2026, 1, 2) : null,
    createdAt: DateTime(2026, 1, 1),
  );
}

UserProfile userProfile() {
  return const UserProfile(
    id: 'customer-1',
    username: 'alice',
    email: 'alice@example.com',
    phoneNumber: '0900000000',
    fullName: 'Alice Nguyen',
    role: 'CUSTOMER',
    status: 'ACTIVE',
  );
}

AiChatHistoryItem aiHistoryItem() {
  return AiChatHistoryItem(
    chatId: 'chat-1',
    prompt: 'Dinner',
    responseSummary: 'Try pho.',
    createdAt: DateTime(2026, 1, 1),
  );
}

AiChatResponse aiChatResponse() {
  return AiChatResponse(
    chatId: 'chat-1',
    prompt: 'Dinner',
    responseSummary: 'Try pho.',
    recommendations: const [
      AiRecommendationItem(
        menuItemId: 'item-1',
        menuItemName: 'Pho Bo',
        restaurantId: 'restaurant-1',
        restaurantName: 'Pho House',
        price: 50000,
        reason: 'Warm and popular.',
      ),
    ],
    createdAt: DateTime(2026, 1, 1),
  );
}

MerchantRestaurant merchantRestaurant({bool open = true}) {
  return MerchantRestaurant(
    id: 'restaurant-1',
    name: 'Pho House',
    cuisineType: 'Vietnamese',
    description: 'Warm bowls',
    addressLine: '1 Nguyen Hue',
    latitude: 10.77,
    longitude: 106.7,
    avgRating: 4.6,
    reviewCount: 10,
    status: 'APPROVED',
    open: open,
    maxDeliveryKm: 5,
  );
}

MerchantRestaurantRequest merchantRestaurantRequest({bool isOpen = true}) {
  return MerchantRestaurantRequest(
    name: 'Pho House',
    cuisineType: 'Vietnamese',
    cuisineTypes: const ['Vietnamese'],
    description: 'Warm bowls',
    addressLine: '1 Nguyen Hue',
    latitude: 10.77,
    longitude: 106.7,
    maxDeliveryKm: 5,
    isOpen: isOpen,
  );
}

MerchantRevenueReport merchantRevenueReport() {
  return MerchantRevenueReport(
    fromDate: DateTime(2026, 1, 1),
    toDate: DateTime(2026, 1, 7),
    revenue: 900000,
    platformProfit: 100000,
    orderCount: 12,
    avgOrderValue: 75000,
    series: const [],
    topSellingItems: const [],
  );
}
