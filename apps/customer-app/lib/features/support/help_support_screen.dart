import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'my_requests_screen.dart';
import 'support_request_screen.dart';

/// Help: answers to common questions, and a way to write to the team and follow replies.
class HelpSupportScreen extends StatelessWidget {
  const HelpSupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    final faq = [
      (l.faqQ1, l.faqA1),
      (l.faqQ2, l.faqA2),
      (l.faqQ3, l.faqA3),
      (l.faqQ4, l.faqA4),
      (l.faqQ5, l.faqA5)
    ];
    return Scaffold(
      appBar: AppBar(title: Text(l.helpAndSupport)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        FilledButton.icon(
            onPressed: () => push(const SupportRequestScreen()),
            icon: const Icon(Icons.edit_outlined),
            label: Text(l.contactSupport)),
        const SizedBox(height: 8),
        OutlinedButton.icon(
            onPressed: () => push(const MyRequestsScreen()),
            icon: const Icon(Icons.inbox_outlined),
            label: Text(l.myRequests)),
        SectionLabel(l.faq),
        AppCard(
          padding: EdgeInsets.zero,
          child: Column(children: [
            for (final (q, a) in faq)
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
