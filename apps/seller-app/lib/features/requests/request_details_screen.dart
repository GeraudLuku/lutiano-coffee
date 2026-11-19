import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import 'new_request_screen.dart';
import 'request_status.dart';
import 'request_summary_screen.dart';

/// Batch 3 · Screens 5 to 8 — one request: its information and items, and where it stands
/// (waiting, approved, on its way and allocated, or rejected with the reason).
class RequestDetailsScreen extends StatelessWidget {
  const RequestDetailsScreen({super.key, required this.requestId});
  final String requestId;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.requestDetails)),
      body: AsyncView<StockRequest>(
        key: ValueKey('request-$requestId-${app.dataVersion}'),
        load: () => app.requests.get(requestId),
        builder: (context, r, _) =>
            ListView(padding: const EdgeInsets.all(16), children: [
          AppCard(
            child: Row(children: [
              const Icon(Icons.inventory_2_outlined,
                  color: AppColors.gold, size: 30),
              const SizedBox(width: 12),
              Expanded(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                    Text(r.reference,
                        style: const TextStyle(
                            fontWeight: FontWeight.w800, fontSize: 16)),
                    Text(context.l10n.requestedOnWhen(dateTime(r.createdAt)),
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                  ])),
              requestStatusChip(context, r.status),
            ]),
          ),
          SectionLabel(context.l10n.requestInformation),
          AppCard(
            child: Column(children: [
              InfoRow(context.l10n.salesOperation, r.operationName),
              InfoRow(
                  context.l10n.requestedDelivery,
                  r.neededBy == null
                      ? context.l10n.asSoonAsPossible
                      : dayMonthYear(r.neededBy!)),
              if (r.reason != null)
                InfoRow(context.l10n.requestReason, r.reason!),
            ]),
          ),
          SectionLabel(context.l10n.requestedItems(r.items.length)),
          AppCard(
            child: Column(children: [
              for (final i in r.items) _ItemRow(item: i, request: r),
              const Divider(),
              InfoRow(context.l10n.totalQuantity, '${r.totalQuantity}',
                  bold: true),
            ]),
          ),
          const SizedBox(height: 12),
          _StatusPanel(request: r),
          if (_canDuplicate(context, r)) ...[
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: () => _duplicate(context, r),
              icon: const Icon(Icons.copy_outlined),
              label: Text(context.l10n.duplicateAndModify),
            ),
          ],
        ]),
      ),
    );
  }

  bool _canDuplicate(BuildContext context, StockRequest r) =>
      AppScope.read(context).user?.canRequestStock == true &&
      r.status != RequestStatus.pending;

  Future<void> _duplicate(BuildContext context, StockRequest r) async {
    final navigator = Navigator.of(context);
    final app = AppScope.read(context);
    final created = await navigator.push<StockRequest>(MaterialPageRoute(
        builder: (_) => NewRequestScreen(initial: [
              for (final i in r.items)
                RequestLine(
                    productId: i.productId,
                    name: i.name,
                    imageUrl: i.imageUrl,
                    quantity: i.quantity)
            ])));
    if (created == null) return;
    app.dataChanged();
    navigator.pushReplacement(MaterialPageRoute(
        builder: (_) => RequestDetailsScreen(requestId: created.id)));
  }
}

class _ItemRow extends StatelessWidget {
  const _ItemRow({required this.item, required this.request});
  final RequestItem item;
  final StockRequest request;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(children: [
          ProductImage(url: item.imageUrl, size: 40),
          const SizedBox(width: 12),
          Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                Text(item.name,
                    style: const TextStyle(fontWeight: FontWeight.w700)),
                if (item.receivedQuantity != null)
                  Text(
                      context.l10n.receivedOfSent(
                          item.receivedQuantity!, item.quantity),
                      style: const TextStyle(
                          color: AppColors.muted, fontSize: 12.5)),
              ])),
          Text('${item.quantity}',
              style: const TextStyle(fontWeight: FontWeight.w800)),
        ]),
      );
}

/// What the status means and what happens next.
class _StatusPanel extends StatelessWidget {
  const _StatusPanel({required this.request});
  final StockRequest request;

  @override
  Widget build(BuildContext context) {
    final r = request;
    final l = context.l10n;
    return switch (r.status) {
      RequestStatus.pending => InfoNote(l.awaitingReview),
      RequestStatus.approved => Column(children: [
          InfoNote(
              '${l.approvedOnWhen(dateTime(r.approvedAt ?? r.createdAt))}\n${l.stockBeingPrepared}',
              icon: Icons.check_circle_outline,
              color: AppColors.green,
              background: AppColors.greenSoft),
          const SizedBox(height: 12),
          _Progress(request: r),
        ]),
      RequestStatus.inFulfillment => Column(children: [
          _Progress(request: r),
          const SizedBox(height: 12),
          InfoNote(l.confirmDeliveriesNote),
        ]),
      RequestStatus.completed => Column(children: [
          InfoNote(l.completedOnWhen(dateTime(r.completedAt ?? r.createdAt)),
              icon: Icons.check_circle_outline,
              color: AppColors.green,
              background: AppColors.greenSoft),
          const SizedBox(height: 12),
          _Progress(request: r),
        ]),
      RequestStatus.rejected || RequestStatus.cancelled => InfoNote(
          [
            r.status == RequestStatus.rejected
                ? l.rejectedOnWhen(dateTime(r.closedAt ?? r.createdAt))
                : l.cancelledOnWhen(dateTime(r.closedAt ?? r.createdAt)),
            if (r.decisionNote != null) l.requestReasonValue(r.decisionNote!),
          ].join('\n'),
          icon: Icons.cancel_outlined,
          color: AppColors.red,
          background: AppColors.redSoft),
    };
  }
}

/// Approved → on its way → allocated, with the date of each step reached.
class _Progress extends StatelessWidget {
  const _Progress({required this.request});
  final StockRequest request;

  @override
  Widget build(BuildContext context) {
    final r = request;
    final l = context.l10n;
    final steps = [
      (l.stepApproved, r.approvedAt),
      (l.stepOnItsWay, r.dispatchedAt),
      (l.stepAllocated, r.completedAt),
    ];
    return AppCard(
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(l.fulfillmentProgress,
            style: const TextStyle(fontWeight: FontWeight.w800)),
        const SizedBox(height: 12),
        Row(children: [
          for (var i = 0; i < steps.length; i++) ...[
            Expanded(
                child: Column(children: [
              Icon(
                  steps[i].$2 != null
                      ? Icons.check_circle
                      : Icons.radio_button_unchecked,
                  color:
                      steps[i].$2 != null ? AppColors.green : AppColors.line),
              const SizedBox(height: 4),
              Text(steps[i].$1,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                      fontWeight: FontWeight.w700, fontSize: 12.5)),
              Text(steps[i].$2 == null ? '—' : dayMonth(steps[i].$2!),
                  style: const TextStyle(color: AppColors.muted, fontSize: 12)),
            ])),
          ],
        ]),
      ]),
    );
  }
}
