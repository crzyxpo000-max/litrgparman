"""Döküm dosyalarından erişilebilirlik ölçümü: 48dp altı dokunma hedefleri, etiketsiz tıklanabilirler, seçenek alanı payı.
Kullanım: python s_a11y.py <tag> <yoğunluk dpi>"""
import json, os, sys, collections
tag, dpi = sys.argv[1], int(sys.argv[2])
root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
d = json.load(open(os.path.join(root, "build", "ui-dumps", tag + ".json"), encoding="utf-8"))
px = 48 * dpi / 160
small = collections.defaultdict(set); unl = collections.defaultdict(int)
for name, nodes in d.items():
    if name.startswith("_"): continue
    for i, (t, desc, click, b) in enumerate(nodes):
        if not click: continue
        w, h = b[2] - b[0], b[3] - b[1]
        if w <= 0 or h <= 0: continue
        # Tıklanabilir düğümün etiketi: kendi metni/açıklaması ya da sınırları içindeki bir sonraki metin düğümü.
        label = t or desc
        if not label:
            for (t2, d2, c2, b2) in nodes[i + 1:i + 6]:
                if (t2 or d2) and b2[0] >= b[0] and b2[2] <= b[2] and b2[1] >= b[1] and b2[3] <= b[3]:
                    label = t2 or d2; break
        if not label:
            unl[name] += 1
        if (w < px - 1 or h < px - 1):
            small[(label or "(etiketsiz)")[:40]].add(f"{name} {round(w*160/dpi)}x{round(h*160/dpi)}dp")
print("48dp altı hedefler:")
for k, v in sorted(small.items(), key=lambda kv: -len(kv[1])):
    print(f"  {k!r}: {len(v)} ekran, örn. {sorted(v)[0]}")
print("etiketsiz tıklanabilir (ekran: adet):", dict(unl))
