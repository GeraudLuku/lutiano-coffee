"""Compares the values stored in the database with the Java enums, so a value in the data that no enum knows (or the reverse) is caught.

    python3 enum_check.py        # needs the lutiano-mariadb-1 container with the seeded data
"""
import glob
import re
import subprocess
import sys

ENUMS = __file__.rsplit('/infrastructure/', 1)[0] + '/backend/lutiano-api/src/main/java/com/lutiano/api/definitions/enums'
# (table, column, enum, allow_extra_enum_values): enum constants that never occur in the data yet are fine (extra=True)
COLUMNS = [
    ('staff_user', 'role', 'Role'), ('staff_user', 'seller_role', 'SellerRole'), ('operation', 'type', 'OperationType'),
    ('customer_order', 'status', 'OrderStatus'), ('announcement', 'audience', 'AnnouncementAudience'),
    ('cash_outflow', 'category', 'OutflowCategory'), ('notification_delivery', 'status', 'DeliveryStatus'),
    ('earning_rule', 'method', 'EarningMethod'), ('earning_rule', 'scope', 'EarningScope'), ('reward', 'reward_type', 'RewardType'),
    ('points_adjustment', 'reason', 'PointsAdjustmentReason'), ('customer', 'status', 'CustomerStatus'),
    ('seller_target', 'metric', 'TargetMetric'), ('seller_target', 'period_type', 'TargetPeriod'), ('seller_target', 'bonus_type', 'BonusType'),
    ('commission_rule', 'basis', 'CommissionBasis'), ('seller_document', 'doc_type', 'SellerDocumentType'),
    ('stock_adjustment', 'reason', 'StockAdjustmentReason'), ('refund', 'status', 'RefundStatus'), ('refund', 'reason', 'RefundReason'),
    ('payout_destination', 'provider', 'PayoutProvider'), ('payout_destination', 'status', 'DestinationStatus'),
    ('payout', 'status', 'PayoutStatus'), ('payout', 'method', 'PayoutMethod'), ('stock_receipt', 'source', 'ReceiptSource'),
    ('support_request', 'type', 'SupportType'), ('support_request', 'status', 'SupportStatus'),
    ('stock_movement', 'movement_type', 'MovementType'), ('search_outbox', 'type', 'SearchType'),
]


def enum_values(name):
    src = open(f'{ENUMS}/{name}.java').read()
    body = src[src.index('{') + 1:src.rindex('}')]
    return {v.strip() for v in re.split(r'[,;]', body) if re.fullmatch(r'\s*[A-Z_0-9a-z]+\s*', v)}


def distinct(table, column):
    sql = f'select distinct {column} from {table} where {column} is not null'
    out = subprocess.run(['docker', 'exec', 'lutiano-mariadb-1', 'mariadb', '-uroot', '-plocal-root', 'lutiano', '-N', '-e', sql], capture_output=True, text=True)
    if out.returncode:
        return None
    return {l.strip() for l in out.stdout.splitlines() if l.strip()}


bad = 0
for table, column, enum in COLUMNS:
    stored = distinct(table, column)
    if stored is None:
        print(f'skip   {table}.{column}: not in this schema')
        continue
    unknown = stored - enum_values(enum)
    status = 'OK    ' if not unknown else 'UNKNOWN'
    bad += bool(unknown)
    print(f'{status} {table}.{column} -> {enum}: {len(stored)} stored values' + (f', not in the enum: {sorted(unknown)}' if unknown else ''))
sys.exit(1 if bad else 0)
