---
name: denge
description: balance-analyst alt ajanıyla denge simülatörünü çalıştırır, önceki raporla karşılaştırır ve önerileri özetler. Kullanım /denge [oynanış sayısı]
argument-hint: [oynanış sayısı, varsayılan 1000]
---

# /denge $ARGUMENTS

1. `balance-analyst` alt ajanını çalıştır; oynanış sayısı olarak $ARGUMENTS'ı (boşsa 1000) ver.
2. Dönen tabloyu olduğu gibi göster.
3. Altına en çok 5 maddelik özet yaz: sınır dışı değerler (son dağılımı %1–%40, hakimiyet ≥ %60), en büyük kaymalar ve
   bunlara yol açmış olabilecek son değişiklikler (`git log -5 --oneline`), önerilen ayar (dosya ve sabit adıyla:
   ör. `WorldEngine.EXPLORE_FIGHT_CHANCE`). Ayarı kendin değiştirme; kullanıcıya sor.
