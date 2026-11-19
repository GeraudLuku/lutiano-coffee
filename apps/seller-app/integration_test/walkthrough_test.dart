import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:lutiano_seller/app.dart';
import 'package:lutiano_seller/state/app_state.dart';

/// Runs the real app against a running local API (seeded seller). Read-only: it signs in and opens each main tab.
///   flutter test integration_test -d DEVICE --dart-define=LUTIANO_API=http://localhost:8080/api/v1
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('seller signs in and every main tab loads', (tester) async {
    await tester.pumpWidget(SellerApp(state: AppState()));
    // forgot password opens and goes back
    await tester.tap(find.text('Forgot password?'));
    await tester.pumpAndSettle();
    expect(find.text('Send Code'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await tester.enterText(
        find.byType(EditableText).at(0), 'seller@lutiano.local');
    await tester.enterText(find.byType(EditableText).at(1), 'Coffee123!');
    await tester.tap(find.text('Sign In'));
    await tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));

    expect(find.text('New Sale'), findsOneWidget,
        reason: 'Home should show after sign-in');
    expect(find.text('My Day'), findsOneWidget,
        reason: "the seller's own summary should load from /me/summary");
    expect(find.text('Orders'), findsOneWidget);

    // Notifications: the bell opens the center, its menu leads to categories, history and preferences
    Future<void> settleHome() => tester.pumpAndSettle(
        const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate,
        const Duration(seconds: 30));
    await tester.tap(find.byIcon(Icons.notifications_none).first);
    await settleHome();
    expect(find.text('Notifications'), findsWidgets);
    expect(find.text('Retry'), findsNothing, reason: 'the center failed');
    for (final entry in [
      'Notification Categories',
      'Notification History',
      'Notification Preferences'
    ]) {
      await tester.tap(find.byType(PopupMenuButton<String>));
      await settleHome();
      await tester.tap(find.text(
          entry == 'Notification Preferences' ? 'Manage preferences' : entry));
      await settleHome();
      expect(find.text(entry), findsOneWidget);
      expect(find.text('Retry'), findsNothing, reason: '$entry failed');
      await tester.pageBack();
      await settleHome();
    }
    await tester.pageBack();
    await settleHome();

    // Profile: edit, security with login activity, settings, help and my support requests
    await tester.tap(find.text('Profile').last);
    await settleHome();
    await tester.tap(find.byTooltip('Edit Profile'));
    await settleHome();
    expect(find.text('Full name'), findsOneWidget);
    await tester.pageBack();
    await settleHome();
    Future<void> profileEntry(String label, String expected) async {
      await tester.scrollUntilVisible(find.text(label).first, 300,
          scrollable: find.byType(Scrollable).last);
      await tester.tap(find.text(label).first);
      await settleHome();
      expect(find.text(expected), findsWidgets);
      expect(find.text('Retry'), findsNothing, reason: '$label failed');
    }

    await profileEntry('Security', 'Login Activity');
    await tester.tap(find.text('Login Activity'));
    await settleHome();
    expect(find.text('Signed in'), findsWidgets);
    await tester.pageBack();
    await settleHome();
    await tester.pageBack();
    await settleHome();
    await profileEntry('App Settings', 'Language');
    await tester.pageBack();
    await settleHome();
    await profileEntry('Help & Support', 'Frequently Asked Questions');
    await tester.tap(find.text('My support requests'));
    await settleHome();
    expect(find.text('Retry'), findsNothing, reason: 'support requests failed');
    await tester.pageBack();
    await settleHome();
    await tester.pageBack();
    await settleHome();
    await tester.tap(find.text('Home').last);
    await settleHome();
    for (final tab in ['Sales', 'Inventory', 'Profile', 'Home']) {
      await tester.tap(find.text(tab).last);
      await tester.pumpAndSettle(const Duration(seconds: 1),
          EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
      expect(find.text('Retry'), findsNothing, reason: '$tab failed to load');
      if (tab == 'Sales') {
        for (final period in ['Today', 'This Week', 'This Month']) {
          await tester.tap(find.text(period));
          await tester.pumpAndSettle(const Duration(seconds: 1),
              EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
          expect(find.text('Retry'), findsNothing,
              reason: 'history for $period failed to load');
        }
      }
    }

    // Inventory: the period summary, the movements of a period, and a product's figures
    Future<void> settle() => tester.pumpAndSettle(const Duration(seconds: 1),
        EnginePhase.sendSemanticsUpdate, const Duration(seconds: 30));
    await tester.tap(find.text('Inventory').last);
    await settle();
    await tester.tap(find.text('Inventory summary'));
    await settle();
    expect(find.text('Stock in (received)'), findsOneWidget);
    expect(find.text('TOP CONSUMED PRODUCTS'), findsOneWidget);
    expect(find.text('Retry'), findsNothing, reason: 'the summary failed');
    await tester.pageBack();
    await settle();
    await tester.tap(find.text('Stock Movements'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'movements failed');
    await tester.tap(find.text('This Week'));
    await settle();
    expect(find.text('Retry'), findsNothing,
        reason: 'a week of movements failed');
    await tester.pageBack();
    await settle();
    await tester.tap(find.text('View Inventory'));
    await settle();
    await tester.tap(find.byIcon(Icons.chevron_right).first);
    await settle();
    expect(find.text('Total received'), findsOneWidget);
    expect(find.text('Consumed today'), findsOneWidget);
    expect(find.text('Retry'), findsNothing, reason: 'product figures failed');

    // Stock requests: the overview and the list load (a fixed point of sale sees the status but has no request button)
    await tester.pageBack(); // product -> stock list
    await settle();
    await tester.pageBack(); // stock list -> inventory
    await settle();
    await tester.tap(find.text('Stock Requests'));
    await settle();
    expect(find.text('Total requests'), findsOneWidget);
    expect(find.text('Request Stock'), findsNothing,
        reason: 'the demo seller is a fixed point of sale');
    await tester.tap(find.text('My Requests'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'my requests failed');

    // Cash: the screen with what needs attention, the lists across shifts, and a past shift's reconciliation
    await tester.pageBack(); // my requests -> stock requests
    await settle();
    await tester.pageBack(); // stock requests -> inventory
    await settle();
    await tester.tap(find.text('Home').last);
    await settle();
    await tester.tap(find.textContaining('Cash shift'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'the cash screen failed');
    await tester.scrollUntilVisible(find.text('All remittances'), 300);
    await tester.tap(find.text('All remittances'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'remittances failed');
    await tester.pageBack();
    await settle();
    await tester.scrollUntilVisible(find.text('All outflow requests'), 300);
    await tester.tap(find.text('All outflow requests'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'outflow requests failed');
    await tester.pageBack();
    await settle();
    await tester.scrollUntilVisible(find.text('All shifts'), 300);
    await tester.tap(find.text('All shifts'));
    await settle();
    await tester.tap(find.byIcon(Icons.chevron_right).first);
    await settle();
    expect(find.text('Shift details'), findsOneWidget);
    expect(find.text('RECONCILIATION'), findsOneWidget);
    expect(find.text('Retry'), findsNothing, reason: 'shift details failed');

    // Earnings: the overview and every screen behind it load for the demo seller
    await tester.pageBack(); // shift details -> all shifts
    await settle();
    await tester.pageBack(); // all shifts -> cash
    await settle();
    await tester.pageBack(); // cash -> shell
    await settle();
    await tester.tap(find.text('Earnings').last);
    await settle();
    expect(find.text('Total earnings'), findsOneWidget);
    expect(find.text('Retry'), findsNothing, reason: 'the overview failed');
    Future<void> openAndBack(String label, {String? expectText}) async {
      await tester.scrollUntilVisible(find.text(label), 300,
          scrollable: find.byType(Scrollable).last);
      await tester.tap(find.text(label));
      await settle();
      if (expectText != null) expect(find.text(expectText), findsWidgets);
      expect(find.text('Retry'), findsNothing, reason: '$label failed');
      await tester.pageBack();
      await settle();
    }

    await tester.tap(find.text('View Breakdown'));
    await settle();
    expect(find.text('Total commissions'), findsOneWidget);
    expect(find.text('Retry'), findsNothing, reason: 'the breakdown failed');
    await tester.pageBack();
    await settle();
    await openAndBack('Earnings by Period');
    await openAndBack('Sales that earned commission');
    await openAndBack('Payouts & Payment History');
    await openAndBack('Earnings by Operation');
    await openAndBack('Trends & Analytics');
    await openAndBack('Commission Rules');
    await openAndBack('Earnings Statement',
        expectText: 'Export Statement (PDF)');

    // Targets and bonuses: the overview, its lists, and one target's screens
    await tester.scrollUntilVisible(find.text('Targets & Bonuses'), 300,
        scrollable: find.byType(Scrollable).last);
    await tester.tap(find.text('Targets & Bonuses'));
    await settle();
    expect(find.text('Active targets'), findsOneWidget);
    expect(find.text('Retry'), findsNothing,
        reason: 'the targets overview failed');
    await openAndBack('My Targets');
    await openAndBack('My Bonuses');
    await openAndBack('Performance Summary');
    await openAndBack('Target Progress Trend');
    await openAndBack('Target Rules & Bonus Policy');
    await openAndBack('Sales Campaigns');
    await tester.tap(find.text('View target details'));
    await settle();
    expect(find.text('Target Details'), findsOneWidget);
    await tester.tap(find.text('Bonus Eligibility'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'eligibility failed');
    await tester.pageBack();
    await settle();
    await tester.tap(find.text('Bonus Details'));
    await settle();
    expect(find.text('Retry'), findsNothing, reason: 'bonus details failed');
  });
}
