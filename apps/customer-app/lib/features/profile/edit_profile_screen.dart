import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../auth/auth_widgets.dart';

/// Name, email, marketing consent and the phone number (changed with an SMS code).
class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen>
    with SubmitState<EditProfileScreen> {
  final _form = GlobalKey<FormState>();
  late final _name =
      TextEditingController(text: AppScope.read(context).customer!.fullName);
  late final _email =
      TextEditingController(text: AppScope.read(context).customer!.email ?? '');
  late bool _marketing = AppScope.read(context).customer!.marketingConsent;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    final app = AppScope.read(context);
    final me = app.customer!;
    final ok = await submit(() async {
      await app.account.update(
          fullName: _name.text.trim(),
          language: me.language,
          email: _email.text.trim().isEmpty ? null : _email.text.trim(),
          birthDate: me.birthDate,
          marketingConsent: _marketing);
      await app.reloadProfile();
    });
    if (ok && mounted) showMessage(context, context.l10n.profileSaved);
  }

  Future<void> _changePhone() async {
    final l = context.l10n;
    final app = AppScope.read(context);
    var sent = false;
    String? problem;
    await showDialog<void>(
      context: context,
      builder: (ctx) => DialogFields(
          count: 2,
          builder: (ctx, f) => StatefulBuilder(
                builder: (ctx, setLocal) => AlertDialog(
                  title: Text(l.changePhoneNumber),
                  content: Column(mainAxisSize: MainAxisSize.min, children: [
                    TextField(
                        controller: f[0],
                        enabled: !sent,
                        keyboardType: TextInputType.phone,
                        decoration: InputDecoration(
                            hintText: l.newPhoneNumber, prefixText: '+237 ')),
                    if (sent)
                      TextField(
                          controller: f[1],
                          keyboardType: TextInputType.number,
                          maxLength: 6,
                          decoration:
                              InputDecoration(hintText: l.sixDigitCode)),
                    if (problem != null)
                      Padding(
                          padding: const EdgeInsets.only(top: 8),
                          child: Text(problem!,
                              style: const TextStyle(color: AppColors.red))),
                  ]),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.of(ctx).pop(),
                        child: Text(l.cancel)),
                    FilledButton(
                      onPressed: () async {
                        try {
                          if (!sent) {
                            if (!phoneValid(f[0].text)) {
                              setLocal(() => problem = l.enterValidPhone);
                              return;
                            }
                            await app.account.startPhoneChange(
                                '+237${f[0].text.replaceAll(' ', '')}');
                            setLocal(() {
                              sent = true;
                              problem = null;
                            });
                          } else {
                            await app.account
                                .confirmPhoneChange(f[1].text.trim());
                            await app.reloadProfile();
                            if (ctx.mounted) Navigator.of(ctx).pop();
                          }
                        } catch (e) {
                          setLocal(() => problem = errorMessage(e));
                        }
                      },
                      child: Text(sent ? l.verify : l.sendCode),
                    ),
                  ],
                ),
              )),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final me = AppScope.of(context).customer!;
    return Scaffold(
      appBar: AppBar(title: Text(l.accountDetails)),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          TextFormField(
            controller: _name,
            textCapitalization: TextCapitalization.words,
            decoration: InputDecoration(labelText: l.fullName),
            validator: (v) =>
                (v == null || v.trim().length < 2) ? l.enterYourName : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            decoration: InputDecoration(labelText: l.emailOptional),
            validator: (v) => (v == null || v.trim().isEmpty || v.contains('@'))
                ? null
                : l.enterValidEmail,
          ),
          const SizedBox(height: 12),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(l.phoneNumber),
            subtitle: Text(me.phone),
            trailing:
                TextButton(onPressed: _changePhone, child: Text(l.change)),
          ),
          SwitchListTile(
              contentPadding: EdgeInsets.zero,
              value: _marketing,
              onChanged: (v) => setState(() => _marketing = v),
              title: Text(l.receiveOffers),
              subtitle: Text(l.receiveOffersHint)),
          const SizedBox(height: 12),
          if (error != null) ErrorBanner(error!),
          FilledButton(
              onPressed: busy ? null : _save,
              child: Text(busy ? l.pleaseWait : l.saveChanges)),
        ]),
      ),
    );
  }
}
