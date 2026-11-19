import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';

/// How points work, in the programme's current numbers, with an estimate for an amount.
class HowItWorksScreen extends StatefulWidget {
  const HowItWorksScreen({super.key});

  @override
  State<HowItWorksScreen> createState() => _HowItWorksScreenState();
}

class _HowItWorksScreenState extends State<HowItWorksScreen> {
  final _amount = TextEditingController(text: '5000');
  int? _estimate;

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _calculate() async {
    final value = int.tryParse(_amount.text.replaceAll(' ', ''));
    if (value == null || value <= 0) return;
    try {
      final points = await AppScope.read(context).loyalty.estimate(value);
      if (mounted) setState(() => _estimate = points);
    } catch (e) {
      if (mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.read(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.howPointsWork)),
      body: AsyncView<LoyaltyRules>(
        load: app.loyalty.rules,
        builder: (context, r, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: Column(children: [
              ListTile(
                  leading: const Icon(Icons.shopping_bag_outlined,
                      color: AppColors.amber),
                  title: Text(l.earnPoints),
                  subtitle:
                      Text(l.earnRule(r.basePoints, xaf(r.baseAmountXaf)))),
              ListTile(
                  leading:
                      const Icon(Icons.card_giftcard, color: AppColors.amber),
                  title: Text(l.usePoints),
                  subtitle: Text(l.useRule(r.minRedeem))),
              ListTile(
                  leading:
                      const Icon(Icons.event_outlined, color: AppColors.amber),
                  title: Text(l.pointsValidity),
                  subtitle: Text(l.validityRule(r.expiryMonths))),
            ]),
          ),
          if (r.rules.isNotEmpty) ...[
            SectionLabel(l.currentPromotions),
            for (final rule in r.rules)
              ListTile(
                  dense: true,
                  leading: const Icon(Icons.bolt, color: AppColors.gold),
                  title: Text(rule.name)),
          ],
          SectionLabel(l.pointsEstimate),
          Row(children: [
            Expanded(
                child: TextField(
                    controller: _amount,
                    keyboardType: TextInputType.number,
                    decoration: InputDecoration(
                        labelText: l.purchaseAmount, suffixText: 'XAF'),
                    onSubmitted: (_) => _calculate())),
            const SizedBox(width: 12),
            FilledButton(onPressed: _calculate, child: Text(l.calculate)),
          ]),
          if (_estimate != null) ...[
            const SizedBox(height: 12),
            HighlightBox(
                label: l.estimatedPoints, value: l.pointsValue(_estimate!)),
            const SizedBox(height: 8),
            InfoNote(l.estimateNote)
          ],
        ]),
      ),
    );
  }
}
