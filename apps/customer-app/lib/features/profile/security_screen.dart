import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';

/// Password, the second step, other devices and deleting the account.
class SecurityScreen extends StatelessWidget {
  const SecurityScreen({super.key});

  Future<void> _run(
      BuildContext context, Future<void> Function() action, String done) async {
    try {
      await action();
      if (context.mounted) showMessage(context, done);
    } catch (e) {
      if (context.mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  Future<void> _changePassword(BuildContext context) async {
    final app = AppScope.read(context);
    final l = context.l10n;
    final me = app.customer!;
    final r = await showDialog<(String, String)>(
      context: context,
      builder: (ctx) => DialogFields(
          count: 2,
          builder: (ctx, f) => AlertDialog(
                title: Text(me.hasPassword ? l.changePassword : l.setPassword),
                content: Column(mainAxisSize: MainAxisSize.min, children: [
                  if (me.hasPassword)
                    TextField(
                        controller: f[0],
                        obscureText: true,
                        decoration:
                            InputDecoration(hintText: l.currentPassword)),
                  TextField(
                      controller: f[1],
                      obscureText: true,
                      decoration: InputDecoration(hintText: l.newPasswordMin)),
                ]),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(l.cancel)),
                  FilledButton(
                      onPressed: () =>
                          Navigator.of(ctx).pop((f[0].text, f[1].text)),
                      child: Text(l.save)),
                ],
              )),
    );
    final (currentText, nextText) = r ?? ('', '');
    if (r != null && context.mounted) {
      await _run(context, () async {
        await app.account
            .changePassword(me.hasPassword ? currentText : null, nextText);
        await app.reloadProfile();
      }, l.passwordChanged);
    }
  }

  Future<void> _delete(BuildContext context) async {
    final app = AppScope.read(context);
    final l = context.l10n;
    final me = app.customer!;
    if (!me.hasPassword) {
      try {
        await app.account.sendDeletionCode();
      } catch (e) {
        if (context.mounted) showMessage(context, errorMessage(e), error: true);
        return;
      }
    }
    if (!context.mounted) return;
    final value = await showDialog<String>(
      context: context,
      builder: (ctx) => DialogFields(
          builder: (ctx, f) => AlertDialog(
                title: Text(l.deleteAccountQuestion),
                content: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(l.deleteAccountWarning),
                  TextField(
                      controller: f[0],
                      obscureText: me.hasPassword,
                      decoration: InputDecoration(
                          hintText:
                              me.hasPassword ? l.password : l.sixDigitCode)),
                ]),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(l.cancel)),
                  FilledButton(
                      style: FilledButton.styleFrom(
                          backgroundColor: AppColors.red),
                      onPressed: () => Navigator.of(ctx).pop(f[0].text),
                      child: Text(l.deleteAccount)),
                ],
              )),
    );
    if (value == null || !context.mounted) return;
    try {
      await app.account.deleteAccount(
          password: me.hasPassword ? value : null,
          code: me.hasPassword ? null : value);
      app.signOut(expired: false);
    } catch (e) {
      if (context.mounted) showMessage(context, errorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final me = app.customer!;
    return Scaffold(
      appBar: AppBar(title: Text(l.securityAndLogin)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.key_outlined,
                title: me.hasPassword ? l.changePassword : l.setPassword,
                onTap: () => _changePassword(context)),
            SwitchListTile(
              secondary: const Icon(Icons.shield_outlined),
              title: Text(l.twoStepVerification),
              subtitle:
                  Text(me.hasPassword ? l.twoStepHint : l.twoStepNeedsPassword),
              value: me.twoFactor,
              onChanged: me.hasPassword
                  ? (v) => _run(context, () async {
                        await app.account.setTwoFactor(v);
                        await app.reloadProfile();
                      }, l.settingSaved)
                  : null,
            ),
            MenuTile(
                icon: Icons.devices_outlined,
                title: l.signOutOtherDevices,
                onTap: () => _run(
                    context,
                    () async =>
                        app.replaceToken(await app.account.signOutEverywhere()),
                    l.signedOutEverywhere)),
          ]),
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.red,
                side: const BorderSide(color: AppColors.red)),
            icon: const Icon(Icons.delete_outline),
            label: Text(l.deleteAccount),
            onPressed: () => _delete(context)),
      ]),
    );
  }
}
