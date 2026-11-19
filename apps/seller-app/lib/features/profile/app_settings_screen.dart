import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../notifications/notification_preferences_screen.dart';

/// Batch 8 · Screen 6 — App settings kept on this phone: language, notification preferences, receipt, default operation.
class AppSettingsScreen extends StatelessWidget {
  const AppSettingsScreen({super.key});

  /// "Français", "English"… the name of a language in that language, for any language the app is translated into.
  static String languageName(String code) => switch (code) {
        'fr' => 'Français',
        'en' => 'English',
        _ => code.toUpperCase(),
      };

  Future<void> _pickLanguage(BuildContext context) async {
    final app = AppScope.read(context);
    final l = context.l10n;
    final current = app.settings.languageCode;
    final choice = await showDialog<String>(
      context: context,
      builder: (ctx) => SimpleDialog(title: Text(l.languageLabel), children: [
        for (final option in <String?>[
          null,
          ...AppLocalizations.supportedLocales.map((e) => e.languageCode)
        ])
          ListTile(
            leading: Icon(
                option == current
                    ? Icons.radio_button_checked
                    : Icons.radio_button_unchecked,
                color: AppColors.navy),
            title: Text(option == null ? l.followPhone : languageName(option)),
            onTap: () => Navigator.pop(ctx, option ?? ''),
          ),
      ]),
    );
    if (choice == null) return;
    final code = choice.isEmpty ? null : choice;
    await app.settings.setLanguage(code);
    // the language of the texts the team sends (SMS) follows, when it is one they are written in
    if (code == 'fr' || code == 'en') {
      try {
        await app.account.update(language: code);
      } catch (_) {
        // the choice is already saved on this phone; the server copy (SMS language) catches up on the next change
      }
    }
  }

  Future<void> _pickOperation(BuildContext context) async {
    final app = AppScope.read(context);
    final chosen = app.settings.defaultOperationId ?? app.operation?.id;
    final id = await showDialog<String>(
      context: context,
      builder: (ctx) =>
          SimpleDialog(title: Text(context.l10n.defaultOperation), children: [
        for (final o in app.operations)
          ListTile(
            leading: Icon(
                o.id == chosen
                    ? Icons.radio_button_checked
                    : Icons.radio_button_unchecked,
                color: AppColors.navy),
            title: Text(o.name),
            onTap: () => Navigator.pop(ctx, o.id),
          ),
      ]),
    );
    if (id != null) await app.settings.setDefaultOperation(id);
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final code = app.settings.languageCode;
    final defaultOp = app.operations
        .where((o) => o.id == app.settings.defaultOperationId)
        .firstOrNull;
    return Scaffold(
      appBar: AppBar(title: Text(l.appSettings)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        SectionLabel(l.general),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.notifications_none,
                title: l.notificationPreferences,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => const NotificationPreferencesScreen()))),
            MenuTile(
                icon: Icons.language,
                title: l.languageLabel,
                subtitle: code == null ? l.followPhone : languageName(code),
                onTap: () => _pickLanguage(context)),
          ]),
        ),
        SectionLabel(l.operational),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            if (app.operations.length > 1)
              MenuTile(
                  icon: Icons.place_outlined,
                  title: l.defaultOperation,
                  subtitle: defaultOp?.name ?? l.defaultOperationSub,
                  onTap: () => _pickOperation(context)),
            SwitchListTile(
              contentPadding: const EdgeInsets.symmetric(horizontal: 6),
              secondary: const Icon(Icons.receipt_long_outlined,
                  color: AppColors.navy),
              title: Text(l.showReceiptAfterSale),
              subtitle: Text(l.receiptOptionsSub),
              value: app.settings.showReceipt,
              onChanged: app.settings.setShowReceipt,
            ),
          ]),
        ),
      ]),
    );
  }
}
