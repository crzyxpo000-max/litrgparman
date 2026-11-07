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
    /** OP MC: [Gizle] / [Açığa vur] öneki. */
    val kindTag: String? = null,
    val kind: com.yukselis.engine.world.ChoiceKind = com.yukselis.engine.world.ChoiceKind.NORMAL,
    /** Zar kontrolü: başarı olasılığı (yüzde); kontrol yoksa null. */
    val odds: Int? = null,
    /** Tek seferlik (kırmızı) kontrol; başarısızsa kilitlenir. */
    val oneShot: Boolean = false,
)

/** Fırsat eli kartı. `region`: başka bir bölgede geçen olay (oynamak o bölgeye bir yan görev başlatır). */
data class CardView(val id: String, val title: String, val tension: Int, val region: String? = null)

data class GateView(val uid: Int, val grade: String, val enemies: String, val daysLeft: Int)

data class RegionLinkView(val id: String, val name: String)

/** Gün akışı (olay dışı): yuvalar, fırsat eli, serbest eylemler, kapılar, yolculuk, kamp. */
data class HubView(
    val day: Int,
    val slotsLeft: Int,
    val slotsTotal: Int,
    val regionName: String,
    val danger: Int,
    val cards: List<CardView>,
    val gates: List<GateView>,
    val travel: List<RegionLinkView>,
    val mustCamp: Boolean,
    /** Kule burada tırmanılabiliyorsa bir sonraki kat (Aşama 7). */
    val tower: TowerView? = null,
    // ---- Bölüm yapısı (DECISIONS #130) ----
    /** Üsteyiz (görev yok). Bölüm yapısı olmayan içerikte hep true. */
    val atBase: Boolean = true,
    /** Görevdeyken görev türü ve yeri ("Keşif · Kordon"). */
    val mission: String? = null,
    /** Üste: girilebilecek ana operasyon(lar). */
    val operations: List<OperationChoiceView> = emptyList(),
    /** Görevdeyken üsse dönülebilir. */
    val canReturn: Boolean = false,
    /** Gün akışında keşif / Kapı yapılabilir (görevdeyken). */
    val canExplore: Boolean = true,
    /** Üste: bölümün hazırlık eylemleri. */
    val preparations: List<PreparationView> = emptyList(),
    /** Üsten ayrılmak operasyonu geciktirebilir: dile çözülmüş uyarı (yoksa null). */
    val deadlineWarning: String? = null,
)

data class PreparationView(val id: String, val name: String, val description: String, val cost: Int, val enabled: Boolean)

data class OperationChoiceView(val index: Int, val title: String, val region: String, val daysLeft: Int)

/** Uzun aradan sonra dönüş özeti (dünya durumu ve önemli seçimler). */
data class RecapView(val title: String, val lines: List<String>)

data class TowerView(val floor: Int, val level: Int, val story: Boolean)

// ---- Aşama 7: son ekranı ----

data class EpilogueCard(val title: String, val text: String)

data class ChronicleView(val day: Int, val event: String, val choice: String, val effect: String?)

/** Son ekranı: sonun adı, OP MC türü, NPC akıbetleri, fraksiyonlar ve en kritik 5 seçim (epilog kartları). */
data class EndingView(
    val id: String,
    val name: String,
    val summary: String,
    val archetype: String?,
    val archetypeText: String?,
    val npcs: List<EpilogueCard>,
    val factions: List<EpilogueCard>,
    val choices: List<ChronicleView>,
    val level: Int,
    val grade: String,
    val day: Int,
    /** Gizli Döngü sonu: New Game+ açılır. */
    val unlocksNewGamePlus: Boolean,
)

data class RegionView(
    val id: String,
    val name: String,
    val danger: Int,
    val control: String,
    val gates: List<GateView>,
    val current: Boolean,
    val neighbor: Boolean,
)

data class QuestView(val name: String, val description: String, val status: com.yukselis.engine.world.QuestStatus)

data class LegendView(val name: String, val count: Int)

data class KnowledgeView(val name: String, val description: String, val kind: String)

data class JournalView(
    val quests: List<QuestView>,
    val legend: List<LegendView>,
    val pendingSeeds: Int,
    /** Bilgi günlüğü: söylentiler, sırlar, gerçek seviyeler (en yeni önce). */
    val knowledge: List<KnowledgeView> = emptyList(),
    /** Bölüm hedefi (Görevler ekranında yan görevlerden ayrı). */
    val chapter: ChapterGoalView? = null,
)

data class ChapterGoalView(val no: Int, val title: String, val goal: String, val threat: String, val daysLeft: Int, val phase: String)

/** Yoldaşın büyük seçimden önceki tek satırlık görüşü. `echo`: Yankı'nın sesi. */
data class CompanionLineView(val npcName: String, val text: String, val favoredChoice: String?, val echo: Boolean)

data class NpcView(
    val id: String,
    val name: String,
    val fullName: String,
    val description: String,
    val trust: Int,
    val respect: Int,
    val fear: Int,
    val bond: Int,
    val level: Int,
    val gradeName: String,
    /** Durum satırı: Yoldaş · Demir Lonca · Öldü … */
    val status: String,
    val alive: Boolean,
    val companion: Boolean,
    val memories: List<String>,
    val traits: List<String>,
    val goal: String?,
)

data class RelationsView(val npcs: List<NpcView>, val companions: Int, val capacity: Int, val armyPower: Int, val army: List<String>)

data class RankRowView(val rank: Int, val name: String, val level: Int, val gradeName: String, val you: Boolean, val npc: Boolean)

data class RankingView(
    /** Dünyanın gördüğü sıralama (görünen seviyenle). */
    val rows: List<RankRowView>,
    val publicRank: Int,
    val trueRank: Int,
    val total: Int,
    val note: String,
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
    /** Gün akışındayken (olay dışı) dolu. */
    val hub: HubView? = null,
    /** Yanındakilerin görüşleri (Aşama 5). */
    val opinions: List<CompanionLineView> = emptyList(),
    /** Her zaman görünen "Bölüm N · Hedef · Kalan gün" satırı (gün akışından önce null). */
    val chapter: com.yukselis.engine.world.ChapterLine? = null,
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
    /** OP MC: Anomali Dikkati ve Yozlaşma (0–100), görünen seviye. */
    val attention: Int = 0,
    val corruption: Int = 0,
    val publicLevel: Int = 1,
    val mercy: Int,
    val order: Int,
    val humanity: Int,
    val day: Int,
    val act: Int,
    val mode: GameMode,
    /** OP MC izler (ad: açıklama). */
    val traits: List<TitleLine> = emptyList(),
    val armyPower: Int = 0,
    /** Küresel Sıralama: dünyanın gördüğü sıra. */
    val publicRank: Int = 0,
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

// ---- Savaş ----

data class EnemyView(
    val uid: Int,
    val name: String,
    val description: String,
    val level: Int,
    val gradeName: String,
    val hp: Int,
    val maxHp: Int,
    /** OP MC: gerçek statlar her zaman görünür. */
    val stats: List<StatLine>,
    val statuses: List<String>,
    /** Analiz edildiyse zaaf metni. */
    val weakness: String?,
    val boss: Boolean,
    val phase: Int,
    val intelligent: Boolean,
    val alive: Boolean,
)

data class CombatLogView(val prose: String, val numbers: String?)

data class ExecutionView(val enemyName: String, val options: List<PromptOptionView>)

data class CombatSkillView(
    val id: String,
    val name: String,
    val costMp: Int,
    val costStamina: Int,
    val cooldownLeft: Int,
    val usable: Boolean,
    val targetsAll: Boolean,
    val self: Boolean,
)

data class EnvironmentView(val tag: String, val name: String, val used: Boolean)

data class CombatView(
    val round: Int,
    val enemies: List<EnemyView>,
    val stamina: Int,
    val maxStamina: Int,
    val stance: com.yukselis.engine.combat.Stance,
    val effort: Int,
    val playerStatuses: List<String>,
    val log: List<CombatLogView>,
    val environment: List<EnvironmentView>,
    val skills: List<CombatSkillView>,
    val items: List<ItemView>,
    /** Son düşman düştü: infaz seçenekleri. */
    val execution: ExecutionView?,
    val minorUsed: Boolean,
    val extraActions: Int,
    val dead: Boolean,
    val allies: List<AllyView> = emptyList(),
)

data class AllyView(val name: String, val level: Int, val hp: Int, val maxHp: Int, val down: Boolean, val role: String)
