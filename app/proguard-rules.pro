# Yükseliş Protokolü — R8 kuralları (Aşama 9).
# Motor (engine) yansıma kullanmaz; içerik YAML'ı kaml'ın düğüm API'siyle, kayıtlar JsonElement API'siyle elle okunur
# (kotlinx.serialization derleyici eklentisi yok). Yine de üçüncü taraf kütüphanelerin iç sınıfları korunur.

# kaml ve YAML ayrıştırıcısı (snakeyaml-engine): ayrıştırıcı durum sınıflarını adla yükleyebilir.
-keep class com.charleskorn.kaml.** { *; }
-keep class it.krzeminski.snakeyaml.engine.** { *; }
-keep class org.snakeyaml.engine.** { *; }
-dontwarn org.snakeyaml.engine.**
-dontwarn it.krzeminski.snakeyaml.engine.**

# kotlinx.serialization JSON (JsonElement API'si)
-keep class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# Hata ayıklama: yığın izlerinde satır numaraları kalsın.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
