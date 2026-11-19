import 'package:material_ui/material_ui.dart';
import 'package:flutter/services.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'payment_method_tile.dart';
import 'sale_complete_screen.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 8 — Payment Collection. Cash is the only method for now; Mobile Money is shown greyed out
/// until its integration exists. The seller confirms cash received; the server validates prices and stock
/// and completes the sale exactly once.
class PaymentScreen extends StatefulWidget {
  const PaymentScreen({super.key});

  @override
  State<PaymentScreen> createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen>
    with SubmitState<PaymentScreen> {
  late final TextEditingController _received;
  List<PaymentMethodOption> _methods = PaymentMethodOption.fallback;
  String _method = 'CASH';

  @override
  void initState() {
    super.initState();
    _received =
        TextEditingController(text: '${AppScope.read(context).cart.totalXaf}');
    _loadMethods();
  }

  /// Keeps the built-in list if the server can't be reached; cash stays selectable either way.
  Future<void> _loadMethods() async {
    try {
      final methods = await AppScope.read(context).sales.paymentMethods();
      if (!mounted || methods.isEmpty) return;
      setState(() {
        _methods = methods;
        if (!methods.any((m) => m.code == _method && m.usable)) {
          _method = methods
              .firstWhere((m) => m.usable, orElse: () => methods.first)
              .code;
        }
      });
    } catch (_) {/* fallback list stays */}
  }

  @override
  void dispose() {
    _received.dispose();
    super.dispose();
  }

  /// Exact amount plus the next round notes a customer is likely to hand over.
  List<int> _suggestions(int total) {
    final out = <int>{total};
    for (final step in [1000, 5000, 10000]) {
      final up = ((total + step - 1) ~/ step) * step;
      if (up > total) out.add(up);
    }
    return out.toList()..sort();
  }

  Future<void> _confirm() async {
    final app = AppScope.read(context);
    final cart = app.cart;
    final received = int.tryParse(_received.text);
    if (received == null || received < cart.totalXaf) {
      setState(
          () => error = context.l10n.cashReceivedAtLeast(xaf(cart.totalXaf)));
      return;
    }
    Sale? sale;
    // The cart and its idempotency key are kept on failure, so retrying cannot record the sale twice.
    final ok = await submit(() async {
      sale = await app.sales.create(
        operationId: app.operation!.id,
        lines: cart.quantities,
        cashReceivedXaf: received,
        idempotencyKey: cart.checkoutKey,
        paymentMethod: _method,
        customerToken: cart.customerToken,
      );
      cart.clear();
      app.dataChanged();
      await app.refreshShift();
    },
        describe: (e) => e is ApiException && e.isNetwork
            ? context.l10n.messageCartSavedDraft(e.message)
            : errorMessage(e));
    if (!ok || !mounted) return;
    Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => SaleCompleteScreen(sale: sale!)));
  }

  @override
  Widget build(BuildContext context) {
    final cart = AppScope.of(context).cart;
    final total = cart.totalXaf;
    final received = int.tryParse(_received.text);
    final change = received == null ? null : received - total;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.collectPayment)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        HighlightBox(label: context.l10n.totalAmount, value: xaf(total)),
        SectionLabel(context.l10n.paymentMethod2),
        for (final m in _methods)
          PaymentMethodTile(
              method: m,
              selected: m.code == _method,
              onTap: () => setState(() => _method = m.code)),
        SectionLabel(context.l10n.amountReceivedCash),
        TextField(
          controller: _received,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          onChanged: (_) => setState(() {}),
          decoration: InputDecoration(suffixText: context.l10n.xaf),
          style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 10),
        Wrap(spacing: 8, children: [
          for (final amount in _suggestions(total))
            ActionChip(
                label: Text(groupDigits(amount)),
                onPressed: () => setState(() => _received.text = '$amount')),
        ]),
        const SizedBox(height: 14),
        AppCard(
          color: change != null && change >= 0
              ? AppColors.greenSoft
              : AppColors.redSoft,
          child: Row(children: [
            Expanded(
                child: Text(context.l10n.changeToGive,
                    style: TextStyle(fontWeight: FontWeight.w700))),
            Text(change == null || change < 0 ? '—' : xaf(change),
                style:
                    const TextStyle(fontSize: 18, fontWeight: FontWeight.w800)),
          ]),
        ),
        const SizedBox(height: 14),
        if (error != null) ErrorBanner(error!),
        InfoNote(context.l10n.confirmOnlyAfterYouHave),
      ]),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: FilledButton.icon(
            onPressed: busy || cart.isEmpty ? null : _confirm,
            icon: const Icon(Icons.check),
            label: Text(busy
                ? context.l10n.recordingSale
                : context.l10n.confirmPayment),
          ),
        ),
      ),
    );
  }
}
