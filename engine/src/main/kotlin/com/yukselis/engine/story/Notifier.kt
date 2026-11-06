package com.yukselis.engine.story

import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.LText
import com.yukselis.engine.npc.NpcEngine
import com.yukselis.engine.world.Seed

/** Oturumlar arası bildirim (GDD: Bildirimler). Her biri dokununca küçük bir olay açar (`eventId`). */
data class PushNote(val id: String, val title: LText, val text: LText, val eventId: String)

/**
 * Bildirim seçici (Aşama 9): kayıtlı oyunun durumuna bakarak en fazla bir bildirim önerir. Saf; arayüz ne zaman
 * göstereceğine karar verir (nadir: en az iki günde bir). Öncelik: rakip seni geçti → üst kademeden bir göz →
 * taşmak üzere olan kapı. Bildirimin olayı içerikte yoksa öneri yapılmaz.
 */
class Notifier(private val content: Content, private val npc: NpcEngine) {

    /**
     * `rivalWasAhead`: önceki kontrolde rakip zaten öndeydi (uygulama saklar). "Seni geçti" yalnızca alttan üste
     * geçişte önerilir.
     */
    fun pick(state: WorldState, rivalWasAhead: Boolean = false): PushNote? {
        if (state.ended || state.dead || state.combat != null) return null
        candidates(state, rivalWasAhead).forEach { return it }
        return null
    }

    /** Rakip şu an dünyanın gördüğü sıralamada oyuncunun önünde mi? */
    fun rivalAhead(state: WorldState): Boolean {
        val r = state.npcs[RIVAL] ?: return false
        if (!r.alive || !r.known || r.companion) return false
        val list = npc.ranking(state, public = true)
        val rivalIdx = list.indexOfFirst { it.kind == NpcEngine.RankKind.NPC && it.id == RIVAL }
        val playerIdx = list.indexOfFirst { it.kind == NpcEngine.RankKind.PLAYER }
        return rivalIdx in 0 until playerIdx
    }

    private fun candidates(state: WorldState, rivalWasAhead: Boolean) = sequence {
        val t = content.texts
        // 1. Rakip seni Küresel Sıralama'da geçti (dünyanın gördüğü listede).
        if (!rivalWasAhead && has(RIVAL_EVENT) && rivalAhead(state)) {
            yield(PushNote("rakip", t.format("bildirim_baslik"), t.formatL("bildirim_rakip", "npc" to npc.fullName(RIVAL)), RIVAL_EVENT))
        }
        // 2. Üst kademeden bir varlık seni fark etti (Dikkat yüksek).
        if (state.attention >= GAZE_ATTENTION && has(GAZE_EVENT)) {
            val grade = LText(GAZE_GRADE, GAZE_GRADE)
            yield(PushNote("goz", t.format("bildirim_baslik"), t.formatL("bildirim_goz", "grade" to grade), GAZE_EVENT))
        }
        // 3. Bulunduğun bölgede yarın taşacak bir kapı.
        // Kapı olayı şehir sokaklarını anlatır: yalnızca Kıyıkent bölgelerinde.
        val gate = state.region?.takeIf { it in CITY }?.let { state.regions[it] }?.gates?.firstOrNull { it.deadline <= state.day + 1 }
        if (gate != null && has(GATE_EVENT)) {
            yield(PushNote("kapi", t.format("bildirim_baslik"), t.formatL("bildirim_kapi", "grade" to LText(gate.grade.name, gate.grade.name)), GATE_EVENT))
        }
    }

    private fun has(eventId: String) = eventId in content.events

    /**
     * Bildirime dokunuldu: olay bugüne tohumlanır, bir sonraki gün akışı girişinde zorunlu olarak açılır.
     * Aynı bildirimin olayı bekliyorsa ikinci kez eklenmez.
     */
    fun open(state: WorldState, noteId: String): WorldState {
        // Bildirim eskidiyse (kapı kapandı, Dikkat düştü) olay açılmaz. Rakip geçişi bu kontrolde "önde mi"ye bakar.
        val stillValid = when (noteId) {
            "rakip" -> rivalAhead(state)
            else -> !state.ended && !state.dead && candidates(state, rivalWasAhead = true).any { it.id == noteId }
        }
        if (!stillValid || state.dead) return state
        val eventId = when (noteId) {
            "rakip" -> RIVAL_EVENT
            "goz" -> GAZE_EVENT
            "kapi" -> GATE_EVENT
            else -> return state
        }
        if (!has(eventId) || state.seeds.any { it.eventId == eventId } || state.ended) return state
        return state.copy(seeds = state.seeds + Seed(eventId, state.day))
    }

    companion object {
        const val RIVAL = "aras_kurt"
        const val RIVAL_EVENT = "bildirim_rakip"
        const val GAZE_EVENT = "bildirim_goz"
        const val GATE_EVENT = "bildirim_kapi"
        const val GAZE_ATTENTION = 90
        const val GAZE_GRADE = "A"
        val CITY = setOf("kiyikent_liman", "kordon", "eski_sehir", "sanayi", "tepe", "kule_dibi")
    }
}
