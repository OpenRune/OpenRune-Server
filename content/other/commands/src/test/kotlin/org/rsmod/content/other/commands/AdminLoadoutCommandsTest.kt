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
import org.mockito.Mockito.`when`
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.combat.ShieldSpecialAttack
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponRegistry
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
    private var Player.specialEnergy by intVarp("varp.sa_energy")

    @Test fun `maxrange supplies necklace of rupture in inventory`() {
        val fixture = Fixture()
        fixture.run("maxrange")
        assertTrue(fixture.player.inv.objs.filterNotNull().any { it.id == "obj.necklace_of_rupture".asRSCM() })
        assertTrue(fixture.player.inv.objs.filterNotNull().none { it.id == "obj.zenyte_necklace_enchanted".asRSCM() })
    }

    @Test
    fun `spres restores energy without changing inventory and is admin only`() {
        val fixture = Fixture()
        fixture.player.specialEnergy = 75
        fixture.player.inv[0] = item("airrune", 17)
        fixture.run("spres")
        assertEquals(1000, fixture.player.vars["varp.sa_energy"])
        assertEquals(17, fixture.player.inv[0]?.count)
        assertEquals(Rights.ADMINISTRATOR, fixture.commands.commands.getValue("spres").requiredRights)
        fixture.player.specialEnergy = 0
        fixture.player.modLevel = Rights.NONE
        fixture.run("spres")
        assertEquals(0, fixture.player.vars["varp.sa_energy"])
    }

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

    @Test
    fun `weapon test pages deliver registered items once and preserve gear`() {
        val fixture = Fixture()
        val originals = ServerCacheManager.getItems().values
            .filter { it.wearpos1 == 3 && !it.stackable }.sortedBy { it.id }.take(25)
        for (type in originals) {
            `when`(fixture.specials[InvObj(type)]).thenReturn(SpecialAttack.Melee(250, mock(MeleeSpecialAttack::class.java)))
        }
        fixture.player.worn[3] = item("abyssal_whip")
        val worn = fixture.player.worn.objs.toList()
        fixture.run("weptest", listOf("1"))
        assertEquals(originals.take(20).map { it.id }, fixture.player.inv.objs.filterNotNull().map { it.id })
        fixture.run("weptest", listOf("2"))
        assertEquals(originals.map { it.id }, fixture.player.inv.objs.filterNotNull().map { it.id })
        assertEquals(worn, fixture.player.worn.objs.toList())
        val before = fixture.player.inv.objs.toList()
        fixture.run("weptest", listOf("1"))
        assertEquals(before, fixture.player.inv.objs.toList())
        for (args in listOf(emptyList(), listOf("0"), listOf("-1"), listOf("3"), listOf("abc"), listOf("1", "2"))) {
            fixture.run("weptest", args)
            assertEquals(before, fixture.player.inv.objs.toList())
        }
        assertEquals(Rights.ADMINISTRATOR, fixture.commands.commands.getValue("weptest").requiredRights)
        fixture.player.modLevel = Rights.NONE
        fixture.run("weptest", listOf("2"))
        assertEquals(before, fixture.player.inv.objs.toList())
    }

    @Test
    fun `weapon test excludes missing specials and unrelated items but includes normal handlers and charged shields`() {
        val fixture = Fixture()
        val normal = item("scythe_of_vitur")
        val pending = item("elder_maul")
        val shield = item("dragonfire_shield")
        `when`(fixture.weapons.getMelee(normal)).thenReturn(mock(MeleeWeapon::class.java))
        `when`(fixture.specialWeapons.getSpecialEnergy(normal.id)).thenReturn(null)
        `when`(fixture.weapons.getMelee(pending)).thenReturn(mock(MeleeWeapon::class.java))
        `when`(fixture.specialWeapons.getSpecialEnergy(pending.id)).thenReturn(500)
        `when`(fixture.specials[shield]).thenReturn(SpecialAttack.Shield(mock(ShieldSpecialAttack::class.java)))
        val command = WeaponTestCommand(fixture.specials, fixture.weapons, fixture.specialWeapons)
        val items = command.items()
        assertEquals(setOf(normal.id, shield.id), items.map { it.id }.toSet())
        assertEquals(50, DragonfireShields.charges(items.single { it.id == shield.id }))
        fixture.run("weptest", listOf("1"))
        assertEquals(items, fixture.player.inv.objs.filterNotNull())
    }

    private class Fixture {
        val specials = mock(SpecialAttackRegistry::class.java)
        val weapons = mock(WeaponRegistry::class.java)
        val specialWeapons = mock(SpecialAttackWeapons::class.java)
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
            with(WeaponTestCommand(specials, weapons, specialWeapons)) { scripts.startup() }
        }

        fun run(command: String, args: List<String> = emptyList()) {
            assertTrue(commands.execute(player, command, args))
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
