/// Display formatting. Money is whole FCFA; business time is Africa/Douala (UTC+1, no daylight saving).
library;

import 'package:intl/intl.dart';

const _douala = Duration(hours: 1);

/// The language of the numbers and dates on screen: the one the app resolved at start-up (French by default).
String get _locale => Intl.defaultLocale ?? 'fr';

String groupDigits(int value) =>
    NumberFormat.decimalPattern(_locale).format(value);

String xaf(num value) => '${groupDigits(value.round())} XAF';

/// A DateTime whose fields read as Douala wall-clock time.
DateTime toDouala(DateTime t) => t.toUtc().add(_douala);

String _two(int n) => n.toString().padLeft(2, '0');
String hhmm(DateTime t) {
  final d = toDouala(t);
  return '${_two(d.hour)}:${_two(d.minute)}';
}

String dayMonth(DateTime t) => DateFormat('d MMM', _locale).format(toDouala(t));

String dayMonthYear(DateTime t) =>
    DateFormat('d MMM y', _locale).format(toDouala(t));

String dateTime(DateTime t) => '${dayMonthYear(t)} · ${hhmm(t)}';

/// Douala calendar date (YYYY-MM-DD), offset by whole days from [now].
String doualaIsoDate(DateTime now, [int offsetDays = 0]) {
  final d = toDouala(now).add(Duration(days: offsetDays));
  return '${d.year}-${_two(d.month)}-${_two(d.day)}';
}

String isoDateOf(DateTime t) {
  final d = toDouala(t);
  return '${d.year}-${_two(d.month)}-${_two(d.day)}';
}

/// Short weekday name for chart labels.
String weekdayShort(String isoDate) =>
    DateFormat('E', _locale).format(DateTime.parse(isoDate));

/// True when [t] (a UTC instant) falls on an earlier Douala day than today.
bool isBeforeToday(DateTime t) => isoDateOf(t) != isoDateOf(DateTime.now());

// ----- months (earnings are reported per calendar month in Douala) -----

/// The current Douala month as yyyy-MM.
String currentMonth([DateTime? now]) {
  final d = toDouala(now ?? DateTime.now());
  return '${d.year}-${_two(d.month)}';
}

/// The last [count] months, newest first, as yyyy-MM.
List<String> recentMonths(int count, [DateTime? now]) {
  final d = toDouala(now ?? DateTime.now());
  return [
    for (var i = 0; i < count; i++)
      () {
        final m = DateTime(d.year, d.month - i);
        return '${m.year}-${_two(m.month)}';
      }()
  ];
}

DateTime _monthStart(String yearMonth) => DateTime(
    int.parse(yearMonth.substring(0, 4)), int.parse(yearMonth.substring(5, 7)));

/// "March 2026" / "mars 2026".
String monthLabel(String yearMonth) =>
    DateFormat('MMMM y', _locale).format(_monthStart(yearMonth));

/// "Mar" / "mars".
String monthShort(String yearMonth) =>
    DateFormat('MMM', _locale).format(_monthStart(yearMonth));

/// A calendar date (no time zone shift), e.g. "5 Mar" for a day name coming from the API.
String plainDayMonth(DateTime d) => DateFormat('d MMM', _locale).format(d);
String plainDayMonthYear(DateTime d) =>
    DateFormat('d MMM y', _locale).format(d);

/// "+12%" / "-5%" for a change in percent.
String signedPercent(double value) =>
    '${value >= 0 ? '+' : '-'}${value.abs().toStringAsFixed(value.abs() < 10 ? 1 : 0)}%';
