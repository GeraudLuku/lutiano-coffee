import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'customer_order_screen.dart';
import 'order_widgets.dart';

/// Orders customers placed at this seller's sales points: those still to handle first.
class CustomerOrdersScreen extends StatefulWidget {
  const CustomerOrdersScreen({super.key});

  @override
  State<CustomerOrdersScreen> createState() => _CustomerOrdersScreenState();
}

class _CustomerOrdersScreenState extends State<CustomerOrdersScreen> {
  String _group = 'PENDING';

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.customerOrders)),
      body: Column(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(children: [
              for (final (value, label) in [
                ('PENDING', l.ordersTabActive),
                ('COMPLETED', l.ordersTabCompleted),
                ('CANCELLED', l.ordersTabCancelled)
              ])
                Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                        label: Text(label),
                        selected: _group == value,
                        showCheckmark: false,
                        selectedColor: AppColors.gold,
                        onSelected: (_) => setState(() => _group = value))),
            ]),
          ),
        ),
        Expanded(
          child: PagedListView<CustomerOrder>(
            key: ValueKey('orders-$_group-${app.dataVersion}'),
            fetch: (page, size) =>
                app.orders.page(group: _group, page: page, size: size),
            empty: EmptyState(
                icon: Icons.receipt_long_outlined,
                title: l.noCustomerOrders,
                message: l.noCustomerOrdersMessage),
            itemBuilder: (context, o, _, __) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: AppCard(
                onTap: () async {
                  await Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => CustomerOrderScreen(orderId: o.id)));
                  app.dataChanged();
                },
                child: Row(children: [
                  Expanded(
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                        Text(o.reference,
                            style:
                                const TextStyle(fontWeight: FontWeight.w800)),
                        Text(o.customerName,
                            style: const TextStyle(color: AppColors.muted)),
                        Text(o.operationName,
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
