import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

/// Batch 5 · Screen 10 — Earnings statement: choose the period and what to include, then open the PDF to print or share.
class StatementExportScreen extends StatefulWidget {
  const StatementExportScreen({super.key});

  @override
  State<StatementExportScreen> createState() => _StatementExportScreenState();
}

class _StatementExportScreenState extends State<StatementExportScreen> {
  int _months = 1;
  final Set<String> _sections = {
    'SUMMARY',
    'BREAKDOWN',
    'BONUSES',
    'ADJUSTMENTS',
    'SALES'
  };

  void _toggle(String section, bool on) =>
      setState(() => on ? _sections.add(section) : _sections.remove(section));

  Future<void> _export() async {
    final app = AppScope.read(context);
    final months = recentMonths(_months);
    final language = Localizations.localeOf(context).languageCode;
    await openPdf(context,
        fetch: () => app.earnings.statementPdf(
            from: months.last,
            to: months.first,
            sections: _sections,
            language: language),
        fileName: 'LUTIANO-earnings-${months.last}-${months.first}.pdf',
        preparing: context.l10n.preparingStatement);
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final sections = {
      'SUMMARY': l.sectionSummary,
      'BREAKDOWN': l.sectionBreakdown,
      'BONUSES': l.sectionBonuses,
      'ADJUSTMENTS': l.sectionAdjustments,
      'SALES': l.sectionSales,
    };
    return Scaffold(
      appBar: AppBar(title: Text(l.earningsStatement)),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton.icon(
            onPressed: _sections.isEmpty ? null : _export,
            icon: const Icon(Icons.download),
            label: Text(l.exportPdf),
          ),
        ),
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        InfoNote(l.statementIntro,
            icon: Icons.description_outlined,
            color: AppColors.navy,
            background: AppColors.goldSoft),
        SectionLabel(l.selectPeriod),
        FilterChips<int>(
          padding: EdgeInsets.zero,
          options: [
            (1, l.periodMonth),
            (3, l.last3Months),
            (6, l.last6Months),
            (12, l.last12Months)
          ],
          selected: _months,
          onSelected: (m) => setState(() => _months = m ?? 1),
        ),
        const SizedBox(height: 6),
        Text(
            _months == 1
                ? monthLabel(currentMonth())
                : '${monthLabel(recentMonths(_months).last)} – ${monthLabel(currentMonth())}',
            style: const TextStyle(color: AppColors.muted)),
        SectionLabel(l.statementIncludes),
        AppCard(
          child: Column(children: [
            for (final e in sections.entries)
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                value: _sections.contains(e.key),
                onChanged: (v) => _toggle(e.key, v == true),
                title: Text(e.value),
              ),
          ]),
        ),
        if (_sections.isEmpty) ...[
          const SizedBox(height: 8),
          Text(l.chooseOneSection,
              style: const TextStyle(color: AppColors.red)),
        ],
      ]),
    );
  }
}
