"""Tüm ekranları bir koşulda yakalar. Kullanım: python s_all.py <lang> <DARK|LIGHT> <MEDIUM|XLARGE> <font> <small|phone|tablet> [core]"""
import json, sys, traceback
from cap import *
import cap

lang, theme, text, font, size = sys.argv[1], sys.argv[2], sys.argv[3], float(sys.argv[4]), sys.argv[5]
core = len(sys.argv) > 6 and sys.argv[6] == "core"
cond(lang, theme, text, font, size)
dumps = {}
log = []

TAB = {"story": r"^(Hikaye|Story)$", "status": r"^(Durum|Status)$", "skills": r"^(Yetenekler|Skills)$", "bag": r"^(Çanta|Bag)$", "more": r"^(Daha fazla|More)$"}


def snap(name, scroll=0):
    try:
        shot(name)
        dumps[name] = cap.nodes()
        for i in range(scroll):
            swipe_up(0.45)
            shot(f"{name}_k{i + 1}")
            dumps[f"{name}_k{i + 1}"] = cap.nodes()
    except Exception as e:
        log.append(f"{name}: {e}")


def step(name, fn):
    try:
        fn()
    except Exception as e:
        log.append(f"{name}: {e!r}")
        traceback.print_exc()


def scene(fix, name=None, scroll=1):
    def f():
        load(fix)
        snap(name or fix, scroll)
    step(fix, f)


def title():
    stop(); start(); time.sleep(4)
    snap("baslik", 1)


def recap():
    load("us_erken", recap=True); snap("donus_ozeti")


def hub_and_tabs():
    load("us_perde2")
    snap("us_perde2", 2)
    for key in ("status", "skills", "bag", "more"):
        if tap(TAB[key], required=False):
            snap(f"sekme_{key}", 1 if key != "more" else 0)
            if key == "status":
                for sub, rx in (("unvan", r"^(Unvanlar|Titles)$"), ("ahlak", r"^(Ahlak|Morality)$")):
                    if tap(rx, required=False):
                        snap(f"durum_{sub}", 1)
    # Daha fazla karoları
    tap(TAB["more"], required=False)
    tiles = [n for n in cap.nodes() if n[0]]
    names = [(t[0], t[3]) for t in tiles]
    dumps["_more_tiles"] = names
    for label, rx in (("gunluk_gorev", r"^(Günlük Görev|Daily Quest)$"), ("gunluk", r"^(Günlük|Journal)$"), ("iliskiler", r"^(İlişkiler|Relationships)$"),
                      ("harita", r"^(Harita|Map)$"), ("siralama", r"^(Küresel Sıralama|Global Ranking|Sıralama|Ranking)$"), ("kayitlar", r"^(Kayıtlar|Saves)$"),
                      ("ayarlar", r"^(Ayarlar|Settings)$"), ("yardim", r"^(Yardım|Help)$")):
        tap(TAB["more"], required=False, wait=0.6)
        if tap(rx, required=False, wait=1.5):
            snap(f"dahafazla_{label}", 1)
            back()
        else:
            log.append(f"karo yok: {label}")


def early_hub():
    load("us_erken"); snap("us_erken", 1)
    tap(TAB["status"], required=False); snap("erken_durum")
    tap(TAB["more"], required=False); snap("erken_dahafazla")


def operation():
    load("us_operasyon_acik_b2"); snap("us_operasyon_b2", 1)
    cap.sh("input swipe 540 600 540 1800 200"); time.sleep(0.6)
    if tap(r"(Operasyon|Operation)", required=False, wait=1.5):
        snap("operasyon_onay")


def moments():
    for m in ("level", "evolution", "unique", "rank", "breach"):
        load("us_erken")
        cap.sh(f"am start -f 0x20000000 -n {cap.PKG}/.MainActivity --es demo_moment {m}")
        time.sleep(2.5)
        snap(f"an_{m}")


def ending():
    load("son"); snap("son", 4)


step("title", title)
step("early_hub", early_hub)
scene("sahne_3_secenek"); scene("sahne_8_secenek", scroll=2); scene("sahne_kilitli")
scene("savas_ilk"); scene("savas_taktik"); scene("savas_infaz")
scene("istem_sinif")
step("operation", operation)
step("hub_and_tabs", hub_and_tabs)
if not core:
    step("recap", recap)
    for f in ("prolog_sahne", "sahne_5_secenek", "sahne_6_secenek", "sahne_uzun", "sahne_sezgi", "sahne_kilitli_p2", "varis", "yenilgi_sahnesi",
              "savas_boss", "savas_boss_faz", "savas_kalabalik", "istem_evolution", "istem_essence_bind", "brifing_1", "brifing_4", "darbogaz_2",
              "gorev_erken", "gorev_perde2", "us_perde3"):
        scene(f)
    step("moments", moments)
    step("ending", ending)

os.makedirs(os.path.join(cap.ROOT, "build", "ui-dumps"), exist_ok=True)
json.dump(dumps, open(os.path.join(cap.ROOT, "build", "ui-dumps", cap.TAG + ".json"), "w", encoding="utf-8"), ensure_ascii=False)
print("TAG", cap.TAG, "görüntü", len(dumps))
print("\n".join(log))
