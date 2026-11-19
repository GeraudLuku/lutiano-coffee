import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../profile/legal_screen.dart';
import 'auth_widgets.dart';
import 'code_screen.dart';

/// Create an account: name, phone (confirmed by an SMS code), optional email and password.
class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen>
    with SubmitState<SignUpScreen> {
  final _form = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  bool _terms = false;
  bool _termsError = false;

  @override
  void dispose() {
    for (final c in [_name, _phone, _email, _password, _confirm]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _termsError = !_terms);
    if (!_form.currentState!.validate() || !_terms) return;
    final app = AppScope.read(context);
    final phone = phoneValue(_phone);
    final language = Localizations.localeOf(context).languageCode;
    final ok = await submit(() => app.auth.signUp(
        fullName: _name.text.trim(),
        phone: phone,
        language: language,
        email: _email.text.trim(),
        password: _password.text));
    if (!ok || !mounted) return;
    await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => CodeScreen(
            phoneHint: phone,
            verifyLabel: context.l10n.verifyAccount,
            onVerify: (code) => app.auth.verifySignUp(phone, code),
            onResend: () => app.auth.signUp(
                fullName: _name.text.trim(),
                phone: phone,
                language: language,
                email: _email.text.trim(),
                password: _password.text))));
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
          Text(l.createYourAccount,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          Text(l.joinLutiano, style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 18),
          TextFormField(
            controller: _name,
            textCapitalization: TextCapitalization.words,
            textInputAction: TextInputAction.next,
            decoration: InputDecoration(
                hintText: l.fullName,
                prefixIcon: const Icon(Icons.person_outline)),
            validator: (v) =>
                (v == null || v.trim().length < 2) ? l.enterYourName : null,
          ),
          const SizedBox(height: 12),
          PhoneField(controller: _phone),
          const SizedBox(height: 12),
          TextFormField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.next,
            decoration: InputDecoration(
                hintText: l.emailOptional,
                prefixIcon: const Icon(Icons.mail_outline)),
            validator: (v) => (v == null || v.trim().isEmpty || v.contains('@'))
                ? null
                : l.enterValidEmail,
          ),
          const SizedBox(height: 12),
          PasswordField(
              controller: _password,
              hint: l.passwordOptional,
              minLength: 10,
              textInputAction: TextInputAction.next,
              optional: true),
          const SizedBox(height: 12),
          PasswordField(
              controller: _confirm,
              hint: l.confirmPassword,
              optional: true,
              match: _password,
              onSubmitted: _submit),
          const SizedBox(height: 8),
          Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Checkbox(
                value: _terms,
                onChanged: (v) => setState(() => _terms = v ?? false)),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.only(top: 10),
                child: Wrap(children: [
                  Text('${l.iAgreeTo} '),
                  GestureDetector(
                      onTap: () => Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => LegalScreen(
                              title: l.termsOfUse, body: l.termsBody))),
                      child: Text(l.termsOfUse,
                          style: const TextStyle(
                              color: AppColors.blue,
                              decoration: TextDecoration.underline))),
                  Text(' ${l.and} '),
                  GestureDetector(
                      onTap: () => Navigator.of(context).push(MaterialPageRoute(
                          builder: (_) => LegalScreen(
                              title: l.privacyPolicy, body: l.privacyBody))),
                      child: Text(l.privacyPolicy,
                          style: const TextStyle(
                              color: AppColors.blue,
                              decoration: TextDecoration.underline))),
                ]),
              ),
            ),
          ]),
          if (_termsError)
            Text(l.acceptTermsToContinue,
                style: const TextStyle(color: AppColors.red, fontSize: 12.5)),
          const SizedBox(height: 12),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : _submit,
              child: Text(busy ? l.pleaseWait : l.createAccount)),
        ]),
      ),
    );
  }
}
