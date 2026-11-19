import 'package:lutiano_core/lutiano_core.dart';

class CashShift {
  const CashShift(
      {required this.id,
      required this.openingFloat,
      required this.expectedCash,
      required this.openedAt,
      required this.saleCount});

  factory CashShift.fromJson(Map<String, dynamic> j) => CashShift(
      id: asString(j['id']),
      openingFloat: asInt(j['opening_float']),
      expectedCash: asInt(j['expected_cash']),
      openedAt: asTime(j['opened_at']),
      saleCount: asInt(j['sale_count']));

  final String id;
  final int openingFloat, expectedCash, saleCount;
  final DateTime openedAt;
}

class ShiftCloseResult {
  const ShiftCloseResult(
      {required this.expectedCash,
      required this.countedCash,
      required this.variance});

  factory ShiftCloseResult.fromJson(Map<String, dynamic> j) => ShiftCloseResult(
      expectedCash: asInt(j['expectedCash']),
      countedCash: asInt(j['countedCash']),
      variance: asInt(j['variance']));

  final int expectedCash, countedCash, variance;
}

/// One permanent movement in a shift's cash ledger. [amountXaf] is signed; [balanceAfterXaf] is the expected cash after it.
class CashLedgerEntry {
  const CashLedgerEntry(
      {required this.type,
      required this.amountXaf,
      required this.balanceAfterXaf,
      required this.createdAt,
      this.sourceReference});

  factory CashLedgerEntry.fromJson(Map<String, dynamic> j) => CashLedgerEntry(
        type: asString(j['entry_type']),
        amountXaf: asInt(j['amount_xaf']),
        balanceAfterXaf: asInt(j['balance_after_xaf']),
        createdAt: asTime(j['created_at']),
        sourceReference: asStringOrNull(j['source_reference']),
      );

  final String type;
  final int amountXaf, balanceAfterXaf;
  final DateTime createdAt;
  final String? sourceReference;
}

/// Cash requested from the drawer for an expense; it leaves the drawer only once a manager approves it.
class CashOutflow {
  const CashOutflow(
      {required this.id,
      required this.reference,
      required this.amountXaf,
      required this.category,
      required this.description,
      required this.status,
      required this.requestedAt,
      this.decisionNote,
      this.decidedByName});

  factory CashOutflow.fromJson(Map<String, dynamic> j) => CashOutflow(
        id: asString(j['id']),
        reference: asString(j['reference']),
        amountXaf: asInt(j['amount_xaf']),
        category: asString(j['category']),
        description: asString(j['description']),
        status: asString(j['status']),
        requestedAt: asTime(j['requested_at']),
        decisionNote: asStringOrNull(j['decision_note']),
        decidedByName: asStringOrNull(j['decided_by_name']),
      );

  static const categories = [
    'SUPPLIES',
    'TRANSPORT',
    'MAINTENANCE',
    'CLEANING',
    'OTHER'
  ];

  final String id, reference, category, description, status;
  final int amountXaf;
  final DateTime requestedAt;
  final String? decisionNote, decidedByName;
}

/// Cash handed to a manager or deposited at a bank. Outstanding until the recipient confirms the amount.
class CashRemittance {
  const CashRemittance(
      {required this.id,
      required this.reference,
      required this.amountXaf,
      required this.method,
      required this.status,
      required this.recordedAt,
      this.recipientName,
      this.bankReference,
      this.receivedXaf,
      this.acknowledgedByName,
      this.acknowledgementNote});

  factory CashRemittance.fromJson(Map<String, dynamic> j) => CashRemittance(
        id: asString(j['id']),
        reference: asString(j['reference']),
        amountXaf: asInt(j['amount_xaf']),
        method: asString(j['method']),
        status: asString(j['status']),
        recordedAt: asTime(j['recorded_at']),
        recipientName: asStringOrNull(j['recipient_name']),
        bankReference: asStringOrNull(j['bank_reference']),
        receivedXaf: asIntOrNull(j['received_xaf']),
        acknowledgedByName: asStringOrNull(j['acknowledged_by_name']),
        acknowledgementNote: asStringOrNull(j['acknowledgement_note']),
      );

  final String id, reference, method, status;
  final int amountXaf;
  final DateTime recordedAt;
  final String? recipientName,
      bankReference,
      acknowledgedByName,
      acknowledgementNote;
  final int? receivedXaf;
}

/// A shift with its ledger totals, counts and custody records.
class CashShiftDetail {
  const CashShiftDetail(
      {required this.id,
      required this.status,
      required this.openedAt,
      required this.openingFloat,
      required this.cashSalesXaf,
      required this.refundsPaidXaf,
      required this.outflowsXaf,
      required this.remittedXaf,
      required this.expectedCash,
      required this.saleCount,
      this.closedAt,
      this.countedCash,
      this.reviewStatus,
      this.reviewNote,
      this.reviewedByName,
      this.reviewedAt,
      this.varianceReason,
      this.counts = const [],
      this.outflows = const [],
      this.remittances = const []});

  factory CashShiftDetail.fromJson(Map<String, dynamic> j) => CashShiftDetail(
        id: asString(j['id']),
        status: asString(j['status']),
        openedAt: asTime(j['opened_at']),
        closedAt: asTimeOrNull(j['closed_at']),
        openingFloat: asInt(j['opening_float']),
        cashSalesXaf: asInt(j['cash_sales_xaf']),
        refundsPaidXaf: asInt(j['refunds_paid_xaf']),
        outflowsXaf: asInt(j['outflows_xaf']),
        remittedXaf: asInt(j['remitted_xaf']),
        expectedCash: asInt(j['expected_cash']),
        saleCount: asInt(j['sale_count']),
        countedCash: asIntOrNull(j['counted_cash']),
        reviewStatus: asStringOrNull(j['review_status']),
        reviewNote: asStringOrNull(j['review_note']),
        reviewedByName: asStringOrNull(j['reviewed_by_name']),
        reviewedAt: asTimeOrNull(j['reviewed_at']),
        varianceReason: asStringOrNull(j['variance_reason']),
        counts: asList(j['counts']).map(CashCount.fromJson).toList(),
        outflows: asList(j['outflows']).map(CashOutflow.fromJson).toList(),
        remittances:
            asList(j['remittances']).map(CashRemittance.fromJson).toList(),
      );

  final String id, status;
  final DateTime openedAt;
  final DateTime? closedAt;
  final int openingFloat,
      cashSalesXaf,
      refundsPaidXaf,
      outflowsXaf,
      remittedXaf,
      expectedCash,
      saleCount;
  final int? countedCash;
  final String? reviewStatus, reviewNote, reviewedByName, varianceReason;
  final DateTime? reviewedAt;

  /// Every count of the shift, oldest first: the closing count and any recount a manager asked for.
  final List<CashCount> counts;
  final List<CashOutflow> outflows;
  final List<CashRemittance> remittances;

  bool get isOpen => status == 'OPEN';
  bool get recountRequested => reviewStatus == 'RECOUNT_REQUESTED';
  int? get variance => countedCash == null ? null : countedCash! - expectedCash;
}

/// A manager or administrator who can receive a cash handover.
class CashRecipient {
  const CashRecipient(this.id, this.name);
  factory CashRecipient.fromJson(Map<String, dynamic> j) =>
      CashRecipient(asString(j['id']), asString(j['full_name']));
  final String id, name;
}

/// One count of a closed shift's drawer: the closing count (INITIAL) or a recount (RECOUNT).
class CashCount {
  const CashCount(
      {required this.kind,
      required this.expectedXaf,
      required this.countedXaf,
      required this.createdAt,
      this.reason,
      this.countedByName});

  factory CashCount.fromJson(Map<String, dynamic> j) => CashCount(
        kind: asString(j['kind']),
        expectedXaf: asInt(j['expected_xaf']),
        countedXaf: asInt(j['counted_xaf']),
        createdAt: asTime(j['created_at']),
        reason: asStringOrNull(j['reason']),
        countedByName: asStringOrNull(j['counted_by_name']),
      );

  final String kind;
  final int expectedXaf, countedXaf;
  final DateTime createdAt;
  final String? reason, countedByName;
  int get variance => countedXaf - expectedXaf;
  bool get isRecount => kind == 'RECOUNT';
}

/// What needs the seller's attention across all of their shifts.
class CashOverview {
  const CashOverview(
      {this.pendingOutflows = 0,
      this.outstandingRemittances = 0,
      this.outstandingRemittancesXaf = 0,
      this.disputedRemittances = 0,
      this.recountsRequested = 0});

  factory CashOverview.fromJson(Map<String, dynamic> j) => CashOverview(
        pendingOutflows: asInt(j['pending_outflows']),
        outstandingRemittances: asInt(j['outstanding_remittances']),
        outstandingRemittancesXaf: asInt(j['outstanding_remittances_xaf']),
        disputedRemittances: asInt(j['disputed_remittances']),
        recountsRequested: asInt(j['recounts_requested']),
      );

  final int pendingOutflows,
      outstandingRemittances,
      outstandingRemittancesXaf,
      disputedRemittances,
      recountsRequested;

  bool get isEmpty =>
      pendingOutflows == 0 &&
      outstandingRemittances == 0 &&
      disputedRemittances == 0 &&
      recountsRequested == 0;
}
