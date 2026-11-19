import 'package:lutiano_core/lutiano_core.dart';

/// A help request to the LUTIANO team and, once answered, the reply.
class SupportRequest {
  const SupportRequest(
      {required this.id,
      required this.reference,
      required this.type,
      required this.subject,
      required this.description,
      required this.status,
      required this.createdAt,
      this.reply,
      this.repliedAt,
      this.hasAttachment = false});
  factory SupportRequest.fromJson(Map<String, dynamic> j) => SupportRequest(
      id: asString(j['id']),
      reference: asString(j['reference']),
      type: asString(j['type']),
      subject: asString(j['subject']),
      description: asString(j['description']),
      status: asString(j['status']),
      createdAt: asTime(j['created_at']),
      reply: asStringOrNull(j['reply']),
      repliedAt: asTimeOrNull(j['replied_at']),
      hasAttachment: asBool(j['has_attachment']));
  final String id, reference, type, subject, description, status;
  final DateTime createdAt;
  final String? reply;
  final DateTime? repliedAt;
  final bool hasAttachment;
}
