import 'package:material_ui/material_ui.dart';

import 'package:lutiano_core/lutiano_core.dart';
import '../../state/app_scope.dart';
import '../../widgets/widgets.dart';
import '../../core/l10n.dart';

/// Batch 1 · Screen 4 — My Permanent QR Code (operation QR). The QR holds an opaque token only.
class OperationQrScreen extends StatelessWidget {
  const OperationQrScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final app = AppScope.of(context);
    final op = app.operation;
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.myQrCode)),
      body: op == null
          ? EmptyState(
              icon: Icons.qr_code_2,
              title: context.l10n.noSalesOperationAssigned)
          : ListView(padding: const EdgeInsets.all(16), children: [
              Text(context.l10n.showThisQrCodeTo,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: AppColors.muted)),
              const SizedBox(height: 16),
              Center(
                child: Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: AppColors.gold, width: 3)),
                  child: Image.network(
                    app.catalog.operationQrUrl(op.id),
                    headers: app.api.authHeaders,
                    width: 240,
                    height: 240,
                    semanticLabel: context.l10n.qrCodeFor(op.name),
                    loadingBuilder: (context, child, progress) =>
                        progress == null
                            ? child
                            : const SkeletonBox(
                                width: 240, height: 240, radius: 12),
                    errorBuilder: (_, __, ___) => SizedBox(
                        width: 240,
                        height: 240,
                        child: Center(
                            child:
                                Text(context.l10n.qrCodeUnavailableCheckYour))),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(op.name,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                      fontWeight: FontWeight.w800, fontSize: 18)),
              Text('${op.type.label(context.l10n)} · ${op.code}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: AppColors.muted)),
              const SizedBox(height: 20),
              InfoNote(context.l10n.thisIsYourPermanentQr),
            ]),
    );
  }
}
