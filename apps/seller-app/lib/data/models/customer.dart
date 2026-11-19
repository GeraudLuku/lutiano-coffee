import 'package:lutiano_core/lutiano_core.dart';

/// The customer behind a scanned loyalty QR: just enough to confirm who it is (never their phone number).
class ScannedCustomer {
  const ScannedCustomer(
      {required this.fullName,
      required this.code,
      required this.pointsBalance,
      this.tierName});

  factory ScannedCustomer.fromJson(Map<String, dynamic> j) => ScannedCustomer(
      fullName: asString(j['full_name']),
      code: asString(j['code']),
      tierName: asStringOrNull(j['tier_name']),
      pointsBalance: asInt(j['points_balance']));

  final String fullName, code;
  final String? tierName;
  final int pointsBalance;
}
