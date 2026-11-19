import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../orders/order_detail_screen.dart';
import '../rewards/reward_qr_screen.dart';
import '../support/request_details_screen.dart';

/// A short title for a message: an announcement brings its own, other events are named by their kind.
String notificationTitle(BuildContext context, AppNotification n) {
  final l = context.l10n;
  if (n.title != null && n.title!.isNotEmpty) return n.title!;
  return switch (n.eventKey) {
    'ORDER_ACCEPTED' => l.ntOrderAccepted,
    'ORDER_REJECTED' => l.ntOrderDeclined,
    'ORDER_READY' => l.ntOrderReady,
    'ORDER_COLLECTED' || 'ORDER_COLLECTED_POINTS' => l.ntOrderComplete,
    'ORDER_CANCELLED' => l.ntOrderCancelled,
    'ORDER_EXPIRED' => l.ntOrderExpired,
    'REWARD_REDEEMED' => l.ntRewardRedeemed,
    'REWARD_USED' => l.ntRewardUsed,
    'REWARD_EXPIRED' => l.ntRewardExpired,
    'SUPPORT_REPLIED' => l.ntSupportReply,
    _ => l.ntLutiano,
  };
}

String categoryLabel(BuildContext context, String category) {
  final l = context.l10n;
  return switch (category) {
    'ORDERS' => l.catOrders,
    'LOYALTY' => l.catLoyalty,
    'PROMOTIONS' => l.catPromotions,
    _ => l.catSystem,
  };
}

IconData categoryIcon(String category) => switch (category) {
      'ORDERS' => Icons.receipt_long_outlined,
      'LOYALTY' => Icons.star_outline,
      'PROMOTIONS' => Icons.local_offer_outlined,
      _ => Icons.info_outline,
    };

/// The screen a message is about, when it has one.
Widget? notificationScreen(AppNotification n) {
  final id = n.relatedId;
  if (id == null) return null;
  return switch (n.relatedType) {
    'customer_order' => OrderDetailScreen(orderId: id),
    'reward_redemption' => RewardQrScreen(itemId: id),
    'support_request' => RequestDetailsScreen(requestId: id),
    _ => null,
  };
}
