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

    fun pick(state: WorldState): PushNote? {
        if (state.ended || state.dead || state.combat != null) return null
        candidates(state).forEach { return it }
        return null
    }

    private fun candidates(state: WorldState) = sequence {
        val t = content.texts
        // 1. Rakip seni Küresel Sıralama'da geçti (dünyanın gördüğü listede).
        val rival = RIVAL
        val r = state.npcs[rival]
        if (r != null && r.alive && r.known && !r.companion && has(RIVAL_EVENT)) {
            val list = npc.ranking(state, public = true)
            val rivalIdx = list.indexOfFirst { it.kind == NpcEngine.RankKind.NPC && it.id == rival }
            val playerIdx = list.indexOfFirst { it.kind == NpcEngine.RankKind.PLAYER }
            if (rivalIdx in 0 until playerIdx) {
                yield(PushNote("rakip", t.format("bildirim_baslik"), t.formatL("bildirim_rakip", "npc" to npc.fullName(rival)), RIVAL_EVENT))
            }
        }
        // 2. Üst kademeden bir varlık seni fark etti (Dikkat yüksek).
        if (state.attention >= GAZE_ATTENTION && has(GAZE_EVENT)) {
            val grade = LText(GAZE_GRADE, GAZE_GRADE)
            yield(PushNote("goz", t.format("bildirim_baslik"), t.formatL("bildirim_goz", "grade" to grade), GAZE_EVENT))
        }
        // 3. Bulunduğun bölgede yarın taşacak bir kapı.
        val gate = state.region?.let { state.regions[it] }?.gates?.firstOrNull { it.deadline <= state.day + 1 }
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
        const val GAZE_ATTENTION = 75
        const val GAZE_GRADE = "A"
    }
}
