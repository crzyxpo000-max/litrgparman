package com.yukselis.engine.world

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.Effect

/**
 * Bölüm yapısı (docs/CHAPTERS.md, DECISIONS #130): oyun ~17 bölüm + finalden oluşur. Her bölüm:
 * Brifing (üs) → Hazırlık (serbest günler) → Ana operasyon (kilitli görev) → Sonrası (üs) → Darboğaz.
 *
 * Takvim sabittir: bölüm `open` gününde açılır, operasyon en geç `deadline` günü başlar (son tarih kaçarsa taşma),
 * bölüm `end` gününde biter; sonraki bölüm ertesi gün açılır.
 */
enum class ChapterPhase(val key: String) {
    /** Bölüm açıldı; brifing bekliyor. */
    BRIEF("brifing"),

    /** Serbest günler; operasyona girilebilir. */
    PREP("hazirlik"),

    /** Ana operasyon sürüyor. */
    OPERATION("operasyon"),

    /** Operasyon bitti; sonrası sahneleri ve darboğaz bekliyor. */
    AFTER("sonrasi"),

    /** Darboğaz geçildi; bölüm sonuna kadar serbest günler. */
    DONE("bitti");

    companion object {
        fun fromKey(key: String?): ChapterPhase? = entries.firstOrNull { it.key == key }
    }
}

enum class MissionKind(val key: String) {
    /** Bölümün ana operasyonu: kapalı sahne dizisi, araya hiçbir şey girmez. */
    OPERATION("operasyon"),

    /** Tek bir olaya (yan görev, fraksiyon görevi, tohum dönüşü) giden görev; olay bitince üsse dönülür. */
    SIDE("yan"),

    /** Bir bölgeyi keşif: keşif, Kapı, Kule; yalnız o bölgeye etiketli olaylar gelir. */
    EXPLORE("kesif");

    companion object {
        fun fromKey(key: String?): MissionKind? = entries.firstOrNull { it.key == key }
    }
}

/** Süren görev. `queue`: sırayla açılacak olaylar (operasyonda bölümün operasyon olayları, yan görevde tek olay). */
data class Mission(
    val kind: MissionKind,
    val region: String,
    val queue: List<String> = emptyList(),
    val startDay: Int = 0,
    /** Operasyon türü (bölümün birden çok operasyon seçeneği varsa hangisi). */
    val variant: Int = 0,
)

/** Bir bölümün ana operasyonu (Bölüm 11'de iki seçenek vardır). */
data class OperationDef(
    val title: LText,
    val region: String?,
    val events: List<String>,
)

data class ChapterDef(
    val no: Int,
    val act: Int,
    val open: Int,
    val deadline: Int,
    val end: Int,
    val title: LText,
    val goal: LText,
    val threat: LText,
    /** Bölüm açılırken üste gelen olaylar (sırayla). */
    val brief: List<String>,
    val operations: List<OperationDef>,
    /** Darboğaz: ilk uygun olay (ör. Deniz hayattaysa ve değilse iki sürüm). */
    val bottleneck: List<String>,
    /** Darboğaz en erken bu gün gelir (final: Hasat). */
    val bottleneckDay: Int? = null,
    /** Son tarih kaçınca: ek etkiler ve bildirim satırı. */
    val overflowEffects: List<Effect> = emptyList(),
    val overflowText: LText = LText.EMPTY,
    /**
     * `erken: false`: tarihli operasyon (dünya olayı: gemi iner, istila başlar). Oyuncu erken giremez; son tarih günü
     * kendiliğinden başlar. Diğerlerinde operasyona girme zamanını oyuncu seçer.
     */
    val earlyEntry: Boolean = true,
) {
    val operationEvents: List<String> get() = operations.flatMap { it.events }
    val managed: Set<String> get() = (brief + operationEvents + bottleneck).toSet()
}

data class ChapterTable(
    val chapters: List<ChapterDef> = emptyList(),
    /** Perde → üs bölgesi (Perde 1: Öğretici kampı, Perde 2–4: Sığınak). */
    val bases: Map<Int, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = chapters.isEmpty()

    fun get(no: Int): ChapterDef? = chapters.firstOrNull { it.no == no }

    fun next(no: Int): ChapterDef? = chapters.firstOrNull { it.no > no }

    /** Günün düştüğü bölüm (eski kayıt geçişi ve ana yay düğümlerinin bölümü). */
    fun forDay(day: Int): ChapterDef? = chapters.lastOrNull { it.open <= day } ?: chapters.firstOrNull()

    /** Bölümün yönettiği (brifing/operasyon/darboğaz) olaylar; Yönetmen bunları kendisi seçmez. */
    val managed: Set<String> by lazy { chapters.flatMap { it.managed }.toSet() }

    fun chapterOf(eventId: String): ChapterDef? = chapters.firstOrNull { eventId in it.managed }

    fun base(act: Int): String? = bases[act] ?: bases.entries.filter { it.key <= act }.maxByOrNull { it.key }?.value

    companion object {
        /** Konum: üste gelen olay. */
        const val BASE = "us"

        /** Konum: herhangi bir keşif görevinde gelebilen olay. */
        const val ANYWHERE = "*"

        /** Varış sahnesi olayı: `varis_<bölge>`. */
        const val ARRIVAL_PREFIX = "varis_"

        /** Bölgeye kaçıncı kez gidildiği (varış sahnesinin ilk/sonraki paragrafları). */
        const val VISIT_PREFIX = "ziyaret_"
        const val OVERFLOW_FLAG_PREFIX = "bolum_"
        const val OVERFLOW_FLAG_SUFFIX = "_tasti"
        const val OVERFLOW_SURVIVORS = -3
        const val OVERFLOW_DANGER = 15

        fun overflowFlag(no: Int) = "$OVERFLOW_FLAG_PREFIX${no}$OVERFLOW_FLAG_SUFFIX"
    }
}

/** Görünüm: her zaman görünen "Bölüm N · Hedef · Kalan gün" satırı. */
data class ChapterLine(
    val no: Int,
    val title: String,
    val goal: String,
    val daysLeft: Int,
    val phase: ChapterPhase,
    val onMission: Boolean,
    val missionLabel: String?,
    val isFinal: Boolean,
)
