"""Olayları taslak bölüm yapısına (docs/CHAPTERS.md) sınıflandırır: bölüm, rol (operasyon / darboğaz / üs-hazırlık / görev-yan),
konum (bölge) ve emin olunamayanlar. Çıktı: tools/qa/akis/siniflama.json + özet.

Kurallar:
  - Ana yay olayları gününe göre bölüme düşer. Bölümün `darbogaz` olayı darboğazdır; `operasyon` listesindekiler ana operasyondur.
  - Tohum olayları: onları eken olayların bölümüne (gecikme eklenir); birden çok bölümden ekiliyorsa EMİN DEĞİL.
  - Havuz olayları: gün aralığı (yoksa perde) ile örtüşen bölümler; tek bölüm → o bölümün hazırlık havuzu;
    birden çok bölüm → "çok bölümlü havuz" (üs olayıysa sorun değil; konumlu sahneyse EMİN DEĞİL).
  - Rol: NPC eşiği / NPC'li ve savaşsız / gerilim ≤ 2 → üs; savaşlı ve konumlu → görev (yan operasyon) adayı.
  - Konum: `bolge` varsa o; yoksa giriş sahnesinin `yer`i (alt yer → bölge); ikisi de yoksa "üs" (yer bağımsız).
"""
import json, sys
from collections import Counter, defaultdict
sys.stdout.reconfigure(encoding='utf-8')
inv = json.load(open('tools/qa/akis/envanter.json', encoding='utf-8'))

# Taslak bölümler (docs/CHAPTERS.md ile aynı): (no, perde, gün başı, gün sonu, operasyon ana yay olayları, darboğaz)
CH = [
    (1, 1, 0, 12, ['kirilma'], 'kirilma'),
    (2, 1, 13, 24, ['yeni_tavan'], 'yeni_tavan'),
    (3, 1, 25, 30, ['ogretici_bekci'], 'ogretici_bekci'),
    (4, 2, 31, 45, ['kule_yukselisi'], 'fraksiyon_secimi'),
    (5, 2, 46, 66, ['kasaba_cekirdegi'], None),
    (6, 2, 67, 95, ['ilk_tasma'], 'deniz_kardes_bulundu|deniz_anma_kardes'),
    (7, 2, 96, 122, ['yanki_ortaya_cikis'], None),
    (8, 2, 123, 150, ['sistem_gercegi'], None),
    (9, 2, 151, 180, ['perde2_son'], 'perde2_son'),
    (10, 3, 181, 205, ['cok_evren_kapisi'], None),
    (11, 3, 206, 230, ['batik_ilk_adim', 'batik_son_fener', 'batik_tuz_arsivi', 'batik_son_basakci',
                       'kervan_tarti_carsisi', 'kervan_ondalik', 'kervan_sayman', 'kervan_bos_kefe'], 'deniz_canavar_mi_tanri_mi|deniz_anma_ayna'),
    (12, 3, 231, 260, ['yol_olusumu'], 'yol_olusumu'),
    (13, 3, 261, 292, ['p3_kapi_firtinasi'], None),
    (14, 3, 293, 305, ['buyuk_ihanet'], 'buyuk_ihanet'),
    (15, 3, 306, 330, ['perde3_son'], 'perde3_son'),
    (16, 4, 331, 342, ['kadim_istilasi'], None),
    (17, 4, 343, 352, ['cekirdege_yol', 'sistem_cekirdegi'], None),
    (18, 4, 353, 365, ['son_karar'], 'hasat_vakti'),  # final
]
# Brifing olarak kullanılacak mevcut ana yay olayları (bölümün açılışı).
BRIEF = {'perde2_donus': 4, 'thessar_inis': 10, 'perde4_acilis': 16}
KIYIKENT = {'kiyikent_liman', 'kordon', 'eski_sehir', 'sanayi', 'tepe', 'kule_dibi'}
SUB = {'kule': 'kule_dibi', 'kiyikent_iskele': 'kiyikent_liman', 'kasaba_cekirdegi': 'eski_sehir', 'ogretici_kapi': 'ogretici_orman'}
ACT_DAYS = {1: (0, 30), 2: (31, 180), 3: (181, 330), 4: (331, 365)}


def chapters_for_days(lo, hi):
    return [c[0] for c in CH if not (hi < c[2] or lo > c[3])]


def chapter_of_day(d):
    for c in CH:
        if c[2] <= d <= c[3]: return c[0]
    return CH[-1][0]


parents = defaultdict(list)
for k, v in inv.items():
    for s in v['seeds']:
        parents[s].append(k)

res, unsure = {}, []
memo = {}


def chap(eid, depth=0):
    if eid in memo: return memo[eid]
    v = inv[eid]
    memo[eid] = None  # döngü kırıcı
    out = set()
    if v['ana_yay'] is not None:
        out = {chapter_of_day(v['ana_yay'])}
    elif v['tohum'] and parents.get(eid) and depth < 8:
        for p in parents[eid]:
            pc = chap(p, depth + 1) or set()
            d = inv[p].get('delays', {}).get(eid, (0, 0))
            for c in pc:
                lo = CH[c - 1][2] + d[0]; hi = CH[c - 1][3] + d[1]
                out |= set(chapters_for_days(lo, hi)) if d[1] > 3 else {c}
    else:
        acts = [a for a in (v['perde'] or [1, 2, 3, 4])]
        lo = v['gun_min'] if v['gun_min'] is not None else min(ACT_DAYS[a][0] for a in acts)
        hi = v['gun_max'] if v['gun_max'] is not None else max(ACT_DAYS[a][1] for a in acts)
        lo = max(lo, min(ACT_DAYS[a][0] for a in acts)); hi = min(hi, max(ACT_DAYS[a][1] for a in acts))
        out = set(chapters_for_days(lo, hi))
    memo[eid] = out
    return out


ops = {e: c[0] for c in CH for e in c[4]}
necks = {n: c[0] for c in CH if c[5] for n in c[5].split('|')}
for eid, v in inv.items():
    cs = sorted(chap(eid) or [])
    place = v['bolge'][0] if v['bolge'] else (SUB.get(v['entry_place'], v['entry_place']) if v['entry_place'] else None)
    if place == 'kiyikent': place = 'kiyikent (şehir geneli)'
    reasons = []
    if eid in necks:
        role = 'darbogaz'
    elif eid in ops:
        role = 'operasyon'
    elif eid in BRIEF:
        role = 'brifing'
    elif v['ana_yay'] is not None:
        # koşullu ya da NPC'ye bağlı ana yay: herkes geçmez → üste gelen yay düğümü (bölüm ilerlemesine bağlanır)
        role = 'us_yay'
        if v['gerilim'] >= 4 and v['fights'] and len(v['places']) > 0:
            reasons.append('ana yay, savaşlı ve konumlu: üs yay düğümü mü, yan görev mi?')
    elif eid.startswith(('esik_', 'dikkat_', 'yozlasma_', 'bildirim_')) or (v['npc'] and not v['fights'] and v['gerilim'] <= 3):
        role = 'us'
    elif eid.startswith('kule_kat_'):
        role = 'kule'
    elif v['fights'] and place and v['gerilim'] >= 3:
        role = 'gorev_yan'
    elif v['quests']:
        role = 'gorev_yan'
    else:
        role = 'us' if not place or v['gerilim'] <= 2 else 'hazirlik_konumlu'
    if not cs:
        reasons.append('hiçbir bölüme düşmedi (ebeveyni yok ya da gün aralığı boş)')
    elif len(cs) > 1 and role not in ('us', 'kule'):
        reasons.append(f'{len(cs)} bölüme yayılıyor')
    if v['tohum'] and len({tuple(sorted(chap(p) or [])) for p in parents.get(eid, [])}) > 1:
        reasons.append('farklı bölümlerden ekiliyor')
    if len(v['places']) > 1 and role != 'darbogaz':
        reasons.append('olay içinde yer değişiyor: ' + ' → '.join(v['places']))
    if role == 'hazirlik_konumlu':
        reasons.append('konumlu, savaşsız: üs mü, yan görev mi?')
    res[eid] = dict(bolumler=cs, rol=role, konum=place or 'üs', dosya=v['file'], neden=reasons)
    if reasons: unsure.append(eid)

json.dump(res, open('tools/qa/akis/siniflama.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('rol:', Counter(r['rol'] for r in res.values()))
print('tek bölümlü:', sum(1 for r in res.values() if len(r['bolumler']) == 1), ' çok bölümlü:', sum(1 for r in res.values() if len(r['bolumler']) > 1),
      ' bölümsüz:', sum(1 for r in res.values() if not r['bolumler']))
print('emin olunamayan:', len(unsure))
print('neden:', Counter(n.split(':')[0] if 'bölüme yayılıyor' not in n else 'çok bölüme yayılıyor' for e in unsure for n in res[e]['neden']))
per = defaultdict(Counter)
for r in res.values():
    if len(r['bolumler']) == 1: per[r['bolumler'][0]][r['rol']] += 1
for c in CH: print(f'  Bölüm {c[0]:2}: ' + ', '.join(f'{k} {n}' for k, n in sorted(per[c[0]].items())))
