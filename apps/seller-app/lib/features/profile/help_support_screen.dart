import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../widgets/widgets.dart';
import 'faq_screen.dart';
import 'my_support_requests_screen.dart';
import 'support_request_screen.dart';

/// Batch 8 · Screen 7 — Help & Support: answers, and ways to contact the LUTIANO team.
class HelpSupportScreen extends StatelessWidget {
  const HelpSupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(title: Text(l.helpSupport)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.menu_book_outlined,
                title: l.faq,
                subtitle: l.faqSub,
                onTap: () => push(const FaqScreen())),
            MenuTile(
                icon: Icons.support_agent,
                title: l.contactSupport,
                subtitle: l.contactSupportSub,
                onTap: () => push(
                    const SupportRequestScreen(type: SupportType.question))),
            MenuTile(
                icon: Icons.chat_bubble_outline,
                title: l.reportIssue,
                subtitle: l.reportIssueSub,
                onTap: () =>
                    push(const SupportRequestScreen(type: SupportType.issue))),
            MenuTile(
                icon: Icons.lightbulb_outline,
                title: l.suggestImprovement,
                subtitle: l.suggestImprovementSub,
                onTap: () => push(
                    const SupportRequestScreen(type: SupportType.improvement))),
            MenuTile(
                icon: Icons.inbox_outlined,
                title: l.mySupportRequests,
                onTap: () => push(const MySupportRequestsScreen())),
          ]),
        ),
      ]),
    );
  }
}
