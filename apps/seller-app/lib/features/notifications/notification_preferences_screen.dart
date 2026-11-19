import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'notification_widgets.dart';

/// Batch 7 · Screens 4 and 8 — Which categories reach the app. Cash and payout categories stay on; push is not available yet.
class NotificationPreferencesScreen extends StatefulWidget {
  const NotificationPreferencesScreen({super.key});

  @override
  State<NotificationPreferencesScreen> createState() =>
      _NotificationPreferencesScreenState();
}

class _NotificationPreferencesScreenState
    extends State<NotificationPreferencesScreen> {
  int _version = 0;

  Future<void> _set(NotificationCategory c, bool on,
      {bool push = false}) async {
    try {
      final repo = AppScope.read(context).notifications;
      await (push ? repo.setPush(c, on) : repo.setInApp(c, on));
      if (mounted) setState(() => _version++);
    } catch (e) {
      if (mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.notificationPreferences)),
      body: AsyncView<List<NotificationPreference>>(
        key: ValueKey('preferences-$_version'),
        load: app.notifications.preferences,
        builder: (context, prefs, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          InfoNote(l.preferencesIntro,
              icon: Icons.tune,
              color: AppColors.amber,
              background: AppColors.amberSoft),
          const SizedBox(height: 12),
          AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: Column(children: [
              for (final p in prefs) ...[
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  secondary: Icon(categoryIcon(p.category),
                      color: categoryColor(p.category)),
                  title: Text(categoryName(context, p.category)),
                  subtitle: Text(p.locked
                      ? l.alwaysOn
                      : categoryHint(context, p.category)),
                  value: p.inApp,
                  onChanged: p.locked ? null : (v) => _set(p.category, v),
                ),
                // push (on the phone) only where this phone is registered for it
                if (app.pushRegistered && p.inApp)
                  Padding(
                    padding: const EdgeInsets.only(left: 40),
                    child: SwitchListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      title: Text(l.pushOnPhone),
                      value: p.push,
                      onChanged: (v) => _set(p.category, v, push: true),
                    ),
                  ),
              ],
            ]),
          ),
          const SizedBox(height: 12),
          if (!app.pushRegistered) InfoNote(l.pushNotAvailable),
        ]),
      ),
    );
  }
}
