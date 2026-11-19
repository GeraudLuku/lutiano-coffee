import 'package:lutiano_core/lutiano_core.dart';

/// One line of the sign-in history: a sign-in or an account-security change, and where it happened.
class AccountActivity {
  const AccountActivity({required this.action, required this.at, this.client});

  factory AccountActivity.fromJson(Map<String, dynamic> j) => AccountActivity(
        action: asString(j['action']),
        at: asTime(j['created_at']),
        client: asStringOrNull(j['client']),
      );

  final String action;
  final DateTime at;

  /// The device, as the server described it ("Seller app", "Chrome on macOS").
  final String? client;
}

enum SupportType {
  question('QUESTION'),
  issue('ISSUE'),
  improvement('IMPROVEMENT'),
  account('ACCOUNT');

  const SupportType(this.api);
  final String api;
  static SupportType fromApi(String v) =>
      values.firstWhere((t) => t.api == v, orElse: () => question);
}

enum SupportStatus {
  open('OPEN'),
  inProgress('IN_PROGRESS'),
  resolved('RESOLVED');

  const SupportStatus(this.api);
  final String api;
  static SupportStatus fromApi(String v) =>
      values.firstWhere((s) => s.api == v, orElse: () => open);
}

/// A help request the seller sent, with the team's reply once there is one.
class SupportRequest {
  const SupportRequest({
    required this.id,
    required this.reference,
    required this.type,
    required this.subject,
    required this.description,
    required this.status,
    required this.createdAt,
    this.attachmentName,
    this.reply,
    this.repliedAt,
  });

  factory SupportRequest.fromJson(Map<String, dynamic> j) => SupportRequest(
        id: asString(j['id']),
        reference: asString(j['reference']),
        type: SupportType.fromApi(asString(j['type'])),
        subject: asString(j['subject']),
        description: asString(j['description']),
        status: SupportStatus.fromApi(asString(j['status'])),
        createdAt: asTime(j['created_at']),
        attachmentName: asStringOrNull(j['attachment_name']),
        reply: asStringOrNull(j['reply']),
        repliedAt: asTimeOrNull(j['replied_at']),
      );

  final String id, reference, subject, description;
  final SupportType type;
  final SupportStatus status;
  final DateTime createdAt;
  final String? attachmentName, reply;
  final DateTime? repliedAt;
}
