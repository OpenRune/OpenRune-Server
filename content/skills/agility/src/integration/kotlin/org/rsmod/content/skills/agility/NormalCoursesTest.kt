package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class NormalCoursesTest {
    private class Course(
        val name: String,
        val start: CoordGrid,
        val lap: List<String>,
        val xpTenths: Int,
        val lapColour: String = "ff0000",
    )

    private fun GameTestScope.runLap(course: Course): List<String> {
        setAgility(99)
        blockMarks()
        startAt(course.start)
        advance()
        val before = agilityXp()

        val messages = crossAll(course.lap)

        assertEquals(before + course.xpTenths, agilityXp())
        val lapCount = messages.last { "lap count is" in it }
        val expected = "Your ${course.name} lap count is: <col=${course.lapColour}>1</col>."
        assertEquals(expected, lapCount)
        return messages
    }

    @Test
    fun GameTestState.`a failed obstacle can be retried without losing the lap`() =
        runAgilityTest(PlayerHitScript::class) {
            setAgility(99)
            setHitpoints(99)
            blockMarks()
            startAt(WILDERNESS_START)
            advance()
            crossAll(WILDERNESS_LAP.take(2))

            random.next = 100
            random.then = 1
            player.opLoc1(locNear("loc.steppingstone1", player.coords))
            advance(ticks = 16)
            assertEquals(CoordGrid(3002, 3963, 0), player.coords)
            assertEquals(79, player.hitpoints)

            random.next = 0
            val messages = crossAll(WILDERNESS_LAP.drop(2))

            assertTrue(messages.any { "lap count is" in it })
        }

    @Test
    fun GameTestState.`ape atoll refuses a boosted level`() = runAgilityTest {
        player.righthand = InvObj(GREEGREE, 1)
        player.statMap.setBaseLevel("stat.agility", 47.toByte())
        player.statMap.setCurrentLevel("stat.agility", 50.toByte())
        startAt(APE_ATOLL_START)
        advance()

        val messages = refusal("loc.100_ilm_stepping_stone")

        assertEquals(listOf("Your Agility boost won't help you here."), messages)
    }

    @Test
    fun GameTestState.`werewolf lap ends with the stick handed to the trainer`() =
        runAgilityTest {
            setAgility(99)
            blockMarks()
            startAt(WEREWOLF_START)
            advance()
            val before = agilityXp()

            crossWerewolfCourse()
            player.inv[0] = InvObj(STICK, 1)
            val messages = handInStick()

            assertEquals(before + 7300, agilityXp())
            assertEquals("You give the stick to the werewolf.", messages.first())
            val lap = "Your Werewolf Agility lap count is: <col=ff0000>1</col>."
            assertEquals(lap, messages.last())
        }

    @Test
    fun GameTestState.`the trainer takes every stick for one hand-in`() = runAgilityTest {
        setAgility(99)
        player.inv[0] = InvObj(STICK, 1)
        player.inv[1] = InvObj(STICK, 1)
        val before = agilityXp()

        handInStick(clicks = 2)
        handInStick()

        assertEquals(before + HAND_IN_XP, agilityXp())
        assertEquals(0, player.inv.count(STICK))
    }

    @Test
    fun GameTestState.`the trainer pays nothing without a stick`() = runAgilityTest {
        setAgility(99)
        val before = agilityXp()

        val messages = handInStick()

        assertEquals(listOf(NO_STICK), messages)
        assertEquals(before, agilityXp())
    }

    @Test
    fun GameTestState.`the hand-in takes the sticks and nothing else`() = runAgilityTest {
        setAgility(99)
        player.inv[0] = InvObj(STICK, 1)
        player.inv[1] = InvObj(COINS, 500)
        player.inv[2] = InvObj(SHARK, 1)
        player.inv[3] = InvObj(STICK, 1)
        val before = agilityXp()

        handInStick()

        assertEquals(0, player.inv.count(STICK))
        assertEquals(500, player.inv.count(COINS))
        assertEquals(1, player.inv.count(SHARK))
        assertEquals(2, player.inv.occupiedSpace())
        assertEquals(before + HAND_IN_XP, agilityXp())
    }

    @Test
    fun GameTestState.`a stick gone before the trainer answers pays nothing`() = runAgilityTest {
        setAgility(99)
        player.inv[0] = InvObj(STICK, 1)
        val before = agilityXp()

        val messages = handInStick { player.inv[0] = null }

        assertEquals(listOf(NO_STICK), messages)
        assertEquals(before, agilityXp())
        assertEquals(0, player.inv.occupiedSpace())
    }

    private fun GameTestScope.crossWerewolfCourse() {
        WEREWOLF_STONES.forEach { crossNear("loc.werewolf_steping_stone", it) }
        crossAll(
            listOf(
                "loc.werewolf_hurdle_mid",
                "loc.werewolf_hurdle_mid",
                "loc.werewolf_hurdle_mid",
                "loc.waa_pipe",
                "loc.werewolf_skull_climb_1",
                "loc.werewolf_slide_center",
            )
        )
        assertEquals(CoordGrid(3528, 9873, 0), player.coords)
    }

    private fun GameTestScope.handInStick(
        clicks: Int = 1,
        afterClick: () -> Unit = {},
    ): List<String> {
        startAt(STICK_TRAINER.translate(0, 1))
        val trainer = spawnNpc(STICK_TRAINER, npcType("npc.werewolf_trainer_stick"))
        repeat(clicks) { player.opNpc1(trainer) }
        afterClick()
        val messages = advanceCollecting(ticks = 4)
        npcRepo.del(trainer, Int.MAX_VALUE)
        return messages
    }

    @Test
    fun GameTestState.`shayzien advanced lap carries the shared start over`() = runAgilityTest {
        player.righthand = InvObj(CROSSBOW, 1)
        player.quiver = InvObj(MITHRIL_GRAPPLE, 1)
        runLap(SHAYZIEN_ADVANCED)
    }

    @Test
    fun GameTestState.`colossal wyrm advanced lap pays its own zipline`() = runAgilityTest {
        runLap(WYRM_ADVANCED)
    }

    private fun GameTestScope.refusal(loc: String): List<String> {
        val before = agilityXp()
        player.opLoc1(locNear(loc, player.coords))
        val messages = mutableListOf<String>()
        repeat(10) {
            advance()
            messages += gameMessages()
            assertEquals(0, player.vars[BUSY])
        }
        assertEquals(before, agilityXp())
        return messages
    }

    private fun GameTestScope.setHitpoints(level: Int) {
        player.statMap.setBaseLevel("stat.hitpoints", level.toByte())
        player.statMap.setCurrentLevel("stat.hitpoints", level.toByte())
    }

    private fun npcType(npc: String) =
        checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))

    private companion object {
        const val GREEGREE = "obj.mm_monkey_greegree_for_small_ninja_monkey"
        const val CROSSBOW = "obj.xbows_crossbow_bronze"
        const val MITHRIL_GRAPPLE = "obj.xbows_grapple_tip_bolt_mithril_rope"
        const val STICK = "obj.waa_stick"
        const val COINS = "obj.coins"
        const val SHARK = "obj.shark"
        const val HAND_IN_XP = 3800
        const val NO_STICK = "You don't have a stick to give the trainer."
        val STICK_TRAINER = CoordGrid(3528, 9865, 0)
        val WEREWOLF_START = CoordGrid(3538, 9873, 0)
        val WEREWOLF_STONES =
            listOf(
                CoordGrid(3538, 9875, 0),
                CoordGrid(3538, 9877, 0),
                CoordGrid(3540, 9877, 0),
                CoordGrid(3540, 9879, 0),
                CoordGrid(3540, 9881, 0),
            )
        val APE_ATOLL_START = CoordGrid(2756, 2742, 0)
        val WILDERNESS_START = CoordGrid(3004, 3937, 0)
        val WILDERNESS_LAP =
            listOf(
                "loc.obstical_pipe2",
                "loc.obstical_ropeswing2",
                "loc.steppingstone1",
                "loc.wilderness_log_balance1",
                "loc.wildclimbingrock",
            )

        private val SHAYZIEN_START =
            listOf(
                "loc.shayzien_agility_both_start_ladder",
                "loc.shayzien_agility_both_rope_climb",
                "loc.shayzien_agility_both_rope_walk",
            )

        /** The shared start pays the basic course's xp: the ladder can't tell the courses apart. */
        val SHAYZIEN_ADVANCED =
            Course(
                name = "Shayzien Advanced Agility Course",
                start = CoordGrid(1554, 3630, 0),
                lap =
                    SHAYZIEN_START +
                        listOf(
                            "loc.shayzien_agility_up_swing_jump_1",
                            "loc.shayzien_agility_up_jump_platform_1",
                            "loc.shayzien_agility_up_jump_platform_2",
                            "loc.shayzien_agility_up_swing_jump_2",
                            "loc.shayzien_agility_up_end_jump",
                        ),
                xpTenths = 5075,
            )

        val WYRM_ADVANCED =
            Course(
                name = "Colossal Wyrm Agility Course (Advanced)",
                start = CoordGrid(1653, 2931, 0),
                lap =
                    listOf(
                        "loc.varlamore_wyrm_agility_start_ladder_trigger",
                        "loc.varlamore_wyrm_agility_balance_1_trigger",
                        "loc.varlamore_wyrm_agility_advanced_ladder_1_trigger",
                        "loc.varlamore_wyrm_agility_advanced_jump_1_trigger",
                        "loc.varlamore_wyrm_agility_advanced_balance_1_trigger",
                        "loc.varlamore_wyrm_agility_end_zipline_trigger",
                    ),
                xpTenths = 10536,
                lapColour = "ff3045",
            )
    }
}
