package com.zincir.saglik

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.zincir.mantik.EgzersizOturumu
import com.zincir.mantik.SaatVerisi
import com.zincir.mantik.Saglik
import com.zincir.mantik.UykuOturumu
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZoneId

/**
 * Health Connect: Android'in telefondaki ortak sağlık veri deposu. Saat uygulaması (ya da köprü uygulama) veriyi
 * oraya yazar, Zincir buradan yalnızca OKUR. Her şey telefonun içinde olur; internet gerekmez.
 * Varsayılan olarak kapalıdır; Ayarlar'dan açılır.
 */
object SaglikBaglantisi {
    enum class Durum { HAZIR, GUNCELLEME_GEREK, YOK }

    private val ADIM = HealthPermission.getReadPermission(StepsRecord::class)
    private val UYKU = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val KALORI = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val EGZERSIZ = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

    /** İstenen izinler: yalnızca okuma. */
    val IZINLER: Set<String> = setOf(ADIM, UYKU, KALORI, EGZERSIZ)

    /** Kardiyo sayılan egzersiz türleri ve Türkçe adları. Ağırlık, yoga gibi türler kardiyo önerisi olmaz. */
    private fun kardiyoAdi(tur: Int): String? = when (tur) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING, ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Yürüyüş"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Koşu"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING, ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Bisiklet"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Eliptik"
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING, ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "Merdiven"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING, ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Kürek"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Yüzme"
        ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT -> "Diğer kardiyo"
        else -> null
    }

    private fun tercih(context: Context) = context.getSharedPreferences("zincir_saglik", Context.MODE_PRIVATE)

    /** Kullanıcı Ayarlar'da eşitlemeyi açtı mı? */
    fun acik(context: Context): Boolean = tercih(context).getBoolean("acik", false)

    fun acikYap(context: Context, acik: Boolean) = tercih(context).edit().putBoolean("acik", acik).apply()

    fun durum(context: Context): Durum = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> Durum.HAZIR
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Durum.GUNCELLEME_GEREK
            else -> Durum.YOK
        }
    } catch (e: Exception) {
        Durum.YOK
    }

    /** İzin penceresini açan sözleşme; sonuç olarak verilen izinlerin kümesini döner. */
    fun izinSozlesmesi(): ActivityResultContract<Set<String>, Set<String>> = PermissionController.createRequestPermissionResultContract()

    /** Hata olursa null döner (iptal hariç): eşitleme hiçbir zaman uygulamayı çökertmemeli. */
    private suspend fun <T> dene(blok: suspend () -> T): T? = try {
        blok()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun verilenIzinler(context: Context): Set<String> {
        if (durum(context) != Durum.HAZIR) return emptySet()
        return dene { HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions() } ?: emptySet()
    }

    /** Bir günün adım, uyku, aktif kalori ve kardiyo egzersizi verisini okur. Health Connect yoksa ya da hiç izin yoksa null. */
    suspend fun gunuOku(context: Context, gun: Long): SaatVerisi? {
        val izinler = verilenIzinler(context)
        if (izinler.isEmpty()) return null
        val istemci = HealthConnectClient.getOrCreate(context)
        val tarih = LocalDate.ofEpochDay(gun)
        // Yerel saatle gün aralığı: kayıtlar hangi saat diliminde tutulduysa o yerin gününe göre süzülür.
        val gunAraligi = TimeRangeFilter.between(tarih.atStartOfDay(), tarih.plusDays(1).atStartOfDay())

        val adim = if (ADIM in izinler) dene {
            istemci.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), gunAraligi))[StepsRecord.COUNT_TOTAL]?.toInt()
        } else null

        val aktifKcal = if (KALORI in izinler) dene {
            istemci.aggregate(AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), gunAraligi))[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories?.toInt()
        } else null

        val uyku = if (UYKU in izinler) dene {
            val bolge = ZoneId.systemDefault()
            // Geniş bir aralık okunur; hangi oturumun bu güne ait olduğuna Saglik.uykuSaati karar verir.
            val aralik = TimeRangeFilter.between(tarih.minusDays(1).atStartOfDay(bolge).toInstant(), tarih.plusDays(2).atStartOfDay(bolge).toInstant())
            val oturumlar = istemci.readRecords(ReadRecordsRequest(SleepSessionRecord::class, aralik)).records
            Saglik.uykuSaati(oturumlar.map { UykuOturumu(it.startTime, it.endTime, it.endZoneOffset) }, gun, bolge)
        } else null

        val egzersizler = if (EGZERSIZ in izinler) dene {
            val oturumlar = istemci.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, gunAraligi)).records
            Saglik.egzersizler(oturumlar.mapNotNull { o -> kardiyoAdi(o.exerciseType)?.let { EgzersizOturumu(it, o.startTime, o.endTime) } })
        } else null

        return SaatVerisi(adim, uyku, aktifKcal, egzersizler ?: emptyList())
    }
}
