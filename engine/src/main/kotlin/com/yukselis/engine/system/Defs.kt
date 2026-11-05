package com.yukselis.engine.system

import com.yukselis.engine.i18n.LText
import com.yukselis.engine.story.Condition

// İçerik tanımları (content/*.json). Hepsi değişmez; oyuncu durumu yalnızca kimlikleri tutar.

enum class ClassKind(val key: String) {
    BASE("temel"), HIDDEN("gizli"), RARE("nadir"), EVOLVED("evrim");

    companion object {
        fun fromKey(key: String?): ClassKind? = entries.firstOrNull { it.key == key }
    }
}

data class ClassDef(
    val id: String,
    val name: LText,
    val description: LText,
    val kind: ClassKind,
    /** Temel sınıfın eğilimi. */
    val affinity: Affinity? = null,
    /** Gizli/nadir sınıfın açılma koşulu. */
    val unlock: Condition? = null,
    /** Evrim sınıfının geldiği sınıf ve kademesi. */
    val evolvesFrom: String? = null,
    val grade: Grade? = null,
    /** Seviye başı otomatik stat artışı. */
    val growth: Map<Stat, Int> = emptyMap(),
    /** Başlangıç yeteneği ya da evrimde kazanılan yetenek. */
    val skill: String? = null,
    /** Evrimde tek seferlik stat bonusu. */
    val bonus: Map<Stat, Int> = emptyMap(),
)

enum class SkillType(val key: String) {
    ACTIVE("aktif"), PASSIVE("pasif");

    companion object {
        fun fromKey(key: String?): SkillType = entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

enum class SkillTarget(val key: String) {
    SINGLE("tek"), ALL("tum"), SELF("kendi");

    companion object {
        fun fromKey(key: String?): SkillTarget = entries.firstOrNull { it.key == key } ?: SINGLE
    }
}

/** Yeteneğin uygulayabildiği durum etkisi (Aşama 3 savaşında işler). */
data class StatusApplication(val effect: String, val chance: Double, val duration: Int)

data class SkillDef(
    val id: String,
    val name: LText,
    val description: LText,
    val type: SkillType,
    val rarity: Rarity,
    val costMp: Int = 0,
    val costStamina: Int = 0,
    val cooldown: Int = 0,
    val stat: Stat? = null,
    val base: Int = 0,
    val multiplier: Double = 0.0,
    val target: SkillTarget = SkillTarget.SINGLE,
    val heals: Boolean = false,
    val status: StatusApplication? = null,
    /** Seviye 10'da evrilebileceği iki dal. */
    val evolutions: List<String> = emptyList(),
    /** Ceza yeteneği: her kademede (ve kazanıldığında) kalıcı stat kaybı. */
    val penalty: Map<Stat, Int> = emptyMap(),
    val tags: List<String> = emptyList(),
)

/** İki maksimum seviye yetenek + koşul → yeni yetenek. */
data class FusionRecipe(
    val id: String,
    val inputs: List<String>,
    val condition: Condition? = null,
    val result: String,
)

enum class ItemType(val key: String) {
    MATERIAL("malzeme"), CONSUMABLE("tuketim");

    companion object {
        fun fromKey(key: String?): ItemType = entries.firstOrNull { it.key == key } ?: MATERIAL
    }
}

/** Tüketim etkisi. `damage`/`inflicts` fırlatılan eşyalar içindir ve yalnızca savaşta işler (Aşama 3). */
data class ItemUse(
    val hp: Int = 0,
    val mp: Int = 0,
    val cures: List<String> = emptyList(),
    val damage: Int = 0,
    val inflicts: String? = null,
) {
    val combatOnly: Boolean get() = damage > 0 && hp == 0 && mp == 0 && cures.isEmpty()
}

data class ItemDef(
    val id: String,
    val name: LText,
    val description: LText,
    val type: ItemType,
    val rarity: Rarity,
    val value: Int = 0,
    val use: ItemUse? = null,
)

data class ProfessionDef(val id: String, val name: LText, val description: LText)

data class RecipeDef(
    val id: String,
    val profession: String,
    val inputs: Map<String, Int>,
    val output: String,
    val count: Int = 1,
    val minLevel: Int = 1,
    val xp: Int = 10,
)

data class EssenceDef(val id: String, val name: LText, val description: LText)

data class DailyTemplate(
    val id: String,
    val title: LText,
    val description: LText,
    /** antrenman, kesif, savas, kapi: `gunluk` etkisi aynı türdeki görevi ilerletir. */
    val kind: String,
    val steps: Int,
    val xp: Int,
)
