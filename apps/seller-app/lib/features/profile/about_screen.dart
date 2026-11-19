import 'package:material_ui/material_ui.dart';

import '../../core/config.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../../widgets/widgets.dart';
import 'legal_screen.dart';
import '../../core/l10n.dart';

/// Batch 8 · Screen 9 — About the App.
class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(context.l10n.aboutLutiano)),
        body: ListView(padding: const EdgeInsets.all(24), children: [
          const Center(child: BrandMark(size: 72)),
          const SizedBox(height: 12),
          BrandTitle(subtitle: context.l10n.coffeeBusinessOs, light: false),
          const SizedBox(height: 8),
          Text(context.l10n.sellerOperationsApp,
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.w700)),
          Text(context.l10n.versionValue(AppConfig.appVersion),
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.muted)),
          const SizedBox(height: 24),
          AppCard(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: Column(children: [
              MenuTile(
                  icon: Icons.gavel_outlined,
                  title: context.l10n.termsOfUse,
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => LegalScreen(
                          title: context.l10n.termsOfUse,
                          body: context.l10n.termsBody)))),
              MenuTile(
                  icon: Icons.privacy_tip_outlined,
                  title: context.l10n.privacyPolicy,
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                      builder: (_) => LegalScreen(
                          title: context.l10n.privacyPolicy,
                          body: context.l10n.privacyBody)))),
              MenuTile(
                  icon: Icons.description_outlined,
                  title: context.l10n.licenses,
                  subtitle: context.l10n.thirdPartySoftwareAndCredits,
                  onTap: () => showLicensePage(
                      context: context,
                      applicationName: context.l10n.appTitle,
                      applicationVersion: AppConfig.appVersion)),
            ]),
          ),
        ]),
      );
}
