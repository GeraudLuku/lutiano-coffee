import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';

/// Membership tiers: what each needs (spending over the last 12 months) and what it gives.
class TiersScreen extends StatelessWidget {
  const TiersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.read(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.membershipTiers)),
      body: AsyncView<List<Tier>>(
        load: app.loyalty.tiers,
        builder: (context, tiers, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          InfoNote(l.tiersHint),
          const SizedBox(height: 10),
          for (final t in tiers)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                color: t.current ? AppColors.goldSoft : Colors.white,
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(children: [
                        Icon(Icons.workspace_premium, color: _color(t.color)),
                        const SizedBox(width: 8),
                        Expanded(
                            child: Text(t.name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.w800,
                                    fontSize: 17))),
                        if (t.current) StatusChip(l.currentTier),
                      ]),
                      const SizedBox(height: 6),
                      Text(
                          t.minSpendXaf == 0
                              ? l.startingTier
                              : l.tierNeeds(xaf(t.minSpendXaf)),
                          style: const TextStyle(color: AppColors.muted)),
                      Text(l.tierEarns(t.multiplierPct),
                          style: const TextStyle(fontWeight: FontWeight.w700)),
                      if (t.benefits != null) Text(t.benefits!),
                    ]),
              ),
            ),
        ]),
      ),
    );
  }

  static Color _color(String hex) {
    final v = int.tryParse(hex.replaceFirst('#', ''), radix: 16);
    return v == null ? AppColors.amber : Color(0xFF000000 | v);
  }
}
