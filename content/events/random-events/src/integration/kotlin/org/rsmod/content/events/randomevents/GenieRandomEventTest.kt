package org.rsmod.content.events.randomevents

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class GenieRandomEventTest {
    @Test
    fun GameTestState.`cancelling at the open dialogue page keeps one lamp and he leaves`() =
        runGenieTest { deps ->
            val genie = talkToGenie(deps)
            assertTrue(player.ui.containsModal("interface.chat_left"))

            player.ifClose()
            advance(ticks = 10)

            assertEquals(1, player.inv.count(LAMP))
            assertFalse(genie.isSlotAssigned)
        }

    @Test
    fun GameTestState.`logging out at the open dialogue page keeps the lamp and removes him`() =
        runGenieTest { deps ->
            val genie = talkToGenie(deps)

            eventBus.publish(SessionStateEvent.Logout(player))

            assertEquals(1, player.inv.count(LAMP))
            assertFalse(genie.isSlotAssigned)
        }

    @Test
    fun GameTestState.`a full inventory drops the lamp under the player`() =
        runGenieTest { deps ->
            player.fillInv()
            val filled = player.inv.objs.toList()

            talkToGenie(deps)

            assertEquals(filled, player.inv.objs.toList())
            val lamp = LAMP.asRSCM(RSCMType.OBJ)
            assertEquals(1, deps.objs.findAll(player.coords).count { it.type == lamp })
        }

    @Test
    fun GameTestState.`rubbing the lamp uses it once for ten times the base level`() =
        runGenieTest {
            player.inv[0] = InvObj(LAMP, 1)
            player.setBaseLevel("stat.attack", 1)
            player.setCurrentLevel("stat.attack", 3)

            player.withProtectedAccess { opHeld1(0) }
            player.activeCoroutine?.resumeWith(ResumePauseButtonInput(LAMP_SKILLS, 0))
            player.ifClose()
            player.withProtectedAccess { opHeld1(0) }
            player.activeCoroutine?.resumeWith(ResumePauseButtonInput(LAMP_SKILLS, 0))

            assertEquals(0, player.inv.count(LAMP))
            assertEquals(10, player.statMap.getXP("stat.attack"))
        }

    private fun GameTestScope.talkToGenie(deps: Deps): Npc {
        deps.cheats.execute(player, "randomevent", emptyList())
        advance(ticks = 3)
        val genie =
            checkNotNull(
                npcRepo.findAll(ZoneKey.from(player.coords), 1).firstOrNull {
                    it.isType(GenieRandomEvent.GENIE)
                }
            )
        player.opNpc1(genie)
        advanceUntil({ player.ui.containsModal("interface.chat_left") })
        return genie
    }

    private fun GameTestState.runGenieTest(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, GenieRandomEvent::class) { deps ->
            player.placeAt(START)
            for (x in -1..1) {
                for (z in -1..1) {
                    allocZoneCollision(START.translate(x * 8, z * 8))
                }
            }
            player.clearAllInvs()
            player.modLevel = Rights.ADMINISTRATOR
            player.observerUUID = 1L
            body(deps)
        }

    class Deps @Inject constructor(val cheats: CheatCommandMap, val objs: ObjRepository)

    private companion object {
        const val LAMP = GenieRandomEvent.LAMP
        const val LAMP_SKILLS = "component.xpreward:universe"
        val START = CoordGrid(0, 50, 50, 20, 20)
    }
}
