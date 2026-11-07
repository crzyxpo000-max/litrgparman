"""Olay sırası kaydını (tools/qa/akis/iz/*.jsonl) çözümler: STORY_FLOW a–e sayıları, örnekler, nedenler.
Kullanım: python tools/qa/akis/cozumle.py [iz_klasörü] [--json çıktı.json]

Tanımlar (docs/STORY_FLOW.md):
  görev penceresi: bir görevin (quests.yaml) iki ardışık olayı arasında ≤ PENCERE gün varsa, aradaki zaman "görev sürüyor".
  a  görev penceresinde, o göreve dokunmayan ve odak NPC'si paylaşmayan olay.
  b  olayın giriş sahnesinin yeri şu anki bölgede değil; ya da bölge olay etkisiyle (yolculuk eylemi olmadan) değişti.
  c  tohum / NPC eşiği / Dikkat eşiği / bildirim olayı, araya oyuncu eylemi girmeden bir savaşın ya da gerilim ≥ 3 olayın hemen ardından.
  d  gün bazlı ana yay olayı bir görev penceresinin içinde (göreve dokunmadan).
  e  araya oyuncu eylemi girmeden ilgisiz bir olaya geçiş (ortak NPC yok, önceki olay tohumlamadı, ortak görev yok).
"""
import glob, json, os, sys
from collections import Counter, defaultdict
sys.stdout.reconfigure(encoding='utf-8')
argv = sys.argv[1:]
jout = None
if '--json' in argv:
    i = argv.index('--json'); jout = argv[i + 1]; del argv[i:i + 2]
iz = argv[0] if argv else 'tools/qa/akis/iz'
PENCERE = 3
OK_MOVES = ('akis:Travel', 'akis:ReturnToBase', 'akis:Card', 'akis:EnterOperation', 'akis:Camp')
inv = json.load(open('tools/qa/akis/envanter.json', encoding='utf-8'))
KIYIKENT = {'kiyikent_liman', 'kordon', 'eski_sehir', 'sanayi', 'tepe', 'kule_dibi'}
PLACE = {'kule': {'kule_dibi'}, 'kiyikent_iskele': {'kiyikent_liman'}, 'kasaba_cekirdegi': {'eski_sehir'}, 'kiyikent': KIYIKENT,
         'ogretici_kapi': {'ogretici_orman', 'harabe_carsi', 'kirik_istasyon'}}
SKIP_PLACE = {'hasat', 'ceza_bolgesi'}
FORCED = {'tohum', 'npc_esik', 'dikkat_esik', 'bildirim'}


def place_ok(place, region):
    if not place or place in SKIP_PLACE or region is None: return True
    return region in PLACE.get(place, {place})


def quests_of(eid):
    v = inv.get(eid, {})
    return set(v.get('quests', [])) | set(v.get('quest_cond', []))


def related(a, b):
    if a is None or b is None: return True
    va, vb = inv.get(a, {}), inv.get(b, {})
    if set(va.get('npc', [])) & set(vb.get('npc', [])): return True
    if b in va.get('seeds', []) or a in vb.get('seeds', []): return True
    if quests_of(a) & quests_of(b): return True
    return False


cnt = Counter()
ex = defaultdict(list)
why = defaultdict(Counter)
per_run = []
total_events = 0
# Bölüm yapısı kayıtları (gorev alanı var) için görev tabanlı tanımlar (docs/STORY_FLOW.md "Sonrası"):
#   a' görevin içinde göreve ait olmayan olay · b' konumu oyuncunun bulunduğu yere uymayan olay ya da olay dışı,
#   görev başlatmayan/bitirmeyen bir eylemle bölge değişimi · d' görevin içinde göreve ait olmayan ana yay olayı.
cnt2 = Counter()
ex2 = defaultdict(list)
for f in sorted(glob.glob(os.path.join(iz, '*.jsonl'))):
    recs = [json.loads(l) for l in open(f, encoding='utf-8') if l.strip()]
    head = recs[0]
    events = [r for r in recs if r['t'] == 'olay']
    total_events += len(events)
    run = Counter()
    # görev pencereleri: görev → olay günleri
    qdays = defaultdict(list)
    for r in events:
        for qid in quests_of(r['olay']): qdays[qid].append(r['gun'])
    windows = []  # (görev, başlangıç, bitiş)
    for qid, days in qdays.items():
        days = sorted(days)
        for x, y in zip(days, days[1:]):
            if 0 < y - x <= PENCERE or (y == x):
                windows.append((qid, x, y))
    def in_window(r):
        out = []
        for qid, x, y in windows:
            if x <= r['gun'] <= y and qid not in quests_of(r['olay']):
                # pencerenin iki ucundaki olayların kendisi değil
                out.append(qid)
        return out
    tag = f"oynanış {head['no']} ({head['profil']})"
    yeni = any('gorev' in r for r in events)
    # bölge değişimleri (b: yolculuksuz)
    for r in recs:
        if yeni and r['t'] == 'bolge':
            # Görev başlatan/bitiren eylemler (yolculuk, görev kartı, operasyon, dönüş, kamp sonrası çağrı) ve olayın
            # kendi `bolge` etkisi (varış sahnesiyle görev başlatır) açıklanmış geçiştir.
            if r['eylem'].startswith('akis:') and not any(r['eylem'].startswith(x) for x in OK_MOVES):
                cnt2['b'] += 1
                if len(ex2['b']) < 5: ex2['b'].append(f"{tag}, Gün {r['gun']}: {r['eylem']} bölgeyi {r['eski']} → {r['yeni']} yaptı")
            continue
        if r['t'] == 'bolge' and r['eski'] is not None and not r['eylem'].startswith('akis:Travel'):
            cnt['b'] += 1; run['b'] += 1; why['b']['bölge olay etkisiyle değişti'] += 1
            if len(ex['b']) < 6: ex['b'].append(f"{tag}, Gün {r['gun']}: `{r['olay']}` bölgeyi {r['eski']} → {r['yeni']} yaptı (yolculuk sahnesi yok)")
    for r in events:
        eid, v = r['olay'], inv.get(r['olay'], {})
        w = in_window(r)
        if w and not related(eid, r['onceki']) and r['neden'] != 'ana_yay':
            cnt['a'] += 1; run['a'] += 1; why['a'][r['neden']] += 1
            if len(ex['a']) < 6: ex['a'].append(f"{tag}, Gün {r['gun']}: `{w[0]}` görevi sürerken `{eid}` ({r['neden']}, {r['bolge']})")
        place = v.get('entry_place')
        if not place_ok(place, r['bolge']):
            cnt['b'] += 1; run['b'] += 1
            why['b']['bölgesiz olay, giriş yeri başka' if not v.get('bolge') else 'bölgeli olay, yeri başka'] += 1
            why['b_neden'][r['neden']] += 1
            if len(ex['b']) < 12: ex['b'].append(f"{tag}, Gün {r['gun']}: `{eid}` ({r['neden']}) yeri `{place}`, oyuncu `{r['bolge']}` bölgesinde")
        if r['neden'] in FORCED and (r['savastan_hemen'] or (r['arada_akis'] == 0 and r['onceki_gerilim'] >= 3)):
            cnt['c'] += 1; run['c'] += 1; why['c'][('savaştan hemen sonra' if r['savastan_hemen'] else 'gerilim ≥3 olaydan hemen sonra') + ' · ' + r['neden']] += 1
            if len(ex['c']) < 6: ex['c'].append(f"{tag}, Gün {r['gun']}: `{r['onceki']}` (gerilim {r['onceki_gerilim']}) biter bitmez `{eid}` ({r['neden']}{', NPC ' + r['tohum_npc'] if r['tohum_npc'] else ''})")
        if r['neden'] == 'ana_yay' and w:
            cnt['d'] += 1; run['d'] += 1; why['d'][f"ana yay gün {r['ana_yay_gun']}"] += 1
            if len(ex['d']) < 6: ex['d'].append(f"{tag}, Gün {r['gun']}: ana yay `{eid}` (gün {r['ana_yay_gun']}) `{w[0]}` görevinin ortasına düştü")
        if r['arada_akis'] == 0 and not r['savastan_hemen'] and r['onceki'] and not related(eid, r['onceki']):
            cnt['e'] += 1; run['e'] += 1; why['e'][r['neden']] += 1
            if len(ex['e']) < 6: ex['e'].append(f"{tag}, Gün {r['gun']}: `{r['onceki']}` → doğrudan `{eid}` ({r['neden']}; ortak NPC/tohum/görev yok)")
        why['neden'][r['neden']] += 1
        if 'gorev' in r:
            if r['gorev'] and r['kendi'] is False:
                cnt2['a'] += 1
                if r['neden'] == 'ana_yay': cnt2['d'] += 1
                if len(ex2['a']) < 5: ex2['a'].append(f"{tag}, Gün {r['gun']}: {r['gorev']}/{r['gorev_bolge']} içinde `{eid}` ({r['neden']})")
            if not r['konum_uyar']:
                cnt2['b'] += 1
                if len(ex2['b']) < 5: ex2['b'].append(f"{tag}, Gün {r['gun']}: `{eid}` ({r['neden']}) konumu uymuyor; yer {r['gorev_bolge'] or 'üs'}")
    per_run.append((tag, len(events), dict(run)))

print(f'{len(per_run)} oynanış, {total_events} olay başlangıcı')
for k in 'abcde':
    print(f'\n{k}) {cnt[k]}  (oynanış başına {cnt[k] / max(1, len(per_run)):.1f})')
    for w, n in why[k].most_common(6): print(f'   {n:5}  {w}')
    for e in ex[k][:4]: print('   ·', e)
if cnt2 or any('gorev' in json.loads(l) for f in glob.glob(os.path.join(iz, '*.jsonl'))[:1] for l in list(open(f, encoding='utf-8'))[1:3] if l.strip()):
    print("\nGörev tabanlı (bölüm yapısı): a'", cnt2['a'], " b'", cnt2['b'], " d'", cnt2['d'])
    for k in 'ab':
        for e in ex2[k][:3]: print('   ·', k, e)
print('\nb olaylarının nedenleri:', dict(why['b_neden']))
print('olay nedenleri:', dict(why['neden']))
if jout:
    json.dump({'runs': len(per_run), 'events': total_events, 'counts': dict(cnt), 'mission_counts': dict(cnt2), 'why': {k: dict(v) for k, v in why.items()},
               'examples': ex, 'per_run': per_run}, open(jout, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
