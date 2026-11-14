# Zincir

Kişisel antrenman, beslenme ve esneklik takip uygulaması (Android). Asıl işi zinciri kırmamanı sağlamak:
program haftanın günlerine değil **sıraya** göre ilerler; atladığın gün sırayı bozmaz.

- Tamamen çevrimdışı. Hesap, sunucu, reklam, analitik yok. İnternet izni istemez.
- Arayüz Türkçe, ondalık ayırıcı virgül.
- Veri yalnızca telefonda durur (Google hesabına da yedeklenmez). Yedeği sen alırsın: Ayarlar › Yedekleme.

Bu klasör, depodaki oyundan bağımsız, kendi başına derlenen bir projedir.

---

## 1. Bilgisayarında gerekenler

İki şey gerekir: **JDK** (Java geliştirme kiti, sürüm 17 ya da üstü) ve **Android SDK**.

### Kurulu mu, nasıl anlarım?

Bir terminal aç (Windows: "PowerShell", Mac: "Terminal") ve şunu yaz:

```bash
java -version
```

- `openjdk version "17..."` (ya da 21, 25) gibi bir satır çıkarsa JDK kurulu.
- "komut bulunamadı" derse kurulu değil.

Android SDK için şu klasörlerden biri var mı bak:

| İşletim sistemi | Android SDK'nın olağan yeri |
| --- | --- |
| Windows | `C:\Users\<adın>\AppData\Local\Android\Sdk` |
| Mac | `/Users/<adın>/Library/Android/sdk` |
| Linux | `/home/<adın>/Android/Sdk` |

### Eksikse: en kolay yol Android Studio

Android Studio ikisini birden getirir (JDK'yı içinde taşır, SDK'yı ilk açılışta indirir).

1. <https://developer.android.com/studio> adresinden Android Studio'yu indir ve kur.
2. İlk açılışta çıkan kurulum sihirbazında **Standard**'ı seç, lisansları kabul et; SDK inene kadar bekle.
3. **Open** de ve bu `zincir` klasörünü seç. Sağ altta "Gradle sync" bitene kadar bekle (ilk seferde 5–10 dakika sürebilir;
   internet gerekir, çünkü kütüphaneler indirilir).
4. Bu kadar. Android Studio, SDK'nın yerini `local.properties` dosyasına kendisi yazar.

Android Studio kullanmadan, yalnızca terminalden derlemek istersen `zincir` klasörüne `local.properties` adında bir dosya açıp
SDK'nın yerini yazman gerekir. Örnek (Mac):

```
sdk.dir=/Users/adin/Library/Android/sdk
```

Terminalde `java` bulunamıyorsa Android Studio'nun içindeki JDK'yı gösterebilirsin:

- Mac: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- Windows (PowerShell): `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`

---

## 2. Derleme

Terminalde bu klasöre gel ve:

```bash
./gradlew assembleDebug        # Mac / Linux
gradlew.bat assembleDebug      # Windows
```

Bitince APK burada olur:

```
app/build/outputs/apk/debug/app-debug.apk
```

Android Studio'da aynısı: menüden **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.

Testleri çalıştırmak için:

```bash
./gradlew test
```

---

## 3. Telefona kurma

### Yol A: APK dosyasını telefona gönder (kablosuz, en basit)

1. `app-debug.apk` dosyasını telefona ulaştır (kendine e-posta, Drive, WhatsApp "kendime mesaj", USB ile kopyalama; hangisi kolaysa).
2. Telefonda dosyaya dokun. Android "Bu kaynaktan uygulama yüklemeye izin verilmiyor" derse **Ayarlar**'a git ve o uygulama
   (Dosyalar, Chrome, Gmail…) için **Bu kaynaktan izin ver**'i aç, geri dön.
3. **Yükle**'ye bas. Play Protect "Geliştiricisi tanınmıyor" diye sorarsa **Yine de yükle** de (uygulamayı sen derledin).

### Yol B: USB kablosuyla doğrudan kur

1. Telefonda **Ayarlar › Telefon hakkında › Yapı numarası**'na 7 kez dokun ("Geliştirici oldunuz" yazar).
2. **Ayarlar › Sistem › Geliştirici seçenekleri › USB hata ayıklama**'yı aç.
3. Telefonu kabloyla bağla; telefonda çıkan "USB hata ayıklamaya izin ver" sorusuna **İzin ver** de.
4. Terminalde: `./gradlew installDebug` (ya da Android Studio'da yeşil ▶ düğmesi).

### Güncellerken dikkat (önemli)

Android, bir uygulamanın yeni sürümünü ancak **aynı imzayla** derlenmişse eskisinin üzerine kurar. "Debug" APK'ları, derlendiği
bilgisayarın kendi anahtarıyla imzalanır. Yani APK'yı farklı bir bilgisayarda derlersen telefon "Uygulama yüklenmedi" der;
eskisini kaldırman gerekir ve **kaldırınca veri silinir**.

Bu yüzden: **her güncellemeden önce yedek al** (aşağıda). Aynı bilgisayarda derlediğin sürece sorun çıkmaz; veri yerinde kalır.

---

## 4. Yedek alma ve geri yükleme

**Ayarlar › Yedekleme** (Bugün ekranının sağ üstündeki "Ayarlar").

- **Yedeği paylaş:** tüm veriyi `zincir-yedek-TARİH.json` adlı tek dosyaya yazar ve paylaşım penceresini açar.
  Kendine e-postayla gönder ya da Drive'a kaydet.
- **Dosyaya kaydet:** aynı dosyayı telefonda seçtiğin klasöre (örneğin İndirilenler) kaydeder.
- **Yedekten geri yükle:** bir yedek dosyası seçersin; onay verince telefondaki tüm kayıtlar silinir ve yedekteki veri yüklenir.

Öneri: haftalık gözden geçirmeden sonra bir yedek paylaş. Telefon değiştirirken ya da uygulamayı kaldırmadan önce mutlaka al.

Yedek dosyası düz metindir (JSON); bir metin düzenleyiciyle açıp içine bakabilirsin.

---

## 5. Uygulama nasıl çalışır (kısa)

| Sekme | Ne yapar |
| --- | --- |
| **Bugün** | Sıradaki iş (Seans A/B, Kardiyo, Yalnızca adım), uçuş günü anahtarı, uyku girişi, günlük hedefler, haftalık seans sayacı, zincir uyarısı |
| **Antrenman** | Set set kg/tekrar kaydı, önceki değerler, dinlenme sayacı (bitince titreşim), "makine yok" alternatifi, kardiyo kaydı |
| **Beslenme** | Tek dokunuşla "planlandığı gibi yedim", gramla hızlı ekleme, haftalık kaçamak sayacı, bilgi kartları |
| **Esneklik** | Kendiliğinden ilerleyen 10 dakikalık germe zamanlayıcısı, haftalık sayaç, aylık ölçüm |
| **İlerleme** | Tartı, haftalık ortalama ve grafik, ayar önerileri, bel çevresi, hareket geçmişi, haftalık özet ve gözden geçirme |
| **Ayarlar** | Hedefler, hareket/öğün/germe/yiyecek düzenleme, bildirimler, tema, yedekleme, eğer-o zaman planları |

### Sıra kuralları

- Döngü 14 adımdır: A, Kardiyo, B, Adım, A, Kardiyo, Adım; sonra B, Kardiyo, A, Adım, B, Kardiyo, Adım.
- Sıra yalnızca iş **yapılınca** ilerler. Yapmadığın seans ertesi gün yine karşına çıkar.
- "Yalnızca adım" günü, o gün geçince kendiliğinden biter.
- "Hafta", program başlangıç tarihinden sayılan 7 günlük dilimdir.
- Ağırlık seansının günü yapılmadan geçerse Bugün ekranında kırmızı uyarı çıkar: "İki seansı üst üste kaçırma."
  Uçuş günleri ve seansı yarına kaydırdığın günler kaçırılmış sayılmaz.
- Sıra kayarsa: **Ayarlar › Program › Sıradaki iş**'ten elle düzeltebilirsin; Bugün'deki "Bu adımı atla" da bir adım ilerletir.

### Antrenman kuralları

- İlk 2 hafta her hareket 2 set ("rahat ağırlık"); 3. haftadan itibaren tam set.
- Bir hareketin önceki seanstaki **tüm** setlerinde üst tekrar sınırına ulaştıysan: "Ağırlığı artır, alt sınırdan başla."
- Son ağırlık seansının üzerinden 7 günden fazla geçtiyse önerilen ağırlıklar son değerlerin %80'idir.
- Her 8 haftada bir hafif hafta önerilir (set sayısı yarıya iner); uygulayıp uygulamamak sana kalır.
- 15 dakika modu yalnızca ilk 2 hareketi gösterir; bitirince seans sayılır.

### Öneriler yalnızca öneridir

Uygulama kalori hedefini kendiliğinden **değiştirmez**. İlerleme sekmesindeki ayar önerilerini okur, hedefi Ayarlar'dan sen
değiştirirsin. Kalori hedefi 1.800 kcal altına ayarlanamaz. Uygulama tıbbi tavsiye vermez.

### Bildirimler

Üç hatırlatma var (sabah tartı, akşam germe, öğün arası); hepsi yereldir ve Ayarlar'dan ayrı ayrı açılır. Telefonun pil
tasarrufu nedeniyle birkaç dakika gecikebilirler. Öğün arası hatırlatması 23:00–07:00 arasında susar.

---

## 6. Kodda ne nerede

```
zincir/
├── app/build.gradle.kts          kullanılan kütüphaneler, sürüm numarası
├── gradle/libs.versions.toml     kütüphane sürümleri
└── app/src/
    ├── main/AndroidManifest.xml  izinler (internet yok), uygulamanın parçaları
    ├── main/kotlin/com/zincir/
    │   ├── MainActivity.kt       tek pencere, alt sekmeler
    │   ├── AnaViewModel.kt       ekranların canlı verisi, zamanlayıcılar
    │   ├── mantik/               saf kurallar (Android'e bağlı değil, testli)
    │   │   ├── Sira.kt           döngü, zincir uyarısı, program haftaları, set sayısı
    │   │   ├── Ilerleme.kt       ilerleme kuralı, %80 dönüş kuralı
    │   │   ├── Kilo.kt           haftalık ortalama, ayar önerileri
    │   │   └── Bicim.kt          sayı ve tarih biçimleri (virgül)
    │   ├── veri/                 veritabanı (Room)
    │   │   ├── Varliklar.kt      tablolar
    │   │   ├── Dao.kt            sorgular
    │   │   ├── BaslangicVerisi.kt  ilk açılışta yazılan program (hareketler, öğünler, germeler…)
    │   │   ├── Depo.kt           yazma işlemleri
    │   │   └── Yedek.kt          JSON dışa/içe aktarma
    │   ├── ui/                   ekranlar ve ortak parçalar (Tema.kt, Bilesenler.kt, Grafik.kt…)
    │   └── bildirim/             yerel hatırlatmalar ve titreşim
    └── test/kotlin/com/zincir/
        ├── mantik/               sıra, ilerleme, %80 kuralı, haftalık ortalama testleri
        └── EkranTesti.kt         uygulamayı telefonsuz açıp temel akışları deneyen test
```

Programı (hareket, öğün, germe, hedef) değiştirmek için koda dokunman gerekmez: hepsi veritabanındadır ve Ayarlar'dan
düzenlenir. `BaslangicVerisi.kt` yalnızca uygulama ilk kez açıldığında okunur.

Tabloya yeni sütun eklersen `veri/Veritabani.kt` içindeki `version` sayısını artırman ve bir "migration" yazman gerekir;
yoksa uygulama eski veriyle açılmaz. Emin değilsen önce yedek al.

---

## 7. Bilinen sınırlar

- Adım sayısı elle girilir. Health Connect bağlantısı bilerek ilk sürümde yok; her şey oturduktan sonra ayrı bir adım olarak eklenebilir.
- "Bugün", telefonun saat dilimine göre belirlenir. Uçuşla saat dilimi değişince gün bir kez erken ya da geç dönebilir; sıra bundan bozulmaz.
- Ekran testi, içinde yazı kutusu olan pencereleri (adım girişi, hızlı ekleme gramı, ayar pencereleri) açmaz: test aracı
  (Robolectric) bu pencerelerde takılıyor. Bu pencereleri telefonda elle denemek gerekir.
