"""Hatırlatma iş listesi (DECISIONS #130): gecikmesi ≥ 3 gün olan her (eken olay → tohum olayı) çifti için bağlam.
Çıktı: tools/qa/akis/hatirlatma/is.json — [{cift, eken, eken_kart, tohum, tohum_kart, tohum_acilis, secimler:[{sahne, secim, etiket}], gecikme}]
ve parti dosyaları is_<perde>.json. Yazılan hatırlatmalar `cevap_*.json` ([{eken, tohum, tr, en}]) olarak gelir;
`hatirlatma_uygula.py` bunları tohum etkilerine ekler."""
import glob, json, os, sys, yaml
from collections import defaultdict
sys.stdout.reconfigure(encoding='utf-8')
L = getattr(yaml, 'CSafeLoader', yaml.SafeLoader)
MIN_DELAY = 3
os.makedirs('tools/qa/akis/hatirlatma', exist_ok=True)

events, scene_event = {}, {}
for f in sorted(glob.glob('content/events/*.yaml')):
    for e in (yaml.load(open(f, encoding='utf-8'), Loader=L) or {}).get('olaylar', []) or []:
        events[e['id']] = (f, e)
        for s in e.get('sahneler', []) or []:
            scene_event[s['id']] = e['id']
story_scenes = {}
for f in sorted(glob.glob('content/story/*.yaml')):
    for s in (yaml.load(open(f, encoding='utf-8'), Loader=L) or {}).get('sahneler', []) or []:
        story_scenes[s['id']] = s


def seeds_in(effects):
    out = []
    for x in effects or []:
        if isinstance(x, dict) and 'tohum' in x:
            t = x['tohum']
            for y in (t if isinstance(t, list) else [t]):
                if isinstance(y, dict) and 'olay' in y: out.append(y)
    return out


def first_par(e):
    sc = (e.get('sahneler') or [{}])[0]
    for p in sc.get('paragraflar', []) or []:
        m = p.get('metin') if isinstance(p, dict) and 'metin' in p else p
        if isinstance(m, dict) and m.get('tr'): return m['tr'][:300]
    return ''


pairs = defaultdict(lambda: {'secimler': [], 'gecikme': 0})
all_scenes = [(eid, s) for eid, (f, e) in events.items() for s in e.get('sahneler', []) or []] + [(None, s) for s in story_scenes.values()]
for eid, s in all_scenes:
    for c in s.get('secenekler', []) or []:
        effs = (c.get('etkiler') or []) + ((c.get('zar') or {}).get('basarisiz_etkiler') or [])
        for t in seeds_in(effs):
            g = t.get('gecikme') or {}
            mx = (g.get('max', g.get('min', 1)) if isinstance(g, dict) else g) or 1
            if mx < MIN_DELAY or t['olay'] not in events: continue
            key = f"{eid or 'prolog'}→{t['olay']}"
            p = pairs[key]
            p['gecikme'] = max(p['gecikme'], mx)
            p['secimler'].append({'sahne': s['id'], 'secim': c.get('id'), 'etiket': (c.get('etiket') or {}).get('tr', '')})
out = []
for key, p in sorted(pairs.items()):
    a, b = key.split('→')
    ea = events.get(a, (None, {}))[1]; eb = events[b][1]
    out.append({'cift': key, 'eken': a, 'eken_kart': (ea.get('kart') or {}).get('tr', 'prolog'),
                'eken_perde': ea.get('perde') or [1], 'tohum': b, 'tohum_kart': eb['kart']['tr'], 'tohum_kart_en': eb['kart']['en'],
                'tohum_acilis': first_par(eb), 'gecikme': p['gecikme'], 'secimler': p['secimler'][:6]})
json.dump(out, open('tools/qa/akis/hatirlatma/is.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
by = defaultdict(list)
for x in out:
    by[min(x['eken_perde'] or [1])].append(x)
for k, v in by.items():
    json.dump(v, open(f'tools/qa/akis/hatirlatma/is_p{k}.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(len(out), 'çift;', {k: len(v) for k, v in sorted(by.items())})
