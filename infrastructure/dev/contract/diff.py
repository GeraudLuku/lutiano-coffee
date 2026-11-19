"""Compares two snapshots. Exit code 0 = identical (ignoring the paths listed in volatile.json), 1 = differences."""
import json
import sys


def load(p):
    return json.load(open(p))


def walk(a, b, path, out):
    if type(a) != type(b):
        out.append((path, a, b))
    elif isinstance(a, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a:
                out.append((f'{path}.{k}', '<missing>', b[k]))
            elif k not in b:
                out.append((f'{path}.{k}', a[k], '<missing>'))
            else:
                walk(a[k], b[k], f'{path}.{k}', out)
    elif isinstance(a, list):
        if len(a) != len(b):
            out.append((f'{path}[len]', len(a), len(b)))
        for i, (x, y) in enumerate(zip(a, b)):
            walk(x, y, f'{path}[{i}]', out)
    elif a != b:
        out.append((path, a, b))


def ignored(route_key, path, volatile):
    import re
    generic = re.sub(r'\[\d+\]', '[]', path)
    return any(generic == v or generic.startswith(v + '[') or generic.startswith(v + '.') for v in volatile.get(route_key, []))


def main(a_path, b_path, limit=40, volatile_path='baseline/volatile.json'):
    a, b = load(a_path)['entries'], load(b_path)['entries']
    try:
        volatile = json.load(open(volatile_path))
    except OSError:
        volatile = {}
    problems = []
    for k in sorted(set(a) | set(b)):
        if k not in a:
            problems.append((k, 'NEW in second snapshot (not in baseline)'))
        elif k not in b:
            problems.append((k, 'MISSING in second snapshot'))
        else:
            out = []
            walk(a[k], b[k], '', out)
            out = [o for o in out if not ignored(k, o[0], volatile)]
            for path, x, y in out[:3]:
                problems.append((k, f'{path or "<root>"}: {str(x)[:70]} -> {str(y)[:70]}'))
    new_only = [p for p in problems if p[1].startswith('NEW')]
    real = [p for p in problems if not p[1].startswith('NEW')]
    for k, msg in real[:limit]:
        print('DIFF', k, '|', msg)
    print(f'{len(a)} baseline entries, {len(real)} differences, {len(new_only)} new entries (new routes are not failures)')
    return 1 if real else 0


if __name__ == '__main__':
    sys.exit(main(*sys.argv[1:3]))
