package com.yukselis.engine.save

import com.yukselis.engine.core.GameMode
import com.yukselis.engine.core.WorldState

/** Kayıtların fiziksel olarak nereye yazıldığı; Android'de uygulama depolaması, testlerde bellek. */
interface SaveStorage {
    fun read(name: String): String?
    fun write(name: String, data: String)
    fun delete(name: String)
}

class InMemorySaveStorage : SaveStorage {
    private val files = mutableMapOf<String, String>()
    override fun read(name: String) = files[name]
    override fun write(name: String, data: String) { files[name] = data }
    override fun delete(name: String) { files.remove(name) }
}

class IronmanSaveException : IllegalStateException("Demir Mod'da elle kayıt yapılamaz")

data class SlotInfo(
    val slot: String,
    val playerName: String,
    val day: Int,
    val level: Int,
    val mode: GameMode,
    val ended: Boolean,
    val savedAtMillis: Long,
)

/**
 * Bir otomatik + üç elle kayıt slotu. Demir Mod'da yalnızca otomatik slot kullanılır.
 */
class SaveManager(
    private val storage: SaveStorage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Otomatik kayıt. Yazmadan önce sağlam olan önceki otomatik kayıt [AUTO_PREV]'e döndürülür: dosya bir gün
     * bozulursa (Demir Mod'da tek kayıt) oyun kaybolmaz, bir önceki adımdan açılır (QA P1-20).
     */
    fun autosave(state: WorldState) {
        storage.read(AUTO)?.takeIf { runCatching { SaveCodec.decode(it) }.isSuccess }?.let { storage.write(AUTO_PREV, it) }
        storage.write(AUTO, SaveCodec.encode(state, now()))
    }

    fun save(slot: String, state: WorldState) {
        require(slot in MANUAL) { "Geçersiz slot: $slot" }
        if (state.mode == GameMode.IRONMAN) throw IronmanSaveException()
        storage.write(slot, SaveCodec.encode(state, now()))
    }

    /** Operasyon başlamadan hemen önceki durum (DECISIONS #141). Demir Mod'da yazılmaz (tek kayıt kuralı). */
    fun saveBeforeOperation(state: WorldState) {
        if (state.mode == GameMode.IRONMAN) return
        storage.write(OPERATION, SaveCodec.encode(state, now()))
    }

    /** Savaş başında: Normal modda ölümden sonra dönülecek nokta. Demir Mod'da yazılmaz. */
    fun saveCheckpoint(state: WorldState) {
        if (state.mode == GameMode.IRONMAN) return
        storage.write(WorldState.CHECKPOINT_SLOT, SaveCodec.encode(state, now()))
    }

    /** Yalnızca aynı oyunun (aynı tohum ve ad) kayıt noktası döner; başka oyunun noktası yok sayılır. */
    fun loadCheckpoint(current: WorldState): WorldState? =
        load(WorldState.CHECKPOINT_SLOT)?.takeIf { it.seed == current.seed && it.playerName == current.playerName && !it.dead }

    fun clearCheckpoint() = storage.delete(WorldState.CHECKPOINT_SLOT)

    /** Bozuk ya da okunamayan kayıt null döner; oyun çökmez. */
    fun load(slot: String): WorldState? = read(slot)?.state

    fun info(slot: String): SlotInfo? = read(slot)?.let { data ->
        val s = data.state
        SlotInfo(slot, s.playerName, s.day, s.player.level, s.mode, s.ended, data.savedAtMillis)
    }

    fun allSlots(): List<Pair<String, SlotInfo?>> = (listOf(AUTO, OPERATION) + MANUAL).map { it to info(it) }

    fun delete(slot: String) {
        storage.delete(slot)
        if (slot == AUTO) storage.delete(AUTO_PREV)
    }

    private fun read(slot: String): SaveData? =
        storage.read(slot)?.let { runCatching { SaveCodec.decode(it) }.getOrNull() }
            ?: if (slot == AUTO) storage.read(AUTO_PREV)?.let { runCatching { SaveCodec.decode(it) }.getOrNull() } else null

    companion object {
        const val AUTO = "auto"
        /** Bir önceki sağlam otomatik kayıt; yalnız otomatik kayıt okunamazsa kullanılır. */
        const val AUTO_PREV = "auto.prev"
        val MANUAL = listOf("slot1", "slot2", "slot3")

        /** Operasyon öncesi otomatik kayıt. */
        const val OPERATION = "operasyon"
    }
}
