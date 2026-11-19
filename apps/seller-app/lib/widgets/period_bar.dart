import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../core/l10n.dart';

enum Period { today, week, month, custom }

/// The period a list or summary covers: a preset or a range the person picked.
@immutable
class PeriodChoice {
  const PeriodChoice(this.period, [this.range]);
  final Period period;
  final DateTimeRange? range;

  /// Douala dates (yyyy-MM-dd), inclusive: today, Monday to today, the 1st to today, or the picked range.
  (String, String) dates(DateTime now) {
    final d = toDouala(now);
    final today = isoDateOf(now);
    return switch (period) {
      Period.today => (today, today),
      Period.week => (doualaIsoDate(now, -(d.weekday - 1)), today),
      Period.month => (doualaIsoDate(now, -(d.day - 1)), today),
      Period.custom => range == null
          ? (today, today)
          : (isoDateOf(range!.start), isoDateOf(range!.end)),
    };
  }
}

/// Today / This Week / This Month / Custom. Custom opens a date-range picker; the choice is reported once a range is picked.
class PeriodBar extends StatelessWidget {
  const PeriodBar(
      {super.key,
      required this.value,
      required this.onChanged,
      this.padding = const EdgeInsets.fromLTRB(16, 12, 16, 0)});
  final PeriodChoice value;
  final ValueChanged<PeriodChoice> onChanged;
  final EdgeInsets padding;

  String _label(BuildContext context, Period p) => switch (p) {
        Period.today => context.l10n.periodToday,
        Period.week => context.l10n.periodWeek,
        Period.month => context.l10n.periodMonth,
        Period.custom => context.l10n.periodCustom,
      };

  Future<void> _choose(BuildContext context, Period p) async {
    if (p != Period.custom) return onChanged(PeriodChoice(p));
    final now = DateTime.now();
    final picked = await showDateRangePicker(
        context: context,
        firstDate: DateTime(now.year - 2),
        lastDate: now,
        initialDateRange: value.range ??
            DateTimeRange(
                start: now.subtract(const Duration(days: 6)), end: now));
    if (picked != null) onChanged(PeriodChoice(Period.custom, picked));
  }

  @override
  Widget build(BuildContext context) => Padding(
        padding: padding,
        child: SegmentedButton<Period>(
          segments: [
            for (final p in Period.values)
              ButtonSegment(value: p, label: Text(_label(context, p)))
          ],
          selected: {value.period},
          onSelectionChanged: (s) => _choose(context, s.first),
          showSelectedIcon: false,
        ),
      );
}
