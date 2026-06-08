import 'package:flutter_test/flutter_test.dart';
import 'package:foodya_mobile/core/cache/ttl_cache.dart';

void main() {
  group('TtlCache', () {
    test('returns the cached value without calling the loader again', () async {
      final cache = TtlCache<String, int>();
      var calls = 0;
      Future<int> loader() async {
        calls++;
        return 42;
      }

      final first = await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      final second = await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));

      expect(first, 42);
      expect(second, 42);
      expect(calls, 1);
    });

    test('reloads once the entry has expired', () async {
      final cache = TtlCache<String, int>();
      var calls = 0;
      Future<int> loader() async {
        calls++;
        return calls;
      }

      final first = await cache.get('a', loader: loader, ttl: Duration.zero);
      await Future<void>.delayed(const Duration(milliseconds: 5));
      final second = await cache.get('a', loader: loader, ttl: Duration.zero);

      expect(first, 1);
      expect(second, 2);
      expect(calls, 2);
    });

    test('forceRefresh bypasses and overwrites the cached entry', () async {
      final cache = TtlCache<String, int>();
      var calls = 0;
      Future<int> loader() async {
        calls++;
        return calls;
      }

      final first = await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      final second = await cache.get(
        'a',
        loader: loader,
        ttl: const Duration(minutes: 5),
        forceRefresh: true,
      );
      final third = await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));

      expect(first, 1);
      expect(second, 2);
      expect(third, 2);
      expect(calls, 2);
    });

    test('invalidate removes a single entry', () async {
      final cache = TtlCache<String, int>();
      var calls = 0;
      Future<int> loader() async {
        calls++;
        return calls;
      }

      await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      await cache.get('b', loader: loader, ttl: const Duration(minutes: 5));
      cache.invalidate('a');

      final reloadedA = await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      final cachedB = await cache.get('b', loader: loader, ttl: const Duration(minutes: 5));

      expect(reloadedA, 3);
      expect(cachedB, 2);
      expect(calls, 3);
    });

    test('clear removes every entry', () async {
      final cache = TtlCache<String, int>();
      var calls = 0;
      Future<int> loader() async {
        calls++;
        return calls;
      }

      await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      await cache.get('b', loader: loader, ttl: const Duration(minutes: 5));
      cache.clear();

      await cache.get('a', loader: loader, ttl: const Duration(minutes: 5));
      await cache.get('b', loader: loader, ttl: const Duration(minutes: 5));

      expect(calls, 4);
    });
  });
}
