---
name: oyun-testi
description: Uygulamayı derleyip bağlı telefona ya da emülatöre kurar (adb), ana ekranların ekran görüntülerini alıp inceler ve bulguları raporlar. Kullanım /oyun-testi [odaklanılacak ekran]
argument-hint: [ekran ya da özellik]
---

# /oyun-testi $ARGUMENTS

Windows'ta: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`, adb yolu genelde
`$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe`; Git Bash'te yollar için `MSYS_NO_PATHCONV=1`.

1. **Cihaz:** `adb devices`. Cihaz yoksa kullanıcıya telefonu bağlamasını ya da emülatörü açmasını söyle ve dur.
2. **Derle ve kur:** `./gradlew installDebug` (hata olursa özetle ve dur).
3. **Başlat:** `adb shell monkey -p com.yukselis.protocol -c android.intent.category.LAUNCHER 1`, birkaç saniye bekle.
4. **Gez ve görüntü al:** her ekran için `adb exec-out screencap -p > <scratchpad>/ekran_<ad>.png`, sonra Read ile bak.
   Sıra: başlık → Hikaye (bölüm satırı, gün akışı paneli) → Durum → Daha fazla → Görevler (bölüm hedefi). Gezinti için
   `adb shell input tap X Y` / `input swipe`; koordinatları önceki görüntüden çıkar. Varsa $ARGUMENTS ekranına odaklan.
5. **İncele:** taşan/kırpılan metin, yanlış dil, çözülmemiş belirteç (`{npc:..}`, `[?..]`), okunmayan kontrast,
   üst üste binen öğeler, boş durumlar, `adb logcat -d -s AndroidRuntime:E` ile çökme.
6. **Rapor:** ekran başına bulgular (görüntü dosyası, sorun, önerilen düzeltme yeri: dosya:satır). Sorun yoksa söyle.
