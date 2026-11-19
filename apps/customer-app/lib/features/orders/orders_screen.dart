import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import '../shell/main_shell.dart';
import 'order_detail_screen.dart';
import 'order_widgets.dart';

/// My orders: all of them, or the pending, completed and cancelled ones.
class OrdersScreen extends StatefulWidget {
  const OrdersScreen({super.key});

  @override
  State<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends State<OrdersScreen> {
  String? _group;

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.myOrders)),
      body: Column(children: [
        FilterChips<String>(options: [
          ('PENDING', l.filterPending),
          ('COMPLETED', l.filterCompleted),
          ('CANCELLED', l.filterCancelled)
        ], selected: _group, onSelected: (g) => setState(() => _group = g)),
        Expanded(
          child: PagedListView<Order>(
            key: ValueKey('orders-$_group-${app.ordersVersion}'),
            fetch: (page, size) =>
                app.orders.page(group: _group, page: page, size: size),
            empty: EmptyState(
                icon: Icons.receipt_long_outlined,
                title: l.noOrdersYet,
                message: l.noOrdersMessage,
                action: FilledButton(
                    onPressed: () => MainShell.goTo(context, MainShell.menu),
                    child: Text(l.browseMenu))),
            itemBuilder: (context, o, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => OrderDetailScreen(orderId: o.id))),
                child: Row(children: [
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(o.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(o.operationName,
                            style: const TextStyle(color: AppColors.muted)),
                        Text(dateTime(o.placedAt),
                            style: const TextStyle(
                                color: AppColors.muted, fontSize: 12.5)),
                      ])),
                  Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                    Text(xaf(o.totalXaf),
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    const SizedBox(height: 4),
                    orderStatusChip(context, o.status),
                  ]),
                ]),
              ),
            ),
          ),
        ),
      ]),
    );
  }
}
