import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';

String orderStatusLabel(BuildContext context, OrderStatus s) {
  final l = context.l10n;
  return switch (s) {
    OrderStatus.placed => l.orderStatusPlaced,
    OrderStatus.accepted => l.orderStatusAccepted,
    OrderStatus.ready => l.orderStatusReady,
    OrderStatus.collected => l.orderStatusCollected,
    OrderStatus.rejected => l.orderStatusRejected,
    OrderStatus.cancelled => l.orderStatusCancelled,
    OrderStatus.expired => l.orderStatusExpired,
  };
}

StatusChip orderStatusChip(BuildContext context, OrderStatus s) {
  final label = orderStatusLabel(context, s);
  return switch (s) {
    OrderStatus.placed =>
      StatusChip(label, color: AppColors.blue, background: AppColors.blueSoft),
    OrderStatus.accepted => StatusChip(label,
        color: AppColors.amber, background: AppColors.amberSoft),
    OrderStatus.ready => StatusChip(label,
        color: AppColors.purple, background: AppColors.purpleSoft),
    OrderStatus.collected => StatusChip(label),
    _ => StatusChip(label, color: AppColors.red, background: AppColors.redSoft),
  };
}
