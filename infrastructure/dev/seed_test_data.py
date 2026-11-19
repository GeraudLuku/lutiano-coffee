#!/usr/bin/env python3
"""
Seeds a local LUTIANO database with realistic test data for every admin module.

Everything is created through the API, so stock, cash, payment and loyalty ledgers stay consistent with the
business rules. After each simulated day, that day's rows are moved back to business hours (07:00–18:00 Douala)
on a past date, giving ~45 days of history for charts, reports and reviews.

Run against a database that holds only the demo data from the migrations (the script refuses to run twice):

    python3 infrastructure/dev/seed_test_data.py

Requirements: the API on http://localhost:8080 and the `lutiano-mariadb-1` container (see infrastructure/docker-compose.yml).
Every account it creates uses the password Coffee123! (staff), as the demo accounts do.
"""
import json
import os
import random
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

API = "http://localhost:8080/api/v1"
DB = ["docker", "exec", "-i", "lutiano-mariadb-1", "mariadb", "-uroot", "-plocal-root", "lutiano", "-N", "-B"]
PASSWORD = "Coffee123!"
OTP_DEV_CODE = os.environ.get("LUTIANO_OTP_DEV_CODE", "123456")
DAYS = 45                       # days of history
OPEN_DAY_SECONDS = 11 * 3600    # each simulated day is spread over 07:00–18:00 Douala (06:00–17:00 UTC)
DOUALA = timezone(timedelta(hours=1))
rnd = random.Random(20261116)


# ----------------------------------------------------------------------------------------------------------------- io

class ApiError(Exception):
    def __init__(self, status, body):
        super().__init__(f"{status}: {body}")
        self.status, self.body = status, body


def call(method, path, token=None, body=None, headers=None, raw=None, content_type="application/json"):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(API + path, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", content_type)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            text = r.read().decode()
            body = json.loads(text) if text else None
            # Every JSON response is a GenericResponse envelope: {success, data, error, timestamp}.
            return body["data"] if isinstance(body, dict) and "success" in body else body
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        try:
            message = json.loads(raw)["error"]["message"]
        except Exception:  # noqa: BLE001
            message = raw[:300]
        raise ApiError(e.code, message) from None


def get_all(path, token):
    """Every row of a paginated list endpoint ({items, page, pages, ...})."""
    rows, page = [], 1
    while True:
        sep = "&" if "?" in path else "?"
        p = call("GET", f"{path}{sep}page={page}&size=200", token)
        rows += p["items"]
        if page >= p["pages"]:
            return rows
        page += 1


def try_call(*args, **kwargs):
    """Calls the API; returns None when the business rules refuse (e.g. out of stock), which the simulation accepts."""
    try:
        return call(*args, **kwargs)
    except ApiError as e:
        if e.status in (400, 409):
            return None
        raise


def sql(statement):
    out = subprocess.run(DB, input=statement.encode(), capture_output=True, check=True)
    return out.stdout.decode().strip()


def login(email, password=PASSWORD):
    session = call("POST", "/auth/login", body={"email": email, "password": password})
    if session.get("mfaRequired"):  # administrators confirm with an SMS code; in SMS dev mode the dev code is accepted
        session = call("POST", "/auth/mfa/verify", body={"challenge": session["challenge"], "code": OTP_DEV_CODE})
    return session["token"]


def upload(path, token, filename, content_type, payload, fields):
    boundary = uuid.uuid4().hex
    parts = []
    for k, v in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode())
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: {content_type}\r\n\r\n'.encode()
                 + payload + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return call("POST", path, token, raw=b"".join(parts), content_type=f"multipart/form-data; boundary={boundary}")


def tiny_pdf(title):
    return f"%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj " \
           f"3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 300 144]>>endobj\n% {title}\ntrailer<</Root 1 0 R>>\n%%EOF".encode()


# ------------------------------------------------------------------------------------------------------------ backdating

# Columns moved when the rows they belong to were written during a simulated day.
TIME_COLUMNS = {
    "sale": ["created_at"], "payment": ["created_at", "confirmed_at"], "payment_event": ["created_at"],
    "cash_shift": ["opened_at", "closed_at", "reviewed_at"], "cash_ledger_entry": ["created_at"], "cash_count": ["created_at"],
    "cash_outflow": ["requested_at", "decided_at"], "cash_remittance": ["recorded_at", "acknowledged_at"],
    "stock_movement": ["created_at"], "stock_receipt": ["created_at"], "stock_discrepancy": ["created_at"],
    "stock_allocation": ["created_at", "approved_at", "dispatched_at", "received_at", "closed_at"],
    "stock_adjustment": ["created_at", "decided_at"], "stocktake": ["created_at", "submitted_at", "decided_at"],
    "refund": ["requested_at", "decided_at", "paid_at"], "refund_event": ["created_at"], "refund_evidence": ["uploaded_at"],
    "loyalty_ledger": ["created_at"], "loyalty_award": ["created_at"], "reward_redemption": ["created_at", "reversed_at"],
    "points_adjustment": ["created_at"], "audit_event": ["created_at"], "customer": ["created_at"],
    "staff_user": ["created_at", "deactivated_at"], "operation": ["created_at"], "product": ["created_at", "updated_at"],
    "product_price_history": ["changed_at"], "product_category": ["created_at"], "addon": ["created_at"],
    "seller_assignment_log": ["created_at"], "seller_document": ["uploaded_at", "reviewed_at"],
    "earning_rule": ["created_at", "updated_at"], "reward": ["created_at", "updated_at"], "loyalty_settings": ["updated_at"],
    "commission_rule": ["created_at", "superseded_at"], "sale_commission": ["created_at"], "seller_target": ["created_at"],
    "earning_adjustment": ["created_at"],
}
# Human-readable references carry the business date (SL-20261116-00012); they follow their row.
REFERENCES = {"sale": "created_at", "payment": "created_at", "refund": "requested_at", "cash_outflow": "requested_at",
              "cash_remittance": "recorded_at", "reward_redemption": "created_at"}


def db_now():
    """Database time to the whole second (row timestamps have second precision)."""
    return sql("select date_format(utc_timestamp(), '%Y-%m-%d %H:%i:%s.000000')")


def tick():
    """Rows are timestamped to the second; pausing between phases lets backdating spread a day over business hours."""
    time.sleep(1.05)


def backdate(since, days_ago):
    """Maps everything written since `since` onto 07:00–18:00 Douala, `days_ago` days back, keeping the order."""
    now = datetime.strptime(db_now(), "%Y-%m-%d %H:%M:%S.%f")
    start = datetime.strptime(since, "%Y-%m-%d %H:%M:%S.%f")
    duration_us = max(1, int((now - start).total_seconds() * 1_000_000))
    factor = min(OPEN_DAY_SECONDS * 1_000_000 / duration_us, 50_000)
    target_day = (datetime.now(DOUALA) - timedelta(days=days_ago)).date()
    base = datetime(target_day.year, target_day.month, target_day.day, 6, 0, 0)  # 07:00 Douala in UTC
    base_s = base.strftime("%Y-%m-%d %H:%M:%S")
    today_ref = datetime.now(DOUALA).strftime("%Y%m%d")
    day_ref = target_day.strftime("%Y%m%d")

    def mapped(col):
        return f"timestampadd(microsecond, round(timestampdiff(microsecond, '{since}', {col}) * {factor:.6f}), '{base_s}')"

    stmts = ["set foreign_key_checks=0;"]
    for table, col in REFERENCES.items():
        stmts.append(f"update {table} set reference=replace(reference, '-{today_ref}-', '-{day_ref}-') where {col} >= '{since}';")
    # Point batches keep their validity period: move expiry by the same amount as the earning time.
    stmts.append(f"update point_batch set expires_at = expires_at - interval timestampdiff(second, {mapped('earned_at')}, earned_at) second, "
                 f"earned_at = {mapped('earned_at')} where earned_at >= '{since}';")
    for table, cols in TIME_COLUMNS.items():
        sets = ", ".join(f"{c} = if({c} >= '{since}', {mapped(c)}, {c})" for c in cols)
        where = " or ".join(f"{c} >= '{since}'" for c in cols)
        stmts.append(f"update {table} set {sets} where {where};")
    sql("\n".join(stmts))


# Columns that keep a text copy of a reference (e.g. "Purchase SL-20261116-00012"); backdating renames the record's own
# reference, so these copies are rewritten from the real records once seeding is done.
REFERENCE_COPIES = {"loyalty_ledger": ["reference", "description"], "stock_movement": ["reference_label"], "audit_event": ["details"],
                    "refund_event": ["details"], "payment_event": ["details"]}
REFERENCE_SOURCES = {"SL": "sale", "PAY": "payment", "RF": "refund", "CO": "cash_outflow", "RM": "cash_remittance", "RWD": "reward_redemption"}


def fix_reference_copies():
    """References are unique by prefix and number, so every copy is mapped back to the record's real (backdated) reference."""
    import re
    real = {}
    for prefix, table in REFERENCE_SOURCES.items():
        for ref in sql(f"select reference from {table}").splitlines():
            parts = ref.split("-")
            real[(prefix, parts[-1])] = ref
    token = re.compile(r"\b(SL|PAY|RF|CO|RM|RWD)-(\d{8})-(\d{5})\b")
    fixed = 0
    for table, cols in REFERENCE_COPIES.items():
        for col in cols:
            rows = sql(f"select id, {col} from {table} where {col} regexp '(SL|PAY|RF|CO|RM|RWD)-[0-9]{{8}}-[0-9]{{5}}'")
            updates = []
            for line in rows.splitlines():
                row_id, _, text = line.partition("\t")
                if "\\" in text:
                    continue  # batch output escapes special characters; never rewrite those rows
                new = token.sub(lambda m: real.get((m.group(1), m.group(3)), m.group(0)), text)
                if new != text:
                    updates.append(f"update {table} set {col}='{new.replace(chr(39), chr(39) * 2)}' where id='{row_id}';")
            for i in range(0, len(updates), 500):
                sql("\n".join(updates[i:i + 500]))
            fixed += len(updates)
    print(f"  fixed {fixed} reference copies")


# ------------------------------------------------------------------------------------------------------------- catalogue

PRODUCTS = [
    # sku, name, category, price, unit, track, loyalty, promotion, low_stock, addons
    ("BEV-AME", "Americano", "Beverage", 1800, "cup", True, True, True, 10, True),
    ("BEV-MOC", "Mocha", "Beverage", 3500, "cup", True, True, True, 10, True),
    ("BEV-FIL", "Filter Coffee", "Beverage", 1500, "cup", False, True, True, 0, False),
    ("CLD-CBR", "Cold Brew 330ml", "Cold Drinks", 1700, "bottle", True, True, True, 12, False),
    ("CLD-ILT", "Iced Latte", "Cold Drinks", 3200, "cup", True, True, True, 8, True),
    ("CLD-RTD", "Ready-to-Drink Coffee 250ml", "Cold Drinks", 1500, "can", True, True, True, 12, False),
    ("PAS-CRO", "Butter Croissant", "Pastries", 1200, "piece", True, True, False, 10, False),
    ("PAS-MUF", "Chocolate Muffin", "Pastries", 1500, "piece", True, True, False, 10, False),
    ("MER-TUM", "LUTIANO Tumbler", "Merchandise", 8000, "piece", True, False, False, 5, False),
]
PARENTS = [
    ("BEV-CAP", "Cappuccino", "Beverage", "cup", True, [("250ml", "BEV-CAP-250", 2500), ("350ml", "BEV-CAP-350", 3200)]),
    ("BNS-ARA", "Arabica Beans", "Coffee Beans", "pack", True, [("250g", "BNS-ARA-250", 6000), ("500g", "BNS-ARA-500", 11000)]),
]
ADDONS = [("Extra Shot", 500), ("Oat Milk", 400), ("Vanilla Syrup", 300), ("Caramel Syrup", 300)]

LOCATIONS = [
    ("Yaoundé Bastos Kiosk", "FIXED_POS", "Rue 1.839, Bastos", "Yaoundé", "Centre", 3.892, 11.508),
    ("Bonamoussadi Bike", "MOBILE", "Carrefour Bonamoussadi", "Douala", "Littoral", 4.095, 9.740),
    ("Ndokoti Market Agent", "MARKET_AGENT", "Marché Ndokoti", "Douala", "Littoral", 4.044, 9.733),
    ("Marché Central Agent", "MARKET_AGENT", "Marché Central", "Yaoundé", "Centre", 3.867, 11.516),
]
STAFF = [
    ("Grace Ewane", "grace.ewane@lutiano.local", "ADMIN", None, "+237 699 10 20 30", None),
    ("Joel Tchami", "joel.tchami@lutiano.local", "INVENTORY_MANAGER", None, "+237 677 44 55 66", None),
    ("Marie Tabe", "marie.tabe@lutiano.local", "SELLER", "FIXED_POS_BARISTA", "+237 670 11 22 33", "Bonapriso Cafe"),
    ("Paul Nguena", "paul.nguena@lutiano.local", "SELLER", "FIXED_POS_BARISTA", "+237 671 22 33 44", "Yaoundé Bastos Kiosk"),
    ("Armel Nguema", "armel.nguema@lutiano.local", "SELLER", "MOBILE_BARISTA", "+237 672 33 44 55", "Akwa Mobile"),
    ("Sandra Eto", "sandra.eto@lutiano.local", "SELLER", "MOBILE_BARISTA", "+237 673 44 55 66", "Bonamoussadi Bike"),
    ("Brice Fokou", "brice.fokou@lutiano.local", "SELLER", "MARKET_AGENT", "+237 674 55 66 77", "Ndokoti Market Agent"),
    ("Aïcha Bello", "aicha.bello@lutiano.local", "SELLER", "MARKET_AGENT", "+237 675 66 77 88", "Marché Central Agent"),
    ("Kevin Mbarga", "kevin.mbarga@lutiano.local", "SELLER", "MOBILE_BARISTA", "+237 676 77 88 99", "Akwa Mobile"),
    ("Ruth Ngo", "ruth.ngo@lutiano.local", "SELLER", "FIXED_POS_BARISTA", "+237 677 88 99 00", "Bonapriso Cafe"),
]
FIRST = ["Lucas", "Marie", "Paul", "Aïcha", "Brice", "Sandra", "Eric", "Claire", "Nadia", "Martin", "Josiane", "Hervé", "Carine", "Patrick",
         "Linda", "Yannick", "Estelle", "Franck", "Mireille", "Serge", "Odile", "Thierry", "Blandine", "Rodrigue", "Vanessa", "Arnaud",
         "Christelle", "Junior", "Prisca", "Didier", "Laure", "Boris"]
LAST = ["Kamga", "Dupont", "Nguena", "Fokou", "Mvondo", "Ekwalla", "Bessala", "Tchoumi", "Ngono", "Ekong", "Abena", "Tamba", "Manga",
        "Essomba", "Njoya", "Owona", "Fotso", "Biyong", "Mballa", "Ndzana"]


# ---------------------------------------------------------------------------------------------------------- simulation

class Seed:
    def __init__(self):
        self.admin = login("admin@lutiano.local")
        self.inventory = login("inventory@lutiano.local")
        self.admin2 = None
        self.sellers = {}      # email -> {id, token, name, ops: [op ids]}
        self.ops = {}          # name -> op dict
        self.products = []     # sellable: {id, name, price, track, addons}
        self.addon_ids = []
        self.customers = []    # {id, token, name}
        self.stock = {}        # (op_id, product_id) -> qty we believe is there
        self.sales_by_day = {}
        self.rewards = []

    # ---- setup -------------------------------------------------------------------------------------------------------

    def setup(self):
        print("• Staff, locations and catalogue")
        for name, kind, address, city, region, lat, lng in LOCATIONS:
            call("POST", "/operations", self.admin, {"name": name, "type": kind, "address": address, "city": city, "region": region,
                                                     "latitude": lat, "longitude": lng})
        for o in get_all("/operations", self.admin):
            self.ops[o["name"]] = o
        for full, email, role, seller_role, phone, op in STAFF:
            u = call("POST", "/staff", self.admin, {"fullName": full, "email": email, "phone": phone, "role": role, "sellerRole": seller_role,
                                                   "address": "Douala" if "Douala" in str(op) or op is None else "Yaoundé", "area": op or "Head office",
                                                   "password": PASSWORD})
            if role == "SELLER":
                call("POST", f"/staff/{u['id']}/operations/{self.ops[op]['id']}", self.admin)
                self.sellers[email] = {"id": u["id"], "name": full, "ops": [self.ops[op]["id"]]}
        demo = get_all("/staff", self.admin)
        demo_seller = next(s for s in demo if s["email"] == "seller@lutiano.local")
        self.sellers["seller@lutiano.local"] = {"id": demo_seller["id"], "name": demo_seller["full_name"], "ops": [self.ops["Bonapriso Cafe"]["id"]]}
        self.admin2 = login("grace.ewane@lutiano.local")
        self.inventory2 = login("joel.tchami@lutiano.local")
        for email, s in self.sellers.items():
            s["token"] = login(email)

        # Eligibility documents: some verified, one pending, one rejected.
        docs = [("marie.tabe@lutiano.local", "NATIONAL_ID", "verify"), ("marie.tabe@lutiano.local", "CONTRACT", "verify"),
                ("paul.nguena@lutiano.local", "NATIONAL_ID", "verify"), ("paul.nguena@lutiano.local", "CONTRACT", None),
                ("armel.nguema@lutiano.local", "NATIONAL_ID", "reject"), ("brice.fokou@lutiano.local", "HEALTH_CERTIFICATE", "verify")]
        for email, kind, decision in docs:
            sid = self.sellers[email]["id"]
            d = upload(f"/staff/{sid}/documents", self.admin, f"{kind.lower()}.pdf", "application/pdf", tiny_pdf(kind),
                       {"type": kind, "expiresOn": (datetime.now() + timedelta(days=rnd.choice([20, 200, 700]))).strftime("%Y-%m-%d"), "note": "Scanned copy"})
            if decision == "verify":
                call("POST", f"/staff/{sid}/documents/{d['id']}/verify", self.admin, {"note": "Checked against the original"})
            elif decision == "reject":
                call("POST", f"/staff/{sid}/documents/{d['id']}/reject", self.admin, {"note": "Photo is blurred, please upload again"})

        for name in ["Cold Drinks", "Pastries", "Merchandise"]:
            call("POST", "/product-categories", self.admin, {"name": name, "description": f"{name} sold at LUTIANO points", "active": True})
        for name, price in ADDONS:
            self.addon_ids.append(call("POST", "/addons", self.admin, {"name": name, "priceXaf": price, "active": True})["id"])
        for sku, name, cat, price, unit, track, loyal, promo, low, addons in PRODUCTS:
            p = call("POST", "/products", self.admin, {"sku": sku, "name": name, "category": cat, "description": f"{name} from LUTIANO",
                                                      "unit": unit, "priceXaf": price, "lowStockThreshold": low, "active": True, "trackStock": track,
                                                      "loyaltyEligible": loyal, "promotionEligible": promo, "requireVariants": False})
            if addons:
                call("PUT", f"/products/{p['id']}/addons", self.admin, {"addonIds": self.addon_ids})
        for sku, name, cat, unit, addons, variants in PARENTS:
            p = call("POST", "/products", self.admin, {"sku": sku, "name": name, "category": cat, "description": f"{name} from LUTIANO", "unit": unit,
                                                      "active": True, "trackStock": True, "loyaltyEligible": True, "promotionEligible": True, "requireVariants": True})
            for vname, vsku, vprice in variants:
                call("POST", f"/products/{p['id']}/variants", self.admin, {"variantName": vname, "sku": vsku, "unit": unit, "priceXaf": vprice,
                                                                         "lowStockThreshold": 8, "trackStock": True, "active": True})
            if addons:
                call("PUT", f"/products/{p['id']}/addons", self.admin, {"addonIds": self.addon_ids})
        for existing in ["Espresso", "Cafe Latte"]:
            pid = next(p["id"] for p in get_all("/products", self.admin) if p["name"] == existing)
            call("PUT", f"/products/{pid}/addons", self.admin, {"addonIds": self.addon_ids[:3]})
        for p in get_all("/products", self.admin):
            if p["kind"] != "PARENT" and p["active"]:
                self.products.append({"id": p["id"], "name": p["name"], "price": p["price_xaf"], "track": bool(p["track_stock"]),
                                      "category": p["category"], "addons": p["name"] in ("Espresso", "Cafe Latte", "Americano", "Mocha", "Iced Latte")
                                      or p["name"].startswith("Cappuccino")})

        print("• Central stock and opening transfers")
        for p in self.products:
            if p["track"]:
                call("POST", "/inventory/receipts", self.inventory, {"productId": p["id"], "quantity": 1500, "source": "SUPPLIER",
                                                                    "supplier": "Café du Cameroun SA", "reference": f"BL-{rnd.randint(10000, 99999)}"})
        self.restock(qty_range=(70, 110), discrepancy=True)

        print("• Commission rules and targets")
        rates = {"FIXED_POS_BARISTA": 4, "MOBILE_BARISTA": 6, "MARKET_AGENT": 8}   # % of paid line value
        for p in self.products:
            for role, pct in rates.items():
                if p["category"] == "Coffee Beans":
                    body = {"basis": "FIXED_PER_UNIT", "rate": 300 if role != "MARKET_AGENT" else 450}
                elif p["category"] == "Merchandise":
                    body = {"basis": "FIXED_PER_UNIT", "rate": 500}
                else:
                    body = {"basis": "PERCENT", "rate": pct * 100}
                call("POST", "/commissions/rules", self.admin, {"productId": p["id"], "sellerRole": role, "note": "Launch rates", **body})
        # A rate change keeps history: Mobile baristas get 7% on Iced Latte.
        call("POST", "/commissions/rules", self.admin, {"productId": self.pid("Iced Latte"), "sellerRole": "MOBILE_BARISTA", "basis": "PERCENT", "rate": 700,
                                                       "note": "Summer push"})
        today = datetime.now(DOUALA).date()
        last_month = (today.replace(day=1) - timedelta(days=1)).replace(day=1)
        for i, (email, s) in enumerate(self.sellers.items()):
            if email == "ruth.ngo@lutiano.local":
                continue
            for month, goal in ((last_month, 180000 + 20000 * (i % 3)), (today.replace(day=1), 200000 + 25000 * (i % 3))):
                call("POST", "/targets", self.admin, {"sellerId": s["id"], "name": f"Monthly sales {month.strftime('%B')}", "metric": "VALUE",
                                                      "periodType": "MONTHLY", "startsOn": month.isoformat(), "targetValue": goal,
                                                      "bonusType": "FIXED", "bonusXaf": 15000})
        for email, product, qty in (("marie.tabe@lutiano.local", "Cappuccino · 250ml", 25), ("armel.nguema@lutiano.local", "Cold Brew 330ml", 30)):
            call("POST", "/targets", self.admin, {"sellerId": self.sellers[email]["id"], "name": f"Weekly {product.split(' ·')[0]} push", "metric": "QUANTITY",
                                                  "productId": self.pid(product), "periodType": "WEEKLY", "startsOn": (today - timedelta(days=7)).isoformat(),
                                                  "targetValue": qty, "bonusType": "PER_UNIT", "bonusXaf": 100})

        print("• Loyalty programme and members")
        rules = [
            {"name": "Cold drinks rate", "method": "RATE", "scope": "CATEGORY", "category": "Cold Drinks", "points": 12, "amountXaf": 1000},
            {"name": "Coffee beans rate", "method": "RATE", "scope": "CATEGORY", "category": "Coffee Beans", "points": 15, "amountXaf": 1000},
            {"name": "Arabica Beans bonus", "method": "PER_UNIT", "scope": "PRODUCT", "productId": self.parent_id("Arabica Beans"), "points": 50},
            {"name": "First purchase bonus", "method": "FIRST_PURCHASE", "points": 200},
            {"name": "Double points weekend", "method": "MULTIPLIER", "scope": "ALL", "multiplierPct": 200,
             "startsOn": (datetime.now(DOUALA) + timedelta(days=5)).strftime("%Y-%m-%d"), "endsOn": (datetime.now(DOUALA) + timedelta(days=7)).strftime("%Y-%m-%d")},
            {"name": "Pastry launch promo", "method": "MULTIPLIER", "scope": "CATEGORY", "category": "Cold Drinks", "multiplierPct": 150,
             "startsOn": (datetime.now(DOUALA) - timedelta(days=DAYS)).strftime("%Y-%m-%d"), "endsOn": (datetime.now(DOUALA) - timedelta(days=20)).strftime("%Y-%m-%d")},
        ]
        for r in rules:
            call("POST", "/loyalty/rules", self.admin, {"active": True, **r})
        rewards = [
            {"name": "Free Espresso", "rewardType": "FREE_PRODUCT", "pointCost": 400, "productId": self.pid("Espresso"), "requiresStock": True},
            {"name": "Free Cappuccino (250ml)", "rewardType": "FREE_PRODUCT", "pointCost": 500, "productId": self.pid("Cappuccino · 250ml"), "requiresStock": True},
            {"name": "Free Cold Brew (330ml)", "rewardType": "FREE_PRODUCT", "pointCost": 700, "productId": self.pid("Cold Brew 330ml"), "requiresStock": True},
            {"name": "Arabica Beans – 250g", "rewardType": "PRODUCT_EXCHANGE", "pointCost": 1500, "productId": self.pid("Arabica Beans · 250g"), "requiresStock": True, "minPurchases": 3},
            {"name": "10% Discount Voucher", "rewardType": "DISCOUNT_VOUCHER", "pointCost": 1000, "discountPct": 10},
            {"name": "20% Discount Voucher", "rewardType": "DISCOUNT_VOUCHER", "pointCost": 2000, "discountPct": 20, "minSpendXaf": 50000},
            {"name": "LUTIANO Tumbler", "rewardType": "MERCHANDISE", "pointCost": 3500, "productId": self.pid("LUTIANO Tumbler"), "requiresStock": True, "active": False},
        ]
        for r in rewards:
            body = {"requiresStock": False, "minPurchases": 0, "minSpendXaf": 0, "active": True, "description": f"Redeem for {r['name']}", **r}
            self.rewards.append(call("POST", "/loyalty/rewards", self.admin, body))
        for i in range(32):
            first, last = FIRST[i % len(FIRST)], LAST[(i * 7) % len(LAST)]
            c = call("POST", "/customers", self.admin, {"fullName": f"{first} {last}", "phone": f"+2376{rnd.randint(50, 99)}{rnd.randint(100000, 999999)}",
                                                       "phoneVerified": rnd.random() > 0.15})
            self.customers.append({"id": c["id"], "token": c["loyalty_token"], "name": c["full_name"], "weight": rnd.choice([1, 1, 2, 3, 5])})

    def pid(self, name):
        if "·" in name:
            base, variant = [x.strip() for x in name.split("·")]
            return next(p["id"] for p in self.products if p["name"].startswith(base) and variant in p["name"])
        return next(p["id"] for p in self.products if p["name"] == name)

    def parent_id(self, name):
        return next(p["id"] for p in get_all("/products", self.admin) if p["name"] == name and p["kind"] == "PARENT")

    def restock(self, qty_range, discrepancy=False):
        """A supplier delivery to central stock, then transfers to every location through the full approval flow
        (request → approve → dispatch → confirm)."""
        if not discrepancy:
            for p in self.products:
                if p["track"]:
                    call("POST", "/inventory/receipts", self.inventory, {"productId": p["id"], "quantity": 600, "source": "SUPPLIER",
                                                                        "supplier": "Café du Cameroun SA", "reference": f"BL-{rnd.randint(10000, 99999)}"})
        first = True
        for op_name, op in self.ops.items():
            receiver = next((s for s in self.sellers.values() if op["id"] in s["ops"]), None)
            for p in self.products:
                if not p["track"] or (op["type"] == "MARKET_AGENT" and p["category"] == "Beverage"):
                    continue
                qty = rnd.randint(*qty_range) if p["category"] != "Merchandise" else 10
                t = call("POST", "/inventory/allocations", self.inventory, {"operationId": op["id"], "productId": p["id"], "quantity": qty, "note": "Weekly replenishment"})
                call("POST", f"/inventory/allocations/{t['id']}/approve", self.admin, {"note": "OK"})
                call("POST", f"/inventory/allocations/{t['id']}/dispatch", self.inventory)
                got = qty - 2 if discrepancy and first and receiver else qty
                first = False
                token = receiver["token"] if receiver else self.inventory
                call("POST", "/inventory/allocations/confirm", token, {"allocationId": t["id"], "quantity": got,
                                                                       "note": "2 bottles broken in transit" if got < qty else None})
                self.stock[(op["id"], p["id"])] = self.stock.get((op["id"], p["id"]), 0) + got

    # ---- one business day --------------------------------------------------------------------------------------------

    def day(self, days_ago):
        weekday = (datetime.now(DOUALA) - timedelta(days=days_ago)).weekday()
        busy = 1.4 if weekday >= 5 else 1.0
        working = [e for e in self.sellers if e != "ruth.ngo@lutiano.local" and not (e == "kevin.mbarga@lutiano.local" and days_ago < 20)]
        if weekday == 6:
            working = working[: max(3, len(working) - 3)]   # fewer sellers on Sundays
        shifts = {}
        for email in working:
            s = self.sellers[email]
            float_ = rnd.choice([5000, 10000, 10000, 15000])
            call("POST", "/shifts/open", s["token"], {"openingFloat": float_})
            shifts[email] = call("GET", "/shifts/open", s["token"])[0]["id"]
        tick()

        # Sales, round-robin so every drawer is busy all day.
        sales_today = []
        rounds = int(rnd.randint(4, 9) * busy)
        for _ in range(rounds):
            tick()
            for email in working:
                if rnd.random() < 0.15:
                    continue
                sale = self.sell(email)
                if sale:
                    sales_today.append(sale)
        self.sales_by_day[days_ago] = sales_today

        # Outflows: requested by sellers, mostly approved.
        tick()
        for email in working:
            if rnd.random() < 0.18:
                s = self.sellers[email]
                o = try_call("POST", "/cash/outflows", s["token"], {"amountXaf": rnd.choice([1000, 1500, 2500, 3000]),
                                                                    "category": rnd.choice(["SUPPLIES", "TRANSPORT", "CLEANING", "MAINTENANCE"]),
                                                                    "description": rnd.choice(["Ice and paper cups", "Taxi to collect milk", "Cleaning products",
                                                                                               "Bike tyre repair", "Sugar sachets"])})
                if o and days_ago > 0:
                    if rnd.random() < 0.85:
                        try_call("POST", f"/cash/outflows/{o['id']}/approve", self.admin, {"note": "Receipt seen"})
                    else:
                        call("POST", f"/cash/outflows/{o['id']}/reject", self.admin, {"note": "Buy supplies from the central store"})

        self.refunds(days_ago, shifts)
        self.loyalty_activity(days_ago)
        if days_ago == 22:
            call("POST", "/earnings/adjustments", self.admin, {"sellerId": self.sellers["kevin.mbarga@lutiano.local"]["id"], "amountXaf": -90000,
                                                               "note": "Recovery of a cash shortage agreed with the seller"})
            call("POST", "/earnings/adjustments", self.admin, {"sellerId": self.sellers["marie.tabe@lutiano.local"]["id"], "amountXaf": 5000,
                                                               "note": "Customer service award"})
        if days_ago == 0:
            return  # today: shifts stay open, remittances and approvals pending

        # Remittances, then close the drawer (sometimes with a variance).
        tick()
        for email in working:
            s = self.sellers[email]
            detail = call("GET", f"/cash/shifts/{shifts[email]}", s["token"])
            expected = int(detail["expected_cash"])
            remit = expected - int(detail["opening_float"])
            if remit > 2000:
                remit = remit // 500 * 500
                if rnd.random() < 0.25:
                    r = call("POST", "/cash/remittances", s["token"], {"amountXaf": remit, "method": "BANK_DEPOSIT", "bankReference": f"AFB-{rnd.randint(100000, 999999)}"})
                    if days_ago > 2:
                        call("POST", f"/cash/remittances/{r['id']}/acknowledge", self.admin, {"receivedXaf": remit})
                else:
                    to_admin = rnd.random() < 0.6
                    recipient = next(x["id"] for x in call("GET", "/cash/recipients", s["token"])
                                     if x["full_name"] == ("LUTIANO Administrator" if to_admin else "Joel Tchami"))
                    r = call("POST", "/cash/remittances", s["token"], {"amountXaf": remit, "method": "HANDOVER", "recipientId": recipient})
                    if days_ago > 2:
                        short = rnd.random() < 0.04
                        call("POST", f"/cash/remittances/{r['id']}/acknowledge", self.admin if to_admin else self.inventory2,
                             {"receivedXaf": remit - (1000 if short else 0), "note": "Envelope was 1,000 XAF short" if short else "Counted"})
                expected -= remit
            variance = 0 if rnd.random() < 0.88 else rnd.choice([-1500, -500, -200, 300, 500])
            call("POST", "/shifts/close", s["token"], {"countedCash": expected + variance,
                                                      "varianceReason": rnd.choice(["Gave wrong change", "Coin miscount", "Customer paid extra and left"]) if variance else None})
            if variance and days_ago > 3:
                if rnd.random() < 0.7:
                    call("POST", f"/cash/shifts/{shifts[email]}/review", self.admin, {"decision": "ACCEPT", "note": "Small variance accepted, seller reminded"})
                else:
                    call("POST", f"/cash/shifts/{shifts[email]}/review", self.admin, {"decision": "RECOUNT", "note": "Please recount with your supervisor"})
                    if days_ago > 6:
                        call("POST", f"/cash/shifts/{shifts[email]}/recount", s["token"], {"countedCash": expected, "reason": None})

        # Inventory events on some days.
        if days_ago in (30, 15, 6):
            self.restock(qty_range=(40, 70))
        if days_ago in (25, 12, 4, 1):
            op = rnd.choice(list(self.ops.values()))
            p = rnd.choice([x for x in self.products if x["track"] and self.stock.get((op["id"], x["id"]), 0) > 3])
            a = try_call("POST", "/inventory/adjustments", self.inventory, {"operationId": op["id"], "productId": p["id"], "quantityDelta": -2,
                                                                            "reason": "DAMAGED", "note": "Packaging damaged during storage"})
            if a and days_ago > 2:
                call("POST", f"/inventory/adjustments/{a['id']}/approve", self.admin, {"note": "Approved"})
                self.stock[(op["id"], p["id"])] -= 2
        if days_ago == 10:
            op = self.ops["Bonapriso Cafe"]
            st = call("POST", "/inventory/stocktakes", self.inventory, {"operationId": op["id"], "note": "Monthly count"})
            st = call("GET", f"/inventory/stocktakes/{st['id']}", self.inventory)
            counts = [{"productId": l["product_id"], "countedQuantity": max(0, int(l["expected_quantity"]) - (1 if i == 0 else 0))}
                      for i, l in enumerate(st["lines"])]
            call("PUT", f"/inventory/stocktakes/{st['id']}/counts", self.inventory, {"counts": counts})
            call("POST", f"/inventory/stocktakes/{st['id']}/submit", self.inventory)
            call("POST", f"/inventory/stocktakes/{st['id']}/approve", self.admin, {"note": "One croissant unaccounted for"})

    def sell(self, email):
        s = self.sellers[email]
        op_id = s["ops"][0]
        op = next(o for o in self.ops.values() if o["id"] == op_id)
        choices = [p for p in self.products if (not p["track"] or self.stock.get((op_id, p["id"]), 0) > 0)
                   and not (op["type"] == "MARKET_AGENT" and p["category"] == "Beverage")]
        if not choices:
            return None
        weights = [5 if p["category"] in ("Beverage", "Cold Drinks") else 2 if p["category"] == "Pastries" else 1 for p in choices]
        lines = {}
        for _ in range(rnd.choice([1, 1, 1, 2, 2, 3])):
            p = rnd.choices(choices, weights)[0]
            qty = rnd.choice([1, 1, 1, 2]) if p["category"] != "Coffee Beans" else 1
            addons = rnd.sample(self.addon_ids, rnd.choice([1, 1, 2])) if p["addons"] and rnd.random() < 0.3 else []
            key = (p["id"], tuple(sorted(addons)))
            lines[key] = lines.get(key, 0) + qty
        body = {"operationId": op_id, "lines": [{"productId": k[0], "quantity": q, "addonIds": list(k[1])} for k, q in lines.items()]}
        if rnd.random() < 0.45:
            c = rnd.choices(self.customers, [c["weight"] for c in self.customers])[0]
            body["customerToken"] = c["token"]
        estimate = sum(next(p["price"] for p in self.products if p["id"] == k[0]) * q + 500 * len(k[1]) * q for k, q in lines.items())
        body["cashReceivedXaf"] = int(-(-estimate // 1000) * 1000 + rnd.choice([0, 0, 1000, 5000]))
        sale = try_call("POST", "/sales", s["token"], body, headers={"Idempotency-Key": str(uuid.uuid4())})
        if sale:
            for k, q in lines.items():
                if (op_id, k[0]) in self.stock:
                    self.stock[(op_id, k[0])] -= q
        return sale

    def refunds(self, days_ago, shifts):
        """A few refunds a week in every state: paid from a drawer or an advance, approved but unpaid, pending, rejected, cancelled."""
        older = [s for d, sales in self.sales_by_day.items() if days_ago < d <= days_ago + 6 for s in sales]
        if not older or rnd.random() > 0.35:
            return
        sale = rnd.choice(older)
        full = rnd.random() < 0.4
        amount = sale["total_xaf"] if full else max(500, sale["total_xaf"] // 2 // 100 * 100)
        r = try_call("POST", "/refunds", self.admin, {"saleId": sale["id"], "amountXaf": amount,
                                                      "reason": rnd.choice(["DAMAGED_PRODUCT", "WRONG_ITEM", "CUSTOMER_REQUEST", "PRODUCT_DEFECT"]),
                                                      "customerName": sale.get("customer_name") or rnd.choice(FIRST) + " " + rnd.choice(LAST)})
        if not r:
            return
        fate = rnd.random() if days_ago > 1 else 0.95
        if fate < 0.1:
            call("POST", f"/refunds/{r['id']}/reject", self.admin2, {"note": "Product was consumed; outside the refund policy"})
        elif fate < 0.15:
            call("POST", f"/refunds/{r['id']}/cancel", self.admin, {"note": "Customer accepted a replacement instead"})
        elif fate < 0.9:
            call("POST", f"/refunds/{r['id']}/approve", self.admin2, {"note": "Verified with the location"})
            if fate < 0.8:
                drawer = next(iter(shifts.values()))
                paid = try_call("POST", f"/refunds/{r['id']}/pay", self.admin, {"fundSource": "SHIFT", "shiftId": drawer,
                                                                               "recipientName": r.get("customer_name") or "Customer", "recipientAcknowledged": True})
                if not paid:
                    call("POST", f"/refunds/{r['id']}/pay", self.admin, {"fundSource": "ADVANCE", "advanceReference": f"ADV-{rnd.randint(1000, 9999)}",
                                                                         "advanceAmountXaf": -(-amount // 5000) * 5000,
                                                                         "advanceReturnedXaf": -(-amount // 5000) * 5000 - amount,
                                                                         "recipientName": r.get("customer_name") or "Customer", "recipientAcknowledged": True})
                if rnd.random() < 0.3:
                    detail = call("GET", f"/refunds/{r['id']}", self.admin)
                    line = next((l for l in detail["returnable"] if l["tracked"] and l["returnable"] > 0), None)
                    if line:
                        try_call("POST", f"/refunds/{r['id']}/stock-returns", self.admin, {"lines": [{"productId": line["product_id"], "quantity": 1}],
                                                                                          "note": "Unopened, fit to sell"})

    def loyalty_activity(self, days_ago):
        if rnd.random() < 0.5:
            for c in rnd.sample(self.customers, 3):
                detail = call("GET", f"/customers/{c['id']}", self.admin)
                affordable = [r for r in self.rewards if r["active"] and r["point_cost"] <= detail["points_balance"]]
                if affordable:
                    reward = rnd.choice(affordable)
                    x = try_call("POST", "/loyalty/redemptions", self.admin, {"customerId": c["id"], "rewardId": reward["id"],
                                                                              "operationId": self.ops["Bonapriso Cafe"]["id"], "customerConfirmed": True})
                    if x and rnd.random() < 0.08:
                        call("POST", f"/loyalty/redemptions/{x['id']}/reverse", self.admin, {"reason": "Item was out of stock at handover"})
                    break
        if rnd.random() < 0.12:
            c = rnd.choice(self.customers)
            reason = rnd.choice(["GOODWILL", "PROMOTION", "CORRECTION", "PROGRAM_REVIEW"])
            try_call("POST", "/loyalty/adjustments", self.admin, {"customerId": c["id"], "points": rnd.choice([100, 200, 300, 500]), "reason": reason,
                                                                  "note": {"GOODWILL": "Order was late", "PROMOTION": "Launch campaign bonus",
                                                                           "CORRECTION": "Points missed on a sale", "PROGRAM_REVIEW": "Quarterly review"}[reason]})

    # ---- final touches -----------------------------------------------------------------------------------------------

    def finish(self):
        print("• Pending work for today (approvals, transfers, stocktake)")
        demo = self.sellers["seller@lutiano.local"]
        ops = list(self.ops.values())
        # Transfers in every state.
        for state in ["PENDING", "PENDING", "APPROVED", "IN_TRANSIT", "REJECTED"]:
            op = rnd.choice(ops)
            p = rnd.choice([x for x in self.products if x["track"]])
            t = call("POST", "/inventory/allocations", self.inventory, {"operationId": op["id"], "productId": p["id"], "quantity": rnd.randint(20, 40),
                                                                        "note": "Stock running low"})
            if state in ("APPROVED", "IN_TRANSIT"):
                call("POST", f"/inventory/allocations/{t['id']}/approve", self.admin, {"note": "OK"})
            if state == "IN_TRANSIT":
                call("POST", f"/inventory/allocations/{t['id']}/dispatch", self.inventory)
            if state == "REJECTED":
                call("POST", f"/inventory/allocations/{t['id']}/reject", self.admin, {"note": "Central stock reserved for the Yaoundé opening"})
        seller_request = call("POST", "/inventory/allocations", demo["token"], {"operationId": self.ops["Bonapriso Cafe"]["id"],
                                                                                "productId": self.pid("Butter Croissant"), "quantity": 30, "note": "Weekend demand"})
        # Adjustment and stocktake waiting for approval.
        try_call("POST", "/inventory/adjustments", self.inventory, {"operationId": self.ops["Akwa Mobile"]["id"], "productId": self.pid("Cold Brew 330ml"),
                                                                    "quantityDelta": -3, "reason": "EXPIRED", "note": "Best-before date passed"})
        st = call("POST", "/inventory/stocktakes", self.inventory, {"operationId": self.ops["Yaoundé Bastos Kiosk"]["id"], "note": "Cycle count"})
        st = call("GET", f"/inventory/stocktakes/{st['id']}", self.inventory)
        call("PUT", f"/inventory/stocktakes/{st['id']}/counts", self.inventory,
             {"counts": [{"productId": l["product_id"], "countedQuantity": int(l["expected_quantity"])} for l in st["lines"][:4]]})
        # A price change (history), a deactivated product, an inactive location and a deactivated seller.
        mocha = self.pid("Mocha")
        prod = call("GET", f"/products/{mocha}", self.admin)
        call("PUT", f"/products/{mocha}", self.admin, {"sku": prod["sku"], "name": prod["name"], "category": prod["category"], "description": prod["description"],
                                                      "unit": prod["unit"], "priceXaf": 3700, "lowStockThreshold": prod["low_stock_threshold"], "active": True,
                                                      "trackStock": True, "loyaltyEligible": True, "promotionEligible": True, "requireVariants": False})
        call("POST", f"/products/{self.pid('Ready-to-Drink Coffee 250ml')}/deactivate", self.admin)
        call("POST", f"/operations/{self.ops['Marché Central Agent']['id']}/deactivate", self.admin)
        ruth = self.sellers["ruth.ngo@lutiano.local"]["id"]
        call("POST", f"/staff/{ruth}/deactivate", self.admin, {"reason": "Contract ended"})
        # Points expiring soon and already expired, so expiry views have data.
        sql("update point_batch set expires_at = utc_timestamp() + interval 12 day where source_type='ADJUSTMENT' and remaining > 0 limit 4;"
            "update point_batch set expires_at = utc_timestamp() - interval 2 day where source_type='SALE' and remaining > 0 and earned_at < utc_timestamp() - interval 40 day limit 3;")
        call("POST", "/loyalty/expire", self.admin)
        print("• Finalising last month's earnings")
        periods = get_all("/earnings/periods", self.admin)
        for p in sorted(periods, key=lambda x: x["starts_on"]):
            if p["status"] == "OPEN" and call("GET", f"/earnings/periods/{p['id']}", self.admin)["can_finalize"]:
                call("POST", f"/earnings/periods/{p['id']}/finalize", self.admin)
        self.payouts()
        return seller_request

    def payouts(self):
        """A12: payout numbers in every state, and payouts of last month's earnings in every state (the approval
        threshold stays 0, so every payout needs a second administrator)."""
        print("• Payout numbers and payouts")
        self.admin2 = self.admin2 or login("grace.ewane@lutiano.local")
        a1, a2 = self.admin, self.admin2
        seller = lambda email: self.sellers[email]["id"]

        def number(maker, checker, email, provider, phone, holder, outcome="VERIFIED"):
            d = call("POST", "/payouts/destinations", maker, {"sellerId": seller(email), "provider": provider, "phone": phone, "accountName": holder})
            if outcome == "VERIFIED":
                call("POST", f"/payouts/destinations/{d['id']}/verify", checker, {"code": OTP_DEV_CODE})
            elif outcome == "REJECTED":
                call("POST", f"/payouts/destinations/{d['id']}/reject", checker, {"reason": "The name does not match the account holder"})
            return d

        number(a1, a2, "marie.tabe@lutiano.local", "MTN_MOMO", "+237 677 41 20 11", "Marie Tabe")
        number(a2, a1, "paul.nguena@lutiano.local", "ORANGE_MONEY", "+237 699 52 31 08", "Paul Nguena")
        number(a1, a2, "sandra.eto@lutiano.local", "MTN_MOMO", "+237 678 90 12 45", "Sandra Eto")
        number(a1, a2, "aicha.bello@lutiano.local", "MTN_MOMO", "+237 670 11 22 33", "Aicha Bello")
        number(a2, a1, "aicha.bello@lutiano.local", "MTN_MOMO", "+237 671 44 55 66", "Aicha Bello")      # replaces the previous number
        number(a1, a2, "armel.nguema@lutiano.local", "MTN_MOMO", "+237 650 33 44 55", "Armel Nguema", "PENDING")
        number(a1, a2, "brice.fokou@lutiano.local", "ORANGE_MONEY", "+237 655 77 88 99", "Brice Fokou", "REJECTED")

        def open_period(email):
            rows = call("GET", f"/payouts/balances/{seller(email)}/periods?openOnly=true&size=5", a1)["items"]
            return rows[0] if rows else None

        def record(maker, email, share, method, note=None):
            p = open_period(email)
            if not p:
                return None
            amount = max(1000, int(p["remaining_xaf"] * share) // 100 * 100) if share < 1 else p["remaining_xaf"]
            return call("POST", "/payouts", maker, {"sellerId": seller(email), "periodId": p["period_id"], "amountXaf": amount, "method": method, "note": note})

        ref = iter(range(48210, 49000))
        def settle(p, approver, payer):
            call("POST", f"/payouts/{p['id']}/approve", approver, {"note": "Checked against the statement"})
            body = {} if p["method"] == "CASH" else {"providerReference": f"MP{next(ref)}{rnd.randint(1000, 9999)}"}
            paid = call("POST", f"/payouts/{p['id']}/pay", payer, body)
            kind = "transfer-confirmation" if p["method"] != "CASH" else "signed-receipt"
            upload(f"/payouts/{p['id']}/evidence", payer, f"{kind}.pdf", "application/pdf", tiny_pdf(kind.replace("-", " ").title()), {})
            return paid

        marie, paul, sandra = "marie.tabe@lutiano.local", "paul.nguena@lutiano.local", "sandra.eto@lutiano.local"
        brice, aicha = "brice.fokou@lutiano.local", "aicha.bello@lutiano.local"
        # Marie: a cash part now, the rest by MTN MoMo (partial payouts of one period).
        p = record(a1, marie, 0.4, "CASH", "First part, in cash at the weekly meeting")
        if p: settle(p, a2, a1)
        p = record(a2, marie, 1, "MTN_MOMO", "Remainder")
        if p: settle(p, a1, a2)
        # Paul: paid, then corrected away (the transfer bounced), and a second one paid whose reversal waits for review.
        p = record(a1, paul, 0.5, "ORANGE_MONEY")
        if p:
            settle(p, a2, a1)
            call("POST", f"/payouts/{p['id']}/reversal", a1, {"reason": "The transfer bounced: wrong number"})
            call("POST", f"/payouts/{p['id']}/reversal/approve", a2, {"note": "Confirmed with Orange Money"})
        p = record(a2, paul, 0.3, "ORANGE_MONEY")
        if p:
            settle(p, a1, a2)
            call("POST", f"/payouts/{p['id']}/reversal", a2, {"reason": "Recorded against the wrong month"})
        # Sandra: approved, waiting to be paid.  Brice: waiting for approval.  Aicha: one rejected, one cancelled.
        p = record(a1, sandra, 0.6, "MTN_MOMO")
        if p: call("POST", f"/payouts/{p['id']}/approve", a2, {})
        record(a2, brice, 0.5, "CASH", "Cash at the Bonapriso office")
        p = record(a1, aicha, 0.5, "CASH")
        if p: call("POST", f"/payouts/{p['id']}/reject", a2, {"reason": "Amount does not match the statement"})
        p = record(a1, aicha, 0.5, "MTN_MOMO")
        if p: call("POST", f"/payouts/{p['id']}/cancel", a1, {"note": "Seller asked to be paid in cash"})


def payouts_only():
    """Adds the A12 payout data to a database that is already seeded."""
    seed = Seed()
    for email in [e for e in (s[1] for s in STAFF) if e.endswith("@lutiano.local")]:
        try:
            seed.sellers[email] = {"id": call("GET", f"/staff?q={email}", seed.admin)["items"][0]["id"]}
        except (ApiError, IndexError):
            pass
    seed.payouts()
    print("Payout data added.")


def main():
    if "payouts" in sys.argv[1:]:
        return payouts_only()
    try:
        call("GET", "/health")
    except Exception as e:  # noqa: BLE001
        sys.exit(f"The API is not reachable on {API}: {e}")
    if sql("select count(*) from staff_user where email='grace.ewane@lutiano.local'") != "0":
        sys.exit("This database is already seeded. Restore a clean database first (see the docstring).")
    started = time.time()
    seed = Seed()
    demo = login("seller@lutiano.local")
    for open_shift in call("GET", "/shifts/open", demo):  # a shift left open by manual testing; closed before history starts
        call("POST", "/shifts/close", demo, {"countedCash": open_shift["expected_cash"], "varianceReason": None})
    tick()
    t0 = db_now()
    seed.setup()
    backdate(t0, DAYS + 15)
    for days_ago in range(DAYS, -1, -1):
        tick()
        since = db_now()
        seed.day(days_ago)
        if days_ago > 0:
            backdate(since, days_ago)
        n = len(seed.sales_by_day.get(days_ago, []))
        print(f"  day -{days_ago:>2}: {n} sales")
    since = db_now()
    seed.finish()
    fix_reference_copies()
    print(sql("select concat(count(*), ' sales, ', sum(total_xaf), ' XAF') from sale"))
    print(f"Done in {time.time() - started:.0f}s. Staff password for every new account: {PASSWORD}")


if __name__ == "__main__":
    main()
