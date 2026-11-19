import 'package:material_ui/material_ui.dart';

import '../data/models.dart';
import 'package:lutiano_core/lutiano_core.dart';
import '../l10n/app_localizations.dart';

export '../l10n/app_localizations.dart';

/// Short access to the translated strings: `context.l10n.signIn`.
extension L10nContext on BuildContext {
  AppLocalizations get l10n => AppLocalizations.of(this);
}

/// Translated labels for API codes (the models keep only the codes).
extension OperationTypeLabels on OperationType {
  String label(AppLocalizations l) => switch (this) {
        OperationType.fixedPos => l.typeFixedPos,
        OperationType.mobile => l.typeMobile,
        OperationType.marketAgent => l.typeMarket,
      };

  /// The seller role that works this kind of operation.
  String sellerRole(AppLocalizations l) => switch (this) {
        OperationType.fixedPos => l.roleFixedPos,
        OperationType.mobile => l.roleMobile,
        OperationType.marketAgent => l.roleMarket,
      };
}

extension CashLedgerEntryLabel on CashLedgerEntry {
  String label(AppLocalizations l) => switch (type) {
        'OPENING_FLOAT' => l.ledgerOpeningFloat,
        'CASH_SALE' => l.ledgerCashSale,
        'REFUND_PAID' => l.ledgerRefundPaid,
        'OUTFLOW' => l.ledgerOutflow,
        'REMITTANCE' => l.ledgerRemittance,
        _ => type,
      };
}

/// Outflow categories as the seller reads them; the codes are what the API stores.
String outflowCategoryLabel(AppLocalizations l, String code) => switch (code) {
      'SUPPLIES' => l.catSupplies,
      'TRANSPORT' => l.catTransport,
      'MAINTENANCE' => l.catMaintenance,
      'CLEANING' => l.catCleaning,
      'OTHER' => l.catOther,
      _ => code,
    };

/// Payment method code as the seller reads it.
String paymentMethodLabel(AppLocalizations l, String code) => switch (code) {
      'CASH' => l.cash,
      'MTN_MOMO' => l.mtnMomo,
      'ORANGE_MONEY' => l.orangeMoney,
      _ => code.isEmpty
          ? l.cash
          : '${code[0]}${code.substring(1).toLowerCase().replaceAll('_', ' ')}',
    };

String greeting(AppLocalizations l, DateTime now) {
  final h = toDouala(now).hour;
  return h < 12
      ? l.greetingMorning
      : (h < 18 ? l.greetingAfternoon : l.greetingEvening);
}

extension CashRemittanceDestination on CashRemittance {
  String destination(AppLocalizations l) => method == 'BANK_DEPOSIT'
      ? l.bankDepositRef(bankReference ?? '')
      : l.handedToName(recipientName ?? l.manager);
}
