import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/formatters/vnd_currency_formatter.dart';

void main() {
  group('formatVndCurrency', () {
    test('formats whole numbers with dot separators and dong suffix', () {
      expect(formatVndCurrency(123456789), '123.456.789 đ');
    });

    test('rounds decimal values before formatting', () {
      expect(formatVndCurrency(1234.5), '1.235 đ');
      expect(formatVndCurrency(1234.4), '1.234 đ');
    });

    test('preserves negative values', () {
      expect(formatVndCurrency(-1234.5), '-1.235 đ');
    });
  });
}
