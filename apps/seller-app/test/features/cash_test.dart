import 'package:flutter_test/flutter_test.dart';
import 'package:lutiano_seller/data/models.dart';
import 'package:lutiano_seller/features/shift/cash_shift_screen.dart';
import 'package:lutiano_seller/features/shift/shift_detail_screen.dart';
import 'package:lutiano_seller/features/shift/shift_dialogs.dart';
import 'package:lutiano_seller/features/shift/shift_lists.dart';
import 'package:lutiano_seller/state/app_scope.dart';
import 'package:lutiano_seller/state/app_state.dart';
import 'package:material_ui/material_ui.dart';

import '../support/fake_api.dart';
import '../support/harness.dart';
import '../support/seller_fakes.dart';

const _overview = {
  'pending_outflows': 1,
  'outstanding_remittances': 2,
  'outstanding_remittances_xaf': 30000,
  'disputed_remittances': 1,
  'recounts_requested': 0,
};

Map<String, Object?> _page(List<Object?> items) => {
      'items': items,
      'page': 1,
      'size': 20,
      'total': items.length,
      'pages': 1,
      'summary': {}
    };

Map<String, Object?> _closedShift({String review = 'RESOLVED'}) => {
      'id': 'sh9',
      'status': 'CLOSED',
      'opened_at': '2026-11-16T07:00:00Z',
      'closed_at': '2026-11-16T18:00:00Z',
      'opening_float': 10000,
      'cash_sales_xaf': 50000,
      'refunds_paid_xaf': 2000,
      'outflows_xaf': 5000,
      'remitted_xaf': 40000,
      'expected_cash': 13000,
      'counted_cash': 12000,
      'sale_count': 9,
      'variance_reason': 'A customer short-changed me',
      'review_status': review,
      'review_note': 'Accepted after checking the drawer cameras.',
      'reviewed_by_name': 'Joel Tchami',
      'reviewed_at': '2026-11-17T08:00:00Z',
      'counts': [
        {
          'kind': 'INITIAL',
          'expected_xaf': 13000,
          'counted_xaf': 12000,
          'reason': 'A customer short-changed me',
          'counted_by_name': 'Sandra Eto',
          'created_at': '2026-11-16T18:00:00Z'
        },
        {
          'kind': 'RECOUNT',
          'expected_xaf': 13000,
          'counted_xaf': 12000,
          'counted_by_name': 'Sandra Eto',
          'created_at': '2026-11-17T09:00:00Z'
        },
      ],
      'outflows': [
        {
          'id': 'o1',
          'reference': 'CO-1',
          'amount_xaf': 5000,
          'category': 'SUPPLIES',
          'description': 'Milk',
          'status': 'APPROVED',
          'requested_at': '2026-11-16T10:00:00Z'
        }
      ],
      'remittances': [
        {
          'id': 'r1',
          'reference': 'RM-1',
          'amount_xaf': 40000,
          'method': 'BANK_DEPOSIT',
          'bank_reference': 'SLIP-9',
          'status': 'DISPUTED',
          'received_xaf': 38000,
          'acknowledgement_note': 'Two notes were torn.',
          'recorded_at': '2026-11-16T16:00:00Z'
        }
      ],
    };

AppState _state(FakeApi api) => AppState(api: api.client())
  ..user = UserProfile(
      id: 'u1',
      email: 's@x',
      fullName: 'Sandra Eto',
      role: 'SELLER',
      sellerRole: 'MOBILE_BARISTA',
      operations: [operation])
  ..selectOperation(operation);

Widget _host(AppState state, Widget home) =>
    AppScope(state: state, child: testApp(home: home));

void main() {
  testWidgets(
      'the cash screen says what needs attention across shifts and opens the list behind each line',
      (tester) async {
    final api = FakeApi({
      'GET /shifts/open': (_) => (200, []),
      'GET /cash/shifts': (_) => (200, _page([])),
      'GET /cash/overview': (_) => (200, _overview),
      'GET /cash/remittances': (_) => (200, _page([])),
    });
    await tester.pumpWidget(_host(_state(api), const CashShiftScreen()));
    await tester.pumpAndSettle();
    expect(find.text('NEEDS ATTENTION'), findsOneWidget);
    expect(find.text('1 remittance disputed'), findsOneWidget);
    expect(find.textContaining('2 remittances awaiting confirmation'),
        findsOneWidget);
    expect(find.text('1 outflow request awaiting approval'), findsOneWidget);

    await tester.tap(find.text('1 remittance disputed'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['status'], 'DISPUTED');
  });

  testWidgets(
      'a closed shift shows how it reconciled: figures, variance and why, the review, every count, and its cash movements',
      (tester) async {
    final api = FakeApi({
      'GET /cash/shifts/sh9': (_) => (200, _closedShift()),
    });
    await tester.pumpWidget(
        _host(_state(api), const ShiftDetailScreen(shiftId: 'sh9')));
    await tester.pumpAndSettle();
    expect(find.text('13,000 XAF'), findsWidgets, reason: 'expected cash');
    expect(find.text('12,000 XAF'), findsWidgets, reason: 'counted cash');
    expect(find.text('-1,000 XAF'), findsWidgets, reason: 'the variance');
    expect(find.textContaining('short-changed'), findsWidgets);
    await tester.scrollUntilVisible(find.text('COUNT HISTORY'), 300);
    expect(find.text('Closing count'), findsOneWidget);
    expect(find.text('Recount'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('CASH MOVEMENTS'), 300);
    expect(find.textContaining('Two notes were torn.'), findsOneWidget,
        reason: 'what the recipient said about a disputed remittance');
    await tester.scrollUntilVisible(find.textContaining('Joel Tchami'), -300);
    expect(find.textContaining('Accepted after checking'), findsOneWidget);
  });

  testWidgets('remittances can be filtered by their acknowledgement status',
      (tester) async {
    final api = FakeApi({
      'GET /cash/remittances': (_) => (
            200,
            _page([
              {
                'id': 'r1',
                'reference': 'RM-1',
                'amount_xaf': 20000,
                'method': 'HANDOVER',
                'recipient_name': 'Joel Tchami',
                'status': 'OUTSTANDING',
                'recorded_at': '2026-11-17T10:00:00Z'
              }
            ])
          ),
    });
    await tester.pumpWidget(_host(_state(api), const RemittancesScreen()));
    await tester.pumpAndSettle();
    expect(find.textContaining('RM-1'), findsOneWidget);
    await tester.tap(find.text('Disputed'));
    await tester.pumpAndSettle();
    expect(api.uris.last.queryParameters['status'], 'DISPUTED');
  });

  testWidgets(
      'closing the shift reminds about waiting outflows and unconfirmed handovers',
      (tester) async {
    final api = FakeApi({'GET /cash/overview': (_) => (200, _overview)});
    final state = _state(api)..shift = CashShift.fromJson(openShiftJson);
    await tester.pumpWidget(_host(
        state,
        Builder(
            builder: (context) => TextButton(
                onPressed: () => showCloseShiftDialog(context),
                child: const Text('go')))));
    await tester.tap(find.text('go'));
    await tester.pumpAndSettle();
    expect(find.textContaining('1 outflow request is still waiting'),
        findsOneWidget);
    expect(find.textContaining('2 remittances (30,000 XAF) not yet confirmed'),
        findsOneWidget);
  });
}
