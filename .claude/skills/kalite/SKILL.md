---
name: kalite
description: scripts/check.sh'i (test + validateContent + simülatör denge testi + uslup + assembleDebug + lint) çalıştırır, başarısız olan her şeyi özetler ve düzeltme önerir. Kullanım /kalite
---

# /kalite

1. Çalıştır (birkaç dakika sürer; arka planda çalıştırıp bitmesini bekle):
   `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" bash scripts/check.sh > <scratchpad>/check.log 2>&1; echo "exit $?"`
2. Geçtiyse tek satırla söyle (süre ve uyarı sayısıyla).
3. Başarısızsa her hata için:
   - Hangi aşama (engine test, validateContent, simülatör DengeTest, uslup, derleme, lint).
   - Test hatalarında JUnit XML'den (`*/build/test-results/test/*.xml`) test adı ve iletisi; lint'te
     `app/build/reports/lint-results-debug.txt`.
   - Olası neden ve önerilen düzeltme (dosya:satır).
4. Düzeltmeleri kendiliğinden uygulama; listeyi ver ve hangisini düzeltmemi istediğini sor.
