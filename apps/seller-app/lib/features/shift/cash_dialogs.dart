import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

final _digitsOnly = [FilteringTextInputFormatter.digitsOnly];

/// Asks for cash from the drawer for a business expense. Nothing leaves the drawer until a manager approves.
Future<bool> showOutflowDialog(BuildContext context) async =>
    await showDialog<bool>(
        context: context, builder: (_) => const _OutflowDialog()) ??
    false;

/// Records cash handed to a manager or deposited at the bank. It leaves the drawer now and stays outstanding until acknowledged.
Future<bool> showRemittanceDialog(BuildContext context, int inDrawer) async =>
    await showDialog<bool>(
        context: context,
        builder: (_) => _RemittanceDialog(inDrawer: inDrawer)) ??
    false;

/// Counts the cash of a closed shift again after a manager asked for it.
Future<bool> showRecountDialog(
        BuildContext context, CashShiftDetail shift) async =>
    await showDialog<bool>(
        context: context, builder: (_) => _RecountDialog(shift: shift)) ??
    false;

class _OutflowDialog extends StatefulWidget {
  const _OutflowDialog();
  @override
  State<_OutflowDialog> createState() => _OutflowDialogState();
}

class _OutflowDialogState extends State<_OutflowDialog>
    with SubmitState<_OutflowDialog> {
  final _amount = TextEditingController();
  final _description = TextEditingController();
  String _category = 'SUPPLIES';

  @override
  void dispose() {
    _amount.dispose();
    _description.dispose();
    super.dispose();
  }

  void _send() {
    final amount = int.tryParse(_amount.text) ?? 0;
    if (amount <= 0) {
      setState(() => error = context.l10n.enterTheAmountNeeded);
      return;
    }
    if (_description.text.trim().isEmpty) {
      setState(() => error = context.l10n.sayWhatTheCashIs);
      return;
    }
    submit(() => AppScope.read(context).cash.requestOutflow(
        amount, _category, _description.text.trim())).then((ok) {
      if (ok && mounted) Navigator.pop(context, true);
    });
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(context.l10n.requestCashOutflow),
        content: SingleChildScrollView(
          child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(context.l10n.cashLeavesYourDrawerOnly,
                    style: TextStyle(color: AppColors.muted)),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: _category,
                  decoration: InputDecoration(labelText: context.l10n.purpose),
                  items: [
                    for (final c in CashOutflow.categories)
                      DropdownMenuItem(
                          value: c,
                          child: Text(outflowCategoryLabel(context.l10n, c)))
                  ],
                  onChanged: (v) => setState(() => _category = v ?? _category),
                ),
                const SizedBox(height: 10),
                TextField(
                    controller: _amount,
                    keyboardType: TextInputType.number,
                    inputFormatters: _digitsOnly,
                    decoration: InputDecoration(
                        labelText: context.l10n.amount,
                        suffixText: context.l10n.xaf)),
                const SizedBox(height: 10),
                TextField(
                    controller: _description,
                    maxLength: 500,
                    decoration:
                        InputDecoration(labelText: context.l10n.whatIsItFor)),
                if (error != null) ErrorBanner(error!),
              ]),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(context.l10n.cancel)),
          TextButton(
              onPressed: busy ? null : _send,
              child: Text(busy ? context.l10n.sending : context.l10n.request)),
        ],
      );
}

class _RemittanceDialog extends StatefulWidget {
  const _RemittanceDialog({required this.inDrawer});
  final int inDrawer;
  @override
  State<_RemittanceDialog> createState() => _RemittanceDialogState();
}

class _RemittanceDialogState extends State<_RemittanceDialog>
    with SubmitState<_RemittanceDialog> {
  final _amount = TextEditingController();
  final _bankReference = TextEditingController();
  String _method = 'HANDOVER';
  String? _recipientId;
  List<CashRecipient> _recipients = const [];

  @override
  void initState() {
    super.initState();
    AppScope.read(context).cash.recipients().then((r) {
      if (mounted) setState(() => _recipients = r);
    }).catchError((Object e) {
      if (mounted) setState(() => error = '$e');
    });
  }

  @override
  void dispose() {
    _amount.dispose();
    _bankReference.dispose();
    super.dispose();
  }

  void _send() {
    final amount = int.tryParse(_amount.text) ?? 0;
    if (amount <= 0) {
      setState(() => error = context.l10n.enterTheAmountYouAre);
      return;
    }
    if (amount > widget.inDrawer) {
      setState(
          () => error = context.l10n.drawerShouldHold(xaf(widget.inDrawer)));
      return;
    }
    if (_method == 'HANDOVER' && _recipientId == null) {
      setState(() => error = context.l10n.chooseWhoReceivesTheCash);
      return;
    }
    if (_method == 'BANK_DEPOSIT' && _bankReference.text.trim().isEmpty) {
      setState(() => error = context.l10n.enterTheDepositSlipReference);
      return;
    }
    submit(() => AppScope.read(context).cash.remit(
        amountXaf: amount,
        method: _method,
        recipientId: _method == 'HANDOVER' ? _recipientId : null,
        bankReference: _method == 'BANK_DEPOSIT'
            ? _bankReference.text.trim()
            : null)).then((ok) {
      if (ok && mounted) Navigator.pop(context, true);
    });
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(context.l10n.recordRemittance),
        content: SingleChildScrollView(
          child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(context.l10n.inYourDrawer(xaf(widget.inDrawer)),
                    style: const TextStyle(color: AppColors.muted)),
                const SizedBox(height: 10),
                SegmentedButton<String>(
                  segments: [
                    ButtonSegment(
                        value: 'HANDOVER',
                        label: Text(context.l10n.handToManager),
                        icon: Icon(Icons.handshake_outlined)),
                    ButtonSegment(
                        value: 'BANK_DEPOSIT',
                        label: Text(context.l10n.bankDeposit),
                        icon: Icon(Icons.account_balance_outlined)),
                  ],
                  selected: {_method},
                  onSelectionChanged: (s) => setState(() => _method = s.first),
                ),
                const SizedBox(height: 12),
                TextField(
                    controller: _amount,
                    keyboardType: TextInputType.number,
                    inputFormatters: _digitsOnly,
                    decoration: InputDecoration(
                        labelText: context.l10n.amount,
                        suffixText: context.l10n.xaf)),
                const SizedBox(height: 10),
                if (_method == 'HANDOVER')
                  DropdownButtonFormField<String>(
                    initialValue: _recipientId,
                    decoration:
                        InputDecoration(labelText: context.l10n.handedTo),
                    items: [
                      for (final r in _recipients)
                        DropdownMenuItem(value: r.id, child: Text(r.name))
                    ],
                    onChanged: (v) => setState(() => _recipientId = v),
                  )
                else
                  TextField(
                      controller: _bankReference,
                      maxLength: 80,
                      decoration: InputDecoration(
                          labelText: context.l10n.depositSlipReference)),
                const SizedBox(height: 8),
                InfoNote(context.l10n.theCashLeavesYourDrawer),
                if (error != null) ...[
                  const SizedBox(height: 8),
                  ErrorBanner(error!)
                ],
              ]),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(context.l10n.cancel)),
          TextButton(
              onPressed: busy ? null : _send,
              child: Text(busy ? context.l10n.recording : context.l10n.record)),
        ],
      );
}

class _RecountDialog extends StatefulWidget {
  const _RecountDialog({required this.shift});
  final CashShiftDetail shift;
  @override
  State<_RecountDialog> createState() => _RecountDialogState();
}

class _RecountDialogState extends State<_RecountDialog>
    with SubmitState<_RecountDialog> {
  final _counted = TextEditingController();
  final _reason = TextEditingController();

  @override
  void dispose() {
    _counted.dispose();
    _reason.dispose();
    super.dispose();
  }

  void _send() {
    final counted = int.tryParse(_counted.text);
    if (counted == null) {
      setState(() => error = context.l10n.enterTheCashYouCounted);
      return;
    }
    final reason = _reason.text.trim();
    if (counted != widget.shift.expectedCash && reason.isEmpty) {
      setState(() => error = context.l10n.explainTheDifference);
      return;
    }
    submit(() => AppScope.read(context).cash.recount(
        widget.shift.id, counted, reason.isEmpty ? null : reason)).then((ok) {
      if (ok && mounted) Navigator.pop(context, true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final counted = int.tryParse(_counted.text);
    final variance =
        counted == null ? null : counted - widget.shift.expectedCash;
    return AlertDialog(
      title: Text(context.l10n.recountCash),
      content: SingleChildScrollView(
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (widget.shift.reviewNote != null)
                InfoNote(context.l10n.managerNote(widget.shift.reviewNote!)),
              InfoRow(context.l10n.expectedCash, xaf(widget.shift.expectedCash),
                  bold: true),
              InfoRow(
                  context.l10n.yourFirstCount,
                  widget.shift.countedCash == null
                      ? '—'
                      : xaf(widget.shift.countedCash!)),
              TextField(
                  controller: _counted,
                  keyboardType: TextInputType.number,
                  inputFormatters: _digitsOnly,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                      labelText: context.l10n.countedCashNow,
                      suffixText: context.l10n.xaf)),
              if (variance != null && variance != 0) ...[
                const SizedBox(height: 8),
                Text(
                    context.l10n.varianceValue(
                        '${variance > 0 ? '+' : ''}${xaf(variance)}'),
                    style: const TextStyle(
                        color: AppColors.red, fontWeight: FontWeight.w700)),
                TextField(
                    controller: _reason,
                    maxLength: 500,
                    decoration: InputDecoration(
                        labelText: context.l10n.reasonForTheDifference)),
              ],
              if (error != null) ...[
                const SizedBox(height: 8),
                ErrorBanner(error!)
              ],
            ]),
      ),
      actions: [
        TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(context.l10n.cancel)),
        TextButton(
            onPressed: busy ? null : _send,
            child:
                Text(busy ? context.l10n.saving : context.l10n.submitRecount)),
      ],
    );
  }
}
