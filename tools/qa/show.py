import re, subprocess, sys
sys.stdout.reconfigure(encoding='utf-8')
RX = re.compile(r'\btr:\s*"((?:[^"\\]|\\.)*)"')
EN = re.compile(r'\ben:\s*"((?:[^"\\]|\\.)*)"')
f = sys.argv[1]
spec = sys.argv[2:]
old = subprocess.run(['git', 'show', 'HEAD:' + f], capture_output=True).stdout.decode('utf-8').replace('\r\n', '\n').split('\n')
new = open(f, encoding='utf-8').read().replace('\r\n', '\n').split('\n')
lines = []
for s in spec:
    if '-' in s:
        a, b = map(int, s.split('-')); lines += range(a, b + 1)
    else:
        lines.append(int(s))
for n in lines:
    o = RX.findall(old[n - 1]); w = RX.findall(new[n - 1])
    if not o:
        if '--ctx' in sys.argv[0:1]: pass
        continue
    e = EN.findall(new[n - 1])
    ctx = new[n - 1].strip()[:40]
    if o == w:
        print(f'{n} = {w[0]}')
    else:
        print(f'{n} - {o[0]}\n{n} + {w[0]}')
    if e and '-e' in sys.argv[0:1]: pass
    if e: print(f'{n} en {e[0]}')
    print()
