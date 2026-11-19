import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/foundation.dart';

/// Sends unexpected crashes to Firebase Crashlytics (Android release builds only), so problems seen by sellers reach the team.
/// No names, emails or phone numbers are attached: a report carries the error, the stack and the phone model. Never blocks the app:
/// if Firebase is not set up, crashes are simply not reported.
Future<void> startCrashReporting() async {
  if (kIsWeb || !kReleaseMode || !Platform.isAndroid) return;
  try {
    if (Firebase.apps.isEmpty) await Firebase.initializeApp();
    final crashlytics = FirebaseCrashlytics.instance;
    FlutterError.onError = crashlytics.recordFlutterFatalError;
    PlatformDispatcher.instance.onError = (error, stack) {
      crashlytics.recordError(error, stack, fatal: true);
      return true;
    };
  } catch (_) {/* reporting is a convenience */}
}
