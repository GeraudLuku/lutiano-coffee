import '../../core/l10n.dart';
import '../../state/app_scope.dart';
import 'package:lutiano_core/lutiano_core.dart';
import 'package:flutter/widgets.dart';

/// Opens the server-generated PDF receipt in the system print / share sheet.
Future<void> openReceipt(
        BuildContext context, String saleId, String reference) =>
    openPdf(context,
        fetch: () => AppScope.read(context).sales.receiptPdf(saleId),
        fileName: 'LUTIANO-receipt-$reference.pdf',
        preparing: context.l10n.preparingReceipt);
