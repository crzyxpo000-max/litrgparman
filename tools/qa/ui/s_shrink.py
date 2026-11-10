"""docs/ui-review görüntülerini 96 renkli palete indirger (depo boyutu)."""
import os, glob
from PIL import Image
from cap import OUT
n = 0
for p in glob.glob(os.path.join(OUT, "*.png")):
    im = Image.open(p)
    if im.mode == "P": continue
    im.convert("RGB").quantize(96, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(p, optimize=True); n += 1
print(n)
