package com.yukselis.engine.world

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.Condition
import com.yukselis.engine.story.Effect
import com.yukselis.engine.system.Grade

// ---- İçerik tanımları ----

/**
 * Olay havuzunun bir olayı (GDD: Olay yapısı). Olayın kendi sahneleri vardır; giriş sahnesi
 * `entry`. Sahnelerin son seçenekleri `"@akis"` ile gün akışına döner.
 */
data class EventDef(
    val id: String,
    val acts: List<Int> = emptyList(),
    val regions: List<String> = emptyList(),
    val dayMin: Int = 0,
    val dayMax: Int = Int.MAX_VALUE,
    val condition: Condition? = null,
    val weight: Double = 1.0,
    val tags: List<String> = emptyList(),
    /** 1 sakin · 2 orta · 3 yüksek · 4 zirve */
    val tension: Int = 2,
    val repeatable: Boolean = false,
    val npcs: List<String> = emptyList(),
    /** Ana yay düğümü: bu günden itibaren ilk fırsatta kesin gelir. */
    val mainArcDay: Int? = null,
    /** Fırsat elindeki kart başlığı. */
    val card: LText,
    /** Önemli olay: seçenekleri en az bir ahlak eksenini oynatmalı (doğrulayıcı uyarısı). */
    val important: Boolean = false,
    /** Yalnızca tohumla ya da eşikle gelir; havuzdan rastgele seçilmez. */
    val seedOnly: Boolean = false,
    val entry: String,
    /**
     * Konum (DECISIONS #130): `us` (üste gelir), bölge kimlikleri (yalnız o bölgeye giden görevde) ya da `*` (herhangi bir
     * keşif görevinde). Boşsa `bolge`'den türetilir, o da yoksa `us`.
     */
    val location: List<String> = emptyList(),
) {
    val where: List<String> get() = location.ifEmpty { regions.ifEmpty { listOf(ChapterTable.BASE) } }
}

data class RegionDef(
    val id: String,
    val act: Int,
    val baseDanger: Int,
    /** Başlangıçta bölgeyi tutan güç: bir fraksiyon kimliği ya da "canavar". */
    val control: String,
    val enemies: List<String>,
    val neighbors: List<String>,
    /** Her sabah kapı açılma olasılığı (tehlikeyle artar). */
    val gateChance: Double,
    val gateEnemies: List<String>,
    /** Keşifte karşılaşma yerine bulunabilecek malzemeler. */
    val forage: List<String> = emptyList(),
    /** Bölgenin canlı olduğu son perde (Öğretici Katman Perde 1'de kapanır). Kapı doğuşu/taşması yalnızca bu aralıkta. */
    val lastAct: Int = 4,
    /** Yolculuk koşulu (çok-evren dünyaları, Sistem çekirdeği). */
    val condition: com.yukselis.engine.story.Condition? = null,
    /** Keşif savaşının ortam etiketi. */
    val environment: String? = null,
) {
    fun liveIn(act: Int): Boolean = act in this.act..lastAct
}

data class FactionDef(val id: String, val relations: Map<String, Int>)

/** Tanıklı başarı: söylentiye dönüşür, fraksiyonlar farklı okur (OP MC: Tanıklar ve Efsane). */
data class FeatDef(
    val id: String,
    val name: LText,
    val reactions: Map<String, Int>,
    val lines: Map<String, LText> = emptyMap(),
    val titleAt: Int? = null,
    val title: String? = null,
)

data class QuestDef(val id: String, val name: LText, val description: LText)

// ---- Durum ----

data class Gate(val uid: Int, val grade: Grade, val enemies: List<String>, val openedDay: Int, val deadline: Int)

data class RegionState(
    val id: String,
    val danger: Int,
    val control: String,
    val gates: List<Gate> = emptyList(),
)

/**
 * Ertelenmiş sonuç: `dueDay` geldiğinde koşul (ham koşul metni) hâlâ doğruysa olay tetiklenir.
 * `origin`: tohumu eken olay; `reminder`: olayın başında bağlantıyı hatırlatan cümle (tohum etkisinin `hatirlatma`sı).
 */
data class Seed(
    val eventId: String,
    val dueDay: Int,
    val condition: String? = null,
    val npc: String? = null,
    val origin: String? = null,
    val reminder: LText? = null,
)

enum class QuestStatus(val key: String) {
    ACTIVE("basla"), DONE("tamam"), FAILED("basarisiz");

    companion object {
        fun fromKey(key: String?): QuestStatus? = entries.firstOrNull { it.key == key }
    }
}

/** Gün akışının durumu (sabah Günlük Görev, gün içi yuvalar, akşam kamp). */
data class DayState(
    val slotsLeft: Int = 0,
    val slotsTotal: Int = 0,
    /** Fırsat eli: bu yuvada gösterilen olay kartları. */
    val hand: List<String> = emptyList(),
    val currentEvent: String? = null,
    val seen: Set<String> = emptySet(),
    val recentTension: List<Int> = emptyList(),
    val recentTags: List<String> = emptyList(),
    val nextGateUid: Int = 1,
    /**
     * Zorunlu olay kilidi: bir olay, savaş ya da görev dönüşünden sonra oyuncu bir eylem yapmadan yeni zorunlu olay
     * gelmez (arka arkaya en çok bir zorunlu olay; STORY_FLOW c/e).
     */
    val forcedLock: Boolean = false,
    /** Süren olayın giriş sahnesinin başında gösterilen hatırlatma (tohumdan doğan olay). */
    val reminder: LText? = null,
)

/** Seçeneğin türü: arayüzde önek ("[Gizle]", "[Açığa vur]", "[GÜÇ 40 · Önemsiz]"). */
enum class ChoiceKind(val key: String) {
    NORMAL(""), CONCEAL("gizle"), REVEAL("aciga_vur"), FLEX("gosteris");

    companion object {
        fun fromKey(key: String?): ChoiceKind = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

/** Görünür olasılıklı zar kontrolü: d20 + stat ≥ zorluk. `oneShot` (kırmızı) başarısızlıkta kilitlenir. */
data class DiceCheck(
    val stat: com.yukselis.engine.system.Stat,
    val difficulty: Int,
    val oneShot: Boolean = false,
    val failNext: String? = null,
    val failEffects: List<Effect> = emptyList(),
)

// ---- Aşama 7: Kule ----

/** Kule katı bandı: bu aralıktaki prosedürel katların düşmanları, ortamı ve açılış satırları. */
data class TowerBand(
    val min: Int,
    val max: Int,
    val enemies: List<String>,
    val environment: List<String>,
    val lines: List<LText>,
)

/**
 * Kule (GDD: 100 katlı dikey zindan; her 10 katta bir hikaye sırrı). Kat seviyesi = taban · çarpan^kat.
 * Her 10. kat yazılmış bir hikaye olayıdır (`hikaye_katlari`); diğerleri bantlardan prosedürel üretilir.
 */
data class TowerDef(
    val region: String,
    val condition: com.yukselis.engine.story.Condition?,
    val baseLevel: Double,
    val growth: Double,
    val floors: Int,
    val bands: List<TowerBand>,
    val storyFloors: Map<Int, String>,
) {
    fun levelOf(floor: Int): Int = kotlin.math.round(baseLevel * Math.pow(growth, floor.toDouble())).toInt().coerceAtLeast(1)

    fun band(floor: Int): TowerBand? = bands.firstOrNull { floor in it.min..it.max }
}
