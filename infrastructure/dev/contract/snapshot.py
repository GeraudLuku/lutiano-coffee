"""Records the API's GET responses for every role into one JSON file (the "golden snapshot").

    python3 snapshot.py baseline/get.json          # run against http://localhost:8080
    python3 diff.py baseline/get.json after.json   # compare two snapshots

Collection routes are called first; ids found in their responses fill the {id} placeholders of detail routes.
The database must not change between two snapshots that are compared (writes are covered by flows.py, which compares shapes).
"""
import json
import re
import sys

from common import PRINCIPALS, call, normalise, tokens as principal_tokens
from routes import routes

# routes that need parameters or non-uuid path values to return something useful
EXTRA_QUERY = {
    '/api/v1/search/suggestions': ['q=cafe'],
    '/api/v1/me/earnings/statement.pdf': ['from=2026-10-01&to=2026-10-31&sections=SUMMARY,COMMISSIONS'],
    '/api/v1/customer/loyalty/estimate': ['amountXaf=5000'],
    '/api/v1/commissions/rules/history': ['productId={product}&sellerRole=FIXED_POS_BARISTA'],
    '/api/v1/customers/scan': ['token=00000000-0000-4000-8000-000000000000'],
    '/api/v1/public/operations/by-qr': ['code=00000000-0000-4000-8000-000000000000'],
    '/api/v1/seller/rewards/lookup': ['code=CU-000006'],
    '/api/v1/refunds/sale': ['reference={sale_ref}'],
    '/api/v1/reports/builder': ['metrics=revenue'],
}
# tried in order when a route answers 400 to a bare call; {operation}, {product}, {sale_ref} come from the principal's own lists
# detail routes whose ids come from a differently named list
ALIAS = {'/api/v1/me/earnings/sales': '/api/v1/sales', '/api/v1/earnings/sellers': '/api/v1/sellers', '/api/v1/payouts/balances': '/api/v1/sellers'}
LADDER = ['operationId={operation}', 'operationId={operation}&from=2026-10-01&to=2026-10-31', 'from=2026-10-01&to=2026-10-31', 'productId={product}']
PATH_VALUES = {'kind': ['products'], 'code': ['COF-LAT'], 'key': ['x'], 'provider': ['google'], 'event': ['PAYOUT_PAID']}
SKIP = re.compile(r'/media/')  # binary files by object key: covered separately in flows.py


def clean(path):
    """{id:[0-9a-f-]{36}} -> {id}"""
    return re.sub(r'\{(\w+):(?:[^{}]|\{[\d,]+\})*\}', r'{\1}', path)


def ids_in(value, found):
    if isinstance(value, dict):
        v = value.get('id')
        if isinstance(v, str) and len(v) == 36:
            found.append(v)
        for x in value.values():
            ids_in(x, found)
    elif isinstance(value, list):
        for x in value:
            ids_in(x, found)


def record(entry):
    status, ct, disp, cache, payload = entry
    out = {'status': status, 'type': ct}
    if disp:
        out['disposition'] = re.sub(r'\d{4}[-\d]*', '<n>', disp)
    if cache:
        out['cache'] = cache
    if isinstance(payload, (dict, list)):
        if isinstance(payload, dict):
            payload = {k: v for k, v in payload.items() if k != 'timestamp'}
        out['body'] = normalise(payload)
    else:
        out['bytes'] = 'empty' if not payload else ('pdf' if payload[:4] == b'%PDF' else 'png' if payload[:4] == b'\x89PNG' else 'zip' if payload[:2] == b'PK' else 'other')
    return out


def main(target):
    gets = sorted({clean(p) for v, p, c in routes() if v == 'GET'})
    tokens = principal_tokens()
    pools = {name: {} for name in tokens}
    refs = {name: {} for name in tokens}
    result = {}
    unresolved = set()

    def fill(q, name):
        values = {'operation': (pools[name].get('/api/v1/operations') or pools['admin'].get('/api/v1/operations') or [''])[0],
                  'product': (pools[name].get('/api/v1/products') or pools['admin'].get('/api/v1/products') or [''])[0],
                  'sale_ref': (refs[name].get('sale') or refs['admin'].get('sale') or [''])[0]}
        for k, v in values.items():
            q = q.replace('{' + k + '}', v)
        return q

    def fetch(name, template, path):
        if '/api/v1/public/' in path and name not in ('anon', 'customer'):
            return  # open endpoints with a per-IP rate limit: two principals are enough
        queries = EXTRA_QUERY.get(template, [''])
        for q in queries:
            q = fill(q, name)
            url = path + ('?' + q if q else '')
            entry = call('GET', url, token=tokens[name])
            if entry[0] == 400 and not q:
                for alt in LADDER:
                    alt = fill(alt, name)
                    trial = call('GET', path + '?' + alt, token=tokens[name])
                    if trial[0] != 400:
                        q, url, entry = alt, path + '?' + alt, trial
                        break
            key = f'{name} GET {template}' + (f' ?{re.sub(r"[0-9a-f]{8}-[0-9a-f-]{27}", "<uuid>", q)}' if q else '')
            result[key] = record(entry)
            if isinstance(entry[4], (dict, list)) and entry[0] == 200:
                found = []
                ids_in(entry[4], found)
                if path.split('?')[0].endswith('/sales') and isinstance(entry[4], dict):
                    refs[name]['sale'] = [i['reference'] for i in entry[4].get('data', entry[4]).get('items', []) if 'reference' in i][:1] if isinstance(entry[4].get('data', entry[4]), dict) else []
                pool = pools[name].setdefault(re.sub(r'/+$', '', path.split('?')[0]), [])
                for i in found:
                    if i not in pool:
                        pool.append(i)

    collections = [p for p in gets if '{' not in p and not SKIP.search(p)]
    details = [p for p in gets if '{' in p and not SKIP.search(p)]
    for _ in range(2):  # twice: some collections need ids from lists that sort after them
        for name in tokens:
            for p in collections:
                fetch(name, p, p)
    # a couple of rounds, because detail ids of one route can come from another detail response
    for name in tokens:
        for template in details:
            prefix = re.sub(r'/+$', '', template.split('{')[0])
            prefix = ALIAS.get(prefix, prefix)
            candidates = list(pools[name].get(prefix, []))[:2]
            for other in ('admin', 'inventory'):
                for i in pools[other].get(prefix, [])[:1]:
                    if i not in candidates:
                        candidates.append(i)
            vars_ = re.findall(r'\{(\w+)\}', template)
            filled = False
            for ident in candidates or [None]:
                path = template
                ok = True
                for v in vars_:
                    value = ident if ident and v in ('id', 'sellerId', 'allocationId', 'documentId') else (PATH_VALUES.get(v) or [None])[0]
                    if value is None:
                        ok = False
                        break
                    path = path.replace('{' + v + '}', value, 1)
                if not ok:
                    break
                fetch(name, template, path)
                filled = True
            if not filled:
                unresolved.add(template)
    with open(target, 'w') as f:
        json.dump({'entries': result, 'unresolved': sorted(unresolved)}, f, indent=1, sort_keys=True)
    statuses = {}
    for e in result.values():
        statuses[e['status']] = statuses.get(e['status'], 0) + 1
    print(len(result), 'responses', dict(sorted(statuses.items())), '| unresolved routes:', len(unresolved))


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'snapshot.json')
