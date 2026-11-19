import 'package:lutiano_core/lutiano_core.dart';
import '../models.dart';

class ShiftRepository {
  ShiftRepository(this._api);
  final ApiClient _api;

  Future<CashShift?> current() async {
    final rows = asList(await _api.get('/shifts/open'));
    return rows.isEmpty ? null : CashShift.fromJson(rows.first);
  }

  Future<void> open(int openingFloat) =>
      _api.post('/shifts/open', body: {'openingFloat': openingFloat});

  Future<ShiftCloseResult> close(int countedCash, String? reason) async =>
      ShiftCloseResult.fromJson((await _api.post('/shifts/close',
                  body: {'countedCash': countedCash, 'varianceReason': reason})
              as Map)
          .cast<String, dynamic>());
}

/// Cash custody for the signed-in seller: shift ledger, outflow requests, remittances and recounts.
class CashRepository {
  CashRepository(this._api);
  final ApiClient _api;

  /// One shift with its totals and its outflow and remittance records (the ledger is paged: see [ledgerPage]).
  Future<CashShiftDetail> shift(String id) async => CashShiftDetail.fromJson(
      (await _api.get('/cash/shifts/$id') as Map).cast<String, dynamic>());

  /// The shift's ledger, newest entry first.
  Future<Paged<CashLedgerEntry>> ledgerPage(String shiftId,
          {int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/cash/shifts/$shiftId/ledger',
              page: page, size: size),
          CashLedgerEntry.fromJson);

  /// The seller's shifts, newest first (summary rows, without ledger).
  Future<Paged<CashShiftDetail>> shiftsPage(
          {int page = 1, int size = 20}) async =>
      Paged.fromJson(await _api.page('/cash/shifts', page: page, size: size),
          CashShiftDetail.fromJson);

  /// What needs attention across all of the seller's shifts (undecided outflows, unconfirmed or disputed remittances, recounts).
  Future<CashOverview> overview() async => CashOverview.fromJson(
      (await _api.get('/cash/overview') as Map).cast<String, dynamic>());

  /// The seller's outflow requests from every shift; waiting ones first. [status]: REQUESTED, APPROVED, REJECTED or CANCELLED.
  Future<Paged<CashOutflow>> outflowsPage(
          {String? status, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/cash/outflows',
              query: {if (status != null) 'status': status},
              page: page,
              size: size),
          CashOutflow.fromJson);

  /// The seller's remittances from every shift, newest first. [status]: OUTSTANDING, ACKNOWLEDGED or DISPUTED.
  Future<Paged<CashRemittance>> remittancesPage(
          {String? status, int page = 1, int size = 20}) async =>
      Paged.fromJson(
          await _api.page('/cash/remittances',
              query: {if (status != null) 'status': status},
              page: page,
              size: size),
          CashRemittance.fromJson);

  Future<List<CashRecipient>> recipients() async =>
      asList(await _api.get('/cash/recipients'))
          .map(CashRecipient.fromJson)
          .toList();

  Future<void> requestOutflow(
          int amountXaf, String category, String description) =>
      _api.post('/cash/outflows', body: {
        'amountXaf': amountXaf,
        'category': category,
        'description': description
      });

  Future<void> cancelOutflow(String id) =>
      _api.post('/cash/outflows/$id/cancel');

  /// [method] HANDOVER (with [recipientId]) or BANK_DEPOSIT (with [bankReference]).
  Future<void> remit(
          {required int amountXaf,
          required String method,
          String? recipientId,
          String? bankReference,
          String? note}) =>
      _api.post('/cash/remittances', body: {
        'amountXaf': amountXaf,
        'method': method,
        'recipientId': recipientId,
        'bankReference': bankReference,
        'note': note,
      });

  Future<void> recount(String shiftId, int countedCash, String? reason) =>
      _api.post('/cash/shifts/$shiftId/recount',
          body: {'countedCash': countedCash, 'reason': reason});
}
