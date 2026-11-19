import 'package:lutiano_core/lutiano_core.dart';
import 'operation.dart';

class UserProfile {
  const UserProfile(
      {required this.id,
      required this.email,
      required this.fullName,
      required this.role,
      required this.operations,
      this.sellerRole,
      this.imageUrl,
      this.phone,
      this.createdAt,
      this.lastLoginAt});

  factory UserProfile.fromJson(Map<String, dynamic> j) => UserProfile(
        id: asString(j['id']),
        email: asString(j['email']),
        fullName: asString(j['full_name']),
        role: asString(j['role']),
        sellerRole: asStringOrNull(j['seller_role']),
        imageUrl: asStringOrNull(j['image_url']),
        phone: asStringOrNull(j['phone']),
        createdAt: asTimeOrNull(j['created_at']),
        lastLoginAt: asTimeOrNull(j['last_login_at']),
        operations: asList(j['operations']).map(Operation.fromJson).toList(),
      );

  final String id, email, fullName, role;
  final String? phone, sellerRole, imageUrl;
  final DateTime? createdAt, lastLoginAt;
  final List<Operation> operations;

  String get firstName => fullName.split(' ').first;
  bool get isSeller => role == 'SELLER';

  /// Whether the app offers stock requests: Mobile Barista and Market Sales Agent (a fixed point of sale is restocked by its manager). The server decides.
  bool get canRequestStock =>
      sellerRole == 'MOBILE_BARISTA' || sellerRole == 'MARKET_AGENT';
}
