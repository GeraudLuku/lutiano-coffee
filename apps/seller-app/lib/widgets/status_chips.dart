import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../core/l10n.dart';
import '../data/models.dart';

/// Status pills with the Seller app's words.
StatusChip activeChip(BuildContext context) =>
    StatusChip(context.l10n.statusActive);

StatusChip completedChip(BuildContext context) =>
    StatusChip(context.l10n.statusCompleted);

StatusChip stockChip(BuildContext context, StockLevel level) => switch (level) {
      StockLevel.healthy => StatusChip(context.l10n.inStock),
      StockLevel.low => StatusChip(context.l10n.lowStock,
          color: AppColors.amber, background: AppColors.amberSoft),
      StockLevel.out => StatusChip(context.l10n.outOfStock,
          color: AppColors.red, background: AppColors.redSoft),
    };
