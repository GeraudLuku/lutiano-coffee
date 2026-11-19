import 'dart:async';

import 'package:flutter/services.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import 'auth_flow.dart';
import 'auth_widgets.dart';

/// Asks for the 6-digit code sent by SMS. [onVerify] checks it and answers the next step; [onResend] sends a new code.
class CodeScreen extends StatefulWidget {
  const CodeScreen(
      {super.key,
      required this.phoneHint,
      required this.onVerify,
      this.onResend,
      this.verifyLabel});

  /// Where the code went (masked or the full number), shown in the text.
  final String phoneHint;
  final Future<AuthStep> Function(String code) onVerify;
  final Future<void> Function()? onResend;
  final String? verifyLabel;

  @override
  State<CodeScreen> createState() => _CodeScreenState();
}

class _CodeScreenState extends State<CodeScreen> with SubmitState<CodeScreen> {
  final _code = TextEditingController();
  Timer? _timer;
  int _wait = 60;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    _wait = 60;
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (_wait <= 1) t.cancel();
      if (mounted) setState(() => _wait--);
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _code.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    if (_code.text.length != 6) return;
    AuthStep? step;
    final ok =
        await submit(() async => step = await widget.onVerify(_code.text));
    if (ok && step != null && mounted) await continueAuth(context, step!);
  }

  Future<void> _resend() async {
    final ok = await submit(() => widget.onResend!());
    if (ok && mounted) {
      _startTimer();
      showMessage(context, context.l10n.codeSent);
    }
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return AuthScaffold(
      showBack: true,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text(l.verifyYourPhone,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800)),
        const SizedBox(height: 6),
        Text(l.enterCodeSentTo(widget.phoneHint),
            style: const TextStyle(color: AppColors.muted)),
        const SizedBox(height: 18),
        TextField(
          controller: _code,
          autofocus: true,
          keyboardType: TextInputType.number,
          textAlign: TextAlign.center,
          maxLength: 6,
          style: const TextStyle(
              fontSize: 26, letterSpacing: 8, fontWeight: FontWeight.w800),
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          onChanged: (_) => setState(() {}),
          onSubmitted: (_) => _verify(),
          decoration: const InputDecoration(counterText: ''),
        ),
        const SizedBox(height: 12),
        if (error != null) ErrorBanner(error!),
        FilledButton(
            onPressed: busy || _code.text.length != 6 ? null : _verify,
            child: Text(busy ? l.verifying : (widget.verifyLabel ?? l.verify))),
        if (widget.onResend != null)
          TextButton(
              onPressed: busy || _wait > 0 ? null : _resend,
              child: Text(_wait > 0 ? l.resendIn(_wait) : l.resendCode)),
      ]),
    );
  }
}
