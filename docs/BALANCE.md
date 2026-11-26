# Denge raporu (simülatör)

`./gradlew :tools:simulator:run --args="1000"` ile üretildi; 1000 oynanış, 32 sn. Nüfus: %30 rastgele oyuncu, %70 sekiz hedefli
oyun tarzı (%25 rastgele seçim gürültüsüyle); her 10 oynanıştan biri Demir Mod. Ayrıntı: `tools/simulator`.

## Son dağılımı

Hedef: her son %1–%40 (gizli Döngü hariç).

| Son | Oynanış | Pay |
| --- | ---: | ---: |
| koruyucu | 194 | 19.4% |
| demir_taht | 78 | 7.8% |
| yukselis | 64 | 6.4% |
| kod_kirici | 168 | 16.8% |
| ozgurluk | 71 | 7.1% |
| vassal | 116 | 11.6% |
| yalniz_avci | 83 | 8.3% |
| hasat | 159 | 15.9% |
| dongu | 67 | 6.7% |

**Sonuç:** dağılım sınırı sağlanıyor.

### Oyun tarzına göre

| Tarz | Oynanış | En sık son | Hedefe ulaşma |
| --- | ---: | --- | ---: |
| demir_taht | 88 | demir_taht (64) | 72.7% |
| dongu | 88 | dongu (67) | 76.1% |
| kod_kirici | 87 | kod_kirici (58) | 66.7% |
| koruyucu | 88 | koruyucu (68) | 77.3% |
| ozgurluk | 87 | ozgurluk (71) | 81.6% |
| rastgele | 300 | hasat (108) | — |
| vassal | 87 | vassal (77) | 88.5% |
| yalniz_avci | 87 | yalniz_avci (51) | 58.6% |
| yukselis | 88 | yukselis (52) | 59.1% |

Kadim istilası (Perde 4 finali) kazanma oranı ve Gün 330 güç oranı (oyuncu ÷ dünya) tarza göre: demir_taht 27.3% ×0.79 · dongu 67.0% ×0.81 · kod_kirici 73.6% ×0.80 · koruyucu 84.1% ×0.80 · ozgurluk 12.6% ×0.86 · rastgele 46.3% ×0.76 · vassal 20.7% ×0.76 · yalniz_avci 51.7% ×0.76 · yukselis 15.9% ×0.74

Hedefe ulaşamayan tarzlarda tutmayan alt koşullar (en sık 3):

- **demir_taht** (13): `HasFlag(flag=lonca_hakimiyeti)` 13
- **dongu** (14): `HasFlag(flag=yanki_son_sirri)` 13 · `VarAtLeast(key=kule_kat, value=100)` 9
- **kod_kirici** (21): `QuestIs(quest=zincir_sessiz_ag, status=DONE)` 13 · `LacksFlag(flag=yanki_silindi)` 8
- **koruyucu** (16): `HasFlag(flag=kadimler_yenildi)` 14 · `ReputationAtLeast(faction=siginak, value=50)` 2
- **ozgurluk** (1): `HasFlag(flag=sistem_cekirdegine_ulasildi)` 1
- **vassal** (5): `VarAtLeast(key=kadim_tehdit, value=70)` 4 · `RelationAtLeast(npc=thessar, axis=BOND, value=50)` 1
- **yalniz_avci** (32): `GradeAtLeast(grade=S)` 17 · `CompanionsAtMost(value=0)` 16 · `HasFlag(flag=uye_yok)` 2
- **yukselis** (29): `QuestIs(quest=zincir_yukselenler, status=DONE)` 21 · `NpcAlive(npc=vahit)` 18

## Seviye eğrisi

| Gün | Dünya seviyesi | Oyuncu p10 | p50 | p90 | Kademe (p50) | NPC ort. (perde sonu) |
| ---: | ---: | ---: | ---: | ---: | --- | ---: |
| 30 | 10 | 8 | 9 | 11 | F | 10 |
| 60 | 29 | 16 | 33 | 42 | E |  |
| 90 | 43 | 32 | 70 | 91 | D |  |
| 120 | 62 | 51 | 86 | 112 | D |  |
| 150 | 91 | 77 | 111 | 126 | C |  |
| 180 | 132 | 129 | 170 | 185 | C | 34 |
| 210 | 193 | 182 | 264 | 336 | B |  |
| 240 | 282 | 244 | 361 | 411 | B |  |
| 270 | 411 | 316 | 468 | 552 | A |  |
| 300 | 599 | 404 | 620 | 743 | A |  |
| 330 | 874 | 446 | 727 | 804 | A | 51 |
| 360 | 874 | 549 | 1076 | 1389 | S |  |
| 365 | 874 | 549 | 1076 | 1389 | S | 60 |

## OP MC ölçüleri

- **Hakimiyet evresi payı** (Gün 31+'de oyuncu seviyesinin dünya seviyesine eşit ya da üstünde olduğu günler): ortalama 69.5% (hedef %60+; sağlanıyor).
- **Ezme payı** (kazanılan savaşların tek satırla çözülen kısmı): ortalama 19.1%.
- **Perde başına oyuncu / NPC güç farkı** (oyuncu seviyesi p50 ÷ ana NPC'lerin ortalama seviyesi; en güçlü NPC ortalaması):
  - Gün 30: ×0.9 (en güçlü NPC ortalaması Sv. 14)
  - Gün 180: ×5.0 (en güçlü NPC ortalaması Sv. 46)
  - Gün 330: ×14.0 (en güçlü NPC ortalaması Sv. 70)
  - Gün 365: ×18.4 (en güçlü NPC ortalaması Sv. 96)
- **Kule:** ortalama en yüksek kat 94; 100'e ulaşan 71.8%.

### Zaman içinde Dikkat

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 6 | 26 | 55 |
| 60 | 0 | 25 | 66 |
| 90 | 0 | 48 | 80 |
| 120 | 0 | 28 | 51 |
| 150 | 0 | 5 | 32 |
| 180 | 0 | 15 | 47 |
| 210 | 30 | 66 | 91 |
| 240 | 9 | 59 | 92 |
| 270 | 1 | 60 | 94 |
| 300 | 0 | 72 | 92 |
| 330 | 0 | 12 | 29 |
| 360 | 12 | 67 | 87 |
| 365 | 12 | 67 | 87 |

### Öz Kristali ekonomisi

- **İsteğe bağlı ödeme seçenekleri (`oz_kristali_min`):** karşılaşılan 22234, yetersiz bakiyeyle kilitli 2519 (11.3%; geri dönüş eşiği <%50, DECISIONS #167).

| Gün | p10 | p50 | p90 |
| ---: | ---: | ---: | ---: |
| 30 | 4 | 10 | 16 |
| 60 | 7 | 29 | 71 |
| 120 | 50 | 141 | 203 |
| 180 | 183 | 309 | 373 |
| 270 | 363 | 658 | 801 |
| 365 | 506 | 983 | 1161 |

## Ölüm oranları

- Normal mod: oynanış başına ortalama 0.00 ölüm (kayıt noktasına dönüş); en az bir ölüm 0.0%; p90 0.
- Demir Mod: 100 oynanış; ölümle Hasat 0.0%. (Simülatörde haddini aşan, bilerek her şeyi açığa vuran bir tarz yok: Demir Mod ölümleri hikaye savaşı ve bosslardan gelir.)
- Savaş: oynanış başına ortalama 320; yere düşülen (yazılmış yenilgi ya da bayılma) 11.4%; İkinci Şans kullanan oynanış 0.0%.
- Bitmeyen oynanış: 0.

## Kapsam

- Oynanış başına görülen olay: ortalama 395.
- Hiç görülmeyen olay: 5 / 526: `atlama_meslek`, `bildirim_goz`, `bildirim_kapi`, `bildirim_rakip`, `hasat_vakti`. *(tamamı yapısal: `atlama_meslek` kapalı meslek sistemi, `bildirim_*` oturum dışı Android bildirimi, `hasat_vakti` Gün 365 yedek güvenlik ağı)*
- En nadir 10 olay: `elif_yoldas_darbe` 0.9%, `kaya_soguk_firin` 4.7%, `thessar_kirik_muhur` 5.6%, `yozlasma_75` 6.4%, `bl15_us_beyaz_kapi` 6.8%, `p3_firtina_olcucu` 7.7%, `yozlasma_50` 9.1%, `bl15_pk_kamp_aras` 9.2%, `sb14_mert_salon` 9.4%, `fener_yavrulari` 13.2%.
- En sık 10 olay: `esik_rakip` 100.0%, `son_karar` 100.0%, `bl13_kd_catilar` 100.0%, `bl13_kd_bekci` 100.0%, `bl13_kd_police` 100.0%, `bl12_kd_senet_donen` 100.0%, `bl12_kd_senet` 100.0%, `bl11_kule_dibi_can` 100.0%, `kule_kat_10` 100.0%, `p3_kapi_firtinasi` 100.0%.
- Bitiş seviyesi: p10 549, p50 1076, p90 1389.
