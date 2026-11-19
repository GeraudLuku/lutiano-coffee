import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> _arb(String lang) =>
    jsonDecode(File('lib/l10n/app_$lang.arb').readAsStringSync())
        as Map<String, dynamic>;

void main() {
  test('French and English have the same texts, with the same placeholders',
      () {
    final en = _arb('en'), fr = _arb('fr');
    Set<String> keys(Map<String, dynamic> m) =>
        m.keys.where((k) => !k.startsWith('@')).toSet();
    expect(keys(fr), keys(en));
    final holder = RegExp(r'\{(\w+)\}');
    for (final k in keys(en)) {
      expect((fr[k] as String).trim(), isNotEmpty, reason: k);
      expect(holder.allMatches(fr[k] as String).map((m) => m.group(1)).toSet(),
          holder.allMatches(en[k] as String).map((m) => m.group(1)).toSet(),
          reason: k);
    }
  });
}
