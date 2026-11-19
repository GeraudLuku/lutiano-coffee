"""Shared helpers for the API contract snapshot tools (stdlib only)."""
import json
import re
import urllib.error
import urllib.request

import os

BASE = os.environ.get('LUTIANO_BASE', 'http://localhost:8080')
PASSWORD = 'Coffee123!'
DEV_CODE = '123456'
UUID = re.compile(r'[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}')
JWT = re.compile(r'^eyJ[\w-]+\.[\w-]+\.[\w-]*$')


def call(method, path, body=None, token=None, headers=None, raw=False):
    """Returns (status, content_type, content_disposition, cache_control, payload). payload is parsed JSON, or bytes when raw/binary."""
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        req.add_header('Content-Type', 'application/json')
    if token:
        req.add_header('Authorization', 'Bearer ' + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    for attempt in range(2):
        try:
            r = urllib.request.urlopen(req, timeout=60)
            status, h, content = r.status, r.headers, r.read()
        except urllib.error.HTTPError as e:
            status, h, content = e.code, e.headers, e.read()
        if status != 429 or attempt:
            break
        import time
        time.sleep(62)  # a per-minute rate limit: wait for the next window, then retry once
    ct = (h.get('Content-Type') or '').split(';')[0]
    info = (status, ct, h.get('Content-Disposition'), h.get('Cache-Control'))
    if raw or not ct.startswith('application/json'):
        return info + (content,)
    try:
        return info + (json.loads(content),)
    except ValueError:
        return info + (content,)


def login(email, password=PASSWORD):
    s, _, _, _, body = call('POST', '/api/v1/auth/login', {'email': email, 'password': password})
    d = body['data']
    if d.get('mfaRequired'):
        s, _, _, _, body = call('POST', '/api/v1/auth/mfa/verify', {'challenge': d['challenge'], 'code': DEV_CODE})
        d = body['data']
    return d['token']


def customer_login(phone='+237690000099', password=PASSWORD):
    s, _, _, _, body = call('POST', '/api/v1/customer/auth/login', {'phone': phone, 'password': password})
    return body['data']['token']


PRINCIPALS = {
    'anon': lambda: None,
    'admin': lambda: login('admin@lutiano.local'),
    'inventory': lambda: login('inventory@lutiano.local'),
    'seller_pos': lambda: login('marie.tabe@lutiano.local'),
    'seller_mobile': lambda: login('armel.nguema@lutiano.local'),
    'seller_market': lambda: login('aicha.bello@lutiano.local'),
    'customer': lambda: customer_login(),
}


def normalise(v):
    """Masks values that legitimately change between runs (ids, tokens, the envelope timestamp) so shapes and data can be compared."""
    if isinstance(v, dict):
        return {k: normalise(x) for k, x in v.items()}
    if isinstance(v, list):
        return [normalise(x) for x in v]
    if isinstance(v, str):
        if JWT.match(v):
            return '<jwt>'
        return UUID.sub('<uuid>', v)
    return v


def shape(v):
    """Keys and JSON types only (lists: the union over their items). Used for write flows where data differs on every run."""
    if isinstance(v, dict):
        return {k: shape(x) for k, x in v.items()}
    if isinstance(v, list):
        merged = {}
        kinds = set()
        for x in v:
            s = shape(x)
            if isinstance(s, dict):
                for k, t in s.items():
                    merged[k] = merge(merged.get(k), t)
            else:
                kinds.add(s)
        return [merged] if merged else sorted(kinds)
    return type(v).__name__ if v is not None else 'null'


def merge(a, b):
    if a is None:
        return b
    if a == b:
        return a
    if isinstance(a, dict) and isinstance(b, dict):
        return {k: merge(a.get(k), b.get(k)) for k in {**a, **b}}
    if a == 'null':
        return b
    if b == 'null':
        return a
    key = lambda v: json.dumps(v, sort_keys=True) if isinstance(v, (dict, list)) else str(v)   # key order is not significant
    return f'{key(a)}|{key(b)}'


TOKEN_CACHE = __file__.rsplit('/', 1)[0] + '/baseline/.tokens.json'


def tokens():
    """Principal tokens, reused for 6 hours so that signing in (which writes a login time and audit rows) does not change the data being compared."""
    import os
    import time
    try:
        cached = json.load(open(TOKEN_CACHE))
        if time.time() - cached['at'] < 6 * 3600:
            return cached['tokens']
    except (OSError, ValueError, KeyError):
        pass
    fresh = {name: fn() for name, fn in PRINCIPALS.items()}
    os.makedirs(os.path.dirname(TOKEN_CACHE), exist_ok=True)
    json.dump({'at': time.time(), 'tokens': fresh}, open(TOKEN_CACHE, 'w'))
    return fresh
