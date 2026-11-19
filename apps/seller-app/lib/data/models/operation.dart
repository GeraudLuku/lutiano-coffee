import 'package:lutiano_core/lutiano_core.dart';

enum OperationType {
  fixedPos('FIXED_POS'),
  mobile('MOBILE'),
  marketAgent('MARKET_AGENT');

  const OperationType(this.code);
  final String code;

  static OperationType parse(Object? code) =>
      values.firstWhere((t) => t.code == code, orElse: () => fixedPos);
}

class Operation {
  const Operation(
      {required this.id,
      required this.name,
      required this.code,
      required this.type,
      required this.active,
      this.address,
      this.city,
      this.region});

  factory Operation.fromJson(Map<String, dynamic> j) => Operation(
        id: asString(j['id']),
        name: asString(j['name']),
        code: asString(j['code']),
        type: OperationType.parse(j['type']),
        active: asBool(j['active']),
        address: asStringOrNull(j['address']),
        city: asStringOrNull(j['city']),
        region: asStringOrNull(j['region']),
      );

  final String id, name, code;
  final OperationType type;
  final bool active;
  final String? address, city, region;

  String get location =>
      [address, city].whereType<String>().where((s) => s.isNotEmpty).join(', ');
}

class OperationStats {
  const OperationStats(
      {required this.todayXaf,
      required this.weekXaf,
      required this.monthXaf,
      required this.transactions});

  factory OperationStats.fromJson(Map<String, dynamic> j) => OperationStats(
      todayXaf: asInt(j['today_xaf']),
      weekXaf: asInt(j['week_xaf']),
      monthXaf: asInt(j['month_xaf']),
      transactions: asInt(j['transactions']));

  final int todayXaf, weekXaf, monthXaf, transactions;
}

class OperationDetail {
  const OperationDetail(
      {required this.operation,
      required this.sellerNames,
      required this.stats});

  factory OperationDetail.fromJson(Map<String, dynamic> j) => OperationDetail(
        operation: Operation.fromJson(j),
        sellerNames:
            asList(j['sellers']).map((s) => asString(s['full_name'])).toList(),
        stats: OperationStats.fromJson(
            (j['stats'] as Map).cast<String, dynamic>()),
      );

  final Operation operation;
  final List<String> sellerNames;
  final OperationStats stats;
}
