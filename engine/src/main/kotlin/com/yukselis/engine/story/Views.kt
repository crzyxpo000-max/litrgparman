package com.yukselis.engine.story

import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.PromptKind
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.system.Grade
import com.yukselis.engine.system.Rarity
import com.yukselis.engine.system.SkillType
import com.yukselis.engine.system.Stat

// ---- Arayüze giden, dili çözülmüş görünüm modelleri ----

data class NoticeView(
    val kind: NoticeKind,
    val title: String,
    val lines: List<String>,
    val rarity: Rarity?,
)

data class ChoiceView(
    val id: String,
    val label: String,
    /** "[ALG 6]" gibi koşul etiketi; koşulsuz seçeneklerde null. */
    val requirementTag: String?,
    val enabled: Boolean,
)

data class PassageView(
    val sceneId: String,
    val placeName: String?,
    val paragraphs: List<String>,
    val notices: List<NoticeView>,
    /** Pasif Sistem sezgisi satırları; yalnızca oyuncu görür, ayrı stilde gösterilir. */
    val insights: List<InsightView> = emptyList(),
    /** Geçmiş pasajlarda oyuncunun seçtiği seçenek. */
    val chosenLabel: String? = null,
)

data class SceneView(
    val day: Int,
    val act: Int,
    val placeName: String?,
    val history: List<PassageView>,
    val current: PassageView,
    val choices: List<ChoiceView>,
    val isEnding: Boolean,
    /** Bekleyen Sistem istemi; varken hikaye seçimi yapılamaz, arayüz tam ekran kart gösterir. */
    val prompt: PromptView? = null,
)

/** `echo`: stat sezgisi değil, Yankı'nın sesi (öneksiz satır). */
data class InsightView(val text: String, val echo: Boolean)

data class PromptOptionView(
    val id: String,
    val name: String,
    val description: String,
    val details: List<String> = emptyList(),
    val rarity: Rarity? = null,
    /** "Yankı'nın önerisi" gibi Yankı satırı; ayrı renkte gösterilir. */
    val echoNote: String? = null,
)

data class PromptView(
    val kind: PromptKind,
    /** İstemin konusu; çözülürken geri gönderilir (çift dokunuş koruması). */
    val subject: String?,
    val title: String,
    val lines: List<String>,
    val options: List<PromptOptionView>,
)

data class StatLine(val stat: Stat, val abbr: String, val label: String, val value: Int)

data class TitleLine(val name: String, val description: String, val rarity: Rarity)

data class StatusView(
    val playerName: String,
    val level: Int,
    val grade: Grade,
    val xp: Long,
    val xpToNext: Long,
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val stamina: Int,
    val freePoints: Int,
    val stats: List<StatLine>,
    val titles: List<TitleLine>,
    val className: String?,
    /** Evrim Görevi bekliyor: seviye kademe eşiğinde durdu. */
    val evolutionPending: Boolean,
    val essences: List<String>,
    val essenceSlots: Int,
    /** İnsanlık yüzdeliği: oyuncudan güçlü insanların yüzdesi. */
    val humanityTopPercent: Double,
    /** Sistem'in verdiği sekmeler (kendini açan arayüz). */
    val tabs: Set<String>,
    val mercy: Int,
    val order: Int,
    val humanity: Int,
    val day: Int,
    val act: Int,
    val mode: GameMode,
)

data class SkillView(
    val id: String,
    val name: String,
    val description: String,
    val type: SkillType,
    val rarity: Rarity,
    val rarityName: String,
    val level: Int,
    val xp: Int,
    /** Maksimum seviyede 0. */
    val xpToNext: Int,
    val costMp: Int,
    val costStamina: Int,
    val cooldown: Int,
    /** Seviye 10'da açılacak dalların adları. */
    val evolutions: List<String>,
    /** Ceza yeteneğinin bedeli ("KAR −1"); değilse null. */
    val penalty: String?,
)

data class SkillsView(
    val className: String?,
    val classDescription: String?,
    val skills: List<SkillView>,
)

data class ItemView(
    val id: String,
    val name: String,
    val description: String,
    val count: Int,
    val rarity: Rarity,
    val usable: Boolean,
)

data class IngredientView(val name: String, val need: Int, val have: Int)

data class RecipeView(
    val id: String,
    val name: String,
    val description: String,
    val count: Int,
    val minLevel: Int,
    val ingredients: List<IngredientView>,
    val craftable: Boolean,
)

data class BagView(
    val credits: Int,
    val crystals: Int,
    val items: List<ItemView>,
    val professionName: String?,
    val professionLevel: Int,
    val professionXp: Int,
    val professionXpToNext: Int,
    val recipes: List<RecipeView>,
)

data class DailyView(
    val title: String,
    val description: String,
    val progress: Int,
    val steps: Int,
    val completed: Boolean,
    /** Antrenman türündeki görev Daha fazla ekranından set set yapılabilir. */
    val trainable: Boolean,
    val streak: Int,
)

data class Outcome(val state: WorldState, val notices: List<NoticeView>)
