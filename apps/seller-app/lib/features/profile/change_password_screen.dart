import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Batch 8 · Screen 5 — Security (change password).
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen>
    with SubmitState<ChangePasswordScreen> {
  final _form = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _next = TextEditingController();
  final _confirm = TextEditingController();

  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    final ok = await submit(() =>
        AppScope.read(context).auth.changePassword(_current.text, _next.text));
    if (!ok || !mounted) return;
    showMessage(context, context.l10n.passwordChanged);
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(context.l10n.security)),
        body: Form(
          key: _form,
          child: ListView(padding: const EdgeInsets.all(16), children: [
            InfoNote(context.l10n.keepYourAccountSecureUse,
                icon: Icons.shield_outlined,
                color: AppColors.green,
                background: AppColors.greenSoft),
            const SizedBox(height: 16),
            TextFormField(
                controller: _current,
                obscureText: true,
                autofillHints: const [AutofillHints.password],
                decoration:
                    InputDecoration(labelText: context.l10n.currentPassword),
                validator: (v) => (v == null || v.isEmpty)
                    ? context.l10n.enterYourCurrentPassword
                    : null),
            const SizedBox(height: 12),
            TextFormField(
                controller: _next,
                obscureText: true,
                autofillHints: const [AutofillHints.newPassword],
                decoration:
                    InputDecoration(labelText: context.l10n.newPassword),
                validator: (v) => (v == null || v.length < 10)
                    ? context.l10n.useAtLeast10
                    : (v.length > 72 ? context.l10n.use72OrFewer : null)),
            const SizedBox(height: 12),
            TextFormField(
                controller: _confirm,
                obscureText: true,
                decoration:
                    InputDecoration(labelText: context.l10n.confirmNewPassword),
                validator: (v) =>
                    v != _next.text ? context.l10n.passwordsDoNotMatch : null),
            const SizedBox(height: 16),
            if (error != null) ErrorBanner(error!),
            FilledButton(
                onPressed: busy ? null : _submit,
                child: Text(busy
                    ? context.l10n.saving
                    : context.l10n.changePasswordButton)),
          ]),
        ),
      );
}
