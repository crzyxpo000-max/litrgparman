package com.yukselis.engine

import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.story.StoryEngine
import com.yukselis.engine.world.HubAction

/**
 * Bölüm yapısı (DECISIONS #130) için test yardımcıları: gün akışının başındaki brifingi geçer ve oyuncunun bir eylem
 * yapmış olduğunu varsayar (zorunlu olay kilidi açılır).
 */
fun StoryEngine.throughBriefing(state: WorldState, lang: Lang = Lang.TR): WorldState {
    var s = state
    var guard = 0
    while (s.sceneId != WorldState.HUB && guard++ < 20 && s.dayState.currentEvent?.let { content.chapters.managed.contains(it) } == true) {
        s = choose(s, view(s, lang).choices.first { it.enabled }.id, lang).state
    }
    return s.copy(dayState = s.dayState.copy(forcedLock = false))
}

/**
 * İstenen eylemi bölüm yapısına çevirir: üste keşif/Kapı/Kule isteği bir bölgeye yolculuk olur, son tarih gelince
 * operasyona girilir, birkaç gün süren keşiften üsse dönülür. Yine geçersizse geçerli eylemlerden biri (kamp yalnız
 * zorunluysa).
 */
fun StoryEngine.legalOr(state: WorldState, wanted: HubAction, rng: kotlin.random.Random): HubAction {
    val legal = world.legalActions(state)
    val ops = legal.filterIsInstance<HubAction.EnterOperation>()
    val ch = content.chapters.get(state.chapter)
    if (ops.isNotEmpty() && ch != null && (ch.deadline - state.day <= 1 || rng.nextDouble() < 0.05)) return ops[rng.nextInt(ops.size)]
    val m = state.mission
    if (m != null && HubAction.ReturnToBase in legal && (state.day - m.startDay >= 3 || ch?.let { it.deadline - state.day <= 1 } == true)) {
        return HubAction.ReturnToBase
    }
    if (wanted in legal) return wanted
    val travel = legal.filterIsInstance<HubAction.Travel>()
    if (m == null && travel.isNotEmpty() && (wanted == HubAction.Explore || wanted is HubAction.EnterGate || wanted == HubAction.ClimbTower)) {
        val tower = content.tower?.region
        return travel.firstOrNull { wanted == HubAction.ClimbTower && it.region == tower } ?: travel[rng.nextInt(travel.size)]
    }
    val pool = legal.filter { it != HubAction.Camp }.ifEmpty { legal }
    return pool[rng.nextInt(pool.size)]
}
