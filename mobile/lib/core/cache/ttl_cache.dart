/// A small in-memory cache that keeps each entry for a fixed [Duration]
/// before re-running its loader. Generalizes the bespoke session-cache
/// pattern that `CustomerHomeCubit` used to hand-roll, so any singleton
/// repository can transparently avoid redundant network calls across
/// page/cubit recreations.
class TtlCache<K, V> {
  final Map<K, _CacheEntry<V>> _entries = {};

  /// Returns the cached value for [key] if it exists and is younger than
  /// [ttl]; otherwise awaits [loader], stores the result, and returns it.
  ///
  /// Pass [forceRefresh] to bypass and overwrite any cached entry.
  Future<V> get(
    K key, {
    required Future<V> Function() loader,
    required Duration ttl,
    bool forceRefresh = false,
  }) async {
    if (!forceRefresh) {
      final entry = _entries[key];
      if (entry != null && !entry.isExpired(ttl)) {
        return entry.value;
      }
    }

    final value = await loader();
    _entries[key] = _CacheEntry(value);
    return value;
  }

  /// Removes the cached entry for [key], if any.
  void invalidate(K key) {
    _entries.remove(key);
  }

  /// Removes every cached entry.
  void clear() {
    _entries.clear();
  }
}

class _CacheEntry<V> {
  _CacheEntry(this.value) : fetchedAt = DateTime.now();

  final V value;
  final DateTime fetchedAt;

  bool isExpired(Duration ttl) => DateTime.now().difference(fetchedAt) > ttl;
}
