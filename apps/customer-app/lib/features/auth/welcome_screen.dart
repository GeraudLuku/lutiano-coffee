import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'login_screen.dart';
import 'signup_screen.dart';

/// First screen: the brand, a promise, and the way in.
class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    final app = AppScope.read(context);
    void open(Widget screen) {
      app.settings.markWelcomeSeen();
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => screen));
    }

    return Scaffold(
      backgroundColor: AppColors.navyDeep,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(children: [
            const Spacer(flex: 2),
            const BrandMark(size: 88),
            const SizedBox(height: 14),
            BrandTitle(subtitle: l.coffee),
            const Spacer(),
            Text(l.welcomeHeadline,
                textAlign: TextAlign.center,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 32,
                    height: 1.1,
                    fontWeight: FontWeight.w800)),
            const SizedBox(height: 12),
            Text(l.welcomeSubtitle,
                textAlign: TextAlign.center,
                style: const TextStyle(color: AppColors.white70, fontSize: 15)),
            const Spacer(flex: 2),
            FilledButton(
                onPressed: () => open(const SignUpScreen()),
                child: Text(l.getStarted)),
            const SizedBox(height: 8),
            TextButton(
                onPressed: () => open(const LoginScreen()),
                child: Text('${l.alreadyHaveAccount} ${l.logIn}',
                    style: const TextStyle(color: Colors.white))),
          ]),
        ),
      ),
    );
  }
}
