import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../operation/operation_picker.dart';
import '../notifications/notifications_screen.dart';
import '../shift/cash_shift_screen.dart';
import '../shift/shift_dialogs.dart';
import '../../core/l10n.dart';

/// Greeting, role chip (the seller role for the current operation type), operation switcher and alerts bell.
class HomeHeader extends StatelessWidget {
  const HomeHeader({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final user = app.user!;
    final op = app.operation;
    return Row(children: [
      CircleAvatar(
        radius: 24,
        backgroundColor: AppColors.navy,
        child: Text(
            user.firstName.isEmpty ? '?' : user.firstName[0].toUpperCase(),
            style: const TextStyle(
                color: AppColors.gold,
                fontWeight: FontWeight.w800,
                fontSize: 20)),
      ),
      const SizedBox(width: 12),
      Expanded(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(greeting(context.l10n, DateTime.now()),
              style: const TextStyle(color: AppColors.muted)),
          Text(user.firstName,
              style:
                  const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
          if (op != null)
            InkWell(
              onTap: app.operations.length > 1
                  ? () => showOperationPicker(context)
                  : null,
              borderRadius: BorderRadius.circular(20),
              child: Container(
                margin: const EdgeInsets.only(top: 4),
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                decoration: BoxDecoration(
                    color: AppColors.gold,
                    borderRadius: BorderRadius.circular(20)),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  Text(op.type.sellerRole(context.l10n),
                      style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 12,
                          color: AppColors.navy)),
                  if (app.operations.length > 1)
                    const Icon(Icons.expand_more,
                        size: 16, color: AppColors.navy),
                ]),
              ),
            ),
        ]),
      ),
      IconButton(
        tooltip: context.l10n.notifications,
        onPressed: () => Navigator.of(context).push(
            MaterialPageRoute(builder: (_) => const NotificationsScreen())),
        icon: Badge(
            isLabelVisible: app.unreadNotifications > 0,
            label: Text('${app.unreadNotifications}'),
            child: const Icon(Icons.notifications_none,
                color: AppColors.navy, size: 28)),
      ),
    ]);
  }
}

/// Loads the signed-in seller's figures once for Home (`GET /me/summary`), for the "My Day" card and the 7-day trend.
/// If it fails, only the cards that need it show the error (with Retry); the rest of Home keeps working.
class SummaryHost extends StatefulWidget {
  const SummaryHost({super.key, required this.child});
  final Widget child;

  @override
  State<SummaryHost> createState() => _SummaryHostState();
}

class _SummaryHostState extends State<SummaryHost> {
  late Future<SellerSummary> _summary;

  @override
  void initState() {
    super.initState();
    _summary = _start();
  }

  /// Starts the request. The failure is handled by whoever builds from it (a card shows it with Retry);
  /// this empty handler only stops a failure from being reported as unhandled while no card is on screen yet.
  Future<SellerSummary> _start() {
    final request = AppScope.read(context).sales.summary();
    request.then((_) {}, onError: (_) {});
    return request;
  }

  void _retry() {
    final next = _start();
    setState(() {
      _summary = next;
    });
  }

  @override
  Widget build(BuildContext context) =>
      _SummaryScope(future: _summary, retry: _retry, child: widget.child);
}

class _SummaryScope extends InheritedWidget {
  const _SummaryScope(
      {required this.future, required this.retry, required super.child});
  final Future<SellerSummary> future;
  final VoidCallback retry;

  @override
  bool updateShouldNotify(_SummaryScope old) => future != old.future;
}

/// Builds from the seller's figures: [skeleton] while they load, an error with Retry if they fail.
class SummaryBuilder extends StatelessWidget {
  const SummaryBuilder(
      {super.key, required this.builder, required this.skeleton});
  final Widget Function(BuildContext context, SellerSummary summary) builder;
  final Widget skeleton;

  @override
  Widget build(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<_SummaryScope>()!;
    return FutureBuilder<SellerSummary>(
      future: scope.future,
      builder: (context, snap) {
        // after a retry the old error stays on the snapshot until the new answer arrives, so check the state first
        final loading = snap.connectionState != ConnectionState.done;
        if (!loading && snap.hasError) {
          return ErrorBanner(errorMessage(snap.error!), onRetry: scope.retry);
        }
        if (loading || !snap.hasData) return SkeletonScope(child: skeleton);
        return builder(context, snap.data!);
      },
    );
  }
}

/// The signed-in seller's own day: sales, orders, commission and loyalty points issued.
class TodayPerformanceCard extends StatelessWidget {
  const TodayPerformanceCard({super.key});

  @override
  Widget build(BuildContext context) => AppCard(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(
                child: Text(context.l10n.myDay,
                    style: const TextStyle(fontWeight: FontWeight.w800))),
            Text(dayMonthYear(DateTime.now()),
                style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
          ]),
          const SizedBox(height: 14),
          SummaryBuilder(
            skeleton: const Column(children: [
              SkeletonMetrics(count: 2),
              SizedBox(height: 14),
              SkeletonMetrics(count: 2)
            ]),
            builder: (context, summary) {
              final today = summary.today;
              return Column(children: [
                Row(children: [
                  Expanded(
                      child: MetricTile(
                          icon: Icons.payments_outlined,
                          label: context.l10n.todaySSales,
                          value: xaf(today.salesXaf))),
                  Expanded(
                      child: MetricTile(
                          icon: Icons.shopping_cart_outlined,
                          label: context.l10n.orders,
                          value: '${today.orders}',
                          iconColor: AppColors.purple,
                          iconBg: AppColors.purpleSoft)),
                ]),
                const SizedBox(height: 12),
                Row(children: [
                  Expanded(
                      child: MetricTile(
                          icon: Icons.savings_outlined,
                          label: context.l10n.myCommission,
                          value: xaf(today.commissionXaf),
                          iconColor: AppColors.amber,
                          iconBg: AppColors.amberSoft)),
                  Expanded(
                      child: MetricTile(
                          icon: Icons.star_outline,
                          label: context.l10n.pointsIssued,
                          value: context.l10n
                              .pointsValue(groupDigits(today.pointsIssued)))),
                ]),
              ]);
            },
          ),
        ]),
      );
}

/// The last seven days' sales as bars, from the seller's own figures.
class SalesTrendCard extends StatelessWidget {
  const SalesTrendCard({super.key});

  @override
  Widget build(BuildContext context) => SummaryBuilder(
        skeleton: const AppCard(
            child:
                SkeletonBox(width: double.infinity, height: 120, radius: 12)),
        builder: (context, summary) =>
            Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SectionLabel(context.l10n.salesTrend7Days,
              trailing: Text(
                  xaf(summary.week.fold<int>(0, (n, d) => n + d.salesXaf)),
                  style: const TextStyle(fontWeight: FontWeight.w800))),
          AppCard(
              child: MiniBarChart(points: [
            for (final d in summary.week)
              BarPoint(weekdayShort(d.day), d.salesXaf)
          ])),
        ]),
      );
}

/// Current cash shift with open/close actions; tapping it opens the cash shift screen (ledger, outflows, remittances).
class ShiftCard extends StatelessWidget {
  const ShiftCard({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final shift = app.shift;
    return AppCard(
      color: AppColors.navy,
      onTap: () => Navigator.of(context)
          .push(MaterialPageRoute(builder: (_) => const CashShiftScreen())),
      child: Row(children: [
        Icon(shift == null ? Icons.lock_clock_outlined : Icons.point_of_sale,
            color: AppColors.gold),
        const SizedBox(width: 12),
        Expanded(
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(
                shift == null
                    ? context.l10n.cashShiftClosed
                    : context.l10n.shiftOpenSince(hhmm(shift.openedAt)),
                style: const TextStyle(
                    color: Colors.white, fontWeight: FontWeight.w700)),
            // a shift left open since an earlier day is flagged: the drawer should be counted and closed
            if (shift != null && isBeforeToday(shift.openedAt))
              Text(context.l10n.shiftOpenedEarlier(dayMonth(shift.openedAt)),
                  maxLines: 2,
                  style: const TextStyle(
                      color: AppColors.gold,
                      fontSize: 12.5,
                      fontWeight: FontWeight.w700))
            else
              Text(
                  shift == null
                      ? context.l10n.openAShiftToStartSelling
                      : context.l10n.expectedCashValue(xaf(shift.expectedCash)),
                  style: const TextStyle(
                      color: AppColors.white70, fontSize: 12.5)),
          ]),
        ),
        TextButton(
          style: TextButton.styleFrom(
              backgroundColor: AppColors.gold, foregroundColor: AppColors.navy),
          onPressed: () => shift == null
              ? showOpenShiftDialog(context)
              : showCloseShiftDialog(context),
          child: Text(shift == null ? context.l10n.open : context.l10n.close,
              style: const TextStyle(fontWeight: FontWeight.w800)),
        ),
      ]),
    );
  }
}

class QuickAction {
  const QuickAction(this.icon, this.label, this.onTap, {this.badge = 0});
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final int badge;
}

class QuickActionGrid extends StatelessWidget {
  const QuickActionGrid({super.key, required this.actions});
  final List<QuickAction> actions;

  @override
  Widget build(BuildContext context) => GridView.count(
        crossAxisCount: 3,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 10,
        crossAxisSpacing: 10,
        childAspectRatio: 1.15,
        children: [
          for (final a in actions)
            AppCard(
              padding: const EdgeInsets.all(8),
              onTap: a.onTap,
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Badge(
                        isLabelVisible: a.badge > 0,
                        label: Text('${a.badge}'),
                        child: Icon(a.icon, color: AppColors.navy, size: 28)),
                    const SizedBox(height: 8),
                    Text(a.label,
                        textAlign: TextAlign.center,
                        maxLines: 2,
                        style: const TextStyle(
                            fontWeight: FontWeight.w600, fontSize: 12.5)),
                  ]),
            ),
        ],
      );
}
