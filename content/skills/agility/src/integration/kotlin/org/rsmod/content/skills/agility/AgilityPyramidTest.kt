package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class PyramidTestDeps
@Inject
constructor(val npcU: NpcUInteractions, val collision: CollisionFlagMap)

@Execution(ExecutionMode.SAME_THREAD)
class AgilityPyramidTest {
    @Test
    fun GameTestState.`the lap bonus is 300 and 8 a base level`() = runPyramidTest {
        player.statMap.setBaseLevel("stat.agility", 50.toByte())
        player.statMap.setCurrentLevel("stat.agility", 60.toByte())
        startAt(GROUND)
        takeStairs(STAIRS_UP)
        startAt(CoordGrid(3045, 4696, 3))
        val before = agilityXp()

        takeStairs(DOORWAY)

        assertEquals(before + 7000, agilityXp())
    }

    @Test
    fun GameTestState.`the lap bonus stops at 1000`() = runPyramidTest {
        setAgility(99)
        startAt(GROUND)
        takeStairs(STAIRS_UP)
        startAt(CoordGrid(3045, 4696, 3))
        val before = agilityXp()

        val messages = takeStairs(DOORWAY)

        assertEquals(before + 10000, agilityXp())
        val lap = "Your Agility Pyramid lap count is: <col=ff0000>1</col>."
        assertTrue(lap in messages) { "$messages" }
    }

    @Test
    fun GameTestState.`the doorway pays nothing without a lap`() = runPyramidTest {
        setAgility(99)
        startAt(CoordGrid(3045, 4696, 3))
        val before = agilityXp()

        val messages = takeStairs(DOORWAY)

        assertEquals(before, agilityXp())
        assertEquals(BASE, player.coords)
        assertTrue(messages.none { "lap count" in it })
    }

    @Test
    fun GameTestState.`a stone block tilts the tick after a player stops on it`() =
        runPyramidTest {
            setAgility(99)
            startAt(CoordGrid(3355, 2837, 1))
            val before = agilityXp()

            player.moveGameClick(CoordGrid(3355, 2841, 1))
            val ticks = mutableListOf<Pair<CoordGrid, Int>>()
            repeat(8) {
                advance()
                ticks += player.coords to player.vars["varbit.agility_pyramid_tilt_1"]
            }

            val arrived = ticks.indexOfFirst { it.first == CoordGrid(3355, 2841, 1) }
            assertEquals(CoordGrid(3355, 2843, 1) to 1, ticks[arrived + 1])
            assertEquals(CoordGrid(3355, 2843, 1) to 0, ticks[arrived + 2])
            assertEquals(before + 120, agilityXp())
        }

    @Test
    fun GameTestState.`the climbing rocks give one pyramid top a lap`() = runPyramidTest {
        setAgility(99)
        startAt(SUMMIT)

        climbRocks()
        climbRocks()

        assertEquals(1, player.inv.count(GOLD_PYRAMID))
        assertEquals(1, player.vars[TOP])
    }

    @Test
    fun GameTestState.`two clicks on the rocks in one tick give one top`() = runPyramidTest {
        setAgility(99)
        startAt(SUMMIT)

        val rocks = locNear(ROCKS, SUMMIT)
        player.opLoc1(rocks)
        player.opLoc1(rocks)
        advance(ticks = 6)
        player.ifClose()
        advance(ticks = 2)

        assertEquals(1, player.inv.count(GOLD_PYRAMID))
    }

    @Test
    fun GameTestState.`the top waits on the rocks for a full inventory`() = runPyramidTest {
        setAgility(99)
        startAt(SUMMIT)
        player.fillInv()

        val messages = climbRocks()

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(0, player.vars[TOP])
        assertEquals(listOf("You don't have enough inventory space."), messages)

        player.inv[0] = null
        climbRocks()

        assertEquals(1, player.inv.count(GOLD_PYRAMID))
        assertEquals(1, player.vars[TOP])
    }

    @Test
    fun GameTestState.`the doorway puts the top back on the rocks`() = runPyramidTest {
        setAgility(99)
        startAt(SUMMIT)
        climbRocks()
        startAt(CoordGrid(3045, 4696, 3))
        takeStairs(DOORWAY)
        player.ifClose()
        startAt(SUMMIT)

        climbRocks()

        assertEquals(2, player.inv.count(GOLD_PYRAMID))
    }

    @Test
    fun GameTestState.`simon buys every top for 10000 coins each`() = runPyramidTest { deps ->
        setVarbit(JOB, 1)
        repeat(3) { player.inv[it] = InvObj(GOLD_PYRAMID, 1) }
        player.inv[3] = InvObj(COINS, 5)
        val simon = spawnSimon()

        useTopOn(deps, simon, slot = 0)

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(30005, player.inv.count(COINS))
        assertEquals(1, player.vars[JOB_DONE])
        assertTrue(player.ui.containsModal("interface.objectbox"))
    }

    @Test
    fun GameTestState.`simon pays a full inventory of tops`() = runPyramidTest { deps ->
        setVarbit(JOB, 1)
        repeat(28) { player.inv[it] = InvObj(GOLD_PYRAMID, 1) }
        val simon = spawnSimon()

        useTopOn(deps, simon, slot = 27)

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(280000, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon keeps nothing when the coins would overflow`() = runPyramidTest {
        deps ->
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(GOLD_PYRAMID, 1)
        player.inv[1] = InvObj(COINS, Int.MAX_VALUE - 5000)
        val simon = spawnSimon()

        useTopOn(deps, simon, slot = 0)

        assertEquals(1, player.inv.count(GOLD_PYRAMID))
        assertEquals(Int.MAX_VALUE - 5000, player.inv.count(COINS))
        assertEquals(0, player.vars[JOB_DONE])
    }

    @Test
    fun GameTestState.`two tops used on simon in one tick are paid once`() = runPyramidTest { deps
        ->
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(GOLD_PYRAMID, 1)
        player.inv[1] = InvObj(GOLD_PYRAMID, 1)
        val simon = spawnSimon()

        useTopOn(deps, simon, slot = 0, advanceAfter = false)
        useTopOn(deps, simon, slot = 1)

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(20000, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon pays nothing for tops dropped before the sale`() = runPyramidTest {
        setVarbit(JOB, 1)
        setVarbit(JOB_DONE, 1)
        player.inv[0] = InvObj(GOLD_PYRAMID, 1)
        val simon = spawnSimon()

        player.opNpc1(simon)
        advanceUntil({ player.ui.containsModal("interface.chat_left") })
        continueDialogue()
        assertTrue(player.ui.containsModal("interface.chatmenu"))
        player.inv[0] = null
        choose(1)

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(0, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon sells through his menu once the job is done`() = runPyramidTest {
        setVarbit(JOB, 1)
        setVarbit(JOB_DONE, 1)
        player.inv[0] = InvObj(GOLD_PYRAMID, 1)
        val simon = spawnSimon()

        player.opNpc1(simon)
        advanceUntil({ player.ui.containsModal("interface.chat_left") })
        continueDialogue()
        choose(1)

        assertEquals(0, player.inv.count(GOLD_PYRAMID))
        assertEquals(10000, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon asks for a chat before he buys a top`() = runPyramidTest { deps ->
        player.inv[0] = InvObj(GOLD_PYRAMID, 1)
        val simon = spawnSimon()

        useTopOn(deps, simon, slot = 0)

        assertTrue(player.ui.containsModal("interface.chat_left"))
        assertEquals(1, player.inv.count(GOLD_PYRAMID))
        assertEquals(0, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`the floor 1 block slides out every 15 ticks and pushes a player off`() =
        runPyramidTest { deps ->
            val block = spawnNpc(EAST_BLOCK_HOME, npcType(EAST_BLOCK))
            val out = CoordGrid(3375, 2848, 1)

            val xs = mutableListOf<Int>()
            var blockedOut = false
            repeat(30) {
                advance()
                xs += block.coords.x
                if (block.coords.x == 3374) {
                    blockedOut = deps.collision.blocksPlayers(out)
                }
            }

            assertEquals(6, xs.count { it == 3374 })
            assertEquals(4, xs.count { it == 3373 })
            assertEquals(20, xs.count { it == 3372 })
            assertTrue(blockedOut)
            assertFalse(deps.collision.blocksPlayers(out))

            player.statMap.setBaseLevel("stat.hitpoints", 40.toByte())
            player.statMap.setCurrentLevel("stat.hitpoints", 40.toByte())
            startAt(CoordGrid(3374, 2848, 1))
            setVarbit(LEVEL, 1)
            advanceUntil({ player.coords.level == 0 }, timeoutTicks = 20)
            advance(ticks = 2)

            assertEquals(CoordGrid(3376, 2848, 0), player.coords)
            assertEquals(32, player.hitpoints)
            assertEquals(0, player.vars[LEVEL])
        }

    @Test
    fun GameTestState.`the floor 3 block slides north every 13 ticks`() = runPyramidTest {
        val block = spawnNpc(NORTH_BLOCK_HOME, npcType(NORTH_BLOCK))

        val zs = mutableListOf<Int>()
        repeat(26) {
            advance()
            zs += block.coords.z
        }

        assertEquals(6, zs.count { it == 2847 })
        assertEquals(4, zs.count { it == 2846 })
        assertEquals(16, zs.count { it == 2845 })
    }

    @Test
    fun GameTestState.`the floor resets on the ground around the pyramid`() = runPyramidTest {
        setVarbit(LEVEL, 3)
        startAt(CoordGrid(3373, 2812, 0))
        assertEquals(3, player.vars[LEVEL])

        startAt(CoordGrid(3373, 2817, 0))

        assertEquals(0, player.vars[LEVEL])
    }

    @Test
    fun GameTestState.`simon buys the lot of artefacts at his prices`() = runPyramidTest {
        setVarbit(JOB, 1)
        repeat(3) { player.inv[it] = InvObj(COMB, 1) }
        player.inv[3] = InvObj(GOLD_STATUETTE, 1)
        player.inv[4] = InvObj(COINS, 5)
        val simon = spawnSimon()

        talkToMenu(simon)
        choose(1)
        finishDialogue()

        assertEquals(0, player.inv.count(COMB) + player.inv.count(GOLD_STATUETTE))
        assertEquals(5 + 3 * 50 + 1250, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`noted artefacts sell only after the easy desert diary`() = runPyramidTest {
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(NOTED_COMB, 10)
        val simon = spawnSimon()

        player.opNpc1(simon)
        advanceUntil({ player.ui.containsModal("interface.chat_right") })
        finishDialogue()
        assertEquals(10, player.inv.count(NOTED_COMB))
        assertEquals(0, player.inv.count(COINS))

        setVarbit("varbit.desert_diary_easy_complete", 1)
        talkToMenu(simon)
        choose(1)
        finishDialogue()

        assertEquals(0, player.inv.count(NOTED_COMB))
        assertEquals(500, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon keeps nothing when the artefacts' coins would overflow`() =
        runPyramidTest {
            setVarbit(JOB, 1)
            player.inv[0] = InvObj(COMB, 1)
            player.inv[1] = InvObj(COINS, Int.MAX_VALUE - 10)
            val simon = spawnSimon()

            talkToMenu(simon)
            choose(1)
            finishDialogue()

            assertEquals(1, player.inv.count(COMB))
            assertEquals(Int.MAX_VALUE - 10, player.inv.count(COINS))
        }

    @Test
    fun GameTestState.`simon pays nothing for artefacts dropped before the sale`() =
        runPyramidTest {
            setVarbit(JOB, 1)
            player.inv[0] = InvObj(COMB, 1)
            player.inv[1] = InvObj(STONE_SEAL, 1)
            val simon = spawnSimon()

            talkToMenu(simon)
            player.inv[0] = null
            choose(1)
            finishDialogue()

            assertEquals(0, player.inv.count(STONE_SEAL))
            assertEquals(150, player.inv.count(COINS))
        }

    @Test
    fun GameTestState.`an artefact used on simon sells just that one`() = runPyramidTest { deps ->
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(COMB, 1)
        player.inv[1] = InvObj(COMB, 1)
        val simon = spawnSimon()

        useOn(deps, simon, slot = 0)
        continueToMenu()
        choose(1)
        finishDialogue()

        assertEquals(1, player.inv.count(COMB))
        assertEquals(50, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`an artefact gone before the answer is not paid for`() = runPyramidTest {
        deps ->
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(COMB, 1)
        val simon = spawnSimon()

        useOn(deps, simon, slot = 0)
        continueToMenu()
        player.inv[0] = null
        choose(1)
        finishDialogue()

        assertEquals(0, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon asks for a chat before he buys an artefact`() = runPyramidTest {
        deps ->
        player.inv[0] = InvObj(COMB, 1)
        val simon = spawnSimon()

        useOn(deps, simon, slot = 0)

        assertTrue(player.ui.containsModal("interface.chat_left"))
        finishDialogue()
        assertEquals(1, player.inv.count(COMB))
        assertEquals(0, player.inv.count(COINS))
    }

    @Test
    fun GameTestState.`simon never buys the pharaoh's sceptre`() = runPyramidTest { deps ->
        setVarbit(JOB, 1)
        player.inv[0] = InvObj(SCEPTRE, 1)
        player.inv[1] = InvObj(COMB, 1)
        val simon = spawnSimon()

        talkToMenu(simon)
        choose(1)
        continueToMenu()
        choose(1)
        finishDialogue()

        assertEquals(1, player.inv.count(SCEPTRE))
        assertEquals(0, player.inv.count(COMB))
        assertEquals(50, player.inv.count(COINS))

        useOn(deps, simon, slot = 0)
        finishDialogue()

        assertEquals(1, player.inv.count(SCEPTRE))
        assertEquals(50, player.inv.count(COINS))
    }

    private fun GameTestScope.talkToMenu(simon: Npc) {
        player.opNpc1(simon)
        advanceUntil({ player.ui.containsModal("interface.chat_right") })
        continueToMenu()
    }

    private fun GameTestScope.continueToMenu() {
        repeat(MAX_DIALOGUE_STEPS) {
            if (player.ui.containsModal("interface.chatmenu")) {
                return
            }
            continueDialogue()
        }
        error("No menu came up.")
    }

    private fun GameTestScope.finishDialogue() {
        repeat(MAX_DIALOGUE_STEPS) {
            if (player.activeCoroutine == null) {
                player.ifClose()
                advance()
                return
            }
            check(!player.ui.containsModal("interface.chatmenu")) { "Stopped at a menu." }
            continueDialogue()
        }
        error("The dialogue did not end.")
    }

    private fun GameTestScope.useOn(deps: PyramidTestDeps, simon: Npc, slot: Int) {
        val obj = checkNotNull(player.inv[slot])
        val type = checkNotNull(ServerCacheManager.getItem(obj.id))
        player.withProtectedAccess {
            deps.npcU.interactOp(this, simon, inv, slot, simon.visType, type)
        }
        advance(ticks = 2)
    }

    private fun GameTestScope.takeStairs(loc: String): List<String> {
        val start = player.coords
        player.opLoc1(locNear(loc, start))
        val messages = mutableListOf<String>()
        repeat(10) {
            advance()
            messages += gameMessages()
            if (player.coords.level != start.level || player.coords.chebyshevDistance(start) > 8) {
                return messages
            }
        }
        error("$loc did not move the player from $start (at ${player.coords}): $messages")
    }

    private fun GameTestScope.climbRocks(): List<String> {
        player.opLoc1(locNear(ROCKS, player.coords))
        val messages = advanceCollecting(ticks = 4)
        player.ifClose()
        advance()
        return messages
    }

    private fun GameTestScope.spawnSimon(): Npc {
        startAt(SIMON_HOME.translate(1, 0))
        return spawnNpc(SIMON_HOME, npcType(SIMON))
    }

    private fun GameTestScope.useTopOn(
        deps: PyramidTestDeps,
        simon: Npc,
        slot: Int,
        advanceAfter: Boolean = true,
    ) {
        val top = checkNotNull(ServerCacheManager.getItem(GOLD_PYRAMID.asRSCM(RSCMType.OBJ)))
        player.withProtectedAccess {
            deps.npcU.interactOp(this, simon, inv, slot, simon.visType, top)
        }
        if (advanceAfter) {
            advance(ticks = 2)
        }
    }

    private fun GameTestScope.continueDialogue() {
        val component =
            DIALOGUE_CONTINUES.entries.first { player.ui.containsModal(it.key) }.value
        player.activeCoroutine?.resumeWith(ResumePauseButtonInput(component, -1))
        advance()
    }

    private fun GameTestScope.choose(option: Int) {
        val input = ResumePauseButtonInput("component.chatmenu:options", option)
        player.activeCoroutine?.resumeWith(input)
        advance()
    }

    private fun CollisionFlagMap.blocksPlayers(tile: CoordGrid): Boolean =
        this[tile.x, tile.z, tile.level] and CollisionFlag.BLOCK_PLAYERS != 0

    private fun npcType(npc: String) =
        checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))

    private fun GameTestState.runPyramidTest(body: GameTestScope.(PyramidTestDeps) -> Unit) =
        runInjectedGameTest(
            PyramidTestDeps::class,
            AgilityModule(),
            AgilityCourseScript::class,
            AgilityPyramidScript::class,
            SimonTempletonScript::class,
            PlayerHitScript::class,
        ) { deps ->
            advance()
            for (placement in PYRAMID_PLACEMENTS) {
                placeMapLoc(
                    placement.coords,
                    placement.loc,
                    LocShape[placement.shape],
                    LocAngle[placement.angle],
                )
            }
            try {
                body(deps)
            } finally {
                val npcs =
                    listOf(SIMON_HOME, EAST_BLOCK_HOME, NORTH_BLOCK_HOME).flatMap {
                        npcRepo.findAll(ZoneKey.from(it), 1).toList()
                    }
                npcs.distinct().forEach { npcRepo.del(it, Int.MAX_VALUE) }
            }
        }

    private companion object {
        const val STAIRS_UP = "loc.agility_pyramid_steps1"
        const val ROCKS = "loc.agility_pyramid_wall_rocks"
        const val DOORWAY = "loc.agility_pyramid_door_hotspot"
        const val GOLD_PYRAMID = "obj.agility_pyramid_gold_pyramid"
        const val COINS = "obj.coins"
        const val SIMON = "npc.agility_pyramid_simon"
        const val EAST_BLOCK = "npc.agility_pyramid_penny_waterfall_npc_east"
        const val NORTH_BLOCK = "npc.agility_pyramid_penny_waterfall_npc_north"
        const val LEVEL = "varbit.agility_pyramid_level"
        const val TOP = "varbit.agility_pyramid_top"
        const val JOB = "varbit.agility_pyramid_simon_job"
        const val JOB_DONE = "varbit.agility_pyramid_simon_job_done"
        const val COMB = "obj.ntk_ivory_comb"
        const val NOTED_COMB = "obj.cert_ntk_ivory_comb"
        const val STONE_SEAL = "obj.ntk_seal_stone"
        const val GOLD_STATUETTE = "obj.ntk_statuette_gold"
        const val SCEPTRE = "obj.pharaohs_sceptre"
        const val MAX_DIALOGUE_STEPS = 30

        val GROUND = CoordGrid(3355, 2830, 0)
        val BASE = CoordGrid(3364, 2830, 0)
        val SUMMIT = CoordGrid(3043, 4697, 3)
        val SIMON_HOME = CoordGrid(3346, 2826, 0)
        val EAST_BLOCK_HOME = CoordGrid(3372, 2847, 1)
        val NORTH_BLOCK_HOME = CoordGrid(3366, 2845, 3)

        val DIALOGUE_CONTINUES =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.objectbox" to "component.objectbox:universe",
                "interface.messagebox" to "component.messagebox:continue",
            )
    }
}
