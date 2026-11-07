"""Hatırlatma cümlelerini tohum etkilerine ekler (DECISIONS #130).
Girdi: tools/qa/akis/hatirlatma/cevap_*.json — [{eken, tohum, tr, en}]. Eken olayın bloğundaki (prolog için
story/00_prolog.yaml) `tohum: { olay: "<tohum>" ... }` satırlarının sonuna `, hatirlatma: { tr: "...", en: "..." }` eklenir.
Zaten hatırlatması olan satıra dokunmaz. Kullanım: python tools/qa/akis/hatirlatma_uygula.py [check|apply] [cevap dosyası ...]"""
import glob, re, sys
sys.stdout.reconfigure(encoding='utf-8')
mode = sys.argv[1] if len(sys.argv) > 1 else 'check'
files = sys.argv[2:] or sorted(glob.glob('tools/qa/akis/hatirlatma/cevap_*.json'))
import json
answers = [a for f in files for a in json.load(open(f, encoding='utf-8'))]


def esc(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')


# olay → (dosya, satır aralığı)
blocks = {}
texts = {}
for f in sorted(glob.glob('content/events/*.yaml')) + ['content/story/00_prolog.yaml']:
    raw = open(f, encoding='utf-8', newline='').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.split(nl)
    texts[f] = (lines, nl)
    if f.endswith('00_prolog.yaml'):
        blocks['prolog'] = (f, 0, len(lines))
        continue
    starts = [(i, re.match(r'^  - id: "([^"]+)"', l).group(1)) for i, l in enumerate(lines) if re.match(r'^  - id: "([^"]+)"', l)]
    for k, (i, eid) in enumerate(starts):
        end = starts[k + 1][0] if k + 1 < len(starts) else len(lines)
        blocks[eid] = (f, i, end)

problems, changed = [], 0
touched = set()
for a in answers:
    if a['eken'] not in blocks:
        problems.append(f"eken yok: {a['eken']}"); continue
    f, i0, i1 = blocks[a['eken']]
    lines, nl = texts[f]
    pat = re.compile(r'tohum: \{ olay: "' + re.escape(a['tohum']) + r'"')
    hits = 0
    for i in range(i0, i1):
        l = lines[i]
        if not pat.search(l): continue
        hits += 1
        if 'hatirlatma:' in l: continue
        if not l.rstrip().endswith('}'):
            problems.append(f"{f}:{i + 1}: tohum satırı tek satır değil"); continue
        add = f', hatirlatma: {{ tr: "{esc(a["tr"])}", en: "{esc(a["en"])}" }}'
        stripped = l.rstrip()
        lines[i] = stripped[:-1].rstrip() + add + ' }' + l[len(stripped):]
        changed += 1
        touched.add(f)
    if hits == 0:
        problems.append(f"{a['eken']} → {a['tohum']}: tohum satırı bulunamadı")
    # belirteç sayısı denetimi: tr/en'de aynı npc/yer belirteçleri
    tok = lambda s: sorted(re.findall(r'\{(?:npc|yer):[a-z_@]+', s))
    if tok(a['tr']) != tok(a['en']):
        problems.append(f"{a['eken']} → {a['tohum']}: belirteçler farklı {tok(a['tr'])} / {tok(a['en'])}")
    if re.search(r'\b(Deniz|Kaya|Elif|Vahit|Aras|Mert|Thessar|Yankı|Devon)\b', a['tr'] + ' ' + a['en']):
        problems.append(f"{a['eken']} → {a['tohum']}: düz yazılmış ad")
for p in problems: print('HATA', p)
print(f'{len(answers)} hatırlatma, {changed} tohum satırı', 'yazılacak' if mode == 'check' else 'yazıldı')
if mode == 'apply' and not problems:
    for f in touched:
        lines, nl = texts[f]
        open(f, 'w', encoding='utf-8', newline='').write(nl.join(lines))
sys.exit(1 if problems else 0)
