"""Arayüz incelemesi yakalama aracı (docs/UI_REVIEW.md).

Emülatörde koşul kurar (dil, tema, yazı boyutu, cihaz yazı ölçeği, ekran boyutu), `:tools:simulator:fixtures` ile üretilmiş
bir kaydı `auto.json` olarak yükler, metne göre dokunur ve yarı çözünürlükte ekran görüntüsü alır.

Kullanım (modül olarak): from cap import *; cond("tr", "DARK", "MEDIUM", 1.0, "small"); load("us_erken"); shot("us")
"""
import os, re, subprocess, sys, time, io

ADB = os.environ.get("ADB", r"C:/Users/alpar/AppData/Local/Android/Sdk/platform-tools/adb.exe")
PKG = "com.yukselis.protocol"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
FIX = os.path.join(ROOT, "build", "ui-fixtures")
OUT = os.path.join(ROOT, "docs", "ui-review")
TAG = "tr_small_dark_n"
SIZES = {"small": ("1080x2400", 480), "phone": ("1080x2400", 420), "tablet": ("1600x2560", 320)}
RECAP_OK = r"^(Kaldığım yerden devam et|Pick up where I left off)"
ALL_TIPS = ["hub", "more", "status", "story", "combat", "skills", "bag", "choices", "tags", "map", "relations", "journal"]


def adb(*args, binary=False, check=False):
    r = subprocess.run([ADB, *args], stdin=subprocess.DEVNULL, capture_output=True)
    if check and r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "replace"))
    return r.stdout if binary else r.stdout.decode("utf-8", "replace")


def sh(cmd):
    return adb("shell", cmd)


def stop():
    sh(f"am force-stop {PKG}")


def start(extra=""):
    sh(f"am start -n {PKG}/.MainActivity {extra}")


def cond(lang="tr", theme="DARK", text="MEDIUM", font=1.0, size="small", tips=True, typewriter=False):
    """Koşulu kurar ve TAG'i günceller. tips=True: bir kerelik ipuçları görülmüş sayılır."""
    global TAG
    stop()
    wm, dens = SIZES[size]
    sh(f"wm size {wm}"); sh(f"wm density {dens}")
    sh(f"settings put system font_scale {font}")
    sh("cmd uimode night " + ("yes" if theme == "DARK" else "no"))
    tipset = "".join(f"<string>{t}</string>" for t in ALL_TIPS) if tips else ""
    xml = f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="haptics" value="false" />
    <boolean name="combat_numbers" value="false" />
    <string name="text_size">{text}</string>
    <string name="theme">{theme}</string>
    <string name="lang">{lang}</string>
    <boolean name="typewriter" value="{'true' if typewriter else 'false'}" />
    <set name="seen_tips">{tipset}</set>
    <boolean name="notifications" value="false" />
</map>
"""
    tmp = os.path.join(ROOT, "build", "settings.xml")
    open(tmp, "w", encoding="utf-8", newline="\n").write(xml)
    adb("push", tmp, "/data/local/tmp/settings.xml")
    sh(f"run-as {PKG} mkdir -p shared_prefs")
    sh(f"run-as {PKG} cp /data/local/tmp/settings.xml shared_prefs/settings.xml")
    f = "n" if font == 1.0 and text == "MEDIUM" else ("f2" if font >= 2.0 else f"f{font}") + ("" if text == "MEDIUM" else text.lower()[0:2])
    TAG = f"{lang}_{size}_{'dark' if theme == 'DARK' else 'light'}_{f}" + os.environ.get("CAP_SUFFIX", "")


def nodes():
    """(metin, açıklama, tıklanabilir, sınırlar) listesi."""
    sh("uiautomator dump /sdcard/u.xml >/dev/null 2>&1")
    xml = adb("shell", "cat /sdcard/u.xml")
    out = []
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?clickable="(true|false)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        t, d, c, x0, y0, x1, y1 = m.groups()
        out.append((t.replace("&#10;", " ").replace("&amp;", "&").replace("&quot;", '"').replace("&apos;", "'"), d, c == "true", (int(x0), int(y0), int(x1), int(y1))))
    return out


def find(pattern, index=0):
    rx = re.compile(pattern)
    hits = [n for n in nodes() if rx.search(n[0]) or rx.search(n[1])]
    return hits[index] if len(hits) > index else None


def tap(pattern, index=0, wait=1.0, required=True):
    n = find(pattern, index)
    if n is None:
        if required:
            raise RuntimeError(f"bulunamadı: {pattern}")
        return False
    x0, y0, x1, y1 = n[3]
    sh(f"input tap {(x0 + x1) // 2} {(y0 + y1) // 2}")
    time.sleep(wait)
    return True


def tap_xy(x, y, wait=1.0):
    sh(f"input tap {x} {y}"); time.sleep(wait)


def swipe_up(frac=0.5, wait=0.8):
    w, h = size_px()
    sh(f"input swipe {w // 2} {int(h * 0.75)} {w // 2} {int(h * (0.75 - frac))} 300"); time.sleep(wait)


def back(wait=0.8):
    sh("input keyevent 4"); time.sleep(wait)


def size_px():
    m = re.search(r"Override size: (\d+)x(\d+)", sh("wm size")) or re.search(r"Physical size: (\d+)x(\d+)", sh("wm size"))
    return int(m.group(1)), int(m.group(2))


def load(fixture, cont=True, wait=2.5, recap=False):
    """Kaydı auto.json olarak yükler, uygulamayı açar ve Devam et'e dokunur."""
    stop()
    src = os.path.join(FIX, fixture + ".json")
    adb("push", src, "/data/local/tmp/fixture.json", check=True)
    sh(f"run-as {PKG} mkdir -p files/saves")
    sh(f"run-as {PKG} cp /data/local/tmp/fixture.json files/saves/auto.json")
    sh(f"run-as {PKG} rm -f files/saves/auto.prev.json files/saves/checkpoint.json")
    start()
    if cont:
        for _ in range(30):
            time.sleep(0.8)
            ns = nodes()
            if not any(re.search(r"^(Devam et|Continue)", n[0]) for n in ns):
                if ns and not any(re.search(r"^(Yeni oyun|New game)", n[0]) for n in ns) and any(n[0] for n in ns):
                    if not recap:
                        tap(RECAP_OK, required=False, wait=1.2)
                    return
                continue
            if any(re.search(r"(yükleniyor|Loading)", n[0]) for n in ns):
                continue
            tap(r"^(Devam et|Continue)", required=False, wait=wait)
        raise RuntimeError("Devam et görünmedi")


def shot(name, tag=None):
    from PIL import Image
    raw = adb("exec-out", "screencap", "-p", binary=True)
    img = Image.open(io.BytesIO(raw)).convert("RGB")
    img = img.resize((img.width // 2, img.height // 2), Image.LANCZOS)
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, f"{name}__{tag or TAG}.png")
    img.save(path, optimize=True)
    return path


def texts():
    return [(n[0] or n[1], n[2], n[3]) for n in nodes() if n[0] or n[1]]


def reset_display():
    sh("wm size reset"); sh("wm density reset"); sh("settings put system font_scale 1.0")


def sheet(names, out, cols=4):
    """Görüntüleri tek sayfada yan yana dizer (inceleme için)."""
    from PIL import Image
    imgs = [Image.open(os.path.join(OUT, n)) for n in names]
    h = max(i.height for i in imgs)
    scale = [(int(i.width * 800 / h) if h > 800 else i.width, min(h, 800) if h > 800 else i.height) for i in imgs]
    imgs = [i.resize(s) for i, s in zip(imgs, scale)]
    rows = [imgs[k:k + cols] for k in range(0, len(imgs), cols)]
    W = max(sum(i.width + 8 for i in r) for r in rows); H = sum(max(i.height for i in r) + 8 for r in rows)
    canvas = Image.new("RGB", (W, H), (60, 60, 60))
    y = 0
    for r in rows:
        x = 0
        for i in r:
            canvas.paste(i, (x, y)); x += i.width + 8
        y += max(i.height for i in r) + 8
    canvas.save(out)
    return out


if __name__ == "__main__":
    for t in texts():
        print(t)
