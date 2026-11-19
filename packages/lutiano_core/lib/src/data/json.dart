/// Tolerant readers for API JSON (numbers may arrive as int or double, booleans as 0/1).
library;

int asInt(Object? v) => v is num ? v.toInt() : int.tryParse('$v') ?? 0;
int? asIntOrNull(Object? v) => v == null ? null : asInt(v);
bool asBool(Object? v) => v == true || v == 1 || v == '1' || v == 'true';
String asString(Object? v) => v?.toString() ?? '';
String? asStringOrNull(Object? v) => v?.toString();
DateTime asTime(Object? v) =>
    DateTime.tryParse('$v') ??
    DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
DateTime? asTimeOrNull(Object? v) => v == null ? null : DateTime.tryParse('$v');
List<Map<String, dynamic>> asList(Object? v) =>
    (v as List? ?? const []).cast<Map<String, dynamic>>();
