package org.rsmod.content.skills.crafting

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.hook.PlayerGroundItemDropHook
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FletchingKnifeEquipTest {
    @Test
    fun `op4 wields the special knife into the offhand with a full inventory`() {
        val f = Fixture()
        f.fillWithSpades()
        f.give(2, KNIFE)
        f.interact(2, HeldOp.Op4)
        assertEquals(27, f.count(SPADE))
        assertEquals(0, f.count(KNIFE))
        assertEquals(null, f.player.inv[2])
        assertEquals(KNIFE.asRSCM(), f.player.worn[f.offhand]?.id)
        assertEquals(1, f.wornCount(KNIFE))
    }

    @Test
    fun `op4 swaps an occupied offhand back into the clicked slot`() {
        val f = Fixture()
        f.fillWithSpades()
        f.give(7, KNIFE)
        f.player.worn[f.offhand] = InvObj(SHIELD)
        f.interact(7, HeldOp.Op4)
        assertEquals(SHIELD.asRSCM(), f.player.inv[7]?.id)
        assertEquals(KNIFE.asRSCM(), f.player.worn[f.offhand]?.id)
        assertEquals(27, f.count(SPADE))
        assertEquals(1, f.count(SHIELD) + f.wornCount(SHIELD))
        assertEquals(1, f.count(KNIFE) + f.wornCount(KNIFE))
    }

    @Test
    fun `repeated or stale op4 and other operations never equip or duplicate the knife`() {
        val f = Fixture()
        f.give(0, SPADE)
        f.give(1, KNIFE)
        f.interact(1, HeldOp.Op4)
        val worn = f.snapshot()

        f.interact(1, HeldOp.Op4) // clicked slot is now empty
        f.interact(0, HeldOp.Op4) // clicked slot now holds a spade, which has no Op4
        assertEquals(worn, f.snapshot())

        val other = Fixture()
        other.give(1, KNIFE)
        val carried = other.snapshot()
        for (op in listOf(HeldOp.Op1, HeldOp.Op2, HeldOp.Op3)) other.interact(1, op)
        assertEquals(carried, other.snapshot())
        assertEquals(0, other.wornCount(KNIFE))
    }

    @Test
    fun `wielding the knife displaces a two handed weapon into the clicked slot`() {
        val f = Fixture()
        f.fillWithSpades()
        f.give(4, KNIFE)
        f.player.worn[WEAPON_SLOT] = InvObj(TWO_HANDED)
        f.interact(4, HeldOp.Op4)
        assertEquals(TWO_HANDED.asRSCM(), f.player.inv[4]?.id)
        assertEquals(KNIFE.asRSCM(), f.player.worn[f.offhand]?.id)
        assertEquals(null, f.player.worn[WEAPON_SLOT])
        assertEquals(27, f.count(SPADE))
    }

    @Test
    fun `two handed equip with no room for the worn knife rolls back every slot`() {
        val f = Fixture()
        f.fillWithSpades()
        f.give(3, TWO_HANDED)
        f.player.worn[WEAPON_SLOT] = InvObj(SWORD)
        f.player.worn[f.offhand] = InvObj(KNIFE)
        val before = f.snapshot()
        f.interact(3, HeldOp.Op2)
        assertEquals(before, f.snapshot())

        // One free slot is enough: the sword takes the clicked slot, the knife the free one.
        f.player.inv[10] = null
        f.interact(3, HeldOp.Op2)
        assertEquals(TWO_HANDED.asRSCM(), f.player.worn[WEAPON_SLOT]?.id)
        assertEquals(null, f.player.worn[f.offhand])
        assertEquals(1, f.count(KNIFE))
        assertEquals(1, f.count(SWORD))
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val player =
            Player().apply {
                client = SilentClient()
                uuid = 802L
                slotId = 1
                assignUid()
                currentMapClock = 100
                processedMapClock = 100
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
        val offhand: Int = item(KNIFE).wearpos1

        fun give(slot: Int, obj: String) {
            player.inv[slot] = InvObj(obj)
        }

        fun fillWithSpades() {
            for (slot in player.inv.indices) player.inv[slot] = InvObj(SPADE)
        }

        fun count(obj: String) = player.inv.count(obj)

        fun wornCount(obj: String) = player.worn.count(obj)

        fun snapshot() =
            player.inv.indices.map { player.inv[it]?.let { o -> o.id to o.count } } +
                player.worn.indices.map { player.worn[it]?.let { o -> o.id to o.count } }

        fun interact(slot: Int, op: HeldOp) {
            check(
                ProtectedAccessLauncher.withProtectedAccess(
                    player,
                    ProtectedAccessContextFactory.empty(),
                ) {
                    held.interact(this, player.inv, slot, op)
                }
            ) {
                "player busy"
            }
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val KNIFE = CraftingConstants.FLETCHING_KNIFE
        private const val SPADE = "obj.spade"
        private const val SHIELD = "obj.iron_kiteshield"
        private const val SWORD = "obj.bronze_sword"
        private const val TWO_HANDED = "obj.bronze_2h_sword"
        private const val WEAPON_SLOT = 3
        private val restored = mutableListOf<() -> Unit>()

        private val events = EventBus()
        private val held: HeldInteractions by lazy {
            Guice.createInjector(
                    object : AbstractModule() {
                        override fun configure() {
                            bind(EventBus::class.java).toInstance(events)
                            bind(MapClock::class.java).toInstance(MapClock())
                            bind(MarketPrices::class.java).toInstance(
                                object : MarketPrices {
                                    override fun get(type: ItemServerType): Int? = null
                                }
                            )
                            Multibinder.newSetBinder(binder(), PlayerGroundItemDropHook::class.java)
                        }
                    }
                )
                .getInstance(HeldInteractions::class.java)
        }

        private fun item(obj: String): ItemServerType =
            checkNotNull(ServerCacheManager.getItem(obj.asRSCM())) { obj }

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
