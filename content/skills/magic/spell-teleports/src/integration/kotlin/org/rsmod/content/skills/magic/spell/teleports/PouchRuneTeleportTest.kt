package org.rsmod.content.skills.magic.spell.teleports

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class PouchRuneTeleportTest {
    @Test
    fun GameTestState.`a teleport paid entirely from a divine pouch casts`() =
        runInjectedGameTest(SpellTeleportScript::class, Module(), *SCRIPTS) {
            prepare("obj.divine_rune_pouch")
            pouch(1, "obj.airrune", 10)
            pouch(2, "obj.firerune", 10)
            pouch(4, "obj.lawrune", 10)

            cast()
            advance(ticks = 10)

            assertTrue(player.coords.chebyshevDistance(VARROCK) <= ARRIVAL_SLACK, "${player.coords}")
            assertEquals(7, player.vars["varbit.rune_pouch_quantity_1"])
            assertEquals(9, player.vars["varbit.rune_pouch_quantity_2"])
            assertEquals(9, player.vars["varbit.rune_pouch_quantity_4"])
        }

    @Test
    fun GameTestState.`a teleport split between pouch and inventory takes both`() =
        runInjectedGameTest(SpellTeleportScript::class, Module(), *SCRIPTS) {
            prepare("obj.bh_rune_pouch")
            pouch(1, "obj.airrune", 10)
            pouch(2, "obj.firerune", 10)
            player.inv[1] = InvObj("obj.lawrune".asRSCM(RSCMType.OBJ), 5)

            cast()
            advance(ticks = 10)

            assertTrue(player.coords.chebyshevDistance(VARROCK) <= ARRIVAL_SLACK, "${player.coords}")
            assertEquals(7, player.vars["varbit.rune_pouch_quantity_1"])
            assertEquals(4, player.inv.count("obj.lawrune"))
        }

    @Test
    fun GameTestState.`a pouch missing a rune refuses the cast and keeps everything`() =
        runInjectedGameTest(SpellTeleportScript::class, Module(), *SCRIPTS) {
            prepare("obj.bh_rune_pouch")
            pouch(1, "obj.airrune", 10)
            pouch(2, "obj.firerune", 10)

            cast()
            advance(ticks = 10)

            assertEquals(START, player.coords)
            assertEquals(10, player.vars["varbit.rune_pouch_quantity_1"])
            assertEquals(10, player.vars["varbit.rune_pouch_quantity_2"])
        }

    private fun GameTestScope.prepare(pouch: String) {
        allocZoneCollision(START)
        for (dx in -6..6) for (dz in -6..6) allocZoneCollision(VARROCK.translate(dx, dz))
        player.teleport(START)
        player.clearInv()
        player.inv[0] = InvObj(pouch.asRSCM(RSCMType.OBJ), 1)
        player.setBaseLevel("stat.magic", 99)
        player.setCurrentLevel("stat.magic", 99)
        player.ifOpenOverlay("interface.magic_spellbook", "component.toplevel_osrs_stretch:side6")
        advance(ticks = 10)
    }

    private fun GameTestScope.pouch(slot: Int, rune: String, count: Int) {
        VarPlayerIntMapSetter.set(player, "varbit.rune_pouch_type_$slot", COMPACT_IDS.getValue(rune))
        VarPlayerIntMapSetter.set(player, "varbit.rune_pouch_quantity_$slot", count)
    }

    private fun GameTestScope.cast() {
        player.ifButton(ServerCacheManager.fromComponent("component.magic_spellbook:varrock_teleport"))
    }

    private class Module : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            Multibinder.newSetBinder(binder(), SpellQuestRequirement::class.java)
            for (repo in REPOSITORIES) bind(repo.java).`in`(Singleton::class.java)
        }
    }

    private companion object {
        val START = CoordGrid(0, 50, 50, 10, 10)
        val VARROCK = CoordGrid(3212, 3424, 0)
        const val ARRIVAL_SLACK = 5
        val COMPACT_IDS = mapOf("obj.airrune" to 1, "obj.firerune" to 4, "obj.lawrune" to 11)

        val SCRIPTS =
            arrayOf(
                SpellTeleportScript::class,
                ComboRuneScript::class,
                CompactRuneScript::class,
                FakeRuneScript::class,
                RuneSubstituteScript::class,
                StaffSubstituteScript::class,
                UnlimitedRuneScript::class,
            )

        val REPOSITORIES =
            listOf(
                ComboRuneRepository::class,
                CompactRuneRepository::class,
                FakeRuneRepository::class,
                RuneSubstituteRepository::class,
                StaffSubstituteRepository::class,
                UnlimitedRuneRepository::class,
            )
    }
}
