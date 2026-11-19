import 'dart:math';

import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';

/// A product chosen for a request, with the quantity asked for.
class RequestLine {
  const RequestLine(
      {required this.productId,
      required this.name,
      required this.quantity,
      this.imageUrl});
  final String productId, name;
  final int quantity;
  final String? imageUrl;

  RequestLine withQuantity(int q) => RequestLine(
      productId: productId, name: name, quantity: q, imageUrl: imageUrl);
}

/// Batch 3 · Screen 3 — Request summary and submit. Pops the created request.
class RequestSummaryScreen extends StatefulWidget {
  const RequestSummaryScreen({super.key, required this.lines});
  final List<RequestLine> lines;

  @override
  State<RequestSummaryScreen> createState() => _RequestSummaryScreenState();
}

class _RequestSummaryScreenState extends State<RequestSummaryScreen>
    with SubmitState<RequestSummaryScreen> {
  final _reason = TextEditingController();
  DateTime? _neededBy;

  /// One key per summary screen: sending again after a lost answer returns the same request instead of a second one.
  final String _key =
      'sr-${DateTime.now().microsecondsSinceEpoch}-${Random().nextInt(1 << 30)}';

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
        context: context,
        helpText: context.l10n.chooseDate,
        initialDate: _neededBy ?? now,
        firstDate: now,
        lastDate: now.add(const Duration(days: 90)));
    if (picked != null && mounted) setState(() => _neededBy = picked);
  }

  Future<void> _submit() async {
    final app = AppScope.read(context);
    StockRequest? created;
    final ok = await submit(() async {
      created = await app.requests.create(app.operation!.id,
          quantities: {for (final l in widget.lines) l.productId: l.quantity},
          idempotencyKey: _key,
          neededBy: _neededBy,
          reason: _reason.text);
    });
    if (!ok || !mounted) return;
    app.dataChanged();
    Navigator.pop(context, created);
  }

  @override
  Widget build(BuildContext context) {
    final op = AppScope.of(context).operation!;
    final total = widget.lines.fold<int>(0, (s, l) => s + l.quantity);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.requestSummary)),
      // pinned, so Submit and any error stay in view however many products there are
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            if (error != null) ...[
              ErrorBanner(error!),
              const SizedBox(height: 8)
            ],
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: busy ? null : _submit,
                icon: const Icon(Icons.send),
                label: Text(busy
                    ? context.l10n.sendingRequest
                    : context.l10n.submitRequest),
              ),
            ),
          ]),
        ),
      ),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        InfoNote(context.l10n.reviewAndSubmit,
            icon: Icons.inventory_2_outlined,
            color: AppColors.navy,
            background: AppColors.goldSoft),
        const SizedBox(height: 12),
        AppCard(
          child: Column(children: [
            InfoRow(context.l10n.salesOperation, op.name),
            ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(context.l10n.requestedDelivery,
                  style: const TextStyle(color: AppColors.muted)),
              subtitle: Text(
                  _neededBy == null
                      ? context.l10n.asSoonAsPossible
                      : dayMonthYear(_neededBy!),
                  style: const TextStyle(fontWeight: FontWeight.w700)),
              trailing: _neededBy == null
                  ? const Icon(Icons.chevron_right)
                  : IconButton(
                      tooltip: context.l10n.asSoonAsPossible,
                      icon: const Icon(Icons.close),
                      onPressed: () => setState(() => _neededBy = null)),
              onTap: _pickDate,
            ),
            TextField(
                controller: _reason,
                maxLength: 500,
                decoration:
                    InputDecoration(labelText: context.l10n.reasonOptional)),
          ]),
        ),
        SectionLabel(context.l10n.requestedItems(widget.lines.length)),
        AppCard(
          child: Column(children: [
            for (final l in widget.lines)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(children: [
                  ProductImage(url: l.imageUrl, size: 40),
                  const SizedBox(width: 12),
                  Expanded(
                      child: Text(l.name,
                          style: const TextStyle(fontWeight: FontWeight.w700))),
                  Text('${l.quantity}',
                      style: const TextStyle(fontWeight: FontWeight.w800)),
                ]),
              ),
            const Divider(),
            InfoRow(context.l10n.totalQuantity, '$total', bold: true),
          ]),
        ),
      ]),
    );
  }
}
