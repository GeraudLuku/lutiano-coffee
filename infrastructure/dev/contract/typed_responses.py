"""Generates typed response records from the golden snapshot and rewrites the controllers to return ResponseEntity<GenericApiResponse<T>>.

    python3 typing.py plan      # what would be generated (counts, names, untyped routes)
    python3 typing.py apply     # writes models/response/<feature>/*.java and rewrites controllers
"""
import collections
import json
import os
import re
import sys

from handlers import handlers, blank
from snapshot import clean

ROOT = __file__.rsplit('/infrastructure/', 1)[0] + '/backend/lutiano-api/src/main/java/com/lutiano/api'
SNAP = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'out', 'get_new.json')
JAVA_KW = {'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final',
           'finally', 'float', 'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private', 'protected', 'public',
           'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'record', 'var'}


# ---- inference
class Node:
    def __init__(self):
        self.kinds = collections.Counter()
        self.fields = collections.OrderedDict()   # key -> Node
        self.present = collections.Counter()      # key -> objects having the key
        self.nulls = collections.Counter()        # key -> objects with a null value
        self.objects = 0
        self.elem = None


def kind_of(v):
    if v is None: return 'null'
    if isinstance(v, bool): return 'bool'
    if isinstance(v, int): return 'int'
    if isinstance(v, float): return 'dec'
    if isinstance(v, str): return 'str'
    return 'obj' if isinstance(v, dict) else 'list'


def feed(node, v):
    k = kind_of(v)
    node.kinds[k] += 1
    if k == 'obj':
        node.objects += 1
        for key, x in v.items():
            node.present[key] += 1
            if x is None: node.nulls[key] += 1
            feed(node.fields.setdefault(key, Node()), x)
    elif k == 'list':
        if node.elem is None: node.elem = Node()
        for x in v: feed(node.elem, x)


def canon(node):
    """A structural key: same key means the same record can be shared."""
    kinds = tuple(sorted(k for k in node.kinds if k != 'null'))
    if kinds == ('obj',):
        return ('obj', tuple((k, canon(f), node.present[k] < node.objects) for k, f in node.fields.items()))
    if kinds == ('list',):
        return ('list', canon(node.elem) if node.elem else None)
    return kinds or ('null',)


def ambiguous(node):
    """A key that is sometimes absent and sometimes null cannot be told apart in a record."""
    if node.objects:
        for k, f in node.fields.items():
            if node.present[k] < node.objects and node.nulls[k]: return True
            if ambiguous(f): return True
    if node.elem and ambiguous(node.elem): return True
    return False


def camel(key):
    parts = re.split(r'[^A-Za-z0-9]+', key)
    c = parts[0][:1].lower() + parts[0][1:] + ''.join(p[:1].upper() + p[1:] for p in parts[1:])
    c = c or 'field'
    if c[0].isdigit(): c = 'n' + c
    return c + 'Value' if c in JAVA_KW else c


def pascal(s):
    return ''.join(p[:1].upper() + p[1:] for p in re.split(r'[^A-Za-z0-9]+', s) if p)


def singular(w):
    if w.endswith('ies'): return w[:-3] + 'y'
    if w.endswith(('sses', 'xes')): return w[:-2]
    if w.endswith('ss') or w.endswith('is') or w.endswith('us'): return w
    if w.endswith('s'): return w[:-1]
    return w


class Gen:
    def __init__(self):
        self.records = {}   # name -> (package, canon, source)
        self.by_canon = {}  # canon -> (package, name)

    def java_type(self, node, hint, pkg, imports):
        kinds = {k for k in node.kinds if k != 'null'}
        if kinds == {'str'}: return 'String'
        if kinds == {'int'}: return 'Long'
        if kinds == {'dec'} or kinds == {'int', 'dec'}:
            imports.add('java.math.BigDecimal'); return 'BigDecimal'
        if kinds == {'bool'}: return 'Boolean'
        if kinds == {'obj'}: return self.record(node, hint, pkg)
        if kinds == {'list'}:
            imports.add('java.util.List')
            e = node.elem
            if e is None or not e.kinds: return 'List<Object>'
            return 'List<' + self.java_type(e, singular(hint), pkg, imports) + '>'
        return 'Object'

    def record(self, node, hint, pkg):
        key = canon(node)
        if key in self.by_canon:
            return self.by_canon[key][1]
        name = pascal(hint) + 'Response' if not hint.endswith('Response') else hint
        base, n = name, 2
        while name in self.records:
            name = base[:-8] + str(n) + 'Response'; n += 1
        self.by_canon[key] = (pkg, name)
        imports, comps = set(), []
        for k, f in node.fields.items():
            t = self.java_type(f, name[:-8] + pascal(singular(k) if f.kinds.get('list') else k), pkg, imports)
            ident = camel(k)
            ann = []
            if node.present[k] < node.objects:
                imports.add('com.fasterxml.jackson.annotation.JsonInclude'); ann.append('@JsonInclude(JsonInclude.Include.NON_NULL)')
            if ident != k:
                imports.add('com.fasterxml.jackson.annotation.JsonProperty'); ann.append(f'@JsonProperty("{k}")')
            comps.append((' '.join(ann) + ' ' if ann else '') + f'{t} {ident}')
        imports |= {'com.fasterxml.jackson.annotation.JsonAnyGetter', 'com.fasterxml.jackson.annotation.JsonAnySetter', 'java.util.Map'}
        comps.append('@JsonAnySetter @JsonAnyGetter Map<String, Object> other')   # keeps keys the sample did not show
        imps = ''.join(f'import {i};\n' for i in sorted(imports))
        src = f'package com.lutiano.api.models.response.{pkg};\n\n{imps}{chr(10) if imps else ""}public record {name}(' + (',\n    '.join(comps) if len(comps) > 3 else ', '.join(comps)).replace('(', '(', 1) + ') {}\n'
        if len(comps) > 3:
            src = src.replace(f'public record {name}(', f'public record {name}(\n    ', 1)
        self.records[name] = (pkg, key, src)
        return name


def route_samples():
    d = json.load(open(SNAP))['entries']
    out = collections.defaultdict(list)
    for k, v in d.items():
        m = re.match(r'\w+ (GET) (\S+)', k)
        if m and v['status'] == 200 and 'body' in v:
            out[m.group(2)].append(v['body'].get('data'))
    return out


def feature_of(h):
    return h['file'].split('/controller/')[1].split('/')[0]


def plan():
    samples = route_samples()
    gen = Gen()
    result = []
    for h in handlers():
        ret = re.sub(r'\s+', ' ', h['ret'])
        kind = None
        if ret.startswith('ResponseEntity') and 'byte[]' in ret: kind = 'binary'
        elif ret.startswith('ResponseEntity'): kind = 'entity'
        elif ret == 'void': kind = 'void'
        elif ret.startswith('PageResponse'): kind = 'page'
        elif ret.startswith('List'): kind = 'list'
        elif ret.startswith('Map'): kind = 'map'
        else: kind = 'other'
        item = dict(h=h, kind=kind, type=None, ret=ret)
        segs = [s for s in h['route'].split('/')[3:] if not s.startswith('{')]
        res = segs[-1] if segs else feature_of(h)
        hint = pascal(singular(res))
        data = samples.get(clean(h['route'])) if h['verb'] == 'GET' else None
        if data and kind in ('map', 'page', 'list'):
            root = Node()
            for dta in data: feed(root, dta)
            pkg = feature_of(h)
            if kind == 'page':
                items = root.fields.get('items')
                if items and items.elem and items.elem.objects and not ambiguous(items.elem):
                    item['type'] = gen.record(items.elem, hint + 'Item', pkg)
            elif kind == 'list':
                if root.elem and root.elem.objects and not ambiguous(root.elem):
                    item['type'] = gen.record(root.elem, hint, pkg)
            else:
                if root.objects and not ambiguous(root):
                    item['type'] = gen.record(root, hint, pkg)
        result.append(item)
    return gen, result


def wrap_returns(raw, blanked, start, end, wrap):
    """Rewrites every `return EXPR;` that belongs to the method itself (not to a lambda or anonymous class inside it)."""
    edits, stack, i = [], [], start
    while i <= end:
        c = blanked[i]
        if c == '{':
            before = blanked[start:i].rstrip()
            stack.append(before.endswith('->') or bool(re.search(r'new\s+[\w.<>]+\s*\((?:[^()]|\([^()]*\))*\)$', before)))
        elif c == '}':
            stack.pop()
        elif blanked.startswith('return', i) and not (blanked[i - 1].isalnum() or blanked[i - 1] == '_') and not blanked[i + 6].isalnum() and not any(stack[1:]):
            j, depth = i + 6, 0
            while True:
                ch = blanked[j]
                if ch in '([{': depth += 1
                elif ch in ')]}': depth -= 1
                elif ch == ';' and depth == 0: break
                j += 1
            expr = raw[i + 6:j].strip()
            edits.append((i, j, f'return {wrap(expr)}'))
            i = j
        i += 1
    for a, b, text in reversed(edits):
        raw = raw[:a] + text + raw[b:]
        blanked = blanked[:a] + ' ' * (len(text)) + blanked[b:] if False else blanked
    return raw, len(edits)


def apply():
    gen, result = plan()
    for name, (pkg, key, src) in gen.records.items():
        d = f'{ROOT}/models/response/{pkg}'
        os.makedirs(d, exist_ok=True)
        open(f'{d}/{name}.java', 'w').write(src)
    byfile = collections.defaultdict(list)
    for r in result: byfile[r['h']['file']].append(r)
    stats = collections.Counter()
    for path, items in byfile.items():
        raw = open(path).read()
        for r in sorted(items, key=lambda r: -r['h']['sig'][0]):
            h, kind, t, ret = r['h'], r['kind'], r['type'], r['ret']
            if kind in ('binary', 'entity', 'other'):
                stats[kind] += 1
                continue
            ann = h['ann']
            created = 'HttpStatus.CREATED' in ann
            accepted = 'HttpStatus.ACCEPTED' in ann
            nocontent = 'NO_CONTENT' in ann
            verb = 'created' if created else 'accepted' if accepted else 'ok'
            if kind == 'void':
                newret = 'ResponseEntity<Void>'; wrap = None
            elif kind == 'map':
                newret = f'ResponseEntity<GenericApiResponse<{t or "Map<String, Object>"}>>'
                wrap = (lambda e, t=t, verb=verb: f'Responses.{verb}({e}, {t}.class)') if t else (lambda e, verb=verb: f'Responses.{verb}({e})')
            elif kind == 'page':
                newret = f'ResponseEntity<GenericApiResponse<PageResponse<{t or "Map<String, Object>"}>>>'
                wrap = (lambda e, t=t: f'Responses.page({e}, {t}.class)') if t else (lambda e: f'Responses.ok({e})')
            else:
                newret = f'ResponseEntity<GenericApiResponse<List<{t}>>>' if t else f'ResponseEntity<GenericApiResponse<{ret}>>'
                wrap = (lambda e, t=t: f'Responses.list({e}, {t}.class)') if t else (lambda e: f'Responses.ok({e})')
            bs, be = h['body']
            blanked = blank(raw)
            if kind == 'void':
                body = raw[bs:be]
                body = body[:-0] if False else body
                raw = raw[:bs] + raw[bs:be].rstrip() + '\n    return Responses.noContent();\n  ' + raw[be:]
            else:
                new_body, n = wrap_returns(raw, blanked, bs, be, wrap)
                # wrap_returns returns the whole text with edits applied inside the body span
                raw = new_body
            # signature and annotation (positions before the body are unchanged by the body edit)
            a, b = h['sig']
            raw = raw[:a] + newret + raw[b:]
            region = raw[a - len(ann):a]
            raw = raw[:a - len(ann)] + re.sub(r'[ \t]*@ResponseStatus\((?:HttpStatus\.)?(?:CREATED|ACCEPTED|NO_CONTENT)\)\s*\n', '', region) + raw[a:]
            stats[kind + ('-typed' if t else '-untyped')] += 1
        # drop @ResponseStatus on rewritten handlers
        if 'import org.springframework.http.ResponseEntity;' not in raw:
            m = list(re.finditer(r'^import .*;$', raw, re.M)); raw = raw[:m[-1].end()] + '\nimport org.springframework.http.ResponseEntity;' + raw[m[-1].end():]
        open(path, 'w').write(raw)
    print(dict(stats))


if __name__ == '__main__':
    if sys.argv[1:] == ['apply']:
        apply(); sys.exit(0)
    gen, result = plan()
    c = collections.Counter((r['kind'], bool(r['type'])) for r in result)
    print(dict(c)); print(len(gen.records), 'record types')
    if sys.argv[1:] == ['plan']:
        for r in result[:0]: pass
