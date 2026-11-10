"""Akış denetimleri: açılış süresi, geri tuşu, kayıt üstüne yazma, yeni oyun, dil değişimi, kaydırma akıcılığı."""
import re, time
from cap import *
import cap


def has(rx):
    return find(rx) is not None


def tx():
    return " | ".join(t[0] for t in texts())[:400]


cond("tr", "DARK", "MEDIUM", 1.0, "small")

# 1) Soğuk açılış
stop(); time.sleep(1)
out = sh(f"am start -W -n {cap.PKG}/.MainActivity")
print("AM START:", re.findall(r"(TotalTime|WaitTime): (\d+)", out))
t0 = time.time()
while has(r"yükleniyor") or not has(r"Devam et"):
    if time.time() - t0 > 40: break
print("Dünya yüklenme süresi (start -W sonrası, s): %.1f" % (time.time() - t0))

# 2) Geri tuşu: hikaye sahnesi
load("sahne_3_secenek")
back(1.5)
print("GERİ hikaye sahnesinde →", "uygulama kapandı/başlık" if not has(r"^Hikaye$") else "aynı ekranda", "|", tx()[:120])
focus = sh("dumpsys window | grep mCurrentFocus")
print("  odak:", focus.strip()[:120])

# Durum sekmesinde geri
load("us_perde2"); tap(r"^Durum$"); back(1.5)
print("GERİ Durum sekmesinde →", sh("dumpsys window | grep mCurrentFocus").strip()[-60:], "| Hikaye sekmesi görünür:", has(r"^Hikaye$"))

# 3) Kayıt üstüne yazma
load("us_perde2"); tap(r"^Daha fazla$|^Daha fa"); tap(r"^Kayıtlar$", wait=1.5)
tap(r"^Kaydet$", 0, wait=1.2); shot("akis_kaydet_1")
print("KAYDET 1 sonrası:", tx()[:300])
tap(r"^Kaydet$", 0, wait=1.2); shot("akis_kaydet_ustune")
print("KAYDET üstüne (onay var mı?):", "ONAY PENCERESİ" if has(r"(Emin|üzerine|Üzerine|Vazgeç|İptal)") else "ONAY YOK", "|", tx()[:200])
# Yükle
tap(r"^Yükle$", 0, wait=1.5); shot("akis_yukle")
print("YÜKLE (onay var mı?):", "ONAY PENCERESİ" if has(r"(Emin|Vazgeç|İptal)") else "ONAY YOK", "|", tx()[:160])

# 4) Yeni oyun (kayıt varken)
load("us_perde2", cont=False); time.sleep(6)
tap(r"^Entegrasyonu başlat$", wait=3); shot("akis_yeni_oyun")
print("YENİ OYUN kayıt varken (onay var mı?):", "ONAY PENCERESİ" if has(r"(Emin|silinecek|Vazgeç|İptal)") else "ONAY YOK", "|", tx()[:200])

# 5) Dil değişimi anında mı
load("us_perde2"); tap(r"^Daha fazla$|^Daha fa"); tap(r"^Ayarlar$", wait=1.5)
tap(r"^English$", wait=1.5); shot("akis_dil_en", tag="tr_to_en")
print("DİL → EN sonrası:", tx()[:260])
back(); tap(r"^(Story|Hikaye)$", wait=1.5); shot("akis_dil_en_hikaye", tag="tr_to_en")
print("  hikaye ekranı:", tx()[:260])

# 6) Kaydırma akıcılığı: Günlük (uzun liste) ve son ekranı
cond("tr", "DARK", "MEDIUM", 1.0, "small")
load("son")
sh(f"dumpsys gfxinfo {cap.PKG} reset")
for _ in range(8):
    swipe_up(0.5, 0.3)
for _ in range(8):
    sh("input swipe 540 900 540 1900 250"); time.sleep(0.3)
g = sh(f"dumpsys gfxinfo {cap.PKG}")
print("GFX son ekranı:", re.findall(r"(Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile): ([^\n]+)", g))

# 7) En büyük yazı: başlıkta Devam et'e ulaşılıyor mu
cond("tr", "DARK", "XLARGE", 2.0, "small")
stop(); start(); time.sleep(7)
print("F2+XL başlık ilk ekran:", tx()[:200])
for i in range(4):
    swipe_up(0.5)
    print("  kaydırma", i + 1, ":", tx()[:200])
shot("baslik_k_son")
cond("tr", "DARK", "MEDIUM", 1.0, "small")
