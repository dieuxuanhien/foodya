import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../auth/user_role.dart';

class FoodyaRoleShell extends StatelessWidget {
  const FoodyaRoleShell({
    super.key,
    required this.role,
    required this.location,
    required this.child,
  });

  final UserRole role;
  final String location;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final destinations = _destinationsFor(role);
    final selectedIndex = _selectedIndex(destinations, location);

    return Scaffold(
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: selectedIndex,
        onDestinationSelected: (index) {
          final path = destinations[index].path;
          if (path != location) {
            context.go(path);
          }
        },
        destinations: destinations
            .map(
              (item) => NavigationDestination(
                icon: Icon(item.icon),
                selectedIcon: Icon(item.selectedIcon),
                label: item.label,
              ),
            )
            .toList(growable: false),
      ),
    );
  }

  int _selectedIndex(List<_ShellDestination> destinations, String location) {
    final exact = destinations.indexWhere((item) => item.path == location);
    if (exact >= 0) {
      return exact;
    }
    final nested = destinations.indexWhere((item) {
      final base = item.path.endsWith('/') ? item.path : '${item.path}/';
      return location.startsWith(base);
    });
    return nested >= 0 ? nested : 0;
  }

  List<_ShellDestination> _destinationsFor(UserRole role) {
    return switch (role) {
      UserRole.customer => const [
        _ShellDestination(
          path: '/customer/home',
          label: 'Home',
          icon: Icons.home_outlined,
          selectedIcon: Icons.home,
        ),
        _ShellDestination(
          path: '/customer/restaurants',
          label: 'Browse',
          icon: Icons.search_outlined,
          selectedIcon: Icons.search,
        ),
        _ShellDestination(
          path: '/customer/cart',
          label: 'Cart',
          icon: Icons.shopping_bag_outlined,
          selectedIcon: Icons.shopping_bag,
        ),
        _ShellDestination(
          path: '/customer/orders',
          label: 'Orders',
          icon: Icons.receipt_long_outlined,
          selectedIcon: Icons.receipt_long,
        ),
        _ShellDestination(
          path: '/customer/profile',
          label: 'Account',
          icon: Icons.person_outline,
          selectedIcon: Icons.person,
        ),
      ],
      UserRole.merchant => const [
        _ShellDestination(
          path: '/merchant/home',
          label: 'Dashboard',
          icon: Icons.dashboard_outlined,
          selectedIcon: Icons.dashboard,
        ),
        _ShellDestination(
          path: '/merchant/orders',
          label: 'Orders',
          icon: Icons.receipt_long_outlined,
          selectedIcon: Icons.receipt_long,
        ),
        _ShellDestination(
          path: '/merchant/catalog',
          label: 'Catalog',
          icon: Icons.restaurant_menu_outlined,
          selectedIcon: Icons.restaurant_menu,
        ),
        _ShellDestination(
          path: '/merchant/revenue',
          label: 'Insights',
          icon: Icons.bar_chart_outlined,
          selectedIcon: Icons.bar_chart,
        ),
        _ShellDestination(
          path: '/merchant/profile',
          label: 'Account',
          icon: Icons.person_outline,
          selectedIcon: Icons.person,
        ),
      ],
    };
  }
}

class _ShellDestination {
  const _ShellDestination({
    required this.path,
    required this.label,
    required this.icon,
    required this.selectedIcon,
  });

  final String path;
  final String label;
  final IconData icon;
  final IconData selectedIcon;
}
