import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'order_widgets.dart';

/// One customer order: what was ordered, the customer's note, its progress, and the next step for the seller.
class CustomerOrderScreen extends StatefulWidget {
  const CustomerOrderScreen({super.key, required this.orderId});
  final String orderId;

  @override
  State<CustomerOrderScreen> createState() => _CustomerOrderScreenState();
}

class _CustomerOrderScreenState extends State<CustomerOrderScreen>
    with SubmitState<CustomerOrderScreen> {
  late Future<CustomerOrder> _order =
      AppScope.read(context).orders.get(widget.orderId);

  void _reload() => setState(
      () => _order = AppScope.read(context).orders.get(widget.orderId));

  Future<void> _run(Future<CustomerOrder> Function() action) async {
    final ok = await submit(() async {
      final updated = await action();
      _order = Future.value(updated);
    });
    if (ok && mounted) {
      setState(() {});
      AppScope.read(context).dataChanged();
    }
  }

  Future<void> _decline() async {
    final l = context.l10n;
    final text = await showDialog<String>(
      context: context,
      builder: (ctx) => DialogFields(
          builder: (ctx, f) => AlertDialog(
                title: Text(l.declineOrder),
                content: TextField(
                    controller: f[0],
                    autofocus: true,
                    maxLength: 200,
                    decoration: InputDecoration(hintText: l.declineReasonHint)),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(l.cancel)),
                  FilledButton(
                      onPressed: () => Navigator.of(ctx).pop(f[0].text.trim()),
                      child: Text(l.declineOrder)),
                ],
              )),
    );
    if (text == null || text.isEmpty || !mounted) return;
    await _run(
        () => AppScope.read(context).orders.reject(widget.orderId, text));
  }

  Future<void> _handOver(CustomerOrder o) async {
    final l = context.l10n;
    final cashText = await showDialog<String>(
      context: context,
      builder: (ctx) => DialogFields(
          initial: ['${o.totalXaf}'],
          builder: (ctx, f) => AlertDialog(
                title: Text(l.handOver),
                content: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(l.handOverMessage(xaf(o.totalXaf))),
                      const SizedBox(height: 8),
                      TextField(
                          controller: f[0],
                          keyboardType: TextInputType.number,
                          decoration: InputDecoration(
                              labelText: l.cashReceivedLabel,
                              suffixText: 'XAF')),
                    ]),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(),
                      child: Text(l.cancel)),
                  FilledButton(
                      onPressed: () => Navigator.of(ctx).pop(f[0].text),
                      child: Text(l.confirmHandOver)),
                ],
              )),
    );
    if (cashText == null || !mounted) return;
    final received = int.tryParse(cashText.trim());
    final app = AppScope.read(context);
    await _run(
        () => app.orders.collect(widget.orderId, cashReceivedXaf: received));
    if (mounted && error == null) showMessage(context, l.orderHandedOver);
  }

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.customerOrder)),
      body: FutureBuilder<CustomerOrder>(
        future: _order,
        builder: (context, snap) {
          if (snap.connectionState != ConnectionState.done) {
            return const SkeletonDetail();
          }
          if (snap.hasError) {
            return ListView(padding: const EdgeInsets.all(16), children: [
              ErrorBanner(errorMessage(snap.error!), onRetry: _reload)
            ]);
          }
          final o = snap.data!;
          return ListView(padding: const EdgeInsets.all(16), children: [
            Row(children: [
              Expanded(
                  child: Text(o.reference,
                      style: const TextStyle(
                          fontSize: 20, fontWeight: FontWeight.w800))),
              orderStatusChip(context, o.status),
            ]),
            const SizedBox(height: 4),
            Text('${o.customerName} · ${o.operationName}',
                style: const TextStyle(color: AppColors.muted)),
            if (o.note != null && o.note!.isNotEmpty) ...[
              const SizedBox(height: 12),
              InfoNote('${l.customerNote}: ${o.note}',
                  icon: Icons.sticky_note_2_outlined)
            ],
            if (o.reason != null &&
                o.reason!.isNotEmpty &&
                !o.status.isOpen &&
                o.status != OrderStatus.collected) ...[
              const SizedBox(height: 12),
              InfoNote(o.reason!,
                  color: AppColors.red, background: AppColors.redSoft)
            ],
            SectionLabel(l.orderItems),
            AppCard(
              child: Column(children: [
                for (final line in o.lines)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${line.quantity}×  ',
                              style:
                                  const TextStyle(fontWeight: FontWeight.w800)),
                          Expanded(
                              child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                Text(line.name,
                                    style: const TextStyle(
                                        fontWeight: FontWeight.w700)),
                                if (line.addons.isNotEmpty)
                                  Text(
                                      '+ ${line.addons.map((a) => a.name).join(', ')}',
                                      style: const TextStyle(
                                          color: AppColors.muted,
                                          fontSize: 12.5)),
                              ])),
                          Text(xaf(line.totalXaf)),
                        ]),
                  ),
                const Divider(),
                InfoRow(l.total, xaf(o.totalXaf), bold: true),
              ]),
            ),
            SectionLabel(l.orderProgress),
            AppCard(
              child: Column(children: [
                for (final step in o.timeline)
                  InfoRow(orderStatusLabel(context, step.status),
                      dateTime(step.at)),
              ]),
            ),
            const SizedBox(height: 16),
            if (error != null) ErrorBanner(error!),
            if (o.status == OrderStatus.placed) ...[
              FilledButton(
                  onPressed: busy
                      ? null
                      : () => _run(
                          () => AppScope.read(context).orders.accept(o.id)),
                  child: Text(l.acceptOrder)),
              OutlinedButton(
                  onPressed: busy ? null : _decline,
                  child: Text(l.declineOrder)),
            ],
            if (o.status == OrderStatus.accepted) ...[
              FilledButton(
                  onPressed: busy
                      ? null
                      : () =>
                          _run(() => AppScope.read(context).orders.ready(o.id)),
                  child: Text(l.markReady)),
              OutlinedButton(
                  onPressed: busy ? null : () => _handOver(o),
                  child: Text(l.handOver)),
              TextButton(
                  onPressed: busy ? null : _decline,
                  child: Text(l.declineOrder)),
            ],
            if (o.status == OrderStatus.ready)
              FilledButton(
                  onPressed: busy ? null : () => _handOver(o),
                  child: Text(l.handOver)),
          ]);
        },
      ),
    );
  }
}
