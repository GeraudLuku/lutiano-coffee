import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'target_details_screen.dart';
import 'target_widgets.dart';

/// Batch 6 · Screen 3 — My Targets: all of them, or the active, upcoming and completed ones.
class MyTargetsScreen extends StatefulWidget {
  const MyTargetsScreen({super.key});

  @override
  State<MyTargetsScreen> createState() => _MyTargetsScreenState();
}

class _MyTargetsScreenState extends State<MyTargetsScreen> {
  TargetState? _state;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.myTargets)),
      body: Column(children: [
        FilterChips<TargetState>(
          options: [
            (TargetState.current, l.targetStateActive),
            (TargetState.upcoming, l.targetStateUpcoming),
            (TargetState.ended, l.targetStateCompleted),
          ],
          selected: _state,
          onSelected: (s) => setState(() => _state = s),
        ),
        Expanded(
          child: PagedListView<SalesTarget>(
            key: ValueKey('my-targets-$_state-${app.dataVersion}'),
            fetch: (page, size) =>
                app.targets.page(state: _state, page: page, size: size),
            empty: EmptyState(
                icon: Icons.emoji_events_outlined,
                title: l.noTargetsYet,
                message: l.noTargetThisMonthMessage),
            itemBuilder: (context, t, _, __) => TargetTile(
                target: t,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => TargetDetailsScreen(targetId: t.id)))),
          ),
        ),
      ]),
    );
  }
}
