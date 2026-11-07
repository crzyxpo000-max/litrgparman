"""Hikayesiz gün açığı, bölge bazında (DECISIONS #144): bölüm başına serbest günlerin hangi bölgede geçtiği, hikayesiz
günler ve %35 hedefine inmek için gereken hikaye günü. Görev günü görevin bölgesine, üs günü 'us'a yazılır.
Kullanım: python tools/qa/akis/bolge_acik.py <klasör> [hedef=0.35]"""
import glob, json, os, sys
from collections import defaultdict
sys.stdout.reconfigure(encoding='utf-8')
iz = sys.argv[1]; target = float(sys.argv[2]) if len(sys.argv) > 2 else 0.35
files = sorted(glob.glob(os.path.join(iz, '*.jsonl')))
days = defaultdict(lambda: defaultdict(int)); empty = defaultdict(lambda: defaultdict(int))
for f in files:
    region = None; today = set()
    for l in open(f, encoding='utf-8'):
        d = json.loads(l)
        if d['t'] == 'bolge':
            region = d['yeni']
            if d['yeni'] not in ('ogretici_orman', 'kiyikent_liman'): today.add(d['yeni'])
        if d['t'] != 'gunozet': continue
        if d['evre'] in ('hazirlik', 'bitti', 'brifing') and d['bolum']:
            where = (sorted(today)[-1] if today else region) if d['gorevde'] else 'us'
            days[d['bolum']][where] += 1
            if not d.get('hikaye', True): empty[d['bolum']][where] += 1
        today = set()
        if region and region not in ('ogretici_orman', 'kiyikent_liman') and d['gorevde']: today.add(region)
n = len(files)
for b in sorted(days):
    tot = sum(days[b].values()); emp = sum(empty[b].values())
    need = max(0.0, emp - target * tot) / n
    if need <= 0: continue
    print(f'Bölüm {b}: serbest {tot/n:.1f} gün, hikayesiz %{emp/tot*100:.0f}, %{target*100:.0f} için oynanış başına {need:.1f} hikaye günü daha')
    for r, c in sorted(days[b].items(), key=lambda x: -empty[b][x[0]]):
        e = empty[b][r]
        if e == 0: continue
        print(f'   {r:18s} gün {c/n:5.1f}  hikayesiz {e/n:5.1f}  pay %{e/emp*100:3.0f}  gereken {need*e/emp:4.1f}')
