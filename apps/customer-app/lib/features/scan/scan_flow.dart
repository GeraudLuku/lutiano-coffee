import 'package:lutiano_core/lutiano_core.dart';
import 'package:material_ui/material_ui.dart';

import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'sales_point_screen.dart';
import 'scan_screen.dart';

/// Scan a sales point's QR code, or type its code, and open that sales point.
Future<void> scanSalesPoint(BuildContext context) async {
  final code = await Navigator.of(context)
      .push<String>(MaterialPageRoute(builder: (_) => const ScanScreen()));
  if (code == null || !context.mounted) return;
  await openByCode(context, code);
}

/// Looks the code up and opens the sales point, or says why it cannot be used.
Future<void> openByCode(BuildContext context, String code) async {
  final app = AppScope.read(context);
  final nav = Navigator.of(context);
  final l = context.l10n;
  try {
    final point = await app.catalogue.salesPointByCode(code);
    await nav.push(MaterialPageRoute(
        builder: (_) =>
            SalesPointScreen(salesPointId: point.id, preloaded: point)));
  } catch (e) {
    if (context.mounted) {
      showMessage(context, e is ApiException ? e.message : l.codeNotRecognised,
          error: true);
    }
  }
}
