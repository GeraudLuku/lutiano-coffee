import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'change_password_screen.dart';
import 'login_activity_screen.dart';

/// Batch 8 · Screen 5 — Security: password, two-factor (administrators only), recent sign-ins, and signing out of other devices.
class SecurityScreen extends StatelessWidget {
  const SecurityScreen({super.key});

  Future<void> _signOutEverywhere(BuildContext context) async {
    final l = context.l10n;
    final app = AppScope.read(context);
    final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
              title: Text(l.signOutEverywhere),
              content: Text(l.signOutEverywhereConfirm),
              actions: [
                TextButton(
                    onPressed: () => Navigator.pop(ctx, false),
                    child: Text(l.cancel)),
                TextButton(
                    onPressed: () => Navigator.pop(ctx, true),
                    child: Text(l.confirm)),
              ],
            ));
    if (ok != true) return;
    try {
      // this phone gets a fresh token, every other one is signed out
      app.replaceToken(await app.account.signOutEverywhere());
      if (context.mounted) showMessage(context, l.signedOutEverywhere);
    } catch (e) {
      if (context.mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(title: Text(l.security)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        InfoNote(l.securityIntro,
            icon: Icons.shield_outlined,
            color: AppColors.green,
            background: AppColors.greenSoft),
        const SizedBox(height: 12),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.lock_outline,
                title: l.changePassword,
                subtitle: l.updateYourAccountPassword,
                onTap: () => push(const ChangePasswordScreen())),
            MenuTile(
                icon: Icons.verified_user_outlined,
                title: l.twoFactor,
                subtitle: l.twoFactorAdminsOnly),
            MenuTile(
                icon: Icons.history,
                title: l.loginActivity,
                subtitle: l.loginActivitySub,
                onTap: () => push(const LoginActivityScreen())),
            MenuTile(
                icon: Icons.devices_other,
                title: l.signOutEverywhere,
                subtitle: l.signOutEverywhereSub,
                onTap: () => _signOutEverywhere(context)),
          ]),
        ),
      ]),
    );
  }
}
