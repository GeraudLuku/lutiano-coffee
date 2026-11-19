import 'package:material_ui/material_ui.dart';

import '../../data/models.dart';
import '../../state/app_scope.dart';
import 'code_screen.dart';
import 'social_phone_screen.dart';

/// Carries on after any sign-in step: a session starts the app, a second step asks for its code, and a new Google/Apple
/// account is asked for its phone number.
Future<void> continueAuth(BuildContext context, AuthStep step) async {
  final app = AppScope.read(context);
  final nav = Navigator.of(context);
  switch (step) {
    case SignedIn():
      await app.startSession(step);
      nav.popUntil((r) => r.isFirst);
    case SecondStep():
      await nav.push(MaterialPageRoute(
          builder: (_) => CodeScreen(
              phoneHint: step.phone,
              onVerify: (code) =>
                  app.auth.verifySecondStep(step.challenge, code))));
    case NeedsPhone():
      await nav.push(MaterialPageRoute(
          builder: (_) =>
              SocialPhoneScreen(ticket: step.ticket, name: step.name)));
  }
}
