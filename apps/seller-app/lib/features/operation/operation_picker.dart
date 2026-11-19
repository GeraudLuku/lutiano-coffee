import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Lets a seller assigned to several operations choose which one they are selling for.
Future<void> showOperationPicker(BuildContext context) {
  final app = AppScope.read(context);
  return showModalBottomSheet<void>(
    context: context,
    showDragHandle: true,
    builder: (ctx) => SafeArea(
      child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          children: [
            Text(context.l10n.chooseSalesOperation,
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
            const SizedBox(height: 4),
            Text(context.l10n.switchingEmptiesTheCurrentCart,
                style: TextStyle(color: AppColors.muted)),
            const SizedBox(height: 8),
            for (final op in app.operations)
              MenuTile(
                icon: Icons.storefront_outlined,
                title: op.name,
                subtitle: '${op.type.label(context.l10n)} · ${op.code}',
                trailing: op.id == app.operation?.id
                    ? const Icon(Icons.check_circle, color: AppColors.green)
                    : null,
                onTap: () {
                  app.selectOperation(op);
                  Navigator.pop(ctx);
                },
              ),
          ]),
    ),
  );
}
