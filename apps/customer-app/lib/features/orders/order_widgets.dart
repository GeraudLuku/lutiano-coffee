import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';

String orderStatusLabel(BuildContext context, OrderStatus s) {
  final l = context.l10n;
  return switch (s) {
    OrderStatus.placed => l.orderPlaced,
    OrderStatus.accepted => l.orderBeingPrepared,
    OrderStatus.ready => l.orderReadyStatus,
    OrderStatus.collected => l.orderCompleted,
    OrderStatus.rejected => l.orderDeclined,
    OrderStatus.cancelled => l.orderCancelledStatus,
    OrderStatus.expired => l.orderExpired,
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

/// What each step means to the customer, in words.
String orderStepMessage(BuildContext context, OrderStatus s) {
  final l = context.l10n;
  return switch (s) {
    OrderStatus.placed => l.stepPlaced,
    OrderStatus.accepted => l.stepAccepted,
    OrderStatus.ready => l.stepReady,
    OrderStatus.collected => l.stepCompleted,
    OrderStatus.rejected => l.stepDeclined,
    OrderStatus.cancelled => l.stepCancelled,
    OrderStatus.expired => l.stepExpired,
  };
}

/// The order's progress as a vertical timeline.
class OrderTimeline extends StatelessWidget {
  const OrderTimeline({super.key, required this.steps});
  final List<OrderStep> steps;

  @override
  Widget build(BuildContext context) => Column(children: [
        for (var i = 0; i < steps.length; i++)
          IntrinsicHeight(
            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Column(children: [
                Icon(Icons.check_circle,
                    size: 22,
                    color: _isBad(steps[i].status)
                        ? AppColors.red
                        : AppColors.green),
                if (i < steps.length - 1)
                  Expanded(child: Container(width: 2, color: AppColors.line)),
              ]),
              const SizedBox(width: 12),
              Expanded(
                  child: Padding(
                padding: const EdgeInsets.only(bottom: 14),
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(orderStatusLabel(context, steps[i].status),
                          style: const TextStyle(fontWeight: FontWeight.w800)),
                      Text(dateTime(steps[i].at),
                          style: const TextStyle(
                              color: AppColors.muted, fontSize: 12.5)),
                      Text(orderStepMessage(context, steps[i].status)),
                    ]),
              )),
            ]),
          ),
      ]);

  static bool _isBad(OrderStatus s) =>
      s == OrderStatus.rejected ||
      s == OrderStatus.cancelled ||
      s == OrderStatus.expired;
}
