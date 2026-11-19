import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../shell/main_shell.dart';

/// A sales point: where it is, whether it is open, its hours and what it accepts, and "Order here".
class SalesPointScreen extends StatelessWidget {
  const SalesPointScreen(
      {super.key,
      required this.salesPointId,
      this.preloaded,
      this.pick = false});
  final String salesPointId;
  final SalesPoint? preloaded;

  /// Returns the sales point to the screen that asked for one, instead of switching to the menu.
  final bool pick;

  static String typeLabel(BuildContext context, String type) => switch (type) {
        'FIXED_POS' => context.l10n.typeFixed,
        'MOBILE' => context.l10n.typeMobile,
        _ => context.l10n.typeMarket,
      };

  @override
  Widget build(BuildContext context) {
    final app = AppScope.read(context);
    final l = context.l10n;
    final days = [
      l.monday,
      l.tuesday,
      l.wednesday,
      l.thursday,
      l.friday,
      l.saturday,
      l.sunday
    ];
    return Scaffold(
      appBar: AppBar(title: Text(l.salesPoint)),
      body: AsyncView<SalesPoint>(
        load: () async =>
            preloaded ?? await app.catalogue.salesPoint(salesPointId),
        builder: (context, p, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          SizedBox(
              height: 180, child: ProductImage(url: p.imageUrl, radius: 16)),
          const SizedBox(height: 14),
          Text(p.name,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          const SizedBox(height: 6),
          Wrap(spacing: 8, children: [
            StatusChip(typeLabel(context, p.type),
                color: AppColors.purple, background: AppColors.purpleSoft),
            p.openNow
                ? StatusChip(l.openNow)
                : StatusChip(l.closedNow,
                    color: AppColors.red, background: AppColors.redSoft),
          ]),
          if (p.place.isNotEmpty) ...[
            const SizedBox(height: 10),
            Row(children: [
              const Icon(Icons.place_outlined,
                  size: 18, color: AppColors.muted),
              const SizedBox(width: 6),
              Expanded(child: Text(p.place))
            ])
          ],
          if (p.description != null && p.description!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(p.description!)
          ],
          if (p.hours.isNotEmpty) ...[
            SectionLabel(l.openingHours),
            AppCard(
                child: Column(children: [
              for (final h in p.hours)
                InfoRow(days[h.weekday - 1], '${h.opens} – ${h.closes}')
            ])),
          ],
          if (p.paymentMethods.isNotEmpty) ...[
            SectionLabel(l.paymentMethods),
            Wrap(spacing: 8, children: [
              for (final m in p.paymentMethods) Chip(label: Text(_method(l, m)))
            ]),
          ],
          const SizedBox(height: 20),
          FilledButton(
            onPressed: () {
              if (pick) {
                Navigator.of(context).pop(p);
                return;
              }
              app.selectSalesPoint(p);
              Navigator.of(context).popUntil((r) => r.isFirst);
              MainShell.goTo(context, MainShell.menu);
            },
            child: Text(pick ? l.selectThisSalesPoint : l.orderHere),
          ),
        ]),
      ),
    );
  }

  static String _method(AppLocalizations l, String code) => switch (code) {
        'CASH' => l.cashAtSalesPoint,
        'MTN_MOMO' => 'MTN MoMo',
        'ORANGE_MONEY' => 'Orange Money',
        _ => code,
      };
}
