import 'dart:async';
import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

/// Push notifications on Android through Firebase. Once someone is signed in it asks permission, registers this phone's token with
/// the server, and reports when a notification is opened. Everything is optional: if Firebase is not set up (or on other platforms)
/// the app simply has no push, and the in-app notifications are unaffected. The app supplies what is specific to it as callbacks.
class PushService {
  PushService(
      {required this.register,
      required this.unregister,
      this.onForeground,
      this.onReady,
      this.onOpen});

  /// Sends this phone's token to the server / asks the server to forget it.
  final Future<void> Function(String token) register, unregister;

  /// A notification arrived while the app is open (refresh the badge), and push is working on this phone.
  final void Function()? onForeground, onReady;

  /// Called with the notification's id when the person taps a push notification.
  final void Function(String notificationId)? onOpen;

  static bool get supported => !kIsWeb && Platform.isAndroid;

  String? _token;
  StreamSubscription<String>? _refresh;
  StreamSubscription<RemoteMessage>? _foreground, _opened;
  bool _started = false;

  Future<void> start() async {
    if (!supported || _started) return;
    try {
      if (Firebase.apps.isEmpty) await Firebase.initializeApp();
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission();
      // the registration token is what the server's Firebase Admin SDK sends to (the newer Installation-ID API is a different target)
      // ignore: deprecated_member_use
      final token = await messaging.getToken();
      if (token == null) return;
      _token = token;
      await register(token);
      // ignore: deprecated_member_use
      _refresh = messaging.onTokenRefresh.listen((t) {
        _token = t;
        register(t).catchError((_) {});
      });
      // while the app is open Android shows nothing by itself: just let the app refresh its badge
      _foreground =
          FirebaseMessaging.onMessage.listen((_) => onForeground?.call());
      _opened = FirebaseMessaging.onMessageOpenedApp.listen(_handleOpen);
      final launching = await messaging.getInitialMessage();
      if (launching != null) _handleOpen(launching);
      _started = true;
      onReady?.call();
    } catch (_) {
      // push is a convenience: any failure (no Firebase, no permission, offline) leaves the app working without it
    }
  }

  void _handleOpen(RemoteMessage m) {
    final id = m.data['notificationId'];
    if (id is String && id.isNotEmpty) onOpen?.call(id);
  }

  /// Stops this phone receiving the signed-in person's notifications. Starts the request at once (before the sign-in token is cleared).
  void stop() {
    final token = _token;
    if (token != null) unregister(token).catchError((_) {});
    _refresh?.cancel();
    _foreground?.cancel();
    _opened?.cancel();
    _refresh = _foreground = _opened = null;
    _token = null;
    _started = false;
  }
}
