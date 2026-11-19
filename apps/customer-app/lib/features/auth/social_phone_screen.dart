import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'auth_widgets.dart';
import 'code_screen.dart';

/// A first Google/Apple sign-in: orders and rewards go with the phone number, so it is asked for and confirmed by SMS.
class SocialPhoneScreen extends StatefulWidget {
  const SocialPhoneScreen({super.key, required this.ticket, this.name});
  final String ticket;
  final String? name;

  @override
  State<SocialPhoneScreen> createState() => _SocialPhoneScreenState();
}

class _SocialPhoneScreenState extends State<SocialPhoneScreen>
    with SubmitState<SocialPhoneScreen> {
  final _form = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.name ?? '');
  final _phone = TextEditingController();
  bool _terms = false;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_form.currentState!.validate() || !_terms) {
      setState(() {});
      return;
    }
    final app = AppScope.read(context);
    final phone = phoneValue(_phone);
    final language = Localizations.localeOf(context).languageCode;
    Future<void> send() =>
        app.auth.socialPhone(widget.ticket, phone, _name.text.trim(), language);
    final ok = await submit(send);
    if (!ok || !mounted) return;
    await Navigator.of(context).push(MaterialPageRoute(
        builder: (_) => CodeScreen(
            phoneHint: phone,
            onVerify: (code) => app.auth.socialVerify(widget.ticket, code),
            onResend: send)));
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
          Text(l.oneLastStep,
              style:
                  const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
          Text(l.addYourPhoneWhy,
              style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 18),
          TextFormField(
            controller: _name,
            textCapitalization: TextCapitalization.words,
            decoration: InputDecoration(
                hintText: l.fullName,
                prefixIcon: const Icon(Icons.person_outline)),
            validator: (v) =>
                (v == null || v.trim().length < 2) ? l.enterYourName : null,
          ),
          const SizedBox(height: 12),
          PhoneField(controller: _phone, textInputAction: TextInputAction.done),
          const SizedBox(height: 8),
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            controlAffinity: ListTileControlAffinity.leading,
            value: _terms,
            onChanged: (v) => setState(() => _terms = v ?? false),
            title: Text(l.iAgreeToTermsAndPrivacy,
                style: const TextStyle(fontSize: 14)),
            subtitle: !_terms && _form.currentState != null
                ? Text(l.acceptTermsToContinue,
                    style: const TextStyle(color: AppColors.red))
                : null,
          ),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : _submit,
              child: Text(busy ? l.pleaseWait : l.sendCode)),
        ]),
      ),
    );
  }
}
