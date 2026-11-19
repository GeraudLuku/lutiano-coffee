import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import '../favourites/favourites_screen.dart';
import '../notifications/notification_preferences_screen.dart';
import '../support/help_support_screen.dart';
import 'about_screen.dart';
import 'edit_profile_screen.dart';
import 'language_screen.dart';
import 'legal_screen.dart';
import 'security_screen.dart';

/// My profile: who I am, and the way into the account screens.
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final l = context.l10n;
    final me = app.customer!;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(title: Text(l.profile)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Center(
          child: Column(children: [
            CircleAvatar(
                radius: 40,
                backgroundColor: AppColors.gold,
                child: Text(
                    me.firstName.isEmpty ? '?' : me.firstName[0].toUpperCase(),
                    style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.w800,
                        color: AppColors.navy))),
            const SizedBox(height: 10),
            Text(me.fullName,
                style:
                    const TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
            Text(me.phone, style: const TextStyle(color: AppColors.muted)),
            if (me.tier != null) ...[
              const SizedBox(height: 6),
              StatusChip(_tier(me.tier!),
                  color: AppColors.amber, background: AppColors.amberSoft)
            ],
          ]),
        ),
        const SizedBox(height: 16),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.person_outline,
                title: l.accountDetails,
                onTap: () => push(const EditProfileScreen())),
            MenuTile(
                icon: Icons.favorite_border,
                title: l.favourites,
                onTap: () => push(const FavouritesScreen())),
            MenuTile(
                icon: Icons.shield_outlined,
                title: l.securityAndLogin,
                onTap: () => push(const SecurityScreen())),
            MenuTile(
                icon: Icons.notifications_none,
                title: l.notificationSettings,
                onTap: () => push(const NotificationPreferencesScreen())),
            MenuTile(
                icon: Icons.language,
                title: l.language,
                onTap: () => push(const LanguageScreen())),
          ]),
        ),
        const SizedBox(height: 12),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.help_outline,
                title: l.helpAndSupport,
                onTap: () => push(const HelpSupportScreen())),
            MenuTile(
                icon: Icons.info_outline,
                title: l.aboutLutiano,
                onTap: () => push(const AboutScreen())),
            MenuTile(
                icon: Icons.description_outlined,
                title: l.termsOfUse,
                onTap: () =>
                    push(LegalScreen(title: l.termsOfUse, body: l.termsBody))),
            MenuTile(
                icon: Icons.privacy_tip_outlined,
                title: l.privacyPolicy,
                onTap: () => push(
                    LegalScreen(title: l.privacyPolicy, body: l.privacyBody))),
          ]),
        ),
        const SizedBox(height: 16),
        OutlinedButton.icon(
          style: OutlinedButton.styleFrom(
              foregroundColor: AppColors.red,
              side: const BorderSide(color: AppColors.red)),
          icon: const Icon(Icons.logout),
          label: Text(l.logOut),
          onPressed: () async {
            final ok = await showDialog<bool>(
              context: context,
              builder: (ctx) => AlertDialog(
                title: Text(l.logOutQuestion),
                content: Text(l.logOutConfirm),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.of(ctx).pop(false),
                      child: Text(l.cancel)),
                  FilledButton(
                      style: FilledButton.styleFrom(
                          backgroundColor: AppColors.red),
                      onPressed: () => Navigator.of(ctx).pop(true),
                      child: Text(l.logOut)),
                ],
              ),
            );
            if (ok == true) app.signOut();
          },
        ),
      ]),
    );
  }

  static String _tier(String code) => code[0] + code.substring(1).toLowerCase();
}
