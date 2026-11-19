import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'bonus_details_screen.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 6 — Bonus history: earned bonuses, waiting or already in earnings, and any taken back.
class BonusHistoryScreen extends StatefulWidget {
  const BonusHistoryScreen({super.key});

  @override
  State<BonusHistoryScreen> createState() => _BonusHistoryScreenState();
}

class _BonusHistoryScreenState extends State<BonusHistoryScreen> {
  BonusState? _status;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.bonusHistory)),
      body: Column(children: [
        FilterChips<BonusState>(
          options: [
            (BonusState.included, l.bonusStateIncluded),
            (BonusState.pending, l.bonusStatePending),
            (BonusState.reversed, l.bonusStateReversed),
          ],
          selected: _status,
          onSelected: (s) => setState(() => _status = s),
        ),
        Expanded(
          child: PagedListView<SalesTarget>(
            key: ValueKey('bonuses-$_status-${app.dataVersion}'),
            fetch: (page, size) => app.targets
                .bonusesPage(status: _status, page: page, size: size),
            empty: EmptyState(
                icon: Icons.card_giftcard,
                title: l.noBonusesYet,
                message: l.noBonusesMessage),
            itemBuilder: (context, t, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => BonusDetailsScreen(targetId: t.id))),
                child: Row(children: [
                  const Icon(Icons.card_giftcard, color: AppColors.amber),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(t.name,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(monthLabel(t.bonusMonth),
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                    Text(xaf(t.bonusPaidXaf ?? t.bonusEarned),
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    bonusStateChip(context, t.bonusState),
                  ]),
                  const Icon(Icons.chevron_right, color: AppColors.muted),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
