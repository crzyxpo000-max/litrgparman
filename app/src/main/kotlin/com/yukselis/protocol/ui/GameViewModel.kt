package com.yukselis.protocol.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState
import com.yukselis.engine.i18n.Lang
import com.yukselis.engine.save.IronmanSaveException
import com.yukselis.engine.save.SaveManager
import com.yukselis.engine.save.SlotInfo
import com.yukselis.engine.story.ActionRejected
import com.yukselis.engine.story.BagView
import com.yukselis.engine.story.DailyView
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.Outcome
import com.yukselis.engine.story.SceneView
import com.yukselis.engine.story.SkillsView
import com.yukselis.engine.story.StatusView
import com.yukselis.engine.system.Stat
import com.yukselis.protocol.YukselisApplication
import com.yukselis.protocol.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class UiMessage { SAVED, LOADED, LOAD_FAILED, IRONMAN_NO_MANUAL, ACTION_FAILED }

data class UiState(
    val lang: Lang,
    val settings: Settings,
    val game: WorldState? = null,
    val scene: SceneView? = null,
    val status: StatusView? = null,
    val skills: SkillsView? = null,
    val bag: BagView? = null,
    val daily: DailyView? = null,
    /** Son seçimin ürettiği bildirimler (XP, seviye, unvan). Bir sonraki seçimde temizlenir. */
    val freshNotices: List<NoticeView> = emptyList(),
    /** Hikaye dışı eylemlerin (dağıtım, üretim, istem) bildirimleri; Hikaye dışındaki sekmelerde kısa kartlar. */
    val actionNotices: List<NoticeView> = emptyList(),
    /** Tam ekran kutlama kartı: seviye atlama, evrim, sınıf. Dokununca kapanır. */
    val celebration: List<NoticeView> = emptyList(),
    /** Her seçimde artar; daktilo efekti ve animasyonlar bunu anahtar olarak kullanır. */
    val turn: Int = 0,
    val slots: List<Pair<String, SlotInfo?>> = emptyList(),
    val message: UiMessage? = null,
) {
    val inGame: Boolean get() = game != null
    val autosave: SlotInfo? get() = slots.firstOrNull { it.first == SaveManager.AUTO }?.second
}

/**
 * Arayüzün tek durum kaynağı. Oyun mantığı burada değil, engine modülündedir;
 * ViewModel yalnızca motoru çağırır, sonucu gösterir ve kaydeder.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YukselisApplication

    private val _ui = MutableStateFlow(
        UiState(lang = app.settings.language(), settings = app.settings.settings()),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        refreshSlots()
    }

    fun newGame(name: String, ironman: Boolean) {
        val mode = if (ironman) GameMode.IRONMAN else GameMode.NORMAL
        val state = app.engine.newGame(name, mode, seed = Random.nextLong())
        show(state, emptyList())
        persistAuto(state)
    }

    fun continueGame() = load(SaveManager.AUTO)

    fun choose(choiceId: String) {
        val current = _ui.value.game ?: return
        val outcome = runCatching { app.engine.choose(current, choiceId, _ui.value.lang) }.getOrNull() ?: return
        show(outcome.state, outcome.notices)
        persistAuto(outcome.state)
    }

    // ---- Sistem eylemleri ----

    fun resolvePrompt(optionId: String) = act { s, l -> app.engine.resolvePrompt(s, optionId, l) }

    fun allocate(points: Map<Stat, Int>) = act { s, l -> app.engine.allocate(s, points, l) }

    fun craft(recipeId: String) = act { s, l -> app.engine.craft(s, recipeId, l) }

    fun useItem(itemId: String) = act { s, l -> app.engine.useItem(s, itemId, l) }

    fun train() = act { s, l -> app.engine.train(s, l) }

    fun clearActionNotices() {
        _ui.update { it.copy(actionNotices = emptyList()) }
    }

    fun dismissCelebration() {
        _ui.update { it.copy(celebration = emptyList()) }
    }

    /** Hikaye dışı eylem: tur sayacını ilerletmez; bildirimler eylem kartı ve gerekirse kutlama olur. */
    private fun act(block: (WorldState, Lang) -> Outcome) {
        val current = _ui.value.game ?: return
        val outcome = try {
            block(current, _ui.value.lang)
        } catch (_: ActionRejected) {
            _ui.update { it.copy(message = UiMessage.ACTION_FAILED) }
            return
        }
        render(outcome.state, _ui.value.freshNotices, advanceTurn = false)
        val big = celebrations(outcome.notices)
        _ui.update { it.copy(actionNotices = outcome.notices - big.toSet(), celebration = it.celebration + big) }
        persistAuto(outcome.state)
    }

    fun setLanguage(lang: Lang) {
        if (lang == _ui.value.lang) return
        app.settings.setLanguage(lang)
        _ui.update { it.copy(lang = lang) }
        // Bildirimler eski dilde çözülmüştü; yeniden çizerken temizlenir.
        _ui.value.game?.let { render(it, emptyList(), advanceTurn = false) }
        _ui.update { it.copy(actionNotices = emptyList(), celebration = emptyList()) }
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        val updated = transform(_ui.value.settings)
        app.settings.save(updated)
        _ui.update { it.copy(settings = updated) }
    }

    fun saveTo(slot: String) {
        val state = _ui.value.game ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val message = try {
                app.saves.save(slot, state)
                UiMessage.SAVED
            } catch (_: IronmanSaveException) {
                UiMessage.IRONMAN_NO_MANUAL
            }
            _ui.update { it.copy(message = message) }
            refreshSlots()
        }
    }

    fun load(slot: String) {
        val state = app.saves.load(slot)
        if (state == null) {
            _ui.update { it.copy(message = UiMessage.LOAD_FAILED) }
            return
        }
        show(state, emptyList())
        if (slot != SaveManager.AUTO) _ui.update { it.copy(message = UiMessage.LOADED) }
    }

    /** Uygulama arka plana alındığında MainActivity çağırır. */
    fun autosave() {
        _ui.value.game?.let { persistAuto(it) }
    }

    fun backToTitle() {
        autosave()
        _ui.update {
            it.copy(game = null, scene = null, status = null, skills = null, bag = null, daily = null,
                freshNotices = emptyList(), actionNotices = emptyList(), celebration = emptyList())
        }
        refreshSlots()
    }

    fun consumeMessage() {
        _ui.update { it.copy(message = null) }
    }

    private fun show(state: WorldState, notices: List<NoticeView>) {
        render(state, notices, advanceTurn = true)
        _ui.update { it.copy(actionNotices = emptyList(), celebration = celebrations(notices)) }
    }

    /** Büyük an (seviye, evrim, sınıf) varsa, o anın yetenek/unvan/Yankı bildirimleri de aynı karta girer. */
    private fun celebrations(notices: List<NoticeView>): List<NoticeView> {
        val big = setOf(NoticeKind.LEVEL_UP, NoticeKind.EVOLUTION, NoticeKind.CLASS)
        if (notices.none { it.kind in big }) return emptyList()
        val shown = big + setOf(NoticeKind.SKILL, NoticeKind.TITLE, NoticeKind.ECHO)
        return notices.filter { it.kind in shown }.sortedBy { if (it.kind in big) 0 else 1 }
    }

    private fun render(state: WorldState, notices: List<NoticeView>, advanceTurn: Boolean) {
        val lang = _ui.value.lang
        val engine = app.engine
        _ui.update {
            it.copy(
                game = state,
                scene = engine.view(state, lang),
                status = engine.status(state, lang),
                skills = engine.skills(state, lang),
                bag = engine.bag(state, lang),
                daily = engine.daily(state, lang),
                freshNotices = notices,
                turn = if (advanceTurn) it.turn + 1 else it.turn,
            )
        }
    }

    private fun persistAuto(state: WorldState) {
        viewModelScope.launch(Dispatchers.IO) {
            app.saves.autosave(state)
            refreshSlots()
        }
    }

    private fun refreshSlots() {
        val slots = app.saves.allSlots()
        _ui.update { it.copy(slots = slots) }
    }
}
