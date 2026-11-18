package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class PenguinTestDeps @Inject constructor(val equip: HeldEquipOp)

@Execution(ExecutionMode.SAME_THREAD)
class PenguinCourseTest {
    @Test
    fun GameTestState.`a player outside the penguin suit is refused the course`() =
        runPenguinTest {
            setAgility(99)
            startAt(CoordGrid(2629, 4057, 0))

            val messages = assertRefused("loc.peng_agility_crushcourse_stepstone01")

            assertEquals(listOf("Only penguins can use this course."), messages)
        }

    @Test
    fun GameTestState.`tuxedo time puts a player carrying a clockwork suit in it`() =
        runPenguinTest {
            startAt(ICEBERG)
            player.inv[0] = InvObj(SUIT, 1)
            val larry = spawnNpc(ICEBERG.translateX(1), npcType("npc.peng_larry_ice"))

            player.opNpc3(larry)
            val messages = advanceCollecting(ticks = 12)

            assertEquals(PENGUIN_FORM.asRSCM(), player.transmog?.id)
            assertEquals(1, player.vars[IN_SUIT])
            assertEquals(1, player.inv.count(SUIT))
            assertEquals("You shrink and Larry puts you in the penguin suit.", messages.last())
        }

    @Test
    fun GameTestState.`tuxedo time refuses a player holding a weapon`() = runPenguinTest {
        startAt(ICEBERG)
        player.inv[0] = InvObj(SUIT, 1)
        player.righthand = InvObj("obj.bronze_sword", 1)
        val larry = spawnNpc(ICEBERG.translateX(1), npcType("npc.peng_larry_ice"))

        player.opNpc3(larry)
        advanceCollecting(ticks = 12)

        assertNull(player.transmog)
        assertEquals(0, player.vars[IN_SUIT])
    }

    @Test
    fun GameTestState.`leaving the penguins takes the suit off`() = runPenguinTest {
        startAt(ICEBERG)
        becomePenguin()
        advance()

        startAt(CoordGrid(3222, 3218, 0))
        val messages = advanceCollecting(ticks = 5)

        assertNull(player.transmog)
        assertEquals(0, player.vars[IN_SUIT])
        val wornOff =
            "The spell wears off as you climb out of the suit, growing back to normal size."
        assertEquals(listOf(wornOff), messages)
    }

    @Test
    fun GameTestState.`the suit hands back whatever is wielded without losing any of it`() =
        runPenguinTest { deps ->
            startAt(ICEBERG)
            becomePenguin()
            player.fillInv(InvObj("obj.beer", 1))
            player.inv[0] = InvObj(DARTS, 250)
            player.inv[1] = InvObj(SHIELD, 1)
            val beers = player.inv.count("obj.beer")
            advance()

            deps.equip.equip(player, 0, player.inv)
            deps.equip.equip(player, 1, player.inv)
            deps.equip.equip(player, 0, player.inv)
            val messages = gameMessages()

            assertNull(player.righthand)
            assertNull(player.lefthand)
            assertEquals(250, player.inv.count(DARTS))
            assertEquals(1, player.inv.count(SHIELD))
            assertEquals(beers, player.inv.count("obj.beer"))
            assertEquals(28, player.inv.occupiedSpace())
            val refusal = "You can't hold anything in your hands in the penguin suit."
            assertEquals(List(3) { refusal }, messages)
        }

    @Test
    fun GameTestState.`a crusher sliding out knocks a penguin back a tile for 4`() =
        runPenguinTest {
            player.statMap.setBaseLevel("stat.hitpoints", 40.toByte())
            player.statMap.setCurrentLevel("stat.hitpoints", 40.toByte())
            val home = CoordGrid(2632, 4056, 0)
            val crusher = spawnNpc(home, npcType("npc.peng_agility_crushcourse_crushblock02_npc"))
            startAt(home.translateZ(-1))
            becomePenguin()

            advanceUntil({ player.coords != home.translateZ(-1) }, timeoutTicks = 12)
            assertEquals(home.translateZ(-1), crusher.coords)
            advance(ticks = 2)

            assertEquals(home.translateZ(-2), player.coords)
            assertEquals(36, player.hitpoints)
        }

    @Test
    fun GameTestState.`a crusher is out two ticks in each of its period`() = runPenguinTest {
        val home = CoordGrid(2631, 4053, 0)
        val crusher = spawnNpc(home, npcType("npc.peng_agility_crushcourse_crushblock03_npc"))

        val zs = mutableListOf<Int>()
        repeat(12) {
            advance()
            zs += crusher.coords.z
        }

        assertEquals(6, zs.count { it == 4054 })
        assertEquals(6, zs.count { it == 4053 })
    }

    @Test
    fun GameTestState.`a ninja greegree makes a monkey on ape atoll only`() = runPenguinTest {
        startAt(APE_ATOLL)
        player.righthand = InvObj(GREEGREE, 1)
        advance()
        val form = "npc.mm_transmogrification_small_ninja_monkey".asRSCM()
        assertEquals(form, player.transmog?.id)

        player.righthand = null
        advance()
        assertNull(player.transmog)

        player.righthand = InvObj(GREEGREE, 1)
        advance()
        startAt(CoordGrid(3222, 3218, 0))
        advance()
        assertNull(player.transmog)
    }

    private fun GameTestScope.becomePenguin() {
        player.setVarBit(IN_SUIT, 1)
        player.transmog = npcType(PENGUIN_FORM)
        player.rebuildAppearance()
    }

    private fun npcType(npc: String) =
        checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))

    private fun GameTestState.runPenguinTest(body: GameTestScope.(PenguinTestDeps) -> Unit) =
        runInjectedGameTest(
            PenguinTestDeps::class,
            AgilityModule(),
            AgilityCourseScript::class,
            AgilityTransformsScript::class,
            PenguinCrushersScript::class,
            PlayerHitScript::class,
        ) { deps ->
            advance()
            player.observerUUID = player.uuid
            for (placement in PLACEMENTS) {
                placeMapLoc(
                    placement.coords,
                    placement.loc,
                    LocShape[placement.shape],
                    LocAngle[placement.angle],
                )
            }
            body(deps)
        }

    private companion object {
        const val PENGUIN_FORM = "npc.peng_suit_npc"
        const val IN_SUIT = "varbit.peng_transmog"
        const val SUIT = "obj.peng_suit_unwound"
        const val DARTS = "obj.bronze_dart"
        const val SHIELD = "obj.bronze_sq_shield"
        const val GREEGREE = "obj.mm_monkey_greegree_for_small_ninja_monkey"

        val ICEBERG = CoordGrid(2660, 3989, 1)
        val APE_ATOLL = CoordGrid(2756, 2742, 0)
    }
}
