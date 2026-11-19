"""Parses every controller handler method: route, return type, body span. Used by the response-typing tools."""
import glob
import re
from routes import SRC, CONTEXT, ANN, CLASS_MAP, first_path

SEG = re.compile(r'"(?:[^"\\\n]|\\.)*"|/\*.*?\*/|//[^\n]*|\'(?:[^\'\\\n]|\\.)\'', re.S)


def blank(s):
    """Same-length copy of s with comments and string contents blanked, so brace matching ignores them."""
    return SEG.sub(lambda m: re.sub(r'[^\n]', ' ', m.group(0)) if m.group(0).startswith('/') else '"' + ' ' * (len(m.group(0)) - 2) + '"', s)


def match(s, i, o, c):
    d = 0
    while True:
        if s[i] == o: d += 1
        elif s[i] == c:
            d -= 1
            if d == 0: return i
        i += 1


def handlers():
    out = []
    for path in sorted(glob.glob(SRC + '/**/controller/**/*Controller.java', recursive=True)):
        raw = open(path).read()
        s = blank(raw)
        consts = dict(re.findall(r'static final String ([A-Z_]+)\s*=\s*"((?:[^"\\]|\\.)*)"', raw))
        cm = CLASS_MAP.search(raw)
        prefix = first_path(cm.group(1), consts) if cm else ''
        for m in ANN.finditer(raw):
            # the method declaration that follows the mapping annotation
            d = re.compile(r'((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)*)public\s+([\w<>?,.\[\] ]+?)\s+(\w+)\s*\(').search(s, m.end())
            if not d: continue
            pstart = d.end() - 1
            pend = match(s, pstart, '(', ')')
            j = pend + 1
            while s[j] != '{' and s[j] != ';': j += 1
            bstart = j
            bend = match(s, bstart, '{', '}')
            out.append(dict(file=path, verb=m.group(1).upper(), route=CONTEXT + prefix + first_path(m.group(2), consts), ret=d.group(2).strip(), name=d.group(3),
                            sig=(d.start(2), d.end(2)), body=(bstart, bend), params=raw[pstart + 1:pend], ann=raw[m.start():d.start(2)]))
    return out


if __name__ == '__main__':
    hs = handlers()
    import collections
    print(len(hs), 'handlers')
    print(collections.Counter(re.sub(r'<.*', '', h['ret']) for h in hs))
