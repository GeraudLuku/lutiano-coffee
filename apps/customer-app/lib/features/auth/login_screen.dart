import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'auth_flow.dart';
import 'auth_widgets.dart';
import 'code_screen.dart';
import 'forgot_password_screen.dart';
import 'signup_screen.dart';
import 'social_sign_in.dart';

/// Log in with phone and password, with a code instead, or with Google / Apple.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SubmitState<LoginScreen> {
  final _form = GlobalKey<FormState>();
  final _phone = TextEditingController();
  final _password = TextEditingController();
  ({bool google, bool apple}) _providers = (google: false, apple: false);

  @override
  void initState() {
    super.initState();
    AppScope.read(context).auth.providers().then((p) {
      if (mounted) setState(() => _providers = p);
    }).catchError((_) {});
  }

  @override
  void dispose() {
    _phone.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_form.currentState!.validate()) return;
    final app = AppScope.read(context);
    AuthStep? step;
    final ok = await submit(() async =>
        step = await app.auth.login(phoneValue(_phone), _password.text));
    if (ok && step != null && mounted) await continueAuth(context, step!);
  }

  Future<void> _sendCode() async {
    if (!phoneValid(_phone.text)) {
      _form.currentState!.validate();
      return;
    }
    final app = AppScope.read(context);
    final phone = phoneValue(_phone);
    final ok = await submit(() => app.auth.requestCode(phone));
    if (!ok || !mounted) return;
    await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => CodeScreen(
            phoneHint: phone,
            onVerify: (code) => app.auth.verifyCode(phone, code),
            onResend: () => app.auth.requestCode(phone))));
  }

  Future<void> _social(Future<SocialToken?> Function() pick) async {
    final app = AppScope.read(context);
    AuthStep? step;
    final ok = await submit(() async {
      final token = await pick();
      if (token != null) {
        step = await app.auth
            .social(token.provider, token.idToken, nonce: token.nonce);
      }
    });
    if (ok && step != null && mounted) await continueAuth(context, step!);
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final expired = AppScope.of(context).sessionExpired;
    return AuthScaffold(
      showBack: Navigator.of(context).canPop(),
      child: Form(
        key: _form,
        child:
            Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(l.logInToYourAccount,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          Text(l.accessYourOrders,
              style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 18),
          if (expired && error == null) ...[
            InfoNote(l.sessionEnded),
            const SizedBox(height: 12)
          ],
          PhoneField(controller: _phone),
          const SizedBox(height: 12),
          PasswordField(
              controller: _password,
              onSubmitted: _login,
              textInputAction: TextInputAction.done),
          const SizedBox(height: 16),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : _login,
              child: Text(busy ? l.signingIn : l.logIn)),
          Wrap(alignment: WrapAlignment.center, children: [
            TextButton(
                onPressed: busy ? null : _sendCode, child: Text(l.sendMeACode)),
            TextButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) =>
                        ForgotPasswordScreen(initialPhone: _phone.text))),
                child: Text(l.forgotPassword)),
          ]),
          if (_providers.google ||
              (_providers.apple && SocialSignIn.appleSupported)) ...[
            Row(children: [
              const Expanded(child: Divider()),
              Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: Text(l.or)),
              const Expanded(child: Divider())
            ]),
            const SizedBox(height: 8),
            if (_providers.google)
              OutlinedButton.icon(
                  onPressed: busy ? null : () => _social(SocialSignIn.google),
                  icon: const Icon(Icons.g_mobiledata, size: 28),
                  label: Text(l.continueWithGoogle)),
            if (_providers.apple && SocialSignIn.appleSupported)
              OutlinedButton.icon(
                  onPressed: busy ? null : () => _social(SocialSignIn.apple),
                  icon: const Icon(Icons.apple),
                  label: Text(l.continueWithApple)),
          ],
          const SizedBox(height: 4),
          Wrap(
              alignment: WrapAlignment.center,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Text(l.noAccount),
                TextButton(
                    onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => const SignUpScreen())),
                    child: Text(l.signUp)),
              ]),
        ]),
      ),
    );
  }
}
