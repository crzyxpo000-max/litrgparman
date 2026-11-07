"""Sanat kaynaklarından (art/promo/) uygulama ve mağaza görsellerini üretir.

Çalıştırma (Pillow gerekir):  python art/build_assets.py
Çıktılar:
  app/src/main/res/mipmap-*/ic_launcher_{foreground,background,monochrome}.webp  (uyarlanabilir simge)
  fastlane/metadata/android/{tr-TR,en-US}/images/{icon,featureGraphic}.png         (Google Play)
  art/promo/preview/*.png                                                         (kontrol önizlemeleri)

Saydamlık "unscreen" ile çıkarılır: zemin rengi R bilinen, parlayan bir çizim için
alfa = max_k (p_k - R_k) / (255 - R_k), renk = R + (p - R) / alfa. R zemini üzerine geri
bindirildiğinde özgün görüntü birebir elde edilir; ışıltı yarı saydam kalır.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
PROMO = ROOT / "art" / "promo"
RES = ROOT / "app" / "src" / "main" / "res"
FASTLANE = ROOT / "fastlane" / "metadata" / "android"
PREVIEW = PROMO / "preview"


def unscreen(rgb: np.ndarray, bg, floor: float = 0.0) -> np.ndarray:
    """RGB (float 0..255) → RGBA (float 0..1), zemin rengi bg saydamlaşır."""
    bg = np.asarray(bg, dtype=np.float32)
    diff = np.clip(rgb - bg, 0, None)
    a_raw = (diff / (255.0 - bg)).max(axis=2)
    col = np.clip(bg + diff / np.maximum(a_raw, 1e-4)[..., None], 0, 255) / 255.0
    a = np.clip((a_raw - floor) / (1 - floor), 0, 1) if floor > 0 else a_raw
    col = np.where(a[..., None] > 0, col, 0)
    return np.dstack([col, a])


def to_image(rgba: np.ndarray) -> Image.Image:
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def bbox(alpha: np.ndarray, thr: float):
    ys, xs = np.where(alpha > thr)
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1


def load(name) -> np.ndarray:
    return np.asarray(Image.open(PROMO / name).convert("RGB"), dtype=np.float32)


# ---------------------------------------------------------------- uyarlanabilir simge
ICON_BG = (3, 10, 27)          # simge görselinin köşe rengi; values/colors.xml ic_launcher_background ile aynı
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
EMBLEM_DP = 46.0               # amblem kutusunun kenarı (dp). Köşegeni 65 dp < 66 dp güvenli daire.


def build_icon():
    src = load("icon.jpg")
    fg = unscreen(src, ICON_BG)
    # Amblem: belirgin parlaklığı olan bölge (ışıltının en soluk kısmı hariç).
    x0, y0, x1, y1 = bbox(fg[..., 3], 0.25)
    side = max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    canvas = side * 108.0 / EMBLEM_DP          # 108 dp tuvalin kaynak piksel karşılığı
    n = int(round(canvas))
    big = np.zeros((n, n, 4), dtype=np.float32)
    ox, oy = int(round(n / 2 - cx)), int(round(n / 2 - cy))
    h, w = fg.shape[:2]
    # Kaynağı tuvale yerleştir (taşan kısım kırpılır).
    sx0, sy0 = max(0, -ox), max(0, -oy)
    dx0, dy0 = max(0, ox), max(0, oy)
    cw, ch = min(w - sx0, n - dx0), min(h - sy0, n - dy0)
    big[dy0:dy0 + ch, dx0:dx0 + cw] = fg[sy0:sy0 + ch, sx0:sx0 + cw]
    # Görüntü kenarına yakın zemin buğusunu söndür (tuval kaynaktan büyük; kenar izi kalmasın).
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)       # 1.0 = tuval kenarının ortası
    fade = np.clip((0.95 - r) / 0.25, 0, 1)               # 54 dp yarıçap güvenli alanın çok dışında
    big[..., 3] *= fade
    fg_img = to_image(big)

    # Güvenli alan denetimi: belirgin piksellerin tümü merkezden 33 dp içinde mi?
    a = big[..., 3]
    ys, xs = np.where(a > 0.25)
    rmax_dp = np.hypot(xs - n / 2, ys - n / 2).max() * 108.0 / n
    print(f"simge: amblem {side}px, en uzak belirgin piksel {rmax_dp:.1f} dp (sınır 33 dp)")
    assert rmax_dp <= 33.0, "ön plan güvenli alanı aşıyor"

    # Tek renkli katman: amblemin net hatları (ışıltı yok), opak beyaz; sistem renklendirir.
    lum = (big[..., :3] * big[..., 3:4]).max(axis=2)
    mono_a = np.clip((lum - 0.62) / 0.12, 0, 1)
    mono = np.dstack([np.ones_like(mono_a)] * 3 + [mono_a])
    mono_img = to_image(mono)

    for d, k in DENSITIES.items():
        px = int(108 * k)
        out = RES / f"mipmap-{d}"
        out.mkdir(parents=True, exist_ok=True)
        fg_img.resize((px, px), Image.LANCZOS).save(out / "ic_launcher_foreground.webp", "WEBP", quality=92, method=6)
        mono_img.resize((px, px), Image.LANCZOS).save(out / "ic_launcher_monochrome.webp", "WEBP", lossless=True, method=6)

    # Bildirim simgesi (24 dp, beyaz siluet): amblemin sıkı kırpılmış tek renkli hali, 2 dp kenar boşluğu.
    mx0, my0, mx1, my1 = bbox(mono_a, 0.1)
    ms = max(mx1 - mx0, my1 - my0)
    mcx, mcy = (mx0 + mx1) // 2, (my0 + my1) // 2
    half = int(ms / 2 * 24 / 20)
    notif = mono_img.crop((mcx - half, mcy - half, mcx + half, mcy + half))
    for d, k in DENSITIES.items():
        px = int(24 * k)
        out = RES / f"drawable-{d}"
        out.mkdir(parents=True, exist_ok=True)
        notif.resize((px, px), Image.LANCZOS).save(out / "ic_notification.webp", "WEBP", lossless=True, method=6)

    # Önizleme: zemin + ön plan, daire maskeli ve maskesiz.
    PREVIEW.mkdir(parents=True, exist_ok=True)
    s = 432
    base = Image.new("RGBA", (s, s), ICON_BG + (255,))
    base.alpha_composite(fg_img.resize((s, s), Image.LANCZOS))
    view = base.crop((s // 6, s // 6, s - s // 6, s - s // 6))       # 72 dp görünür alan
    mask = Image.new("L", view.size, 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).ellipse((0, 0, view.size[0] - 1, view.size[1] - 1), fill=255)
    circ = Image.new("RGBA", view.size, (40, 40, 40, 255))
    circ.paste(view, (0, 0), mask)
    sheet = Image.new("RGBA", (s + view.size[0] * 2 + 40, s), (40, 40, 40, 255))
    sheet.paste(base, (0, 0))
    sheet.paste(circ, (s + 20, (s - view.size[1]) // 2))
    m = Image.new("RGBA", (s, s), (40, 40, 40, 255))
    m.alpha_composite(mono_img.resize((s, s), Image.LANCZOS))
    sheet.paste(m.crop((s // 6, s // 6, s - s // 6, s - s // 6)), (s + view.size[0] + 40, (s - view.size[1]) // 2))
    sheet.convert("RGB").save(PREVIEW / "launcher_icon.png")


# ---------------------------------------------------------------- Google Play
def build_store():
    icon = Image.open(PROMO / "icon.jpg").convert("RGB").resize((512, 512), Image.LANCZOS)
    for loc, banner in (("tr-TR", "banner_tr.jpg"), ("en-US", "banner_en.jpg")):
        out = FASTLANE / loc / "images"
        out.mkdir(parents=True, exist_ok=True)
        icon.save(out / "icon.png", optimize=True)
        b = Image.open(PROMO / banner).convert("RGB")
        target_h = round(b.width * 500 / 1024)               # 1024×500 oranı
        top = (b.height - target_h) // 2
        b.crop((0, top, b.width, top + target_h)).resize((1024, 500), Image.LANCZOS).save(out / "featureGraphic.png", optimize=True)
    print("play: icon 512×512, featureGraphic 1024×500 (tr-TR, en-US)")


if __name__ == "__main__":
    PREVIEW.mkdir(parents=True, exist_ok=True)
    build_icon()
    build_store()
