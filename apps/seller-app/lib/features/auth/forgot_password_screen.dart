import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Forgot password: ask for a code by SMS to the phone on file, then choose a new password.
/// The first answer is the same whether or not the email has an account, so it reveals nothing.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, this.initialEmail = ''});
  final String initialEmail;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen>
    with SubmitState<ForgotPasswordScreen> {
  late final TextEditingController _email =
      TextEditingController(text: widget.initialEmail);
  final _code = TextEditingController();
  final _password = TextEditingController();
  bool _codeSent = false, _obscure = true;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    if (!_email.text.contains('@')) {
      setState(() => error = context.l10n.enterYourEmail);
      return;
    }
    if (await submit(
        () => AppScope.read(context).auth.forgotPassword(_email.text))) {
      setState(() => _codeSent = true);
    }
  }

  Future<void> _reset() async {
    if (_code.text.trim().length < 4) {
      setState(() => error = context.l10n.enterTheCodeFromThe);
      return;
    }
    if (_password.text.length < 10) {
      setState(() => error = context.l10n.chooseAPasswordOfAt);
      return;
    }
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    final done = context.l10n.passwordChangedSignInWith;
    final ok = await submit(() => AppScope.read(context)
        .auth
        .resetPassword(_email.text, _code.text, _password.text));
    if (!ok) return;
    messenger.showSnackBar(SnackBar(content: Text(done)));
    navigator.pop();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(context.l10n.forgotPassword)),
        body: ListView(padding: const EdgeInsets.all(20), children: [
          if (!_codeSent) ...[
            InfoNote(context.l10n.enterTheEmailOfYour),
            const SizedBox(height: 14),
            TextField(
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              decoration: InputDecoration(
                  hintText: context.l10n.email,
                  prefixIcon: Icon(Icons.mail_outline)),
            ),
            const SizedBox(height: 14),
            if (error != null) ErrorBanner(error!),
            FilledButton(
                onPressed: busy ? null : _sendCode,
                child:
                    Text(busy ? context.l10n.sending : context.l10n.sendCode)),
          ] else ...[
            InfoNote(context.l10n.ifAnAccountWithA),
            const SizedBox(height: 14),
            TextField(
              controller: _code,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              maxLength: 6,
              decoration: InputDecoration(
                  hintText: context.l10n.codeFromTheSms,
                  prefixIcon: Icon(Icons.sms_outlined),
                  counterText: ''),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _password,
              obscureText: _obscure,
              decoration: InputDecoration(
                hintText: context.l10n.newPassword10Characters,
                prefixIcon: const Icon(Icons.lock_outline),
                suffixIcon: IconButton(
                  tooltip: _obscure
                      ? context.l10n.showPassword
                      : context.l10n.hidePassword,
                  icon: Icon(_obscure
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
            ),
            const SizedBox(height: 14),
            if (error != null) ErrorBanner(error!),
            FilledButton(
                onPressed: busy ? null : _reset,
                child: Text(
                    busy ? context.l10n.saving : context.l10n.setNewPassword)),
            TextButton(
                onPressed: busy
                    ? null
                    : () => setState(() {
                          _codeSent = false;
                          error = null;
                        }),
                child: Text(context.l10n.useADifferentEmail)),
          ],
          const SizedBox(height: 8),
          Center(
              child: Text(context.l10n.lutiano,
                  style: TextStyle(color: AppColors.muted, letterSpacing: 2))),
        ]),
      );
}
