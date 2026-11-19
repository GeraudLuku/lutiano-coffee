import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../widgets/widgets.dart';

/// Terms of Use or Privacy Policy: the text comes from the translations as paragraphs, where a line starting with "## " is a heading.
class LegalScreen extends StatelessWidget {
  const LegalScreen({super.key, required this.title, required this.body});

  final String title, body;

  @override
  Widget build(BuildContext context) {
    final blocks = body.split('\n\n');
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          child: Text(context.l10n.legalDraftNotice,
              style: const TextStyle(color: AppColors.muted, fontSize: 12.5)),
        ),
        const SizedBox(height: 12),
        for (final block in blocks) _Block(block),
      ]),
    );
  }
}

class _Block extends StatelessWidget {
  const _Block(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    final lines = text.split('\n');
    final heading = lines.first.startsWith('## ');
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        if (heading)
          Semantics(
            header: true,
            child: Text(lines.first.substring(3),
                style:
                    const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
          ),
        if (heading && lines.length > 1) const SizedBox(height: 4),
        Text((heading ? lines.skip(1) : lines).join('\n'),
            style: const TextStyle(height: 1.4)),
      ]),
    );
  }
}
