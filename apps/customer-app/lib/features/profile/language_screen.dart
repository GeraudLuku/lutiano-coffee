import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';

/// App language: follow the phone, French or English. The choice is also kept on the account so messages arrive in it.
class LanguageScreen extends StatelessWidget {
  const LanguageScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final current = app.settings.languageCode;
    Future<void> pick(String? code) async {
      await app.settings.setLanguage(code);
      final me = app.customer;
      if (me != null && (code == 'fr' || code == 'en')) {
        try {
          await app.account.update(
              fullName: me.fullName,
              language: code!,
              email: me.email,
              birthDate: me.birthDate,
              marketingConsent: me.marketingConsent);
        } catch (_) {
          // the choice is already saved on this phone; the account copy catches up on the next change
        }
      }
    }

    Widget option(String? code, String label) => ListTile(
          title: Text(label),
          trailing: current == code
              ? Icon(Icons.check_circle, color: AppColors.green)
              : const Icon(Icons.circle_outlined),
          onTap: () => pick(code),
        );
    return Scaffold(
      appBar: AppBar(title: Text(l.language)),
      body: ListView(children: [
        option(null, l.followPhone),
        option('fr', 'Français'),
        option('en', 'English')
      ]),
    );
  }
}
