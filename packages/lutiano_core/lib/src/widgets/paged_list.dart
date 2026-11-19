import 'package:material_ui/material_ui.dart';

import '../core/api_client.dart';
import '../data/paged.dart';
import 'feedback.dart';
import 'skeleton.dart';

/// Loads a list one page at a time: the first page now, the next ones as the person scrolls.
/// A newer request (refresh) always wins over an older one still in flight.
class PagedController<T> extends ChangeNotifier {
  PagedController({required this.fetch, this.pageSize = 20});

  final Future<Paged<T>> Function(int page, int size) fetch;
  final int pageSize;

  final List<T> items = [];
  Paged<T>? last;
  Object? error, moreError;
  bool loadingFirst = false, loadingMore = false;
  int _generation = 0;
  bool _disposed = false;

  bool get hasMore => last?.hasMore ?? false;
  int get total => last?.total ?? 0;
  Map<String, dynamic> get summary => last?.summary ?? const {};

  /// Starts again from page 1 (first load, pull to refresh, retry after an error).
  Future<void> refresh() async {
    final generation = ++_generation;
    loadingFirst = true;
    error = null;
    moreError = null;
    notifyListeners();
    try {
      final page = await fetch(1, pageSize);
      if (_disposed || generation != _generation) return;
      items
        ..clear()
        ..addAll(page.items);
      last = page;
    } catch (e) {
      if (_disposed || generation != _generation) return;
      error = e;
    }
    loadingFirst = false;
    notifyListeners();
  }

  /// Fetches the next page, if there is one and none is already on its way.
  Future<void> loadMore() async {
    if (_disposed || loadingFirst || loadingMore || !hasMore) return;
    final generation = _generation;
    loadingMore = true;
    moreError = null;
    notifyListeners();
    try {
      final page = await fetch(last!.page + 1, pageSize);
      if (_disposed || generation != _generation) return;
      items.addAll(page.items);
      last = page;
    } catch (e) {
      if (_disposed || generation != _generation) return;
      moreError = e;
    }
    loadingMore = false;
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}

/// A list that loads lazily: skeleton rows on the first load, more rows as the end comes into view,
/// pull to refresh, an empty state, and an error with Retry (for the first page or for a later one).
///
/// Give it a `key` that changes with the filters, so a different filter starts a fresh list.
class PagedListView<T> extends StatefulWidget {
  const PagedListView({
    super.key,
    required this.fetch,
    required this.itemBuilder,
    required this.empty,
    this.header,
    this.skeleton = const SkeletonCard(),
    this.pageSize = 20,
    this.padding = const EdgeInsets.all(16),
    this.onPage,
    this.columns = 1,
    this.childAspectRatio = 1,
  });

  final Future<Paged<T>> Function(int page, int size) fetch;

  /// Builds one row. [items] is everything loaded so far, so a row can look at its neighbours (to show a day heading, say).
  final Widget Function(BuildContext context, T item, int index, List<T> items)
      itemBuilder;
  final Widget empty;

  /// Shown above the rows (summary cards, filters). It can read the totals once the first page is in.
  final Widget Function(BuildContext context, PagedController<T> controller)?
      header;
  final Widget skeleton;
  final int pageSize;
  final EdgeInsets padding;

  /// Called with every page as it arrives (to read its summary, for example the categories).
  final void Function(Paged<T> page)? onPage;

  /// More than one column shows the rows as a grid ([childAspectRatio] is width over height of a cell).
  final int columns;
  final double childAspectRatio;

  @override
  State<PagedListView<T>> createState() => _PagedListViewState<T>();
}

class _PagedListViewState<T> extends State<PagedListView<T>> {
  static const _threshold = 400.0;

  late final PagedController<T> _controller =
      PagedController<T>(fetch: _fetch, pageSize: widget.pageSize);

  Future<Paged<T>> _fetch(int page, int size) async {
    final result = await widget.fetch(page, size);
    widget.onPage?.call(result);
    return result;
  }

  @override
  void initState() {
    super.initState();
    _controller.refresh();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
      listenable: _controller, builder: (context, _) => _content(context));

  Widget _content(BuildContext context) {
    final c = _controller;
    final header = widget.header?.call(context, c);
    if (c.loadingFirst && c.items.isEmpty) {
      return SkeletonScope(
        child: ListView(
          physics: const NeverScrollableScrollPhysics(),
          padding: widget.padding,
          children: [
            if (header != null) header,
            if (widget.columns > 1)
              GridView.count(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisCount: widget.columns,
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: widget.childAspectRatio,
                children: [
                  for (var i = 0; i < widget.columns * 3; i++) widget.skeleton
                ],
              )
            else
              for (var i = 0; i < 6; i++) widget.skeleton,
          ],
        ),
      );
    }
    if (c.error != null && c.items.isEmpty) {
      return ListView(padding: widget.padding, children: [
        if (header != null) header,
        ErrorBanner(errorMessage(c.error!), onRetry: c.refresh)
      ]);
    }
    final showFooter = c.hasMore || c.moreError != null;
    final grid = widget.columns > 1;
    final side = widget.padding;
    final cell = SliverChildBuilderDelegate(
      (context, index) =>
          widget.itemBuilder(context, c.items[index], index, c.items),
      childCount: c.items.length,
    );
    return RefreshIndicator(
      onRefresh: c.refresh,
      child: NotificationListener<Notification>(
        onNotification: _nearEndHandler,
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverPadding(
              padding: EdgeInsets.fromLTRB(side.left, side.top, side.right, 0),
              sliver: SliverMainAxisGroup(slivers: [
                if (header != null) SliverToBoxAdapter(child: header),
                if (c.items.isEmpty) SliverToBoxAdapter(child: widget.empty),
              ]),
            ),
            if (c.items.isNotEmpty)
              SliverPadding(
                padding: EdgeInsets.symmetric(horizontal: side.left),
                sliver: grid
                    ? SliverGrid(
                        delegate: cell,
                        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                            crossAxisCount: widget.columns,
                            mainAxisSpacing: 12,
                            crossAxisSpacing: 12,
                            childAspectRatio: widget.childAspectRatio),
                      )
                    : SliverList(delegate: cell),
              ),
            if (showFooter)
              SliverPadding(
                padding: EdgeInsets.symmetric(horizontal: side.left),
                sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                        (context, _) => _footer(c),
                        childCount: 1)),
              ),
            SliverToBoxAdapter(child: SizedBox(height: side.bottom)),
          ],
        ),
      ),
    );
  }

  /// Asks for the next page when the end is within [_threshold] pixels (also for a list too short to scroll).
  bool _nearEndHandler(Notification n) {
    final metrics = n is ScrollNotification
        ? n.metrics
        : n is ScrollMetricsNotification
            ? n.metrics
            : null;
    if (metrics != null &&
        metrics.axis == Axis.vertical &&
        metrics.extentAfter < _threshold &&
        _controller.hasMore &&
        _controller.moreError == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && _controller.moreError == null) _controller.loadMore();
      });
    }
    return false;
  }

  /// The end of the list: a retry after a failure, otherwise skeleton rows while the next page loads.
  Widget _footer(PagedController<T> c) {
    if (c.moreError != null) {
      return ErrorBanner(errorMessage(c.moreError!), onRetry: c.loadMore);
    }
    return SkeletonScope(
        animate: c.loadingMore,
        child: Column(children: [widget.skeleton, widget.skeleton]));
  }
}
