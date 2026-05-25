import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/pages/auth_loading_page.dart';
import '../../features/auth/presentation/pages/password_recovery_page.dart';
import '../../features/auth/presentation/pages/role_login_page.dart';
import '../../features/customer/presentation/pages/customer_home_page.dart';
import '../../features/customer/presentation/pages/customer_cart_page.dart';
import '../../features/customer/presentation/pages/customer_checkout_page.dart';
import '../../features/customer/presentation/pages/customer_ai_chat_page.dart';
import '../../features/customer/presentation/pages/customer_notifications_page.dart';
import '../../features/customer/presentation/pages/customer_orders_page.dart';
import '../../features/customer/presentation/pages/customer_order_detail_page.dart';
import '../../features/customer/presentation/pages/customer_profile_page.dart';
import '../../features/customer/presentation/pages/restaurant_browse_page.dart';
import '../../features/customer/presentation/pages/restaurant_detail_page.dart';
import '../../features/merchant/presentation/pages/merchant_home_page.dart';
import '../../features/merchant/presentation/pages/merchant_catalog_page.dart';
import '../../features/merchant/presentation/pages/merchant_restaurant_page.dart';
import '../../features/merchant/presentation/pages/merchant_orders_page.dart';
import '../../features/merchant/presentation/pages/merchant_reviews_page.dart';
import '../../features/merchant/presentation/pages/merchant_revenue_page.dart';
import '../../features/merchant/presentation/pages/merchant_notifications_page.dart';
import '../auth/session_cubit.dart';
import '../auth/user_role.dart';
import 'go_router_refresh_stream.dart';

class AppRouter {
  AppRouter(this._sessionCubit);

  final SessionCubit _sessionCubit;

  late final GoRouter router = GoRouter(
    initialLocation: '/auth/loading',
    refreshListenable: GoRouterRefreshStream(_sessionCubit.stream),
    redirect: _redirect,
    routes: [
      GoRoute(
        path: '/auth/loading',
        name: 'auth-loading',
        builder: (context, state) => const AuthLoadingPage(),
      ),
      GoRoute(
        path: '/login',
        name: 'login',
        builder: (context, state) => const RoleLoginPage(),
      ),
      GoRoute(
        path: '/forgot-password',
        name: 'forgot-password',
        builder: (context, state) => const PasswordRecoveryPage(),
      ),
      GoRoute(
        path: '/customer/home',
        name: 'customer-home',
        builder: (context, state) => const CustomerHomePage(),
      ),
      GoRoute(
        path: '/customer/restaurants',
        name: 'customer-restaurant-browse',
        builder: (context, state) => const RestaurantBrowsePage(),
      ),
      GoRoute(
        path: '/customer/restaurants/:id',
        name: 'customer-restaurant-detail',
        builder:
            (context, state) => RestaurantDetailPage(
              restaurantId: state.pathParameters['id'] ?? '',
            ),
      ),
      GoRoute(
        path: '/customer/cart',
        name: 'customer-cart',
        builder: (context, state) => const CustomerCartPage(),
      ),
      GoRoute(
        path: '/customer/checkout',
        name: 'customer-checkout',
        builder: (context, state) => const CustomerCheckoutPage(),
      ),
      GoRoute(
        path: '/customer/orders',
        name: 'customer-orders',
        builder: (context, state) => const CustomerOrdersPage(),
      ),
      GoRoute(
        path: '/customer/orders/:id',
        name: 'customer-order-detail',
        builder:
            (context, state) => CustomerOrderDetailPage(
              orderId: state.pathParameters['id'] ?? '',
            ),
      ),
      GoRoute(
        path: '/customer/profile',
        name: 'customer-profile',
        builder: (context, state) => const CustomerProfilePage(),
      ),
      GoRoute(
        path: '/customer/notifications',
        name: 'customer-notifications',
        builder: (context, state) => const CustomerNotificationsPage(),
      ),
      GoRoute(
        path: '/customer/ai',
        name: 'customer-ai',
        builder: (context, state) => const CustomerAiChatPage(),
      ),
      GoRoute(
        path: '/merchant/home',
        name: 'merchant-home',
        builder: (context, state) => const MerchantHomePage(),
      ),
      GoRoute(
        path: '/merchant/restaurant',
        name: 'merchant-restaurant',
        builder: (context, state) => const MerchantRestaurantPage(),
      ),
      GoRoute(
        path: '/merchant/catalog',
        name: 'merchant-catalog',
        builder: (context, state) => const MerchantCatalogPage(),
      ),
      GoRoute(
        path: '/merchant/orders',
        name: 'merchant-orders',
        builder: (context, state) => const MerchantOrdersPage(),
      ),
      GoRoute(
        path: '/merchant/reviews',
        name: 'merchant-reviews',
        builder: (context, state) => const MerchantReviewsPage(),
      ),
      GoRoute(
        path: '/merchant/revenue',
        name: 'merchant-revenue',
        builder: (context, state) => const MerchantRevenuePage(),
      ),
      GoRoute(
        path: '/merchant/notifications',
        name: 'merchant-notifications',
        builder: (context, state) => const MerchantNotificationsPage(),
      ),
    ],
  );

  String? _redirect(BuildContext context, GoRouterState state) {
    final session = _sessionCubit.state;
    final location = state.matchedLocation;
    final isLoading = location == '/auth/loading';
    final isLogin = location == '/login';

    if (session.isChecking) {
      return isLoading ? null : '/auth/loading';
    }

    if (!session.isAuthenticated) {
      final isPasswordRecovery = location == '/forgot-password';
      return isLogin || isPasswordRecovery ? null : '/login';
    }

    final role = session.role;
    if (role == null) {
      return '/login';
    }

    if (isLoading || isLogin || location == '/') {
      return role.homePath;
    }

    if (role == UserRole.customer && location.startsWith('/merchant/')) {
      return UserRole.customer.homePath;
    }

    if (role == UserRole.merchant && location.startsWith('/customer/')) {
      return UserRole.merchant.homePath;
    }

    return null;
  }
}
