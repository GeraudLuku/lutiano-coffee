import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';
import '../earnings/earnings_screen.dart';
import '../earnings/payout_details_screen.dart';
import '../inventory/incoming_stock_screen.dart';
import '../inventory/low_stock_screen.dart';
import '../orders/customer_order_screen.dart';
import '../requests/request_details_screen.dart';
import '../shift/shift_detail_screen.dart';
import '../shift/shift_lists.dart';
import '../profile/support_details_screen.dart';
import '../targets/target_details_screen.dart';

String categoryName(BuildContext context, NotificationCategory c) {
  final l = context.l10n;
  return switch (c) {
    NotificationCategory.inventory => l.catInventory,
    NotificationCategory.requests => l.catRequests,
    NotificationCategory.sales => l.catSales,
    NotificationCategory.earnings => l.catEarnings,
    NotificationCategory.targets => l.catTargets,
    NotificationCategory.operations => l.catOperations,
    NotificationCategory.system => l.catSystem,
  };
}

String categoryHint(BuildContext context, NotificationCategory c) {
  final l = context.l10n;
  return switch (c) {
    NotificationCategory.inventory => l.catInventorySub,
    NotificationCategory.requests => l.catRequestsSub,
    NotificationCategory.sales => l.catSalesSub,
    NotificationCategory.earnings => l.catEarningsSub,
    NotificationCategory.targets => l.catTargetsSub,
    NotificationCategory.operations => l.catOperationsSub,
    NotificationCategory.system => l.catSystemSub,
  };
}

IconData categoryIcon(NotificationCategory c) => switch (c) {
      NotificationCategory.inventory => Icons.inventory_2_outlined,
      NotificationCategory.requests => Icons.assignment_outlined,
      NotificationCategory.sales => Icons.receipt_long_outlined,
      NotificationCategory.earnings => Icons.savings_outlined,
      NotificationCategory.targets => Icons.emoji_events_outlined,
      NotificationCategory.operations => Icons.point_of_sale,
      NotificationCategory.system => Icons.campaign_outlined,
    };

Color categoryColor(NotificationCategory c) => switch (c) {
      NotificationCategory.inventory => AppColors.green,
      NotificationCategory.requests => AppColors.amber,
      NotificationCategory.sales => AppColors.purple,
      NotificationCategory.earnings => AppColors.amber,
      NotificationCategory.targets => AppColors.red,
      NotificationCategory.operations => AppColors.blue,
      NotificationCategory.system => AppColors.purple,
    };

/// An event's title in the seller's language; an announcement brings its own.
String notificationTitle(BuildContext context, AppNotification n) {
  final l = context.l10n;
  if (n.title != null && n.title!.isNotEmpty) return n.title!;
  return switch (n.eventKey) {
    'STOCK_LOW' => l.ntLowStock,
    'STOCK_OUT' => l.ntOutOfStock,
    'STOCK_IN_TRANSIT' => l.ntStockOnItsWay,
    'STOCK_REQUEST_APPROVED' => l.ntRequestApproved,
    'STOCK_REQUEST_REJECTED' => l.ntRequestRejected,
    'PAYOUT_APPROVED' => l.ntPayoutApproved,
    'PAYOUT_PAID' => l.ntPayoutPaid,
    'PAYOUT_REVERSED' => l.ntPayoutReversed,
    'PAYOUT_NUMBER_VERIFIED' => l.ntNumberVerified,
    'PAYOUT_NUMBER_REJECTED' => l.ntNumberRejected,
    'EARNINGS_FINALIZED' => l.ntEarningsFinal,
    'TARGET_REACHED' || 'TARGET_GOAL_REACHED' => l.ntTargetReached,
    'TARGET_MILESTONE' => l.ntTargetMilestone,
    'CASH_HANDOVER' => l.ntHandover,
    'SHIFT_STILL_OPEN' => l.ntShiftOpen,
    'SUPPORT_REPLIED' => l.ntSupportReply,
    'ORDER_PLACED' => l.ntNewOrder,
    'ANNOUNCEMENT' => l.ntAnnouncement,
    _ => l.ntGeneric,
  };
}

/// "Now", "15m", "3h", "2d", or the date for older ones.
String timeAgo(BuildContext context, DateTime t, [DateTime? now]) {
  final l = context.l10n;
  final diff = (now ?? DateTime.now()).toUtc().difference(t.toUtc());
  if (diff.inMinutes < 1) return l.agoNow;
  if (diff.inMinutes < 60) return l.agoMinutes(diff.inMinutes);
  if (diff.inHours < 24) return l.agoHours(diff.inHours);
  if (diff.inDays < 7) return l.agoDays(diff.inDays);
  return dayMonth(t);
}

/// Where a notification leads, when it is about something with a screen of its own.
({String label, Widget Function() screen})? notificationLink(
    BuildContext context, AppNotification n) {
  final id = n.relatedId;
  if (id == null) return null;
  final l = context.l10n;
  return switch (n.relatedType) {
    'stock_request' when n.eventKey == 'STOCK_IN_TRANSIT' => (
        label: l.receiveStock,
        screen: () => const IncomingStockScreen()
      ),
    'stock_request' => (
        label: l.requestDetails,
        screen: () => RequestDetailsScreen(requestId: id)
      ),
    'allocation' => (
        label: l.receiveStock,
        screen: () => const IncomingStockScreen()
      ),
    'stock_alert' => (
        label: l.lowStockAlerts,
        screen: () => const LowStockScreen()
      ),
    'seller_target' => (
        label: l.targetDetails,
        screen: () => TargetDetailsScreen(targetId: id)
      ),
    'payout' => (
        label: l.payoutDetails,
        screen: () => PayoutDetailsScreen(payoutId: id)
      ),
    'earning_period' => (
        label: l.earnings,
        screen: () => const EarningsScreen()
      ),
    'cash_shift' => (
        label: l.shiftDetails,
        screen: () => ShiftDetailScreen(shiftId: id)
      ),
    'customer_order' => (
        label: l.customerOrder,
        screen: () => CustomerOrderScreen(orderId: id)
      ),
    'support_request' => (
        label: l.supportRequest,
        screen: () => SupportDetailsScreen(requestId: id)
      ),
    'cash_remittance' => (
        label: l.allRemittances,
        screen: () => const RemittancesScreen()
      ),
    _ => null,
  };
}

/// One notification in a list: its category icon, title, text, how long ago, and a dot while unread.
class NotificationTile extends StatelessWidget {
  const NotificationTile(
      {super.key,
      required this.notification,
      required this.onTap,
      this.onLongPress,
      this.selected,
      this.padding = const EdgeInsets.only(bottom: 8)});
  final AppNotification notification;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  /// Non-null in selection mode: whether this one is ticked.
  final bool? selected;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final n = notification;
    final color = categoryColor(n.category);
    return Padding(
      padding: padding,
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        onLongPress: onLongPress,
        child: AppCard(
          color: selected == true ? AppColors.goldSoft : Colors.white,
          child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            if (selected != null)
              Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Icon(
                      selected!
                          ? Icons.check_box
                          : Icons.check_box_outline_blank,
                      color: selected! ? AppColors.navy : AppColors.muted)),
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12)),
              child: Icon(categoryIcon(n.category), color: color),
            ),
            const SizedBox(width: 12),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(notificationTitle(context, n),
                      style: TextStyle(
                          fontWeight:
                              n.isRead ? FontWeight.w600 : FontWeight.w800)),
                  const SizedBox(height: 2),
                  Text(n.body,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                          color: AppColors.muted, fontSize: 12.5)),
                ])),
            const SizedBox(width: 8),
            Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
              Text(timeAgo(context, n.createdAt),
                  style: const TextStyle(color: AppColors.muted, fontSize: 12)),
              const SizedBox(height: 6),
              if (!n.isRead)
                Container(
                    width: 9,
                    height: 9,
                    decoration: const BoxDecoration(
                        color: AppColors.red, shape: BoxShape.circle)),
            ]),
          ]),
        ),
      ),
    );
  }
}
