"""Hikaye akışı teşhisi (STORY_FLOW): olay envanteri -> tools/qa/akis/envanter.json
Her olay: dosya, perde, bölge, gün, ana_yay, yalnız_tohum, npc, etiketler, gerilim, önemli, giriş sahnesi yeri,
olayın tüm sahnelerinin yerleri, dokunduğu görevler (etki/koşul), ektiği tohumlar, savaş içerip içermediği."""
import glob, json, os, sys, yaml
sys.stdout.reconfigure(encoding='utf-8')
Loader = getattr(yaml, 'CSafeLoader', yaml.SafeLoader)

def walk(o, fn):
    if isinstance(o, dict):
        fn(o)
        for v in o.values(): walk(v, fn)
    elif isinstance(o, list):
        for v in o: walk(v, fn)

out = {}
for f in sorted(glob.glob('content/events/*.yaml')):
    d = yaml.load(open(f, encoding='utf-8'), Loader=Loader) or {}
    for e in d.get('olaylar', []) or []:
        scenes = e.get('sahneler', []) or []
        entry = e.get('sahne') or (scenes[0]['id'] if scenes else None)
        places = [s.get('yer') for s in scenes if s.get('yer')]
        entry_place = next((s.get('yer') for s in scenes if s['id'] == entry), None)
        q_eff, q_cond, seeds, flags_set, flags_need = set(), set(), set(), set(), set()
        delays = {}
        fights = [0]
        def fn(o):
            g = o.get('gorev')
            if isinstance(g, dict):
                for k, v in g.items():
                    (q_eff if v in ('basla', 'tamam', 'basarisiz') else q_cond).add(k)
            t = o.get('tohum')
            for x in (t if isinstance(t, list) else [t]):
                if isinstance(x, dict) and 'olay' in x:
                    seeds.add(x['olay'])
                    g = x.get('gecikme') or {}
                    delays.setdefault(x['olay'], (g.get('min', 0), g.get('max', g.get('min', 0))) if isinstance(g, dict) else (g, g))
            if 'savas' in o and isinstance(o['savas'], dict): fights[0] += 1
            b = o.get('bayrak')
            if isinstance(b, str): flags_set.add(b)
        for s in scenes:
            for c in s.get('secenekler', []) or []:
                walk(c.get('etkiler'), fn)
                if 'savas' in c: fights[0] += 1
                walk(c.get('zar'), fn)
        walk(e.get('kosul'), lambda o: (flags_need.update([o['bayrak']] if isinstance(o.get('bayrak'), str) else o.get('bayrak') or []),
                                         q_cond.update(o['gorev'].keys() if isinstance(o.get('gorev'), dict) else [])))
        gun = e.get('gun') or {}
        bolge = e.get('bolge')
        out[e['id']] = dict(
            file=os.path.basename(f), perde=e.get('perde'), bolge=bolge if isinstance(bolge, list) else ([bolge] if bolge else []),
            gun_min=gun.get('min'), gun_max=gun.get('max'), ana_yay=(e.get('ana_yay') or {}).get('gun'),
            tohum=bool(e.get('yalniz_tohum')), npc=e.get('npc') or [], tags=e.get('etiketler') or [], gerilim=e.get('gerilim', 2),
            onemli=bool(e.get('onemli')), tekrar=bool(e.get('tekrar')), entry_place=entry_place, places=sorted(set(places)),
            n_scenes=len(scenes), quests=sorted(q_eff), quest_cond=sorted(q_cond), seeds=sorted(seeds), delays=delays, fights=fights[0],
            flags_need=sorted(x for x in flags_need if x), flags_set=sorted(flags_set),
        )
json.dump(out, open('tools/qa/akis/envanter.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
from collections import Counter
print(len(out), 'olay')
print('perde:', Counter(str(v['perde']) for v in out.values()))
print('bölgesiz:', sum(1 for v in out.values() if not v['bolge']), ' yalnız_tohum:', sum(v['tohum'] for v in out.values()),
      ' ana_yay:', sum(1 for v in out.values() if v['ana_yay'] is not None))
print('bölgesiz ama giriş yeri var:', sum(1 for v in out.values() if not v['bolge'] and v['entry_place']))
print('çok yerli olay:', sum(1 for v in out.values() if len(v['places']) > 1))
print('görev dokunan:', sum(1 for v in out.values() if v['quests']))
