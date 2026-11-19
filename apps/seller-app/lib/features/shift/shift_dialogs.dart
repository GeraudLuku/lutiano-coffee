import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Opens a cash shift with an opening float. Returns true when opened.
Future<bool> showOpenShiftDialog(BuildContext context) async {
  final opened = await showDialog<bool>(
      context: context, builder: (_) => const _OpenShiftDialog());
  return opened ?? false;
}

/// Closes the open shift: counted cash vs expected, reason required for any variance.
Future<void> showCloseShiftDialog(BuildContext context) async {
  final message = await showDialog<String>(
      context: context, builder: (_) => const _CloseShiftDialog());
  if (message != null && context.mounted) showMessage(context, message);
}

final _digitsOnly = [FilteringTextInputFormatter.digitsOnly];

class _OpenShiftDialog extends StatefulWidget {
  const _OpenShiftDialog();
  @override
  State<_OpenShiftDialog> createState() => _OpenShiftDialogState();
}

class _OpenShiftDialogState extends State<_OpenShiftDialog>
    with SubmitState<_OpenShiftDialog> {
  final _float = TextEditingController(text: '0');

  @override
  void dispose() {
    _float.dispose();
    super.dispose();
  }

  Future<void> _open() async {
    final ok = await submit(
        () => AppScope.read(context).openShift(int.tryParse(_float.text) ?? 0));
    if (ok && mounted) Navigator.pop(context, true);
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(context.l10n.openCashShift),
        content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(context.l10n.countTheCashInYour,
                  style: TextStyle(color: AppColors.muted)),
              const SizedBox(height: 14),
              TextField(
                  controller: _float,
                  keyboardType: TextInputType.number,
                  inputFormatters: _digitsOnly,
                  decoration: InputDecoration(
                      labelText: context.l10n.openingFloat,
                      suffixText: context.l10n.xaf)),
              if (error != null) ...[
                const SizedBox(height: 12),
                ErrorBanner(error!)
              ],
            ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: Text(context.l10n.cancel)),
          TextButton(
              onPressed: busy ? null : _open,
              child: Text(
                  busy ? context.l10n.openingShift : context.l10n.openShift)),
        ],
      );
}

class _CloseShiftDialog extends StatefulWidget {
  const _CloseShiftDialog();
  @override
  State<_CloseShiftDialog> createState() => _CloseShiftDialogState();
}

class _CloseShiftDialogState extends State<_CloseShiftDialog>
    with SubmitState<_CloseShiftDialog> {
  final _counted = TextEditingController();
  final _reason = TextEditingController();

  /// What is still open across the seller's shifts, to remind them before they leave (null while loading or if it could not be read).
  CashOverview? _overview;

  @override
  void initState() {
    super.initState();
    AppScope.read(context).cash.overview().then((o) {
      if (mounted) setState(() => _overview = o);
    }, onError: (_) {});
  }

  @override
  void dispose() {
    _counted.dispose();
    _reason.dispose();
    super.dispose();
  }

  Future<void> _close(int expected) async {
    final counted = int.tryParse(_counted.text);
    if (counted == null) {
      setState(() => error = context.l10n.enterTheCashYouCounted);
      return;
    }
    if (counted != expected && _reason.text.trim().isEmpty) {
      setState(() => error = context.l10n.explainTheDifferenceBeforeClosing);
      return;
    }
    ShiftCloseResult? result;
    final ok = await submit(() async {
      result = await AppScope.read(context).closeShift(
          counted, _reason.text.trim().isEmpty ? null : _reason.text.trim());
    });
    if (!ok || !mounted) return;
    Navigator.pop(
        context,
        result!.variance == 0
            ? context.l10n.shiftClosedMatches
            : context.l10n.shiftClosedVariance(xaf(result!.variance)));
  }

  @override
  Widget build(BuildContext context) {
    final shift = AppScope.of(context).shift;
    final expected = shift?.expectedCash ?? 0;
    final counted = int.tryParse(_counted.text);
    final variance = counted == null ? null : counted - expected;
    return AlertDialog(
      title: Text(context.l10n.closeCashShift),
      content: SingleChildScrollView(
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              InfoRow(context.l10n.openingFloat, xaf(shift?.openingFloat ?? 0)),
              InfoRow(context.l10n.expectedCash, xaf(expected), bold: true),
              // reminders before closing: undecided outflow requests are withdrawn, and unconfirmed handovers need a follow-up
              if (_overview != null && _overview!.pendingOutflows > 0) ...[
                const SizedBox(height: 10),
                InfoNote(
                    context.l10n
                        .closeRemindOutflows(_overview!.pendingOutflows),
                    icon: Icons.outbox_outlined,
                    color: AppColors.amber,
                    background: AppColors.amberSoft),
              ],
              if (_overview != null &&
                  _overview!.outstandingRemittances > 0) ...[
                const SizedBox(height: 10),
                InfoNote(
                    context.l10n.closeRemindRemittances(
                        _overview!.outstandingRemittances,
                        xaf(_overview!.outstandingRemittancesXaf)),
                    icon: Icons.payments_outlined,
                    color: AppColors.amber,
                    background: AppColors.amberSoft),
              ],
              const SizedBox(height: 10),
              TextField(
                  controller: _counted,
                  keyboardType: TextInputType.number,
                  inputFormatters: _digitsOnly,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                      labelText: context.l10n.countedCash,
                      suffixText: context.l10n.xaf)),
              if (variance != null && variance != 0) ...[
                const SizedBox(height: 10),
                Text(
                    context.l10n.varianceValue(
                        '${variance > 0 ? '+' : ''}${xaf(variance)}'),
                    style: const TextStyle(
                        color: AppColors.red, fontWeight: FontWeight.w700)),
                const SizedBox(height: 8),
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
            onPressed: () => Navigator.pop(context),
            child: Text(context.l10n.cancel)),
        TextButton(
            onPressed: busy ? null : () => _close(expected),
            child: Text(busy ? context.l10n.closing : context.l10n.closeShift)),
      ],
    );
  }
}
