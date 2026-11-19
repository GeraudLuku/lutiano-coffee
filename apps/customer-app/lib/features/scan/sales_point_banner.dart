import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'sales_points_screen.dart';

/// Where the customer is ordering from (green), or a prompt to choose a sales point (amber). Tapping it changes the sales point.
class SalesPointBanner extends StatelessWidget {
  const SalesPointBanner({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final point = AppScope.of(context).salesPoint;
    return AppCard(
      color: point == null ? AppColors.goldSoft : AppColors.greenSoft,
      onTap: () => Navigator.of(context)
          .push(MaterialPageRoute(builder: (_) => const SalesPointsScreen())),
      child: Row(children: [
        Icon(point == null ? Icons.storefront_outlined : Icons.check_circle,
            color: point == null ? AppColors.amber : AppColors.green),
        const SizedBox(width: 10),
        Expanded(
            child: Text(
                point == null
                    ? l.chooseSalesPointHint
                    : l.orderingFrom(point.name),
                style: const TextStyle(fontWeight: FontWeight.w700))),
        Text(point == null ? l.choose : l.change,
            style: const TextStyle(
                color: AppColors.blue, fontWeight: FontWeight.w700)),
      ]),
    );
  }
}
