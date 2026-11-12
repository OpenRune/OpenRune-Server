package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.feet
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class AgilityShortcutsTest {
    @Test
    fun GameTestState.`a telejump lands the player on the tick it plays`() = runShortcutTest {
        setAgility(99)
        startAt(CoordGrid(1648, 10009, 0))

        val ticks = crossShortcut(CATACOMBS_CRACK)

        assertEquals(CoordGrid(1648, 10009, 0), ticks[0].coords)
        assertEquals(listOf("You squeeze through the narrow tunnel."), ticks[0].messages)
        assertEquals(CoordGrid(1646, 10000, 0), ticks[1].coords)
    }

    @Test
    fun GameTestState.`an exact move glides the player and the xp waits for its tick`() =
        runShortcutTest {
            setAgility(99)
            startAt(CoordGrid(2552, 3561, 0))
            val before = agilityXp()

            val ticks = crossShortcut(BARBARIAN_PIPE)

            assertEquals(CoordGrid(2552, 3558, 0), ticks[0].coords)
            assertEquals(before, ticks[3].xp)
            assertEquals(before + 100, ticks[4].xp)
        }

    @Test
    fun GameTestState.`a mesbox tick shows its line in a box that closes with the lane`() =
        runShortcutTest {
            setAgility(99)
            startAt(CoordGrid(2991, 9547, 0))

            val ticks = crossShortcut(TITANS_TUNNEL)

            assertEquals(listOf(SCRAMBLE), ticks[0].messages)
            assertTrue(ticks[0].mesbox)
            assertEquals(CoordGrid(2969, 9551, 0), ticks[2].coords)
            assertEquals(listOf(SCRAMBLE), ticks[2].messages)
            assertFalse(ticks.last().busy)
            assertFalse(ticks.last().mesbox)
        }

    @Test
    fun GameTestState.`below its level a shortcut refuses`() = runShortcutTest {
        setAgility(42)
        startAt(CoordGrid(2886, 3683, 0))

        val messages = refuseShortcut(TROLL_ROCKS)

        assertEquals(listOf("You need an Agility level of 43 to do that."), messages)
        assertTrue(player.ui.containsModal("interface.messagebox"))
    }

    @Test
    fun GameTestState.`a level refusal with an obj shows it in an objbox`() = runShortcutTest {
        setAgility(81)
        startAt(CoordGrid(3013, 9550, 0))

        val messages = refuseShortcut(WYVERN_TUNNEL)

        val refusal =
            "The tunnel is very dark and dangerous. You'll need an Agility level of 82 to try this."
        assertEquals(listOf(refusal), messages)
        assertTrue(player.ui.containsModal("interface.objectbox"))
    }

    @Test
    fun GameTestState.`a boosted level opens a shortcut`() = runShortcutTest {
        player.statMap.setBaseLevel("stat.agility", 40)
        player.statMap.setCurrentLevel("stat.agility", 43)
        startAt(CoordGrid(2886, 3683, 0))

        val ticks = crossShortcut(TROLL_ROCKS)

        assertEquals(CoordGrid(2884, 3683, 0), ticks.last().coords)
    }

    @Test
    fun GameTestState.`rocks that need climbing boots refuse bare feet`() = runShortcutTest {
        setAgility(99)
        startAt(CoordGrid(2820, 3635, 0))

        val refused = refuseShortcut(DEATH_PLATEAU_ROCKS)
        player.feet = InvObj("obj.death_climbingboots", 1)
        random.next = 0
        val ticks = crossShortcut(DEATH_PLATEAU_ROCKS)

        assertEquals(listOf("You need the right equipment to do that."), refused)
        assertEquals(CoordGrid(2822, 3635, 0), ticks.last().coords)
    }

    @Test
    fun GameTestState.`a grapple shortcut needs its gear and then its stat levels`() =
        runShortcutTest {
            setAgility(99)
            startAt(CoordGrid(2874, 3142, 0))

            val noGear = refuseShortcut(JUNGLE_TREE)
            player.righthand = InvObj("obj.xbows_crossbow_bronze", 1)
            player.quiver = InvObj("obj.xbows_grapple_tip_bolt_mithril_rope", 1)
            val noRanged = refuseShortcut(JUNGLE_TREE)
            setStat("stat.ranged", 42)
            setStat("stat.strength", 21)
            val ticks = crossShortcut(JUNGLE_TREE)

            val grapple = "You need a mithril grapple-tipped bolt with a rope to do that."
            assertEquals(listOf(grapple), noGear)
            assertEquals(listOf("You need a Ranged level of 42 to do that."), noRanged)
            assertEquals(CoordGrid(2874, 3136, 0), ticks[3].coords)
        }

    @Test
    fun GameTestState.`a varbit gate keeps a shortcut shut until it is set`() = runShortcutTest {
        setAgility(99)
        startAt(CoordGrid(2899, 9902, 0))

        val refused = refuseShortcut(HEROES_GUILD_CREVICE)
        setVarbit("varbit.falador_diary_hard_complete", 1)
        val ticks = crossShortcut(HEROES_GUILD_CREVICE)

        assertEquals(listOf("You can't use this shortcut yet."), refused)
        assertEquals(CoordGrid(2915, 9894, 0), ticks.last().coords)
    }

    @Test
    fun GameTestState.`a failed log leaves the player where they stood and hurts`() =
        runShortcutTest(PlayerHitScript::class) {
            setAgility(99)
            setShortcutHitpoints(20)
            startAt(CoordGrid(2602, 3336, 0))
            val before = agilityXp()
            random.next = 100
            random.then = 1

            val ticks = crossShortcut(ARDOUGNE_LOG)
            advance(ticks = 2)

            assertEquals(CoordGrid(2602, 3336, 0), player.coords)
            assertEquals(before + 20, agilityXp())
            assertEquals(19, player.hitpoints)
            assertTrue(ticks.none { it.coords.x < 2602 })
        }

    private fun GameTestScope.setStat(stat: String, level: Int) {
        player.statMap.setBaseLevel(stat, level.toByte())
        player.statMap.setCurrentLevel(stat, level.toByte())
    }

    private companion object {
        const val TROLL_ROCKS = "loc.troll_mountain_shortcut_climbingrocks1"
        const val ARDOUGNE_LOG = "loc.ardougne_log_balance_right_sc"
        const val CATACOMBS_CRACK = "loc.zeah_cata_crack"
        const val BARBARIAN_PIPE = "loc.agility_obstical_pipe_barbarian"
        const val DEATH_PLATEAU_ROCKS = "loc.troll_climbingrocks"
        const val HEROES_GUILD_CREVICE = "loc.heroes_guild_shortcut_to_fountain"
        const val TITANS_TUNNEL = "loc.cavewall_shortcut_royal_titans_east"
        const val WYVERN_TUNNEL = "loc.cavewall_shortcut_wyvern_west"
        const val JUNGLE_TREE = "loc.xbows_jungletree_karamja_basic"
        const val SCRAMBLE = "You scramble through the dark and dangerous tunnel..."
    }
}
