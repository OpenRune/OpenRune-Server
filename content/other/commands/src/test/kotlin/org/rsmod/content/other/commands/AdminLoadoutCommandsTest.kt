package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.getBits

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AdminLoadoutCommandsTest {
    @Test
    fun `all runes adds real stacks including combinations and the current cache runes`() {
        val fixture = Fixture()
        fixture.player.inv[0] = item("airrune", 17)
        fixture.run("allrunes")

        val expected = AdminLoadout.RUNES.items()
        assertEquals(24, fixture.player.inv.occupiedSpace())
        for (rune in expected) {
            val actual = fixture.player.inv.objs.filterNotNull().single { it.id == rune.id }
            assertEquals(if (rune.id == "obj.airrune".asRSCM()) 5_017 else 5_000, actual.count)
            assertTrue(ServerCacheManager.getItem(actual.id)!!.stackable)
        }
    }

    @Test
    fun `a full inventory prevents partial delivery and preserves original items`() {
        val fixture = Fixture()
        repeat(20) { fixture.player.inv[it] = item("abyssal_whip") }
        val before = fixture.player.inv.objs.toList()
        fixture.run("maxmelee")
        assertEquals(before, fixture.player.inv.objs.toList())
    }

    @Test
    fun `gear is added to inventory without touching worn items and shadow is charged`() {
        for (loadout in listOf(AdminLoadout.MELEE, AdminLoadout.RANGED, AdminLoadout.MAGIC)) {
            val fixture = Fixture()
            fixture.player.worn[3] = item("abyssal_whip")
            val worn = fixture.player.worn.objs.toList()
            fixture.run(loadout.command)
            assertEquals(loadout.items(), fixture.player.inv.objs.filterNotNull())
            assertEquals(worn, fixture.player.worn.objs.toList())
        }
        val shadow = AdminLoadout.MAGIC.items().single { it.id == "obj.tumekens_shadow".asRSCM() }
        val chargeType = ServerCacheManager.getVarObj("varobj.tumeken_charges".asRSCM())!!
        assertEquals(20_000, shadow.vars.getBits(chargeType.bits))
    }

    @Test
    fun `all four commands are registered for the commands menu and restricted to administrators`() {
        val fixture = Fixture()
        for (loadout in AdminLoadout.entries) {
            val handler = fixture.commands.commands.getValue(loadout.command)
            assertEquals(Rights.ADMINISTRATOR, handler.requiredRights)
            assertTrue(handler.desc.orEmpty().contains("Spawn"))
        }
        fixture.player.modLevel = Rights.NONE
        for (loadout in AdminLoadout.entries) {
            assertTrue(fixture.commands.execute(fixture.player, loadout.command, emptyList()))
        }
        assertTrue(fixture.player.inv.isEmpty())
    }

    @Test
    fun `rune stack overflow also rolls back the complete request`() {
        val fixture = Fixture()
        fixture.player.inv[0] = item("airrune", Int.MAX_VALUE)
        val before = fixture.player.inv.objs.toList()
        fixture.run("allrunes")
        assertEquals(before, fixture.player.inv.objs.toList())
    }

    private class Fixture {
        val commands = CheatCommandMap()
        val scripts = ScriptContext(EventBus(), commands, EngineQueueCache())
        val player = Player(RecordingClient()).apply {
            username = "loadout-test"
            modLevel = Rights.ADMINISTRATOR
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        init {
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { scripts.startup() }
            with(AdminLoadoutCommands()) { scripts.startup() }
        }

        fun run(command: String) {
            assertTrue(commands.execute(player, command, emptyList()))
        }
    }

    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) = Unit
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }

    companion object {
        @JvmStatic @BeforeAll
        fun loadCache() { ServerCacheManager.init(240).close() }

        private fun item(symbol: String, count: Int = 1) = InvObj(
            checkNotNull(ServerCacheManager.getItem("obj.$symbol".asRSCM())), count,
        )
    }
}
