import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';

String supportTypeLabel(BuildContext context, SupportType t) {
  final l = context.l10n;
  return switch (t) {
    SupportType.question => l.supportTypeQuestion,
    SupportType.issue => l.supportTypeIssue,
    SupportType.improvement => l.supportTypeImprovement,
    SupportType.account => l.supportTypeAccount,
  };
}

String supportStatusLabel(BuildContext context, SupportStatus s) {
  final l = context.l10n;
  return switch (s) {
    SupportStatus.open => l.supportOpen,
    SupportStatus.inProgress => l.supportInProgress,
    SupportStatus.resolved => l.supportResolved,
  };
}

StatusChip supportStatusChip(BuildContext context, SupportStatus s) {
  final label = supportStatusLabel(context, s);
  return switch (s) {
    SupportStatus.open => StatusChip(label,
        color: AppColors.amber, background: AppColors.amberSoft),
    SupportStatus.inProgress =>
      StatusChip(label, color: AppColors.blue, background: AppColors.blueSoft),
    SupportStatus.resolved => StatusChip(label),
  };
}
