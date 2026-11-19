import 'package:material_ui/material_ui.dart';

import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Batch 8 · Screen 3 — My Role & Permissions (what the server allows a seller to do in V1).
class RolePermissionsScreen extends StatelessWidget {
  const RolePermissionsScreen({super.key});

  static List<String> _allowed(AppLocalizations l) => [
        l.permRecordCashSales,
        l.permOpenCloseShift,
        l.permViewInventory,
        l.permConfirmDeliveries,
        l.permShowQr,
        l.permViewOwnSales,
      ];
  static List<String> _notAllowed(AppLocalizations l) => [
        l.permChangePrices,
        l.permApproveRefunds,
        l.permManageProducts,
      ];

  @override
  Widget build(BuildContext context) {
    final op = AppScope.of(context).operation;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.myRolePermissions)),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        AppCard(
          color: AppColors.purpleSoft,
          child: Row(children: [
            const Icon(Icons.badge_outlined, color: AppColors.purple, size: 30),
            const SizedBox(width: 12),
            Expanded(
                child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                  Text(op?.type.sellerRole(context.l10n) ?? context.l10n.seller,
                      style: const TextStyle(
                          fontWeight: FontWeight.w800, fontSize: 16)),
                  Text(context.l10n.operationsRole,
                      style: TextStyle(color: AppColors.muted)),
                ])),
            activeChip(context),
          ]),
        ),
        SectionLabel(context.l10n.roleDescription),
        Text(_description(context, op?.type.code),
            style: const TextStyle(height: 1.4)),
        SectionLabel(context.l10n.myPermissions),
        AppCard(
          child: Column(children: [
            for (final p in _allowed(context.l10n))
              _PermissionRow(label: p, allowed: true),
            for (final p in _notAllowed(context.l10n))
              _PermissionRow(label: p, allowed: false),
          ]),
        ),
        const SizedBox(height: 12),
        InfoNote(context.l10n.permissionsAreEnforcedByThe),
      ]),
    );
  }

  static String _description(BuildContext context, String? type) =>
      switch (type) {
        'MOBILE' => context.l10n.roleDescMobile,
        'MARKET_AGENT' => context.l10n.roleDescMarket,
        _ => context.l10n.roleDescFixed,
      };
}

class _PermissionRow extends StatelessWidget {
  const _PermissionRow({required this.label, required this.allowed});
  final String label;
  final bool allowed;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(children: [
          Icon(allowed ? Icons.check_circle : Icons.cancel_outlined,
              color: allowed ? AppColors.green : AppColors.muted, size: 20),
          const SizedBox(width: 10),
          Expanded(
              child: Text(label,
                  style: TextStyle(
                      color: allowed ? AppColors.ink : AppColors.muted))),
        ]),
      );
}
