import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'notification_target.dart';

/// What the customer wants to hear about, in the app and as push on this phone. Order updates cannot be switched off.
class NotificationPreferencesScreen extends StatefulWidget {
  const NotificationPreferencesScreen({super.key});

  @override
  State<NotificationPreferencesScreen> createState() =>
      _NotificationPreferencesScreenState();
}

class _NotificationPreferencesScreenState
    extends State<NotificationPreferencesScreen> {
  late Future<List<NotificationPreference>> _prefs =
      AppScope.read(context).inbox.preferences();

  Future<void> _set(String category, {bool? inApp, bool? push}) async {
    try {
      final updated = await AppScope.read(context)
          .inbox
          .setPreference(category, inApp: inApp, push: push);
      if (mounted) setState(() => _prefs = Future.value(updated));
    } catch (e) {
      if (mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notificationSettings)),
      body: FutureBuilder<List<NotificationPreference>>(
        future: _prefs,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const SkeletonList();
          }
          if (snap.hasError) {
            return ListView(
                padding: const EdgeInsets.all(16),
                children: [ErrorBanner(errorMessage(snap.error!))]);
          }
          return ListView(padding: const EdgeInsets.all(16), children: [
            if (!app.pushRegistered) InfoNote(l.pushNotOnThisPhone),
            const SizedBox(height: 8),
            AppCard(
              child: Column(children: [
                for (final p in snap.data!) ...[
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    secondary:
                        Icon(categoryIcon(p.category), color: AppColors.amber),
                    title: Text(categoryLabel(context, p.category)),
                    subtitle:
                        Text(p.locked ? l.alwaysOn : _hint(l, p.category)),
                    value: p.inApp,
                    onChanged:
                        p.locked ? null : (v) => _set(p.category, inApp: v),
                  ),
                  if (app.pushRegistered && p.inApp)
                    Padding(
                      padding: const EdgeInsets.only(left: 40),
                      child: SwitchListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text(l.pushOnPhone),
                          value: p.push,
                          onChanged: (v) => _set(p.category, push: v)),
                    ),
                ],
              ]),
            ),
          ]);
        },
      ),
    );
  }

  static String _hint(AppLocalizations l, String c) => switch (c) {
        'LOYALTY' => l.hintLoyalty,
        'PROMOTIONS' => l.hintPromotions,
        _ => l.hintSystem,
      };
}
