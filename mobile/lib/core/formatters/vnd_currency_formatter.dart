class VndCurrencyFormatter {
  const VndCurrencyFormatter._();

  static String format(num value) {
    final rounded = value.round();
    final isNegative = rounded < 0;
    final digits = rounded.abs().toString();
    final groups = <String>[];

    for (var end = digits.length; end > 0; end -= 3) {
      final start = end - 3 < 0 ? 0 : end - 3;
      groups.insert(0, digits.substring(start, end));
    }

    final sign = isNegative ? '-' : '';
    return '$sign${groups.join('.')} đ';
  }
}

String formatVndCurrency(num value) => VndCurrencyFormatter.format(value);
