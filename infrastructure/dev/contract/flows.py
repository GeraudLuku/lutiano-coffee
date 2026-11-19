"""Records the status, content type and JSON *shape* (keys and types, not values) of write flows and error cases.

    python3 flows.py baseline/flows.json    # creates test records with a unique suffix on every run
    python3 diff.py baseline/flows.json after_flows.json

Values differ on every run (ids, references, dates), so only the shape is compared. Covers the envelope for 200/201/202/204,
idempotent replays, validation, authentication, permission, not-found, conflict, size and media-type errors, binary downloads,
and the main business flows (catalogue, locations, staff, stock requests, shifts and sales, orders, support, loyalty).
"""
import json
import sys
import time
import urllib.request
import uuid

from common import BASE, call, shape, tokens as principal_tokens, login

RUN = str(int(time.time()))[-6:]
T = principal_tokens()
out = {}


def rec(label, entry):
    status, ct, disp, cache, payload = entry
    r = {'status': status, 'type': ct}
    if isinstance(payload, (dict, list)):
        body = payload.get('data') if isinstance(payload, dict) and 'success' in payload else payload
        r['envelope'] = sorted(k for k in payload) if isinstance(payload, dict) else 'list'
        r['shape'] = shape(body if body is not None else payload.get('error') if isinstance(payload, dict) else None)
    else:
        r['bytes'] = 'empty' if not payload else ('pdf' if payload[:4] == b'%PDF' else 'png' if payload[:4] == b'\x89PNG' else 'zip' if payload[:2] == b'PK' else 'other')
    out[label] = r
    return payload.get('data') if isinstance(payload, dict) and 'data' in payload else payload


def step(label, who, method, path, body=None, headers=None, token=None):
    return rec(label, call(method, '/api/v1' + path, body, token if token else T.get(who), headers))


def multipart(label, who, path, filename, content, field='file', ctype='image/png', fields=None):
    boundary = '----flow' + uuid.uuid4().hex
    text = ''.join(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n' for k, v in (fields or {}).items())
    data = text.encode() + (f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"; filename="{filename}"\r\nContent-Type: {ctype}\r\n\r\n').encode() + content + f'\r\n--{boundary}--\r\n'.encode()
    req = urllib.request.Request(BASE + '/api/v1' + path, data=data, method='POST')
    req.add_header('Content-Type', 'multipart/form-data; boundary=' + boundary)
    req.add_header('Authorization', 'Bearer ' + T[who])
    try:
        r = urllib.request.urlopen(req, timeout=60)
        status, h, raw = r.status, r.headers, r.read()
    except urllib.error.HTTPError as e:
        status, h, raw = e.code, e.headers, e.read()
    except (urllib.error.URLError, BrokenPipeError, ConnectionError):
        out[label] = {'status': 'rejected (413, or the connection closed before the upload finished)'}  # which one happens varies run to run
        return None
    ct = (h.get('Content-Type') or '').split(';')[0]
    if status == 413:
        out[label] = {'status': 'rejected (413, or the connection closed before the upload finished)'}
        return None
    return rec(label, (status, ct, None, None, json.loads(raw) if ct.startswith('application/json') else raw))


PNG = bytes.fromhex('89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000d4944415478da63f8ffff3f0005fe02fea7d6c9b30000000049454e44ae426082')


def main(target):
    def section_0():  # errors and the envelope
        step('err anon login wrong password', 'anon', 'POST', '/auth/login', {'email': 'admin@lutiano.local', 'password': 'wrong'})
        step('err anon login empty body', 'anon', 'POST', '/auth/login', {})
        step('err anon no token', 'anon', 'GET', '/products')
        step('err seller forbidden', 'seller_pos', 'GET', '/staff')
        step('err customer on staff route', 'customer', 'GET', '/products')
        step('err staff on customer route', 'admin', 'GET', '/customer/me')
        step('err not found id', 'admin', 'GET', '/products/00000000-0000-4000-8000-000000000000')
        step('err unknown route', 'admin', 'GET', '/no/such/route')
        step('err method not allowed', 'admin', 'PATCH', '/products')
        step('err missing field', 'admin', 'POST', '/products', {})
        step('err bad email', 'admin', 'POST', '/staff', {'email': 'bad', 'fullName': 'X', 'role': 'ADMIN', 'password': 'Coffee123!'})
        step('err wrong media type', 'admin', 'POST', '/products', headers={'Content-Type': 'text/plain'})
        step('err bad json', 'admin', 'POST', '/product-categories', {'name': 5, 'sortOrder': 'x'})
        step('err bad page param', 'admin', 'GET', '/sales?page=abc')
        step('err page size over max', 'admin', 'GET', '/sales?size=999')
        sellers = step('list sellers', 'admin', 'GET', '/sellers?size=1')
        admin_staff = step('list staff', 'admin', 'GET', '/staff?size=1')
        sid = admin_staff['items'][0]['id']
        multipart('err upload too large', 'admin', f'/staff/{sid}/image', 'big.png', b'0' * (6 * 1024 * 1024))
        multipart('err upload wrong type', 'admin', f'/staff/{sid}/image', 'x.txt', b'hello', ctype='text/plain')

    def section_1():  # catalogue
        cat = step('create category', 'admin', 'POST', '/product-categories', {'name': f'Flow Cat {RUN}', 'description': 'd', 'sortOrder': 9, 'active': True})
        step('err duplicate category', 'admin', 'POST', '/product-categories', {'name': f'Flow Cat {RUN}', 'description': 'd', 'sortOrder': 9, 'active': True})
        step('update category', 'admin', 'PUT', f'/product-categories/{cat["id"]}', {'name': f'Flow Cat {RUN}', 'description': 'changed', 'sortOrder': 8, 'active': True})
        add = step('create addon', 'admin', 'POST', '/addons', {'name': f'Flow Addon {RUN}', 'priceXaf': 250, 'sortOrder': 9, 'active': True})
        step('update addon', 'admin', 'PUT', f'/addons/{add["id"]}', {'name': f'Flow Addon {RUN}', 'priceXaf': 300, 'sortOrder': 8, 'active': True})
        prod = step('create product', 'admin', 'POST', '/products', {'kind': 'SIMPLE', 'sku': f'FLW-{RUN}', 'name': f'Flow Product {RUN}', 'category': cat['name'],
                    'description': 'flow', 'unit': 'cup', 'priceXaf': 1200, 'lowStockThreshold': 3, 'active': True, 'trackStock': True})
        step('get product', 'admin', 'GET', f'/products/{prod["id"]}')
        step('update product', 'admin', 'PUT', f'/products/{prod["id"]}', {'kind': 'SIMPLE', 'sku': f'FLW-{RUN}', 'name': f'Flow Product {RUN}', 'category': cat['name'],
             'description': 'flow2', 'unit': 'cup', 'priceXaf': 1300, 'lowStockThreshold': 3, 'active': True, 'trackStock': True})
        step('product addons', 'admin', 'PUT', f'/products/{prod["id"]}/addons', {'addonIds': [add['id']]})
        multipart('product image', 'admin', f'/products/{prod["id"]}/image', 'p.png', PNG)
        step('deactivate product', 'admin', 'POST', f'/products/{prod["id"]}/deactivate')
        step('activate product', 'admin', 'POST', f'/products/{prod["id"]}/activate')
        step('public product by id', 'anon', 'GET', f'/public/products/{prod["id"]}')

    def section_2():  # locations and staff
        op = step('create operation', 'admin', 'POST', '/operations', {'name': f'Flow Point {RUN}', 'type': 'FIXED_POS', 'address': 'Rue 1', 'city': 'Douala', 'region': 'Littoral',
                  'latitude': 4.05, 'longitude': 9.7, 'active': True, 'acceptedMethods': ['CASH']})
        step('update operation', 'admin', 'PUT', f'/operations/{op["id"]}', {'name': f'Flow Point {RUN}', 'type': 'FIXED_POS', 'address': 'Rue 2', 'city': 'Douala', 'region': 'Littoral',
             'latitude': 4.05, 'longitude': 9.7, 'active': True, 'acceptedMethods': ['CASH']})
        step('operation qr png', 'admin', 'GET', f'/operations/{op["id"]}/qr.png')
        step('operation qr zip', 'admin', 'GET', '/operations/qr-codes.zip')
        seller = step('create seller', 'admin', 'POST', '/staff', {'email': f'flow{RUN}@lutiano.local', 'fullName': f'Flow Seller {RUN}', 'phone': f'+2376{RUN}01'[:13], 'role': 'SELLER',
                      'sellerRole': 'FIXED_POS_BARISTA', 'address': 'x', 'area': 'y', 'password': 'Coffee123!', 'language': 'fr'})
        step('assign seller', 'admin', 'POST', f'/staff/{seller["id"]}/operations/{op["id"]}')
        step('unassign seller', 'admin', 'DELETE', f'/staff/{seller["id"]}/operations/{op["id"]}')
        step('deactivate seller', 'admin', 'POST', f'/staff/{seller["id"]}/deactivate', {'reason': 'flow'})
        step('activate seller', 'admin', 'POST', f'/staff/{seller["id"]}/activate')
        step('err duplicate staff email', 'admin', 'POST', '/staff', {'email': f'flow{RUN}@lutiano.local', 'fullName': 'Dup', 'role': 'ADMIN', 'password': 'Coffee123!'})

    def section_3():  # seller shift and sale on the seeded seller's own location
        seller_tok = login('seller@lutiano.local')
        ops = call('GET', '/api/v1/operations', None, seller_tok)[4]['data']['items']
        opid = ops[0]['id']
        shifts = rec('open shifts', call('GET', '/api/v1/shifts/open', None, seller_tok))
        if not shifts:
            step('open shift', None, 'POST', '/shifts/open', {'openingFloat': 5000}, token=seller_tok)
        stock = call('GET', f'/api/v1/inventory?operationId={opid}&size=50', None, seller_tok)[4]['data']['items']
        item = next((s for s in stock if s.get('quantity', 0) > 3 and s.get('kind') != 'PARENT'), None)
        if item:
            pid = item.get('product_id') or item.get('id')
            key = f'flow-{RUN}'
            sale_body = {'operationId': opid, 'lines': [{'productId': pid, 'quantity': 1, 'addonIds': []}], 'cashReceivedXaf': 100000, 'paymentMethod': 'CASH'}
            sale = step('sale create', None, 'POST', '/sales', sale_body, {'Idempotency-Key': key}, token=seller_tok)
            step('sale replay (idempotent)', None, 'POST', '/sales', sale_body, {'Idempotency-Key': key}, token=seller_tok)
            step('err sale without key', None, 'POST', '/sales', sale_body, token=seller_tok)
            step('err sale empty lines', None, 'POST', '/sales', {'operationId': opid, 'lines': [], 'cashReceivedXaf': 1}, {'Idempotency-Key': key + 'x'}, token=seller_tok)
            if sale:
                step('sale detail', None, 'GET', f'/sales/{sale["id"]}', token=seller_tok)
                step('sale receipt pdf', None, 'GET', f'/sales/{sale["id"]}/receipt.pdf', token=seller_tok)
                step('refund lookup', 'admin', 'GET', f'/refunds/sale?saleReference={sale["reference"]}')
                refund = step('refund request', 'admin', 'POST', '/refunds', {'saleId': sale['id'], 'amountXaf': 100, 'reason': 'CUSTOMER_REQUEST', 'note': 'flow'})
                if refund:
                    step('refund cancel', 'admin', 'POST', f'/refunds/{refund["id"]}/cancel', {'reason': 'flow'})
            out_row = step('cash outflow request', None, 'POST', '/cash/outflows', {'amountXaf': 100, 'category': 'SUPPLIES', 'description': 'flow'}, token=seller_tok)
            if isinstance(out_row, dict) and out_row.get('id'):
                step('cash outflow cancel', None, 'POST', f'/cash/outflows/{out_row["id"]}/cancel', token=seller_tok)

    def section_4():  # stock request (mobile barista at an operation of his own)
        mob = T['seller_mobile']
        mops = call('GET', '/api/v1/operations', None, mob)[4]['data']['items']
        if mops:
            mstock = call('GET', f'/api/v1/inventory?operationId={mops[0]["id"]}&size=5', None, mob)[4]['data']['items']
            if mstock:
                mpid = mstock[0].get('product_id') or mstock[0].get('id')
                req = step('stock request create', 'seller_mobile', 'POST', '/inventory/requests', {'operationId': mops[0]['id'], 'items': [{'productId': mpid, 'quantity': 2}],
                           'reason': 'flow', 'idempotencyKey': f'sr-{RUN}'})
                step('stock request replay', 'seller_mobile', 'POST', '/inventory/requests', {'operationId': mops[0]['id'], 'items': [{'productId': mpid, 'quantity': 2}],
                     'reason': 'flow', 'idempotencyKey': f'sr-{RUN}'})
                if isinstance(req, dict) and req.get('id'):
                    step('stock request detail', 'seller_mobile', 'GET', f'/inventory/requests/{req["id"]}')
                    step('stock request reject', 'inventory', 'POST', f'/inventory/requests/{req["id"]}/reject', {'note': 'flow'})

    def section_5():  # customer
        cust_op = step('public operations', 'customer', 'GET', '/public/operations?size=1')
        cprods = step('public products', 'customer', 'GET', '/public/products?size=5')
        if cust_op['items'] and cprods['items']:
            o = cust_op['items'][0]
            p = next((x for x in cprods['items'] if x.get('id')), None)
            if p:
                body = {'operationId': o['id'], 'lines': [{'productId': p['id'], 'quantity': 1, 'addonIds': []}], 'note': 'flow'}
                order = step('order place', 'customer', 'POST', '/customer/orders', body, {'Idempotency-Key': f'ord-{RUN}'})
                step('order replay', 'customer', 'POST', '/customer/orders', body, {'Idempotency-Key': f'ord-{RUN}'})
                if isinstance(order, dict) and order.get('id'):
                    step('order detail', 'customer', 'GET', f'/customer/orders/{order["id"]}')
                    step('order cancel', 'customer', 'POST', f'/customer/orders/{order["id"]}/cancel')
                step('favourite add', 'customer', 'PUT', f'/customer/favourites/{p["id"]}')
                step('favourite remove', 'customer', 'DELETE', f'/customer/favourites/{p["id"]}')
        step('customer profile update', 'customer', 'PUT', '/customer/me', {'fullName': 'Snapshot Customer', 'email': None, 'language': 'fr', 'birthDate': None, 'marketingConsent': False})
        multipart('customer support create', 'customer', '/customer/support', 'a.png', PNG, fields={'type': 'QUESTION', 'subject': 'flow', 'description': 'flow'})
        multipart('seller support create', 'seller_pos', '/me/support', 'a.png', PNG, fields={'type': 'ISSUE', 'subject': 'flow', 'description': 'flow'})

    def section_6():  # notifications, support, loyalty
        step('mark notifications read', 'admin', 'POST', '/notifications/mine/read', {})
        step('loyalty customer create', 'admin', 'POST', '/customers', {'fullName': f'Flow Customer {RUN}', 'phone': f'+2376{RUN}02'[:13], 'phoneVerified': True, 'status': 'ACTIVE'})

    for section in (section_0, section_1, section_2, section_3, section_4, section_5, section_6):
        try:
            section()
        except Exception as e:  # a failed step must not hide the rest of the snapshot
            out['script error ' + section.__name__] = {'status': 0, 'error': type(e).__name__ + ': ' + str(e)[:80]}

    with open(target, 'w') as f:
        json.dump({'entries': out, 'unresolved': []}, f, indent=1, sort_keys=True)
    codes = {}
    for v in out.values():
        codes[str(v['status'])] = codes.get(str(v['status']), 0) + 1
    print(len(out), 'flow steps', dict(sorted(codes.items())))


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'flows.json')
