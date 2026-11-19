import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../widgets/widgets.dart';

/// Frequently asked questions, answered from how the app works.
class FaqScreen extends StatelessWidget {
  const FaqScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final items = [
      (l.faqQ1, l.faqA1),
      (l.faqQ2, l.faqA2),
      (l.faqQ3, l.faqA3),
      (l.faqQ4, l.faqA4),
      (l.faqQ5, l.faqA5),
      (l.faqQ6, l.faqA6),
    ];
    return Scaffold(
      appBar: AppBar(title: Text(l.faq)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          padding: EdgeInsets.zero,
          child: Column(children: [
            for (final (q, a) in items)
              ExpansionTile(
                title: Text(q,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
                expandedCrossAxisAlignment: CrossAxisAlignment.start,
                children: [Text(a)],
              ),
          ]),
        ),
      ]),
    );
  }
}
