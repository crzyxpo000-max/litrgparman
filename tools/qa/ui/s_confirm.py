"""Onay pencereleri ve geri tuşu görüntüleri (UI_REVIEW B1–B4, düzeltme sonrası)."""
from cap import *
import cap, os
os.environ["CAP_SUFFIX"] = "_sonra"
cond("tr", "DARK", "MEDIUM", 1.0, "small")
load("us_perde2"); tap(r"^Daha fazla$"); tap(r"^Kayıtlar$", wait=1.5)
if not tap(r"^Üstüne yaz$", required=False):
    tap(r"^Kaydet$", 0, wait=1.2)
    if find(r"^Üstüne yaz$"): pass
    else: tap(r"^Kaydet$", 0, wait=1.2)
print("kaydet onayı:", find(r"^Kaydın üstüne yaz$") is not None); shot("akis_kaydet_ustune")
tap(r"^Vazgeç$", required=False)
tap(r"^Yükle$", 0, wait=1.2)
print("yükle onayı:", find(r"^Kaydı yükle$") is not None); shot("akis_yukle")
tap(r"^Vazgeç$", required=False)
tap(r"^Hikaye$"); back(0.6); shot("akis_geri_ipucu")
print("geri ipucu:", find(r"Çıkmak için") is not None, "| oyunda:", find(r"^Hikaye$") is not None)
load("us_perde2", cont=False); time.sleep(7)
swipe_up(0.3)
tap(r"^Entegrasyonu başlat$", wait=1.5)
print("yeni oyun onayı:", find(r"^Yeni oyuna başla$") is not None); shot("akis_yeni_oyun")
tap(r"^Vazgeç$", required=False)
print("hâlâ başlıkta:", find(r"^Devam et$") is not None)
