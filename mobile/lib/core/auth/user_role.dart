enum UserRole {
  customer,
  merchant,
  delivery;

  static const selfRegistrationRoles = [customer, merchant, delivery];

  String get apiValue {
    switch (this) {
      case UserRole.customer:
        return 'CUSTOMER';
      case UserRole.merchant:
        return 'MERCHANT';
      case UserRole.delivery:
        return 'DELIVERY';
    }
  }

  String get label {
    switch (this) {
      case UserRole.customer:
        return 'Customer';
      case UserRole.merchant:
        return 'Merchant';
      case UserRole.delivery:
        return 'Delivery';
    }
  }

  String get homePath {
    switch (this) {
      case UserRole.customer:
        return '/customer/home';
      case UserRole.merchant:
        return '/merchant/home';
      case UserRole.delivery:
        return '/delivery/home';
    }
  }

  String get routeNamespace => '/$name/';

  static UserRole? fromApiValue(String? value) {
    switch (value?.toUpperCase()) {
      case 'CUSTOMER':
        return UserRole.customer;
      case 'MERCHANT':
        return UserRole.merchant;
      case 'DELIVERY':
        return UserRole.delivery;
      default:
        return null;
    }
  }
}
