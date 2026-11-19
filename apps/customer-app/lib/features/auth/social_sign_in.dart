import 'dart:convert';
import 'dart:io' show Platform;
import 'dart:math';

import 'package:crypto/crypto.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:google_sign_in/google_sign_in.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';

import '../../core/config.dart';

/// What Google or Apple gave the app: the ID token the server checks, and for Apple the nonce it was bound to.
class SocialToken {
  const SocialToken(this.provider, this.idToken, {this.nonce, this.name});
  final String provider, idToken;
  final String? nonce, name;
}

/// Sign in with Google or Apple on this phone. A person who backs out gets null; a real failure is thrown.
class SocialSignIn {
  static bool _googleReady = false;

  /// Apple sign-in is offered on iPhones only (on Android it needs a web flow that is not set up).
  static bool get appleSupported => !kIsWeb && Platform.isIOS;

  static Future<SocialToken?> google() async {
    final google = GoogleSignIn.instance;
    if (!_googleReady) {
      await google.initialize(
          serverClientId: AppConfig.googleClientId.isEmpty
              ? null
              : AppConfig.googleClientId);
      _googleReady = true;
    }
    try {
      final account = await google.authenticate();
      final token = account.authentication.idToken;
      if (token == null) return null;
      return SocialToken('GOOGLE', token, name: account.displayName);
    } on GoogleSignInException catch (e) {
      if (e.code == GoogleSignInExceptionCode.canceled) return null;
      rethrow;
    }
  }

  static Future<SocialToken?> apple() async {
    final nonce = _nonce();
    try {
      final credential = await SignInWithApple.getAppleIDCredential(scopes: [
        AppleIDAuthorizationScopes.email,
        AppleIDAuthorizationScopes.fullName
      ], nonce: _sha256Hex(nonce));
      final token = credential.identityToken;
      if (token == null) return null;
      final name = [credential.givenName, credential.familyName]
          .whereType<String>()
          .join(' ')
          .trim();
      return SocialToken('APPLE', token,
          nonce: nonce, name: name.isEmpty ? null : name);
    } on SignInWithAppleAuthorizationException catch (e) {
      if (e.code == AuthorizationErrorCode.canceled) return null;
      rethrow;
    }
  }

  static String _nonce() {
    final r = Random.secure();
    return List.generate(
        32, (_) => r.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
  }

  static String _sha256Hex(String s) =>
      sha256.convert(utf8.encode(s)).toString();
}
