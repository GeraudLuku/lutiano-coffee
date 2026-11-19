import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../widgets/widgets.dart';
import 'consumption_detail_screen.dart';
import '../../core/l10n.dart';

/// One product row with its quantity and stock status.
class StockTile extends StatelessWidget {
  const StockTile({super.key, required this.item, this.onTap});
  final StockItem item;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: AppCard(
          padding: const EdgeInsets.all(10),
          onTap: onTap,
          child: Row(children: [
            ProductImage(url: item.product.imageUrl, size: 52),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(item.product.name,
                        style: const TextStyle(fontWeight: FontWeight.w800)),
                    Text(item.product.category,
                        style: const TextStyle(
                            color: AppColors.muted, fontSize: 12.5)),
                    const SizedBox(height: 2),
                    Text.rich(TextSpan(children: [
                      TextSpan(text: context.l10n.stock),
                      TextSpan(
                          text: '${item.quantity}',
                          style: const TextStyle(fontWeight: FontWeight.w800)),
                      if (item.level != StockLevel.healthy)
                        TextSpan(
                            text: context.l10n
                                .minLevelSuffix(item.product.lowStockThreshold),
                            style: const TextStyle(color: AppColors.muted)),
                    ])),
                  ]),
            ),
            stockChip(context, item.level),
            if (onTap != null)
              const Icon(Icons.chevron_right, color: AppColors.muted),
          ]),
        ),
      );
}

/// One stock movement; a sale's consumption opens its detail (and the sale).
class MovementTile extends StatelessWidget {
  const MovementTile({super.key, required this.movement});
  final StockMovement movement;

  @override
  Widget build(BuildContext context) {
    final m = movement;
    final color = m.isIn ? AppColors.green : AppColors.red;
    return ListTile(
      contentPadding: EdgeInsets.zero,
      onTap: m.saleId == null
          ? null
          : () => Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => ConsumptionDetailScreen(movement: m))),
      leading: CircleAvatar(
        backgroundColor: m.isIn ? AppColors.greenSoft : AppColors.redSoft,
        child: Icon(m.isIn ? Icons.south : Icons.north, color: color),
      ),
      title: Text(
          m.isIn
              ? context.l10n.stockInTitle(m.productName)
              : context.l10n.stockOutTitle(m.productName),
          style: const TextStyle(fontWeight: FontWeight.w700)),
      subtitle: Text(
          '${dateTime(m.occurredAt)}\n${m.isIn ? m.source : context.l10n.saleReferenceValue(m.reference ?? '')}',
          style: const TextStyle(fontSize: 12.5)),
      isThreeLine: true,
      trailing: Text('${m.isIn ? '+' : '−'}${m.quantity}',
          style: TextStyle(
              color: color, fontWeight: FontWeight.w800, fontSize: 16)),
    );
  }
}
