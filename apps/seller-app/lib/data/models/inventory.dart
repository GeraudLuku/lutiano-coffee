import 'package:lutiano_core/lutiano_core.dart';

class Product {
  const Product(
      {required this.id,
      required this.sku,
      required this.name,
      required this.category,
      required this.priceXaf,
      required this.lowStockThreshold,
      this.description,
      this.imageUrl});

  factory Product.fromJson(Map<String, dynamic> j) => Product(
        id: asString(j['id']),
        sku: asString(j['sku']),
        name: asString(j['name']),
        category: asString(j['category']),
        priceXaf: asInt(j['price_xaf']),
        lowStockThreshold: asInt(j['low_stock_threshold']),
        description: asStringOrNull(j['description']),
        imageUrl: asStringOrNull(j['image_url']),
      );

  final String id, sku, name, category;
  final int priceXaf, lowStockThreshold;
  final String? description, imageUrl;
}

enum StockLevel { healthy, low, out }

/// A product and its quantity at one sales operation.
class StockItem {
  const StockItem({required this.product, required this.quantity});

  factory StockItem.fromJson(Map<String, dynamic> j) => StockItem(
        product: Product.fromJson({...j, 'id': j['product_id']}),
        quantity: asInt(j['quantity']),
      );

  final Product product;
  final int quantity;

  StockLevel get level => quantity <= 0
      ? StockLevel.out
      : quantity <= product.lowStockThreshold
          ? StockLevel.low
          : StockLevel.healthy;
}

/// Stock dispatched to the seller's operation and awaiting receipt confirmation.
class IncomingAllocation {
  const IncomingAllocation(
      {required this.id,
      required this.operationId,
      required this.operationName,
      required this.productName,
      required this.sentQuantity,
      required this.dispatchedAt,
      this.imageUrl});

  factory IncomingAllocation.fromJson(Map<String, dynamic> j) =>
      IncomingAllocation(
        id: asString(j['id']),
        operationId: asString(j['operation_id']),
        operationName: asString(j['operation_name']),
        productName: asString(j['product_name']),
        sentQuantity: asInt(j['sent_quantity']),
        dispatchedAt: asTime(j['created_at']),
        imageUrl: asStringOrNull(j['image_url']),
      );

  final String id, operationId, operationName, productName;
  final int sentQuantity;
  final DateTime dispatchedAt;
  final String? imageUrl;
}

class StockMovement {
  const StockMovement(
      {required this.isIn,
      required this.productId,
      required this.productName,
      required this.quantity,
      required this.occurredAt,
      required this.source,
      required this.movementType,
      required this.balanceAfter,
      this.reference,
      this.saleId});

  factory StockMovement.fromJson(Map<String, dynamic> j) {
    final isIn = j['direction'] == 'IN';
    return StockMovement(
      isIn: isIn,
      productId: asString(j['product_id']),
      productName: asString(j['product_name']),
      quantity: asInt(j['quantity']),
      occurredAt: asTime(j['occurred_at']),
      source: asString(j['source']),
      movementType: asString(j['movement_type']),
      balanceAfter: asInt(j['balance_after']),
      reference: asStringOrNull(j['reference']),
      // only a sale's movement points at a sale (an adjustment's points at the adjustment)
      saleId:
          j['movement_type'] == 'SALE' ? asStringOrNull(j['source_id']) : null,
    );
  }

  final bool isIn;
  final String productId, productName, source, movementType;
  final int quantity, balanceAfter;
  final DateTime occurredAt;
  final String? reference, saleId;
}

/// Units that came in and went out over a period, and the products consumed most.
class StockSummary {
  const StockSummary(
      {required this.received, required this.consumed, required this.top});

  factory StockSummary.fromJson(Map<String, dynamic> j) => StockSummary(
        received: asInt(j['received']),
        consumed: asInt(j['consumed']),
        top: [
          for (final t in (j['top_consumed'] as List? ?? const []))
            (
              productId: asString((t as Map)['product_id']),
              name: asString(t['product_name']),
              quantity: asInt(t['quantity'])
            )
        ],
      );

  final int received, consumed;
  final List<({String productId, String name, int quantity})> top;
  int get net => received - consumed;
}
