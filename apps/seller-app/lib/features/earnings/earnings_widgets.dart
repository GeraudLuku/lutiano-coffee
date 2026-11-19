import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../widgets/widgets.dart';

/// Provisional until an administrator closes the month, then final.
StatusChip earningsStatusChip(BuildContext context, bool isFinal) => isFinal
    ? StatusChip(context.l10n.finalChip)
    : StatusChip(context.l10n.provisionalChip,
        color: AppColors.amber, background: AppColors.amberSoft);

/// "+12% vs previous month" in green or red, or nothing when there is no month before to compare with.
class ChangeLine extends StatelessWidget {
  const ChangeLine({super.key, required this.percent});
  final double? percent;

  @override
  Widget build(BuildContext context) {
    final p = percent;
    if (p == null) return const SizedBox.shrink();
    final up = p >= 0;
    return Row(mainAxisSize: MainAxisSize.min, children: [
      Icon(up ? Icons.arrow_upward : Icons.arrow_downward,
          size: 16, color: up ? AppColors.green : AppColors.red),
      const SizedBox(width: 4),
      // long in French: it wraps instead of running off the card
      Flexible(
          child: Text(context.l10n.vsPreviousMonth(signedPercent(p)),
              style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: up ? AppColors.green : AppColors.red))),
    ]);
  }
}

/// An amount with its sign shown for adjustments (which can be negative).
String signedXaf(int value) => '${value > 0 ? '+' : ''}${xaf(value)}';

/// The commission rule of a sale line or product in the seller's words ("5% of the sale value", "300 XAF per unit").
String ruleText(BuildContext context, String basis, int rate) =>
    basis == 'PERCENT'
        ? context.l10n.rulePercentOf('${rate / 100}%')
        : context.l10n.ruleFixedPerUnit(xaf(rate));

/// Four-figure summary used on the overview and the by-period screens.
class EarningsFigures extends StatelessWidget {
  const EarningsFigures(
      {super.key,
      required this.commission,
      required this.bonus,
      required this.adjustments,
      this.total});
  final int commission, bonus, adjustments;
  final int? total;

  @override
  Widget build(BuildContext context) => AppCard(
        child: Column(children: [
          InfoRow(context.l10n.commissionsLabel, xaf(commission)),
          InfoRow(context.l10n.bonusesLabel, xaf(bonus)),
          InfoRow(context.l10n.adjustmentsLabel, signedXaf(adjustments)),
          if (total != null) ...[
            const Divider(),
            InfoRow(context.l10n.totalEarnings, xaf(total!), bold: true),
          ],
        ]),
      );
}

/// The share of [part] in [whole], in percent text; empty when there is no whole.
String sharePercent(num part, num whole) => whole <= 0
    ? ''
    : '${(part * 100 / whole).toStringAsFixed(part * 100 / whole < 10 ? 1 : 0)}%';
