import 'package:lutiano_core/lutiano_core.dart';

/// The signed-in customer, as the API shows them.
class Customer {
  const Customer({
    required this.id,
    required this.code,
    required this.fullName,
    required this.phone,
    required this.language,
    required this.twoFactor,
    required this.hasPassword,
    required this.loyaltyQr,
    this.email,
    this.imageUrl,
    this.tier,
    this.birthDate,
    this.marketingConsent = false,
    this.linked = const [],
  });

  factory Customer.fromJson(Map<String, dynamic> j) => Customer(
        id: asString(j['id']),
        code: asString(j['code']),
        fullName: asString(j['full_name']),
        phone: asString(j['phone']),
        email: asStringOrNull(j['email']),
        language:
            asString(j['language']).isEmpty ? 'fr' : asString(j['language']),
        imageUrl: asStringOrNull(j['image_url']),
        tier: asStringOrNull(j['tier_code']),
        birthDate: asStringOrNull(j['birth_date']),
        twoFactor: asBool(j['two_factor_enabled']),
        hasPassword: asBool(j['has_password']),
        marketingConsent: asBool(j['marketing_consent']),
        loyaltyQr: asString(j['loyalty_qr']),
        linked: asList(j['linked_accounts'])
            .map((a) => asString(a['provider']))
            .toList(),
      );

  final String id, code, fullName, phone, language, loyaltyQr;
  final String? email, imageUrl, tier, birthDate;
  final bool twoFactor, hasPassword, marketingConsent;

  /// Linked sign-in providers (GOOGLE, APPLE).
  final List<String> linked;

  String get firstName => fullName.trim().split(RegExp(r'\s+')).first;
}

/// What the API answered to a sign-in step: a session, a code to enter, or a phone number to add.
sealed class AuthStep {
  const AuthStep();

  factory AuthStep.fromJson(Map<String, dynamic> j) {
    if (j['token'] != null) {
      return SignedIn(asString(j['token']),
          Customer.fromJson((j['customer'] as Map).cast<String, dynamic>()));
    }
    if (asBool(j['mfaRequired'])) {
      return SecondStep(asString(j['challenge']), asString(j['phone']));
    }
    return NeedsPhone(asString(j['ticket']), asStringOrNull(j['name']));
  }
}

class SignedIn extends AuthStep {
  const SignedIn(this.token, this.customer);
  final String token;
  final Customer customer;
}

/// Password was right; a code was sent to [phone] (masked).
class SecondStep extends AuthStep {
  const SecondStep(this.challenge, this.phone);
  final String challenge, phone;
}

/// A new Google/Apple account: the phone number is still needed.
class NeedsPhone extends AuthStep {
  const NeedsPhone(this.ticket, this.name);
  final String ticket;
  final String? name;
}
