import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'earnings_widgets.dart';

/// Batch 5 · Screen 7 — Commission rules in force for the seller's role (information only).
class CommissionRulesScreen extends StatelessWidget {
  const CommissionRulesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.commissionRulesTitle)),
      body: PagedListView<CommissionRule>(
        key: ValueKey('rules-${app.dataVersion}'),
        fetch: (page, size) => app.earnings.rulesPage(page: page, size: size),
        header: (context, _) => Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Column(children: [
            InfoNote(l.rulesIntro,
                icon: Icons.info_outline,
                color: AppColors.amber,
                background: AppColors.amberSoft),
            const SizedBox(height: 8),
            InfoNote(l.rulesNote),
          ]),
        ),
        empty: EmptyState(icon: Icons.rule, title: l.noRulesForRole),
        itemBuilder: (context, r, _, __) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: AppCard(
            child: Row(children: [
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.productName,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(r.category,
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ])),
              Text(ruleText(context, r.basis, r.rate),
                  style: const TextStyle(fontWeight: FontWeight.w700)),
            ]),
          ),
        ),
      ),
    );
  }
}
