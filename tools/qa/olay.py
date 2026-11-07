"""QA okuma aracı: bir olayın (ya da sahnenin) metnini TR ve EN olarak sade biçimde basar.

Kullanım (proje kökünde):
  python tools/qa/olay.py <olay_id|sahne_id> [...]      # olayın tüm sahneleri ya da tek sahne
  python tools/qa/olay.py --son <son_id> [...]           # endings.yaml'daki sonun tanımı + son sahneleri
  python tools/qa/olay.py --tr ...                       # yalnız Türkçe (ya da --en)

Çıktı: sahne başlığı, paragraflar (koşul kısaltmasıyla), sezgiler, yoldaş tepkileri, bildirimler, seçenekler
(etiket → sonraki; gerekli/goster; savaş; kronik). Koşul ve etkiler kısaltılır; amaç metni okumaktır.
"""
import glob, json, sys
import yaml

sys.stdout.reconfigure(encoding='utf-8')
args = sys.argv[1:]
only = 'tr' if '--tr' in args else 'en' if '--en' in args else None
args = [a for a in args if a not in ('--tr', '--en')]

files = sorted(glob.glob('content/events/*.yaml')) + sorted(glob.glob('content/story/*.yaml'))
events, scenes = {}, {}
for f in files:
    d = yaml.safe_load(open(f, encoding='utf-8')) or {}
    for e in d.get('olaylar', []) or []:
        events[e['id']] = (f, e)
        for s in e.get('sahneler', []) or []:
            scenes[s['id']] = (f, s)
    for s in d.get('sahneler', []) or []:
        scenes[s['id']] = (f, s)


def short(o, n=90):
    t = json.dumps(o, ensure_ascii=False)
    return t if len(t) <= n else t[:n - 1] + '…'


def lt(x, ind):
    if isinstance(x, dict) and ('tr' in x or 'en' in x):
        if only != 'en': print(f'{ind}TR: {x.get("tr", "")}')
        if only != 'tr': print(f'{ind}EN: {x.get("en", "")}')


def scene(s):
    print(f'\n### sahne {s["id"]}' + (f'  (yer: {s["yer"]})' if 'yer' in s else ''))
    for p in s.get('paragraflar', []) or []:
        if isinstance(p, dict) and 'metin' in p:
            print(f'  ¶ [{short(p.get("kosul"), 70)}]' if p.get('kosul') else '  ¶')
            lt(p['metin'], '    ')
        else:
            print('  ¶'); lt(p, '    ')
    for z in s.get('sezgiler', []) or []:
        print(f'  ~ sezgi [{short(z.get("kosul"), 60)}]'); lt(z.get('metin'), '    ')
    for y in s.get('yoldas_tepkileri', []) or []:
        print(f'  ~ yoldaş {y.get("npc")} [{short(y.get("kosul"), 50)}]'); lt(y.get('metin'), '    ')
    for b in s.get('bildirimler', []) or []:
        print(f'  ! bildirim {b.get("tur")}' + (f' [{short(b.get("kosul"), 60)}]' if b.get('kosul') else '')); lt(b.get('baslik'), '    ')
        for r in b.get('satirlar', []) or []: lt(r, '      ')
    for c in s.get('secenekler', []) or []:
        extra = []
        for k in ('gerekli', 'goster', 'tur', 'zar'):
            if k in c: extra.append(f'{k}={short(c[k], 50)}')
        if 'savas' in c: extra.append(f'savaş={short(c["savas"].get("dusmanlar"), 50)}')
        print(f'  > [{c.get("id")}] → {c.get("sonraki", "savaş")}  ' + ' '.join(extra))
        lt(c.get('etiket'), '    ')
        if 'kronik' in c:
            print('    kronik:'); lt(c['kronik'], '      ')


for a in args:
    if a == '--son':
        continue
    if args and args[0] == '--son':
        d = yaml.safe_load(open('content/endings.yaml', encoding='utf-8'))
        e = (d.get('sonlar') or {}).get(a)
        print(f'\n## SON {a}: {short(e, 400)}')
        for sid, (f, s) in scenes.items():
            if sid.startswith('son_' + a) or sid == a:
                scene(s)
        continue
    if a in events:
        f, e = events[a]
        print(f'\n## OLAY {a} ({f}) gün={e.get("gun")} ana_yay={e.get("ana_yay")} npc={e.get("npc")} koşul={short(e.get("kosul"), 80)}')
        lt(e.get('kart'), '  kart ')
        for s in e.get('sahneler', []) or []:
            scene(s)
    elif a in scenes:
        scene(scenes[a][1])
    else:
        print(f'\n?? bulunamadı: {a}')
