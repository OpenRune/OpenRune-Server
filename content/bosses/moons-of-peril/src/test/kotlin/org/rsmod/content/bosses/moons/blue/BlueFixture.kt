package org.rsmod.content.bosses.moons.blue

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import io.mockk.mockk
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

var Player.wearPrevent by intVarBit("varbit.pmoon_boss_blue_wearprevent")
var Player.bankCapacityVar by intVarBit("varbit.bank_capacity")
var Player.bankMainSize by intVarBit("varbit.bank_tab_main")

object BlueTestCache {
    private val restored = mutableListOf<() -> Unit>()

    @OptIn(InternalApi::class)
    fun init() {
        ServerCacheManager.init(241).close()
        val fields =
            listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )
        for ((owner, name) in fields) {
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
}

@OptIn(InternalApi::class)
class BlueFixture {
    val events = EventBus()
    val frozen = FrozenWeapons(events)
    val player =
        Player().apply {
            client = mockk(relaxed = true)
            uuid = 4321L
            observerUUID = uuid
            slotId = 1
            assignUid()
            inv =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                    arrayOfNulls(28),
                )
            worn =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                    arrayOfNulls(14),
                )
        }

    val holding: Inventory
        get() = frozen.holding(player)

    val bank: Inventory
        get() = player.invMap.getOrPut("inv.bank")

    init {
        player.bankCapacityVar = 800
    }

    fun protectWorn() {
        val wornType = player.worn.type.let { it.copy(flags = it.flags or dev.openrune.types.InventoryServerType.PROTECT_FLAG) }
        val objs = player.worn.objs
        player.worn = Inventory(wornType, objs)
        player.ui.modals.backing.put(1, 1)
    }

    fun type(name: String): ItemServerType = checkNotNull(ServerCacheManager.getItem(name.asRSCM()))

    fun fillInv(name: String = "obj.black_bead") {
        for (slot in 0 until 28) player.inv[slot] = InvObj(name, 1)
    }

    fun total(name: String): Long {
        val id = name.asRSCM()
        var sum = 0L
        for (inv in listOf(player.worn, player.inv, holding, bank)) {
            for (obj in inv) if (obj?.id == id) sum += obj.count
        }
        return sum
    }
}
