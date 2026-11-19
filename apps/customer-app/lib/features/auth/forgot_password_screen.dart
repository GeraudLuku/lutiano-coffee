import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'auth_widgets.dart';

/// Forgotten password: a code by SMS, then a new password.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, this.initialPhone = ''});
  final String initialPhone;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen>
    with SubmitState<ForgotPasswordScreen> {
  final _form = GlobalKey<FormState>();
  late final _phone = TextEditingController(text: widget.initialPhone);
  final _code = TextEditingController();
  final _password = TextEditingController();
  bool _sent = false;

  @override
  void dispose() {
    _phone.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _send() async {
    if (!phoneValid(_phone.text)) {
      _form.currentState!.validate();
      return;
    }
    final app = AppScope.read(context);
    final ok = await submit(() => app.auth.forgotPassword(phoneValue(_phone)));
    if (ok && mounted) setState(() => _sent = true);
  }

  Future<void> _reset() async {
    if (!_form.currentState!.validate()) return;
    final app = AppScope.read(context);
    final ok = await submit(() => app.auth
        .resetPassword(phoneValue(_phone), _code.text.trim(), _password.text));
    if (!ok || !mounted) return;
    showMessage(context, context.l10n.passwordChanged);
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return AuthScaffold(
      showBack: true,
      child: Form(
        key: _form,
        child:
            Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(l.resetPassword,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          Text(_sent ? l.enterCodeAndNewPassword : l.weWillSendCode,
              style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 18),
          PhoneField(controller: _phone),
          if (_sent) ...[
            const SizedBox(height: 12),
            TextFormField(
              controller: _code,
              keyboardType: TextInputType.number,
              maxLength: 6,
              decoration: InputDecoration(
                  hintText: l.sixDigitCode,
                  prefixIcon: const Icon(Icons.sms_outlined),
                  counterText: ''),
              validator: (v) =>
                  (v ?? '').trim().length == 6 ? null : l.enterTheCode,
            ),
            const SizedBox(height: 12),
            PasswordField(
                controller: _password,
                hint: l.newPassword,
                minLength: 10,
                onSubmitted: _reset),
          ],
          const SizedBox(height: 16),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : (_sent ? _reset : _send),
              child: Text(busy
                  ? l.pleaseWait
                  : (_sent ? l.resetPassword : l.sendCode))),
          if (_sent)
            TextButton(
                onPressed: busy ? null : _send, child: Text(l.resendCode)),
        ]),
      ),
    );
  }
}
