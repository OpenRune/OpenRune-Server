package org.rsmod.content.areas.misc.warriorsguild

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
internal object GuildTestFixture {
    private val restored = mutableListOf<() -> Unit>()

    fun loadCache() {
        ServerCacheManager.init(241).close()
        for ((owner, name) in
            listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
            val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
            val old = field.get(null)
            restored += { field.set(null, old) }
        }
        val oldStorage = InvVirtualStorageHolder.instance
        restored += { InvVirtualStorageHolder.instance = oldStorage }
        with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
            ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
        }
    }

    fun restore() {
        restored.asReversed().forEach { it() }
        restored.clear()
    }

    fun player(): Player =
        Player().apply {
            inv = inventory("inv.inv", 28)
            worn = inventory("inv.worn", 14)
        }

    fun Player.fillInv(obj: String = "obj.bronze_dagger") {
        for (slot in inv.indices) inv[slot] = InvObj(obj, 1)
    }

    private fun inventory(name: String, size: Int) =
        Inventory(checkNotNull(ServerCacheManager.getInventory(name.asRSCM())), arrayOfNulls(size))
}
