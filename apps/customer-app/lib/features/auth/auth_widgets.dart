import 'package:flutter/services.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';

/// The dark sign-in page: the LUTIANO mark, a title and a white card for the form.
class AuthScaffold extends StatelessWidget {
  const AuthScaffold({super.key, required this.child, this.showBack = false});
  final Widget child;
  final bool showBack;

  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: AppColors.navyDeep,
        body: SafeArea(
          child: Column(children: [
            // the back arrow sits on the same dark page (no separate bar, so nothing is cut off behind it)
            SizedBox(
              height: 48,
              child: Align(
                alignment: Alignment.centerLeft,
                child: showBack
                    ? IconButton(
                        tooltip:
                            MaterialLocalizations.of(context).backButtonTooltip,
                        icon: const Icon(Icons.arrow_back, color: Colors.white),
                        onPressed: () => Navigator.of(context).maybePop())
                    : null,
              ),
            ),
            Expanded(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 420),
                    child: Column(children: [
                      const BrandMark(size: 64),
                      const SizedBox(height: 12),
                      BrandTitle(subtitle: context.l10n.coffee),
                      const SizedBox(height: 24),
                      Card(
                          child: Padding(
                              padding: const EdgeInsets.all(22), child: child)),
                    ]),
                  ),
                ),
              ),
            ),
          ]),
        ),
      );
}

/// A Cameroonian mobile number; the +237 is added by the app.
class PhoneField extends StatelessWidget {
  const PhoneField(
      {super.key,
      required this.controller,
      this.textInputAction = TextInputAction.next});
  final TextEditingController controller;
  final TextInputAction textInputAction;

  @override
  Widget build(BuildContext context) => TextFormField(
        controller: controller,
        keyboardType: TextInputType.phone,
        textInputAction: textInputAction,
        autofillHints: const [AutofillHints.telephoneNumberNational],
        inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9 ]'))],
        decoration: InputDecoration(
            hintText: context.l10n.phoneNumber,
            prefixIcon: const Padding(
                padding: EdgeInsets.only(left: 12, right: 8),
                child: Row(mainAxisSize: MainAxisSize.min, children: [
                  Icon(Icons.phone_outlined),
                  SizedBox(width: 8),
                  Text('+237', style: TextStyle(fontWeight: FontWeight.w700))
                ]))),
        validator: (v) => phoneValid(v) ? null : context.l10n.enterValidPhone,
      );
}

bool phoneValid(String? v) =>
    RegExp(r'^6\d{8}$').hasMatch((v ?? '').replaceAll(' ', ''));

/// The number as the API wants it.
String phoneValue(TextEditingController c) =>
    '+237${c.text.replaceAll(' ', '')}';

class PasswordField extends StatefulWidget {
  const PasswordField(
      {super.key,
      required this.controller,
      this.hint,
      this.onSubmitted,
      this.minLength = 0,
      this.textInputAction,
      this.optional = false,
      this.match});
  final TextEditingController controller;
  final String? hint;
  final VoidCallback? onSubmitted;
  final int minLength;
  final TextInputAction? textInputAction;

  /// An empty value is fine (a password is not required at sign-up).
  final bool optional;

  /// Must equal this field (a confirmation).
  final TextEditingController? match;

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _obscure = true;

  @override
  Widget build(BuildContext context) => TextFormField(
        controller: widget.controller,
        obscureText: _obscure,
        textInputAction: widget.textInputAction,
        onFieldSubmitted: (_) => widget.onSubmitted?.call(),
        decoration: InputDecoration(
          hintText: widget.hint ?? context.l10n.password,
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
        validator: (v) {
          if (widget.match != null) {
            return (v ?? '') == widget.match!.text
                ? null
                : context.l10n.passwordsDoNotMatch;
          }
          if (v == null || v.isEmpty) {
            return widget.optional ? null : context.l10n.enterYourPassword;
          }
          if (v.length < widget.minLength) return context.l10n.passwordTooShort;
          return null;
        },
      );
}
