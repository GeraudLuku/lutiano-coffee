"""Lists every HTTP route declared by the backend controllers (method, full path template), by reading the Java source.

Works on the current layout and on the restructured one: it scans every *Controller.java under the backend source tree.
"""
import glob
import re
import sys

SRC = __file__.rsplit('/infrastructure/', 1)[0] + '/backend/lutiano-api/src/main/java'
CONTEXT = '/api/v1'
ANN = re.compile(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(((?:[^()]|\([^()]*\))*)\))?')
CLASS_MAP = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?((?:[^()]|\([^()]*\))*)\)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+class')


def literal_parts(expr, consts):
    """'ID + "/x"' -> '/{id}/x' by substituting known String constants and concatenating literals."""
    out = ''
    for tok in re.findall(r'"((?:[^"\\]|\\.)*)"|([A-Z_][A-Z0-9_]*)', expr):
        out += tok[0] if tok[0] or not tok[1] else consts.get(tok[1], '')
    return out


def first_path(args, consts):
    if not args:
        return ''
    m = re.search(r'(?:value|path)\s*=\s*([^,]+)', args)
    expr = m.group(1) if m else args.split(',')[0]
    if re.search(r'produces|consumes', expr):
        return ''
    return literal_parts(expr, consts)


def routes():
    found = []
    for path in sorted(glob.glob(SRC + '/**/*Controller.java', recursive=True)):
        s = open(path).read()
        consts = dict(re.findall(r'static final String ([A-Z_]+)\s*=\s*"((?:[^"\\]|\\.)*)"', s))
        cm = CLASS_MAP.search(s)
        prefix = first_path(cm.group(1), consts) if cm else ''
        for m in ANN.finditer(s):
            verb, args = m.group(1).upper(), m.group(2)
            found.append((verb, CONTEXT + prefix + first_path(args, consts), path.split('/')[-1][:-5]))
    return found


if __name__ == '__main__':
    rs = routes()
    for v, p, c in rs:
        print(v, p, c)
    print(len(rs), 'routes', sum(1 for r in rs if r[0] == 'GET'), 'GET', file=sys.stderr)
