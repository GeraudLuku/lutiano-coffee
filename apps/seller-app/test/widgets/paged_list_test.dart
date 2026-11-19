import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:lutiano_seller/widgets/widgets.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';

/// 45 items served 20 at a time (3 pages), with a summary on every page.
FakeApi _server({int total = 45, int? failPage}) {
  var failed = false;
  return FakeApi({
    'GET /items': (r) {
      final page = int.parse(r.url.queryParameters['page']!);
      final size = int.parse(r.url.queryParameters['size']!);
      if (page == failPage && !failed) {
        failed = true;
        return (500, {});
      }
      final from = (page - 1) * size;
      final rows = [
        for (var i = from; i < from + size && i < total; i++)
          {'name': 'Item ${i + 1}'}
      ];
      return (
        200,
        {
          'items': rows,
          'page': page,
          'size': size,
          'total': total,
          'pages': (total / size).ceil(),
          'summary': {'sum': 'S$total'}
        }
      );
    },
  });
}

Widget _list(FakeApi api, {Future<void>? gate}) {
  final client = api.client();
  return testApp(
    home: Scaffold(
      body: PagedListView<String>(
        fetch: (page, size) async {
          await gate;
          return Paged.fromJson(
              await client.page('/items', page: page, size: size),
              (j) => j['name'] as String);
        },
        header: (context, c) => Text(c.last == null
            ? 'header…'
            : 'total ${c.total} ${c.summary['sum']}'),
        empty: const Text('nothing here'),
        itemBuilder: (context, item, _, __) =>
            SizedBox(height: 60, child: Text(item)),
      ),
    ),
  );
}

/// The footer's skeleton pulses for as long as more pages exist, so the tree never "settles": let a few frames pass instead.
Future<void> _settle(WidgetTester tester) async {
  for (var i = 0; i < 6; i++) {
    await tester.pump(const Duration(milliseconds: 100));
  }
}

List<String?> _pages(FakeApi api) =>
    api.uris.map((u) => u.queryParameters['page']).toList();

void main() {
  testWidgets('shows skeleton rows while the first page loads, then the rows',
      (tester) async {
    final api = _server();
    final gate = Completer<void>();
    await tester.pumpWidget(_list(api, gate: gate.future));
    await tester.pump();
    expect(find.byType(SkeletonCard), findsWidgets);
    expect(find.text('header…'), findsOneWidget);
    gate.complete();
    await _settle(tester);
    expect(find.byType(SkeletonCard), findsNothing);
    expect(find.text('Item 1'), findsOneWidget);
    expect(find.text('total 45 S45'), findsOneWidget,
        reason: 'the header can read the totals of the whole list');
  });

  testWidgets('loads the next pages only as the end comes into view',
      (tester) async {
    final api = _server();
    await tester.pumpWidget(_list(api));
    await _settle(tester);
    expect(_pages(api), ['1'],
        reason: 'nothing more is asked for until the end is near');

    await tester.scrollUntilVisible(find.text('Item 45'), 300,
        scrollable: find.byType(Scrollable).first);
    await _settle(tester);
    expect(_pages(api), ['1', '2', '3']);
    expect(find.text('Item 45'), findsOneWidget);
    expect(find.byType(SkeletonCard), findsNothing,
        reason: 'no skeleton once the last page is in');
  });

  testWidgets('an empty list shows the empty state, not skeletons',
      (tester) async {
    await tester.pumpWidget(_list(_server(total: 0)));
    await _settle(tester);
    expect(find.text('nothing here'), findsOneWidget);
    expect(find.byType(SkeletonCard), findsNothing);
  });

  testWidgets('a failed first page shows the error with Retry, which loads it',
      (tester) async {
    await tester.pumpWidget(_list(_server(failPage: 1)));
    await _settle(tester);
    expect(find.text('Retry'), findsOneWidget);
    await tester.tap(find.text('Retry'));
    await _settle(tester);
    expect(find.text('Item 1'), findsOneWidget);
  });

  testWidgets(
      'a failed later page keeps what is shown and offers Retry at the end',
      (tester) async {
    final api = _server(failPage: 2);
    await tester.pumpWidget(_list(api));
    await _settle(tester);
    await tester.scrollUntilVisible(find.text('Retry'), 300,
        scrollable: find.byType(Scrollable).first);
    await _settle(tester);
    expect(_pages(api), ['1', '2'],
        reason: 'the first page stays, only page 2 failed');
    await tester.tap(find.text('Retry'));
    await _settle(tester);
    await tester.scrollUntilVisible(find.text('Item 45'), 300,
        scrollable: find.byType(Scrollable).first);
    expect(find.text('Item 45'), findsOneWidget);
  });

  testWidgets('pulling down starts again from page 1', (tester) async {
    final api = _server();
    await tester.pumpWidget(_list(api));
    await _settle(tester);
    await tester.fling(find.text('Item 1'), const Offset(0, 400), 1000);
    await _settle(tester);
    expect(_pages(api).where((p) => p == '1').length, 2);
  });

  test(
      'PagedController: a newer refresh wins over an older request still in flight',
      () async {
    final slow = Completer<void>();
    var call = 0;
    final c = PagedController<String>(fetch: (page, size) async {
      final mine = ++call;
      if (mine == 1) await slow.future;
      return Paged(items: ['call $mine'], page: 1, pages: 1, total: 1);
    });
    final first = c.refresh();
    await c.refresh();
    slow.complete();
    await first;
    expect(c.items, ['call 2']);
  });

  testWidgets('a short list can still be pulled down to refresh',
      (tester) async {
    var loads = 0;
    await tester.pumpWidget(testApp(
      home: Scaffold(
        body: AsyncView<int>(
          load: () async => ++loads,
          builder: (context, n, _) => ListView(children: [Text('loaded $n')]),
        ),
      ),
    ));
    await _settle(tester);
    expect(find.text('loaded 1'), findsOneWidget);
    await tester.fling(find.text('loaded 1'), const Offset(0, 300), 1000);
    await _settle(tester);
    expect(loads, 2, reason: 'one short row, yet the pull reloaded it');
    expect(find.text('loaded 2'), findsOneWidget);
  });
}
