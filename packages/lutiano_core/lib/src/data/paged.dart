/// One page of a list endpoint (`{items, page, size, total, pages, summary}`) with its rows already parsed.
/// [summary] carries totals computed over every match, not just this page.
class Paged<T> {
  const Paged(
      {required this.items,
      required this.page,
      required this.pages,
      required this.total,
      this.summary = const {}});

  factory Paged.fromJson(
          Map<String, dynamic> j, T Function(Map<String, dynamic>) parse) =>
      Paged(
        items: [
          for (final r in (j['items'] as List? ?? const []))
            parse((r as Map).cast<String, dynamic>())
        ],
        page: (j['page'] as num? ?? 1).toInt(),
        pages: (j['pages'] as num? ?? 1).toInt(),
        total: (j['total'] as num? ?? 0).toInt(),
        summary: (j['summary'] as Map?)?.cast<String, dynamic>() ?? const {},
      );

  final List<T> items;
  final int page, pages, total;
  final Map<String, dynamic> summary;

  bool get hasMore => page < pages;
}
