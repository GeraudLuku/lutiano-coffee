import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Batch 3 — Receipt confirmation: the seller confirms what actually arrived. Stock becomes available
/// only after confirmation, and any shortfall is recorded as a discrepancy with a reason.
class IncomingStockScreen extends StatelessWidget {
  const IncomingStockScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.receiveStock)),
      body: op == null
          ? EmptyState(
              icon: Icons.local_shipping_outlined,
              title: context.l10n.noSalesOperationAssigned)
          : PagedListView<IncomingAllocation>(
              key: ValueKey('incoming-${op.id}-${app.dataVersion}'),
              fetch: (page, size) =>
                  app.inventory.incomingPage(op.id, page: page, size: size),
              header: (context, _) => Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child:
                      InfoNote(context.l10n.countWhatArrivedBeforeConfirming)),
              empty: EmptyState(
                  icon: Icons.local_shipping_outlined,
                  title: context.l10n.noDeliveriesWaiting,
                  message: context.l10n.stockSentToYourOperation),
              itemBuilder: (context, a, _, __) => Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: AppCard(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(children: [
                          ProductImage(url: a.imageUrl, size: 52),
                          const SizedBox(width: 12),
                          Expanded(
                              child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                Text(a.productName,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w800)),
                                Text(
                                    context.l10n.unitsSentAt(a.sentQuantity,
                                        dateTime(a.dispatchedAt)),
                                    style: const TextStyle(
                                        color: AppColors.muted,
                                        fontSize: 12.5)),
                              ])),
                          StatusChip(context.l10n.inTransit,
                              color: AppColors.blue,
                              background: AppColors.blueSoft),
                        ]),
                        const SizedBox(height: 10),
                        FilledButton(
                            onPressed: () => _confirm(context, a),
                            child: Text(context.l10n.confirmReceipt)),
                      ]),
                ),
              ),
            ),
    );
  }

  Future<void> _confirm(BuildContext context, IncomingAllocation a) async {
    final message = await showDialog<String>(
        context: context, builder: (_) => _ConfirmDialog(allocation: a));
    if (message == null || !context.mounted) return;
    AppScope.read(context).dataChanged();
    showMessage(context, message);
  }
}

class _ConfirmDialog extends StatefulWidget {
  const _ConfirmDialog({required this.allocation});
  final IncomingAllocation allocation;

  @override
  State<_ConfirmDialog> createState() => _ConfirmDialogState();
}

class _ConfirmDialogState extends State<_ConfirmDialog>
    with SubmitState<_ConfirmDialog> {
  late final TextEditingController _qty =
      TextEditingController(text: '${widget.allocation.sentQuantity}');
  final _note = TextEditingController();

  @override
  void dispose() {
    _qty.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final sent = widget.allocation.sentQuantity;
    final qty = int.tryParse(_qty.text);
    if (qty == null || qty < 0 || qty > sent) {
      setState(() => error = context.l10n.enterQuantityBetween(sent));
      return;
    }
    if (qty != sent && _note.text.trim().isEmpty) {
      setState(() => error = context.l10n.explainWhyTheQuantityIs);
      return;
    }
    var discrepancy = false;
    final ok = await submit(() async {
      discrepancy = await AppScope.read(context).inventory.confirmReceipt(
          widget.allocation.id, qty,
          note: _note.text.trim().isEmpty ? null : _note.text.trim());
    });
    if (!ok || !mounted) return;
    Navigator.pop(
        context,
        discrepancy
            ? context.l10n.receiptConfirmedDifference
            : context.l10n.receiptConfirmedUnits(qty));
  }

  @override
  Widget build(BuildContext context) {
    final sent = widget.allocation.sentQuantity;
    final differs = int.tryParse(_qty.text) != sent;
    return AlertDialog(
      title: Text(context.l10n.confirmProduct(widget.allocation.productName)),
      content: SingleChildScrollView(
        child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(context.l10n.unitsDispatched(sent)),
              const SizedBox(height: 12),
              TextField(
                  controller: _qty,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                      labelText: context.l10n.quantityReceived)),
              if (differs) ...[
                const SizedBox(height: 10),
                TextField(
                    controller: _note,
                    maxLength: 500,
                    decoration: InputDecoration(
                        labelText: context.l10n.reasonMissingDamaged)),
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
            onPressed: busy ? null : _submit,
            child: Text(busy ? context.l10n.saving : context.l10n.confirm)),
      ],
    );
  }
}
