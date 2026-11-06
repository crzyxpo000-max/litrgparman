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
import com.yukselis.engine.combat.CombatAction
import com.yukselis.engine.story.CombatView
import com.yukselis.engine.story.JournalView
import com.yukselis.engine.story.RegionView
import com.yukselis.engine.world.HubAction
import com.yukselis.engine.story.DailyView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.yukselis.engine.story.NoticeKind
import com.yukselis.engine.story.NoticeView
import com.yukselis.engine.story.Outcome
import com.yukselis.engine.story.SceneView
import com.yukselis.engine.story.SkillsView
import com.yukselis.engine.story.StatusView
import com.yukselis.engine.system.Stat
import com.yukselis.protocol.YukselisApplication
import com.yukselis.protocol.data.Settings
import android.util.Log
import com.yukselis.engine.core.PromptKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "Yukselis"
private const val AUTO_DELAY_MS = 650L

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
    /** Süren savaş; varken Hikaye sekmesi savaş ekranını gösterir. */
    val combat: CombatView? = null,
    /** Otomatik tur açık. */
    val autoCombat: Boolean = false,
    val map: List<RegionView> = emptyList(),
    val journal: JournalView? = null,
    val relations: com.yukselis.engine.story.RelationsView? = null,
    val ranking: com.yukselis.engine.story.RankingView? = null,
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
    /** Son ekranı: epilog kartları (Aşama 7). */
    val ending: com.yukselis.engine.story.EndingView? = null,
    /** Döngü sonu görüldü: başlıkta New Game+ seçeneği. */
    val newGamePlusUnlocked: Boolean = false,
    /** Android 13+ bildirim izni reddedildi (bir daha kendiliğinden istenmez). */
    val notificationDenied: Boolean = false,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val saveDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val _ui = MutableStateFlow(
        UiState(lang = app.settings.language(), settings = app.settings.settings(), newGamePlusUnlocked = app.settings.newGamePlusUnlocked(), notificationDenied = app.settings.notificationPermissionDenied()),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        refreshSlots()
    }

    fun newGame(name: String, ironman: Boolean, newGamePlus: Boolean = false) {
        stopAuto()
        app.saves.clearCheckpoint()
        val mode = if (ironman) GameMode.IRONMAN else GameMode.NORMAL
        val ngPlus = newGamePlus && app.settings.newGamePlusUnlocked()
        val state = app.engine.newGame(name, mode, seed = Random.nextLong(), previousGames = app.settings.completedGames(), newGamePlus = ngPlus)
        show(state, emptyList())
        persistAuto(state)
    }

    fun continueGame() = load(SaveManager.AUTO)

    fun choose(choiceId: String) {
        val current = _ui.value.game ?: return
        val outcome = runCatching { app.engine.choose(current, choiceId, _ui.value.lang) }
            .onFailure { e ->
                Log.e(TAG, "Seçim başarısız: $choiceId", e)
                _ui.update { it.copy(message = UiMessage.ACTION_FAILED) }
            }
            .getOrNull() ?: return
        // Savaş başladı: Normal modda ölüm sonrası dönülecek kayıt noktası seçimden önceki durumdur.
        if (current.combat == null && outcome.state.combat != null) {
            viewModelScope.launch(saveDispatcher) { app.saves.saveCheckpoint(current) }
        }
        show(outcome.state, outcome.notices)
        persistAuto(outcome.state)
    }

    // ---- Gün akışı ----

    /** Gün akışı eylemi: bir yuva harcar (kamp günü bitirir); olay ya da savaş başlayabilir. */
    fun hubAction(action: HubAction) {
        val current = _ui.value.game ?: return
        val outcome = try {
            app.engine.hubAction(current, action, _ui.value.lang)
        } catch (e: RuntimeException) {
            Log.i(TAG, "Akış eylemi reddedildi: ${e.message}")
            _ui.update { it.copy(message = UiMessage.ACTION_FAILED) }
            return
        }
        if (current.combat == null && outcome.state.combat != null) {
            viewModelScope.launch(saveDispatcher) { app.saves.saveCheckpoint(current) }
        }
        show(outcome.state, outcome.notices)
        persistAuto(outcome.state)
    }

    // ---- Savaş ----

    private var autoJob: Job? = null

    fun combatAction(action: CombatAction) {
        val current = _ui.value.game ?: return
        if (current.combat == null) return
        act(advanceTurn = false) { s, l -> app.engine.combatAction(s, action, l) }
        // Savaş bitti: hikaye yeni sahneyle sürer, daktilo efekti yeniden başlasın.
        if (_ui.value.game?.combat == null) _ui.update { it.copy(turn = it.turn + 1, freshNotices = it.actionNotices, actionNotices = emptyList(), autoCombat = false) }
    }

    private fun stopAuto() {
        autoJob?.cancel()
        autoJob = null
        _ui.update { it.copy(autoCombat = false) }
    }

    /** Otomatik tur: savaş bitene, infaz gelene ya da can %30'un altına düşene kadar önerilen eylemleri oynar. */
    fun toggleAuto() {
        if (_ui.value.autoCombat) {
            autoJob?.cancel()
            _ui.update { it.copy(autoCombat = false) }
            return
        }
        _ui.update { it.copy(autoCombat = true) }
        autoJob = viewModelScope.launch {
            while (_ui.value.autoCombat) {
                val s = _ui.value.game ?: break
                val c = s.combat ?: break
                if (s.dead || c.pendingExecution != null || s.player.hp * 100 / s.player.maxHp < 30) break
                combatAction(app.engine.autoCombatAction(s))
                // Eylem reddedildiyse durum değişmez: sonsuz döngüye girme.
                if (_ui.value.game === s) break
                delay(AUTO_DELAY_MS)
            }
            _ui.update { it.copy(autoCombat = false) }
        }
    }

    /** Normal modda ölüm: savaş başındaki kayıt noktasına döner; yoksa otomatik kayda. */
    fun returnToCheckpoint() {
        stopAuto()
        val current = _ui.value.game ?: return backToTitle()
        val state = (app.saves.loadCheckpoint(current) ?: app.saves.load(SaveManager.AUTO)?.takeIf { !it.dead })
            ?.let { runCatching { app.engine.sanitize(it) }.getOrNull() }
        if (state == null) {
            backToTitle()
            return
        }
        show(state, emptyList())
        persistAuto(state)
    }

    // ---- Sistem eylemleri ----

    /** Kart, gösterildiği istemin türü ve konusuyla birlikte yanıt verir; istem değiştiyse motor reddeder. */
    fun resolvePrompt(kind: PromptKind, subject: String?, optionId: String) =
        act { s, l -> app.engine.resolvePrompt(s, optionId, l, kind, subject) }

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
    private fun act(advanceTurn: Boolean = false, block: (WorldState, Lang) -> Outcome) {
        val current = _ui.value.game ?: return
        val outcome = try {
            block(current, _ui.value.lang)
        } catch (e: ActionRejected) {
            Log.i(TAG, "Eylem reddedildi: ${e.message}")
            _ui.update { it.copy(message = UiMessage.ACTION_FAILED) }
            return
        } catch (e: RuntimeException) {
            // Bozuk ya da eski içerikle uyumsuz kayıt oyunu çökertmesin.
            Log.e(TAG, "Eylem başarısız", e)
            _ui.update { it.copy(message = UiMessage.ACTION_FAILED) }
            return
        }
        render(outcome.state, _ui.value.freshNotices, advanceTurn)
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
        val before = _ui.value.settings
        val updated = transform(before)
        app.settings.save(updated)
        _ui.update { it.copy(settings = updated) }
        if (updated.notifications != before.notifications) {
            com.yukselis.protocol.data.Notifications.schedule(getApplication(), updated.notifications)
        }
    }

    /** Android 13+ bildirim izni sonucu: reddedildiyse ayar kapanır ve bir daha kendiliğinden istenmez. */
    fun onNotificationPermission(granted: Boolean) {
        app.settings.setNotificationPermissionDenied(!granted)
        _ui.update { it.copy(notificationDenied = !granted) }
        updateSettings { it.copy(notifications = granted) }
    }


    /** Bildirimden açıldı: otomatik kayıt yüklenir ve bildirimin küçük olayı tohumlanır. */
    fun openFromNotification(noteId: String) {
        val current = _ui.value.game ?: app.saves.load(SaveManager.AUTO)?.let { runCatching { app.engine.sanitize(it) }.getOrNull() } ?: return
        if (current.ended) return
        val opened = app.engine.openNotification(current, noteId)
        show(opened, emptyList())
        persistAuto(opened)
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
        stopAuto()
        val state = app.saves.load(slot)?.let { runCatching { app.engine.sanitize(it) }.getOrNull() }
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
        stopAuto()
        autosave()
        _ui.update {
            it.copy(game = null, scene = null, status = null, skills = null, bag = null, daily = null, combat = null, autoCombat = false,
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
                combat = engine.combatView(state, lang),
                map = engine.map(state, lang),
                journal = engine.journal(state, lang),
                relations = engine.relations(state, lang),
                ranking = engine.ranking(state, lang),
                ending = engine.endingView(state, lang),
                freshNotices = notices,
                turn = if (advanceTurn) it.turn + 1 else it.turn,
            )
        }
    }

    /** Son: bitirilmiş oyun meta kaydına girer (Döngü sonu New Game+'ı açar). */
    private fun recordEnding(state: WorldState) {
        val ending = app.engine.endingOf(state) ?: return
        app.settings.recordEnding(state.seed, ending.id, ending.id == com.yukselis.engine.story.StoryEngine.LOOP_ENDING)
        _ui.update { it.copy(newGamePlusUnlocked = app.settings.newGamePlusUnlocked()) }
    }

    private fun persistAuto(state: WorldState) {
        if (state.ended) recordEnding(state)
        // Tek iş parçacıklı kuyruk: kayıtlar sırayla yazılır, eski durum yenisinin üstüne yazılamaz.
        viewModelScope.launch(saveDispatcher) {
            app.saves.autosave(state)
            refreshSlots()
        }
    }

    private fun refreshSlots() {
        val slots = app.saves.allSlots()
        _ui.update { it.copy(slots = slots) }
    }
}
