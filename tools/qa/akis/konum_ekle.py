"""Olaylara `konum` etiketini ekler (DECISIONS #130). Kural:
  - `bolge` varsa konum = bolge;
  - yoksa giriş sahnesinin `yer`i bölgeye çevrilir (kule → kule_dibi, kiyikent_iskele → kiyikent_liman,
    kasaba_cekirdegi → eski_sehir, ogretici_kapi → ogretici_orman; kiyikent, hasat, ceza_bolgesi → us);
  - eşik / Dikkat / Yozlaşma / bildirim olayları yeri yoksa us;
  - Kule hikaye katları kule_dibi;
  - hiçbiri yoksa us.
  - `KARAR` sözlüğü elle verilen kararları (emin olunamayanlar) uygular.
Zaten `konum` olan olaya dokunmaz. Kullanım: python tools/qa/akis/konum_ekle.py [--dry]"""
import glob, json, re, sys
sys.stdout.reconfigure(encoding='utf-8')
dry = '--dry' in sys.argv
inv = json.load(open('tools/qa/akis/envanter.json', encoding='utf-8'))
SUB = {'kule': 'kule_dibi', 'kiyikent_iskele': 'kiyikent_liman', 'kasaba_cekirdegi': 'eski_sehir', 'ogretici_kapi': 'ogretici_orman',
       'kiyikent': 'us', 'hasat': 'us', 'ceza_bolgesi': 'us'}
# Elle kararlar (docs/CHAPTERS.md §3 "emin olunamayanlar"; DECISIONS #130).
KARAR = {
}


def konum(eid, v):
    if eid in KARAR: return KARAR[eid]
    if v['bolge']: return v['bolge']
    if eid.startswith('kule_kat_'): return ['kule_dibi']
    p = v['entry_place']
    if p: return [SUB.get(p, p)]
    return ['us']


out = {}
for f in sorted(glob.glob('content/events/*.yaml')):
    raw = open(f, encoding='utf-8', newline='').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    lines = raw.split(nl)
    res, i, changed = [], 0, 0
    cur = None
    has = set()
    # önce konumu olanları bul
    for j, l in enumerate(lines):
        m = re.match(r'^  - id: "([^"]+)"', l)
        if m: cur = m.group(1)
        elif cur and re.match(r'^    konum:', l): has.add(cur)
    for l in lines:
        res.append(l)
        m = re.match(r'^  - id: "([^"]+)"\s*$', l)
        if m and m.group(1) in inv and m.group(1) not in has:
            k = konum(m.group(1), inv[m.group(1)])
            out[m.group(1)] = k
            res.append('    konum: [' + ', '.join(f'"{x}"' for x in k) + ']')
            changed += 1
    if changed and not dry:
        open(f, 'w', encoding='utf-8', newline='').write(nl.join(res))
    print(f, changed)
from collections import Counter
print(Counter(tuple(v) if len(v) == 1 else ('çok',) for v in out.values()).most_common(20))
