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
)

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
)

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

/** Ertelenmiş sonuç: `dueDay` geldiğinde koşul (ham koşul metni) hâlâ doğruysa olay tetiklenir. */
data class Seed(val eventId: String, val dueDay: Int, val condition: String? = null)

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
