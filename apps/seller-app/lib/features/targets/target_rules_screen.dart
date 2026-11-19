import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../widgets/widgets.dart';

/// Batch 6 · Screen 8 — Target rules and bonus policy, in words. It states what the system does; it has no figures of its own.
class TargetRulesScreen extends StatelessWidget {
  const TargetRulesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    Widget bullets(List<String> lines) => AppCard(
          child:
              Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            for (final t in lines)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Padding(
                          padding: EdgeInsets.only(top: 6, right: 10),
                          child: Icon(Icons.circle,
                              size: 6, color: AppColors.amber)),
                      Expanded(child: Text(t)),
                    ]),
              ),
          ]),
        );
    return Scaffold(
      appBar: AppBar(title: Text(l.targetRulesPolicy)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        SectionLabel(l.rulesTargetsTitle),
        bullets([l.ruleT1, l.ruleT2]),
        SectionLabel(l.rulesBonusTitle),
        bullets([l.ruleB1, l.ruleB2, l.ruleB3, l.ruleB4]),
      ]),
    );
  }
}
