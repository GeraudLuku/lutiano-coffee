import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/config.dart';
import '../../core/l10n.dart';
import 'legal_screen.dart';

class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l = context.l10n;
    return Scaffold(
      appBar: AppBar(title: Text(l.aboutLutiano)),
      body: ListView(padding: const EdgeInsets.all(24), children: [
        const Center(child: BrandMark(size: 72)),
        const SizedBox(height: 12),
        BrandTitle(subtitle: l.coffee, light: false),
        const SizedBox(height: 8),
        Text(l.tagline,
            textAlign: TextAlign.center,
            style: const TextStyle(fontWeight: FontWeight.w700)),
        Text(l.versionValue(AppConfig.appVersion),
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.muted)),
        const SizedBox(height: 24),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.description_outlined,
                title: l.termsOfUse,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) =>
                        LegalScreen(title: l.termsOfUse, body: l.termsBody)))),
            MenuTile(
                icon: Icons.privacy_tip_outlined,
                title: l.privacyPolicy,
                onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => LegalScreen(
                        title: l.privacyPolicy, body: l.privacyBody)))),
            MenuTile(
                icon: Icons.code,
                title: l.licenses,
                onTap: () => showLicensePage(
                    context: context,
                    applicationName: l.appTitle,
                    applicationVersion: AppConfig.appVersion)),
          ]),
        ),
      ]),
    );
  }
}
