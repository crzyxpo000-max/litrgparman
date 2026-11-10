"""Kalan P1 düzeltmeleri sonrası görüntüler ve ölçümler (UI_REVIEW B5, D2, E1–E3, F1, F2, G1, G2)."""
import os, re, time, json
os.environ["CAP_SUFFIX"] = "_sonra"
from cap import *
import cap

cond("tr", "DARK", "MEDIUM", 1.0, "small")
# B5: yükleme süresi (3 ölçüm)
for i in range(3):
    stop(); time.sleep(1)
    t0 = time.time()
    sh(f"am start -W -n {cap.PKG}/.MainActivity")
    t1 = time.time()
    while True:
        ns = nodes()
        if any(re.search(r"^Devam et", n[0]) for n in ns) and not any("yükleniyor" in n[0] for n in ns): break
        if time.time() - t1 > 40: break
    print("yükleme %d: start %.1f sn + dünya %.1f sn" % (i + 1, t1 - t0, time.time() - t1))

dumps = {}
def snap(name, scroll=0):
    shot(name); dumps[name] = nodes()
    for i in range(scroll):
        swipe_up(0.45); shot(f"{name}_k{i+1}"); dumps[f"{name}_k{i+1}"] = nodes()

for f in ("brifing_1", "brifing_4", "darbogaz_2"):
    load(f); snap(f)
load("son"); snap("son", 4)
load("savas_taktik"); snap("savas_taktik")
load("us_perde2")
tap(r"^Çanta$"); snap("sekme_bag", 1)
tap(r"^Durum$"); snap("sekme_status")
tap(r"^Daha fazla$"); snap("sekme_more")
tap(r"^Ayarlar$", wait=1.5); snap("dahafazla_ayarlar", 1)
stop(); start(); time.sleep(8); snap("baslik")
os.makedirs(os.path.join(cap.ROOT, "build", "ui-dumps"), exist_ok=True)
json.dump(dumps, open(os.path.join(cap.ROOT, "build", "ui-dumps", "p1_sonra.json"), "w", encoding="utf-8"), ensure_ascii=False)
