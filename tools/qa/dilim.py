import re, sys
sys.stdout.reconfigure(encoding='utf-8')
RX = re.compile(r'\btr:\s*"((?:[^"\\]|\\.)*)"')
LIMIT = int(sys.argv[1])
for f in sys.argv[2:]:
    L = open(f, encoding='utf-8').read().split('\n')
    starts = [i for i, l in enumerate(L) if l.startswith('  - id: "')] or [0]
    bounds = [0] + starts[1:] + [len(L)]
    # olay blokları: [bounds[k], bounds[k+1])
    blocks = []
    for k in range(len(bounds) - 1):
        a, b = bounds[k], bounds[k + 1]
        w = sum(len(x.split()) for l in L[a:b] for x in RX.findall(l))
        blocks.append((a, b, w))
    cur = None
    out = []
    for a, b, w in blocks:
        if cur and cur[2] + w > LIMIT:
            out.append(cur); cur = None
        cur = (cur[0], b, cur[2] + w) if cur else (a, b, w)
    if cur: out.append(cur)
    for a, b, w in out:
        print(f'{f} {a + 1} {b} {w}')
