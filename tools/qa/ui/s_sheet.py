"""İnceleme sayfası: python s_sheet.py <çıkış.png> <ad1> <ad2> ... (adlar docs/ui-review içindeki dosya adları, .png'siz olabilir)"""
import sys, os
from cap import sheet, OUT
names = [n if n.endswith(".png") else n + ".png" for n in sys.argv[2:]]
names = [n for n in names if os.path.exists(os.path.join(OUT, n))]
print(sheet(names, sys.argv[1], cols=len(names)), len(names))
