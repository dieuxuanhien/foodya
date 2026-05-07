class PagedResult<T> {
  const PagedResult({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<T> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  bool get hasNextPage => page + 1 < totalPages;
}
