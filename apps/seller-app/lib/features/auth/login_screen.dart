import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'forgot_password_screen.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 1 — Login / Authentication.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SubmitState<LoginScreen> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate()) return;
    await submit(
        () => AppScope.read(context).signIn(_email.text, _password.text));
  }

  @override
  Widget build(BuildContext context) {
    final expired = AppScope.of(context).sessionExpired;
    return Scaffold(
      backgroundColor: AppColors.navyDeep,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(children: [
                const BrandMark(size: 64),
                const SizedBox(height: 12),
                BrandTitle(subtitle: context.l10n.coffeeBusinessOs),
                const SizedBox(height: 28),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(22),
                    child: Form(
                      key: _form,
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Text(context.l10n.welcomeBack,
                                style: TextStyle(
                                    fontSize: 22, fontWeight: FontWeight.w800)),
                            Text(context.l10n.signInToYourSeller,
                                style: TextStyle(color: AppColors.muted)),
                            const SizedBox(height: 18),
                            if (expired && error == null) ...[
                              InfoNote(context.l10n.yourSessionEndedPleaseSign),
                              const SizedBox(height: 12)
                            ],
                            TextFormField(
                              controller: _email,
                              keyboardType: TextInputType.emailAddress,
                              autofillHints: const [AutofillHints.username],
                              textInputAction: TextInputAction.next,
                              decoration: InputDecoration(
                                  hintText: context.l10n.email,
                                  prefixIcon: Icon(Icons.mail_outline)),
                              validator: (v) => (v == null || !v.contains('@'))
                                  ? context.l10n.enterYourEmailShort
                                  : null,
                            ),
                            const SizedBox(height: 12),
                            TextFormField(
                              controller: _password,
                              obscureText: _obscure,
                              autofillHints: const [AutofillHints.password],
                              onFieldSubmitted: (_) => _submit(),
                              decoration: InputDecoration(
                                hintText: context.l10n.password,
                                prefixIcon: const Icon(Icons.lock_outline),
                                suffixIcon: IconButton(
                                  tooltip: _obscure
                                      ? context.l10n.showPassword
                                      : context.l10n.hidePassword,
                                  icon: Icon(_obscure
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined),
                                  onPressed: () =>
                                      setState(() => _obscure = !_obscure),
                                ),
                              ),
                              validator: (v) => (v == null || v.isEmpty)
                                  ? context.l10n.enterYourPassword
                                  : null,
                            ),
                            const SizedBox(height: 16),
                            if (error != null) ErrorBanner(error!),
                            FilledButton(
                                onPressed: busy ? null : _submit,
                                child: Text(busy
                                    ? context.l10n.signingIn
                                    : context.l10n.signIn)),
                            TextButton(
                              onPressed: () => Navigator.of(context).push(
                                  MaterialPageRoute(
                                      builder: (_) => ForgotPasswordScreen(
                                          initialEmail: _email.text))),
                              child: Text(context.l10n.forgotPassword2),
                            ),
                          ]),
                    ),
                  ),
                ),
              ]),
            ),
          ),
        ),
      ),
    );
  }
}
