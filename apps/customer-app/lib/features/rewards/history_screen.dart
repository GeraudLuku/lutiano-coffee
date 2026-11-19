import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';

/// Points earned, used and expired, newest first.
class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  String? _type;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.pointsHistory)),
      body: Column(children: [
        FilterChips<String>(options: [
          ('EARNED', l.typeEarned),
          ('USED', l.typeUsed),
          ('EXPIRED', l.stateExpired)
        ], selected: _type, onSelected: (t) => setState(() => _type = t)),
        Expanded(
          child: PagedListView<LedgerEntry>(
            key: ValueKey('history-$_type-${app.loyaltyVersion}'),
            fetch: (page, size) =>
                app.loyalty.history(type: _type, page: page, size: size),
            empty: EmptyState(
                icon: Icons.star_outline,
                title: l.noPointsYet,
                message: l.noPointsYetMessage),
            itemBuilder: (context, e, _, __) {
              final gain = e.points > 0;
              return Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: AppCard(
                  child: Row(children: [
                    Icon(gain ? Icons.add_circle : Icons.remove_circle,
                        color: gain ? AppColors.green : AppColors.red),
                    const SizedBox(width: 12),
                    Expanded(
                        child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                          Text(_label(l, e),
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700)),
                          Text(dateTime(e.at),
                              style: const TextStyle(
                                  color: AppColors.muted, fontSize: 12.5)),
                        ])),
                    Text('${gain ? '+' : ''}${e.points} ${l.pts}',
                        style: TextStyle(
                            fontWeight: FontWeight.w800,
                            color: gain ? AppColors.green : AppColors.red)),
                  ]),
                ),
              );
            },
          ),
        ),
      ]),
    );
  }

  static String _label(AppLocalizations l, LedgerEntry e) => switch (e.type) {
        'EARNED' => l.pointsEarned,
        'REDEEMED' => l.pointsUsed,
        'EXPIRED' => l.pointsExpired,
        'REDEMPTION_REVERSED' || 'REFUND_REVERSAL' => l.pointsReturned,
        _ => e.description ?? l.pointsAdjusted,
      };
}
