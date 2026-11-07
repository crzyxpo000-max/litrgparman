"""Serbest gün ölçümü (docs/STORY_FLOW.md "Boş serbest günler"): kayıttaki gün özetlerinden bölüm başına
serbest gün sayısı (hazırlık ve bitti evreleri; operasyon/sonrası hariç), havuzun tükendiği gün (bölümün açılışından
itibaren: o günden sonra üste fırsat eli hep boş; atlanan günler sayılmaz), salt savaşla geçen serbest günlerin oranı
(≥ 1 savaş, 0 olay), zaman atlamayla geçen günlerin oranı (DECISIONS #142) ve hikayesiz günlerin oranı: o gün hiçbir
olayda ya da Sistem isteminde seçim yapılmadı (varış sahnesi ve atlama özeti sayılmaz; savaş, Ezme dahil, hikaye değildir).
Hedef ölçüt hikayesiz gündür (DECISIONS #144).
Kullanım: python tools/qa/akis/serbest.py [klasör] [--md]"""
import glob, json, os, sys, statistics
from collections import defaultdict
sys.stdout.reconfigure(encoding='utf-8')
args = [a for a in sys.argv[1:] if not a.startswith('--')]
iz = args[0] if args else 'tools/qa/akis/iz_sonra'
import yaml
ch = {c.get('no', c.get(False)): c for c in yaml.safe_load(open('content/chapters.yaml', encoding='utf-8'))['bolumler']}
free = defaultdict(list); skipd = defaultdict(list); nostory = defaultdict(list); fight = defaultdict(list); empty = defaultdict(list); exhaust = defaultdict(list); quiet = defaultdict(list)
for f in sorted(glob.glob(os.path.join(iz, '*.jsonl'))):
    days = [json.loads(l) for l in open(f, encoding='utf-8') if '"t":"gunozet"' in l]
    by = defaultdict(list)
    for d in days:
        if d['evre'] in ('hazirlik', 'bitti', 'brifing'): by[d['bolum']].append(d)
    for no, ds in by.items():
        if no == 0: continue
        n = len(ds)
        only_fight = sum(1 for d in ds if d['savas'] > 0 and d['olay'] == 0)
        skipped = sum(1 for d in ds if d.get('atla') and d['olay'] == 0 and d['savas'] == 0)
        nothing = sum(1 for d in ds if d['savas'] == 0 and d['olay'] == 0) - skipped
        skipd[no].append(skipped / n if n else 0)
        nostory[no].append(sum(1 for d in ds if not d.get('hikaye', True)) / n if n else 0)
        free[no].append(n); fight[no].append(only_fight / n if n else 0); quiet[no].append(nothing / n if n else 0)
        # havuz tükendi: sondan geriye, el hep 0 olan ilk gün
        live = [d for d in ds if not d.get('atla')]
        k = len(live)
        while k > 0 and live[k - 1]['el'] == 0: k -= 1
        exhaust[no].append(live[k]['gun'] - ch[no]['acilis'] if k < len(live) else None)
rows = []
for no in sorted(free):
    ex = [x for x in exhaust[no] if x is not None]
    rows.append((no, statistics.mean(free[no]), (statistics.median(ex) if ex else None), len(ex), len(exhaust[no]),
                 statistics.mean(fight[no]) * 100, statistics.mean(quiet[no]) * 100, statistics.mean(skipd[no]) * 100, statistics.mean(nostory[no]) * 100))
if '--md' in sys.argv:
    print('| Bölüm | Serbest gün (ort.) | Havuzun tükendiği gün (açılıştan, medyan) | Tükenen oynanış | Salt savaş günü | Ne olay ne savaş | Atlanan gün | Hikayesiz gün |')
    print('| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |')
    for no, fr, ex, ne, nt, fp, qp, sp, hp in rows:
        print(f'| {no} | {fr:.1f} | {"—" if ex is None else f"{ex:.0f}"} | {ne}/{nt} | %{fp:.0f} | %{qp:.0f} | %{sp:.0f} | %{hp:.0f} |')
else:
    for r in rows: print(r)
print('salt savaş > %35 olan bölümler:', [r[0] for r in rows if r[5] > 35])
print('hikayesiz > %35 olan bölümler:', [r[0] for r in rows if r[8] > 35])
