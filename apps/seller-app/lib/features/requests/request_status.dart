import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';

String requestStatusLabel(BuildContext context, RequestStatus s) => switch (s) {
      RequestStatus.pending => context.l10n.requestsPending,
      RequestStatus.approved => context.l10n.requestsApproved,
      RequestStatus.inFulfillment => context.l10n.requestsInFulfillment,
      RequestStatus.completed => context.l10n.requestsCompleted,
      RequestStatus.rejected => context.l10n.requestsRejected,
      RequestStatus.cancelled => context.l10n.requestsCancelled,
    };

/// The coloured pill for a request's status, the same on every request screen.
StatusChip requestStatusChip(BuildContext context, RequestStatus s) {
  final label = requestStatusLabel(context, s);
  return switch (s) {
    RequestStatus.pending => StatusChip(label,
        color: AppColors.amber, background: AppColors.amberSoft),
    RequestStatus.approved || RequestStatus.completed => StatusChip(label),
    RequestStatus.inFulfillment =>
      StatusChip(label, color: AppColors.blue, background: AppColors.blueSoft),
    RequestStatus.rejected =>
      StatusChip(label, color: AppColors.red, background: AppColors.redSoft),
    RequestStatus.cancelled => StatusChip(label,
        color: AppColors.muted, background: AppColors.background),
  };
}
