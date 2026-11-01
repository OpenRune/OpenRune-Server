package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class TabletTeleportScriptTest {
    @Test
    fun `break is every bound tablet's first inventory op`() {
        for (tablet in TabletTeleportScript.TABLETS) {
            val type = checkNotNull(ServerCacheManager.getItem(tablet.asRSCM(RSCMType.OBJ)))
            assertEquals(0, type.interfaceOptions.indexOf("Break"), tablet)
        }
    }

    @Test
    fun GameTestState.`breaking a tablet consumes one on the second tick and lands on the fourth`() =
        runInjectedGameTest(TabletTeleportScript::class, MagicSpellModule(), TabletTeleportScript::class) {
            script ->
            val start = CoordGrid(0, 50, 50, 10, 10)
            val centre = CoordGrid(3213, 3424, 0)
            allocZoneCollision(start)
            for (dx in -1..1) for (dz in -1..1) allocZoneCollision(centre.translate(dx, dz))
            val tablet = ServerCacheManager.getItem(VARROCK.asRSCM(RSCMType.OBJ))!!
            player.teleport(start)
            player.clearInv()
            player.inv[3] = InvObj(tablet.id, 5)

            with(script) { player.withProtectedAccess { breakTablet(tablet, slot = 3) } }
            assertEquals(1, player.vars["varbit.busy"])

            advance(ticks = 1)
            assertEquals(5, player.inv.count(VARROCK))

            advance(ticks = 1)
            assertEquals(4, player.inv.count(VARROCK))
            assertEquals(start, player.coords)

            advance(ticks = 2)
            assertEquals(4, player.inv.count(VARROCK))
            assertTrue(player.coords.chebyshevDistance(centre) <= 1, "${player.coords}")
            assertEquals(0, player.vars["varbit.busy"])
        }

    @Test
    fun GameTestState.`a quest-locked tablet stays in the inventory`() =
        runInjectedGameTest(TabletTeleportScript::class, MagicSpellModule(), TabletTeleportScript::class) {
            script ->
            val previous = QuestRequirements.activePolicy()
            QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
            try {
                val start = CoordGrid(0, 50, 50, 10, 10)
                allocZoneCollision(start)
                val tablet = ServerCacheManager.getItem(ARDOUGNE.asRSCM(RSCMType.OBJ))!!
                player.teleport(start)
                player.clearInv()
                player.inv[0] = InvObj(tablet.id, 1)

                with(script) { player.withProtectedAccess { breakTablet(tablet, slot = 0) } }
                advance(ticks = 5)

                assertEquals(1, player.inv.count(ARDOUGNE))
                assertEquals(start, player.coords)
                assertEquals(0, player.vars["varbit.busy"])
            } finally {
                QuestRequirements.install(previous)
            }
        }

    private companion object {
        const val VARROCK = "obj.poh_tablet_varrockteleport"
        const val ARDOUGNE = "obj.poh_tablet_ardougneteleport"
    }
}
