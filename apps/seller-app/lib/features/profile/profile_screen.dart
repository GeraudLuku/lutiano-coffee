import 'package:material_ui/material_ui.dart';

import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../operation/my_operation_screen.dart';
import 'about_screen.dart';
import 'app_settings_screen.dart';
import 'edit_profile_screen.dart';
import 'help_support_screen.dart';
import 'security_screen.dart';
import 'role_permissions_screen.dart';
import '../../core/l10n.dart';

/// Batch 8 · Screen 1 — My Profile. Profile details are managed by the administrator (read-only here).
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final user = app.user!;
    final op = app.operation;
    void push(Widget w) =>
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => w));
    return Scaffold(
      appBar: AppBar(
          title: Text(context.l10n.myProfile),
          automaticallyImplyLeading: false),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          child: Row(children: [
            CircleAvatar(
              radius: 34,
              backgroundColor: AppColors.navy,
              backgroundImage: app.api.mediaUrl(user.imageUrl) == null
                  ? null
                  : NetworkImage(app.api.mediaUrl(user.imageUrl)!),
              child: app.api.mediaUrl(user.imageUrl) != null
                  ? null
                  : Text(
                      user.firstName.isEmpty
                          ? '?'
                          : user.firstName[0].toUpperCase(),
                      style: const TextStyle(
                          color: AppColors.gold,
                          fontSize: 28,
                          fontWeight: FontWeight.w800)),
            ),
            const SizedBox(width: 14),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(user.fullName,
                      style: const TextStyle(
                          fontWeight: FontWeight.w800, fontSize: 18)),
                  Text(op?.type.sellerRole(context.l10n) ?? context.l10n.seller,
                      style: const TextStyle(color: AppColors.muted)),
                  const SizedBox(height: 6),
                  activeChip(context),
                ])),
            IconButton(
                tooltip: context.l10n.editProfile,
                icon: const Icon(Icons.edit_outlined),
                onPressed: () => push(const EditProfileScreen())),
          ]),
        ),
        const SizedBox(height: 12),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.storefront_outlined,
                title: context.l10n.salesOperation,
                subtitle: op == null
                    ? context.l10n.notAssigned
                    : '${op.name} · ${op.code}',
                onTap:
                    op == null ? null : () => push(const MyOperationScreen())),
            MenuTile(
                icon: Icons.mail_outline,
                title: context.l10n.email,
                subtitle: user.email),
            MenuTile(
                icon: Icons.phone_outlined,
                title: context.l10n.phone,
                subtitle: user.phone ?? context.l10n.notSet),
          ]),
        ),
        const SizedBox(height: 12),
        Row(children: [
          _Shortcut(
              icon: Icons.badge_outlined,
              label: context.l10n.myRole,
              color: AppColors.purple,
              onTap: () => push(const RolePermissionsScreen())),
          const SizedBox(width: 10),
          _Shortcut(
              icon: Icons.storefront_outlined,
              label: context.l10n.operation,
              color: AppColors.amber,
              onTap: op == null ? null : () => push(const MyOperationScreen())),
          const SizedBox(width: 10),
          _Shortcut(
              icon: Icons.shield_outlined,
              label: context.l10n.security,
              color: AppColors.green,
              onTap: () => push(const SecurityScreen())),
        ]),
        const SizedBox(height: 12),
        AppCard(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
          child: Column(children: [
            MenuTile(
                icon: Icons.settings_outlined,
                title: context.l10n.appSettings,
                subtitle: context.l10n.appSettingsSub,
                onTap: () => push(const AppSettingsScreen())),
            MenuTile(
                icon: Icons.help_outline,
                title: context.l10n.helpSupport,
                subtitle: context.l10n.getHelpFromTheLutiano,
                onTap: () => push(const HelpSupportScreen())),
            MenuTile(
                icon: Icons.info_outline,
                title: context.l10n.aboutLutiano,
                subtitle: context.l10n.versionAndLegalInformation,
                onTap: () => push(const AboutScreen())),
          ]),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          style: FilledButton.styleFrom(
              backgroundColor: AppColors.red, foregroundColor: Colors.white),
          onPressed: () => _confirmSignOut(context),
          icon: const Icon(Icons.logout),
          label: Text(context.l10n.signOut),
        ),
        const SizedBox(height: 8),
        Text(context.l10n.yourNamePhoneAndRole,
            textAlign: TextAlign.center,
            style: TextStyle(color: AppColors.muted, fontSize: 12.5)),
      ]),
    );
  }

  Future<void> _confirmSignOut(BuildContext context) async {
    final app = AppScope.read(context);
    final hasCart = !app.cart.isEmpty;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(context.l10n.signOut2),
        content: Text(hasCart
            ? context.l10n.cartWillBeCleared
            : context.l10n.needToSignInAgain),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: Text(context.l10n.cancel)),
          TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: Text(context.l10n.signOut3)),
        ],
      ),
    );
    if (ok == true) app.signOut();
  }
}

class _Shortcut extends StatelessWidget {
  const _Shortcut(
      {required this.icon,
      required this.label,
      required this.color,
      this.onTap});
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => Expanded(
        child: AppCard(
          onTap: onTap,
          padding: const EdgeInsets.symmetric(vertical: 14),
          child: Column(children: [
            Icon(icon, color: color, size: 28),
            const SizedBox(height: 6),
            Text(label,
                style: const TextStyle(
                    fontWeight: FontWeight.w700, fontSize: 12.5)),
          ]),
        ),
      );
}
