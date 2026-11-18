package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.hat
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

data class DorgeshKaanTestDeps
@Inject
constructor(val parts: DorgeshKaanParts)

@Execution(ExecutionMode.SAME_THREAD)
class DorgeshKaanCourseTest {
    private class Talk(val messages: List<String>, val lines: List<String>)

    @Test
    fun GameTestState.`out by the pylons and back by the cables pays a leg of each`() =
        runDorgeshKaanTest { (parts) ->
            wearGrapple()
            val turgall = spawnTurgall()
            giveJob()
            val agility = agilityXp()
            val ranged = xp(RANGED)

            crossRoute(CoordGrid(2719, 5241, 3), GRAPPLE_SOUTH)
            takePart(parts, heavy = true)
            crossRoute(CoordGrid(2707, 5192, 3), AGILITY_NORTH)
            talk(parts, turgall, GOODBYE)

            val obstacles = 3 * PYLON_XP + AGILITY_ROUTE_XP
            assertEquals(agility + obstacles + AGILITY_LEG_BONUS, agilityXp())
            assertEquals(ranged + 3 * PYLON_XP + GRAPPLE_LEG_BONUS, xp(RANGED))
        }

    @Test
    fun GameTestState.`a second lap counts on`() = runDorgeshKaanTest { (parts) ->
        val turgall = spawnTurgall()
        giveJob()
        repeat(2) {
            takePart(parts, heavy = true)
            assertEquals(1, player.inv.count(POWERBOX))
            val talk = talk(parts, turgall, GOODBYE)
            assertTrue(talk.messages.any { "lap count is" in it })
            giveJob()
        }

        assertEquals(2, player.vars[LAPS])
    }

    @Test
    fun GameTestState.`the spanner is handed over once`() = runDorgeshKaanTest { (parts) ->
        val turgall = spawnTurgall()

        talk(parts, turgall, WHAT_HELP, OKAY)
        val second = talk(parts, turgall, WHAT_HELP, OKAY)

        assertEquals(1, player.inv.count(SPANNER))
        assertTrue(second.lines.any { it.startsWith("Excellent!") })
    }

    @Test
    fun GameTestState.`a full inventory gets no spanner`() = runDorgeshKaanTest { (parts) ->
        val turgall = spawnTurgall()
        player.fillInv()

        val talk = talk(parts, turgall, WHAT_HELP, OKAY)

        assertEquals(0, player.inv.count(SPANNER))
        assertTrue("You don't have enough inventory space." in talk.messages)
    }

    @Test
    fun GameTestState.`a same tick double use of the spanner gives one part`() =
        runDorgeshKaanTest { (parts) ->
            giveJob()

            player.withProtectedAccess { with(parts) { takePart(heavy = true) } }
            player.withProtectedAccess { with(parts) { takePart(heavy = true) } }
            advance()

            assertEquals(1, player.inv.count(POWERBOX))
        }

    @Test
    fun GameTestState.`the generator gives one part at a time`() = runDorgeshKaanTest { (parts) ->
        giveJob()

        takePart(parts, heavy = true)
        takePart(parts, heavy = false)

        assertEquals(1, player.inv.count(POWERBOX))
        assertEquals(0, player.inv.count(FUSE))
    }

    @Test
    fun GameTestState.`a full inventory takes no part and starts no return`() =
        runDorgeshKaanTest { (parts) ->
            giveJob()
            player.fillInv()

            val messages = takePart(parts, heavy = true)

            assertEquals(0, player.inv.count(POWERBOX))
            assertEquals(0, player.vars[RETURNING])
            assertEquals(listOf("You don't have enough inventory space."), messages)
        }

    @Test
    fun GameTestState.`a part dropped before the hand-in pays nothing`() =
        runDorgeshKaanTest { (parts) ->
            val turgall = spawnTurgall()
            giveJob()
            takePart(parts, heavy = true)
            player.inv[slotOf(POWERBOX)] = null
            val before = agilityXp()

            val talk = talk(parts, turgall, GOODBYE)

            assertTrue(GREETING in talk.lines)
            assertEquals(before, agilityXp())
            assertEquals(0, player.vars[LAPS])
            assertEquals(1, player.vars[RETURNING])
        }

    @Test
    fun GameTestState.`the hand-in takes only the part`() = runDorgeshKaanTest { (parts) ->
        val turgall = spawnTurgall()
        giveJob()
        takePart(parts, heavy = true)
        player.inv[10] = InvObj(SPANNER, 1)
        player.inv[11] = InvObj(COINS, 500)

        talk(parts, turgall, GOODBYE)

        assertEquals(0, player.inv.count(POWERBOX))
        assertEquals(1, player.inv.count(SPANNER))
        assertEquals(500, player.inv.count(COINS))
        assertEquals(0, player.vars[RETURNING])
    }

    @Test
    fun GameTestState.`a second part brings no second lap without a new trip`() =
        runDorgeshKaanTest { (parts) ->
            val turgall = spawnTurgall()
            giveJob()
            takePart(parts, heavy = true)
            player.inv[20] = InvObj(POWERBOX, 1)
            player.inv[21] = InvObj(FUSE, 1)

            talk(parts, turgall, GOODBYE)
            val after = agilityXp()
            val second = talk(parts, turgall, GOODBYE)

            assertTrue(GREETING in second.lines)
            assertEquals(after, agilityXp())
            assertEquals(1, player.vars[LAPS])
            assertEquals(2, player.inv.count(POWERBOX) + player.inv.count(FUSE))
        }

    @Test
    fun GameTestState.`turgall takes every broken part and nothing else`() =
        runDorgeshKaanTest { (parts) ->
            val turgall = spawnTurgall()
            player.inv[0] = InvObj(POWERBOX_BROKEN, 1)
            player.inv[1] = InvObj(FUSE_BROKEN, 1)
            player.inv[2] = InvObj(FUSE_BROKEN, 1)
            player.inv[3] = InvObj(FUSE, 1)

            val talk = talk(parts, turgall, GOODBYE)

            assertEquals(0, player.inv.count(POWERBOX_BROKEN) + player.inv.count(FUSE_BROKEN))
            assertEquals(1, player.inv.count(FUSE))
            assertTrue("Oh, look at those broken parts. Let me take them off you." in talk.lines)
        }

    @Test
    fun GameTestState.`a delicate part breaks on the cables`() = runDorgeshKaanTest { (parts) ->
        giveJob()
        takePart(parts, heavy = false)
        player.inv[5] = InvObj(POWERBOX, 1)
        startAt(CoordGrid(2721, 5241, 3))

        crossNear(CABLE, CoordGrid(2721, 5240, 3))

        assertEquals(0, player.inv.count(FUSE))
        assertEquals(1, player.inv.count(FUSE_BROKEN))
        assertEquals(1, player.inv.count(POWERBOX))
    }

    @Test
    fun GameTestState.`a heavy part breaks on a pylon`() = runDorgeshKaanTest { (parts) ->
        wearGrapple()
        giveJob()
        takePart(parts, heavy = true)
        startAt(CoordGrid(2719, 5241, 3))

        crossNear(PYLON, CoordGrid(2710, 5242, 3))

        assertEquals(0, player.inv.count(POWERBOX))
        assertEquals(1, player.inv.count(POWERBOX_BROKEN))
    }

    @Test
    fun GameTestState.`each swing takes the pylon that was clicked`() = runDorgeshKaanTest { _ ->
        wearGrapple()
        startAt(CoordGrid(2704, 5221, 3))

        crossNear(PYLON, CoordGrid(2704, 5224, 3))
        assertEquals(CoordGrid(2705, 5233, 3), player.coords)
        crossNear(PYLON, CoordGrid(2704, 5224, 3))
        assertEquals(CoordGrid(2705, 5223, 3), player.coords)
        crossNear(PYLON, CoordGrid(2703, 5212, 3))

        assertEquals(CoordGrid(2704, 5211, 3), player.coords)
    }

    @Test
    fun GameTestState.`teleporting out of the caves breaks every carried part`() =
        runDorgeshKaanTest { (parts) ->
            giveJob()
            takePart(parts, heavy = true)
            player.inv[5] = InvObj(FUSE, 1)
            startAt(CoordGrid(2720, 5230, 3))

            startAt(CoordGrid(2720, 5231, 3))
            assertEquals(1, player.inv.count(POWERBOX))

            startAt(CoordGrid(3222, 3218, 0))
            advance(ticks = 2)

            assertEquals(0, player.inv.count(POWERBOX) + player.inv.count(FUSE))
            assertEquals(1, player.inv.count(POWERBOX_BROKEN))
            assertEquals(1, player.inv.count(FUSE_BROKEN))
        }

    @Test
    fun GameTestState.`one pylon swing in 25 destroys a grapple`() = runDorgeshKaanTest { _ ->
        wearGrapple()
        startAt(CoordGrid(2719, 5241, 3))
        random.next = 0

        val messages = crossNear(PYLON, CoordGrid(2710, 5242, 3))

        assertEquals(GRAPPLES - 1, player.quiver?.count)
        assertTrue(GRAPPLE_BREAKS in messages)

        random.next = 1
        crossNear(PYLON, CoordGrid(2710, 5242, 3))

        assertEquals(GRAPPLES - 1, player.quiver?.count)
    }

    @Test
    fun GameTestState.`the last grapple breaks out of the quiver and nothing else goes`() =
        runDorgeshKaanTest { _ ->
            player.righthand = InvObj(CROSSBOW, 1)
            player.quiver = InvObj(GRAPPLE, 1)
            player.inv[0] = InvObj(COINS, 500)
            startAt(CoordGrid(2719, 5241, 3))
            random.next = 0

            val messages = crossNear(PYLON, CoordGrid(2710, 5242, 3))

            assertNull(player.quiver)
            assertEquals(CROSSBOW.asRSCM(RSCMType.OBJ), player.righthand?.id)
            assertEquals(500, player.inv.count(COINS))
            assertEquals(1, player.inv.occupiedSpace())
            assertTrue(GRAPPLE_BREAKS in messages)
        }

    @Test
    fun GameTestState.`a grapple taken off mid-swing is not broken`() = runDorgeshKaanTest { _ ->
        wearGrapple()
        startAt(CoordGrid(2719, 5241, 3))
        random.next = 0

        player.opLoc1(locNear(PYLON, CoordGrid(2710, 5242, 3)))
        advanceUntil({ player.vars[BUSY] != 0 }, timeoutTicks = 10)
        player.inv[0] = player.quiver
        player.quiver = null
        val messages = advanceCollecting(ticks = 12)

        assertEquals(0, player.vars[BUSY])
        assertEquals(GRAPPLES, player.inv.count(GRAPPLE))
        assertNull(player.quiver)
        assertTrue(GRAPPLE_BREAKS !in messages)
    }

    @Test
    fun GameTestState.`every carried delicate part breaks one for one`() =
        runDorgeshKaanTest { _ ->
            giveJob()
            player.fillInv(InvObj(BEER, 1))
            repeat(3) { player.inv[it] = InvObj(FUSE, 1) }
            player.inv[3] = InvObj(POWERBOX, 1)
            startAt(CoordGrid(2721, 5241, 3))

            crossNear(CABLE, CoordGrid(2721, 5240, 3))

            assertEquals(0, player.inv.count(FUSE))
            assertEquals(3, player.inv.count(FUSE_BROKEN))
            assertEquals(1, player.inv.count(POWERBOX))
            assertEquals(24, player.inv.count(BEER))
            assertEquals(28, player.inv.occupiedSpace())
        }

    @Test
    fun GameTestState.`a pylon swing starts from ten tiles away`() = runDorgeshKaanTest { _ ->
        wearGrapple()
        startAt(CoordGrid(2703, 5203, 3))

        crossNear(PYLON, CoordGrid(2703, 5212, 3))

        assertEquals(CoordGrid(2704, 5221, 3), player.coords)
    }

    @Test
    fun GameTestState.`the course needs a light source`() = runDorgeshKaanTest { _ ->
        player.hat = null
        startAt(CoordGrid(2721, 5241, 3))

        val messages = assertRefused(CABLE)

        val refusal = "It's too dark to see where you're going without a light source."
        assertEquals(listOf(refusal), messages)

        player.inv[0] = InvObj("obj.lit_candle", 1)
        crossNear(CABLE, CoordGrid(2721, 5240, 3))

        assertEquals(CoordGrid(2721, 5230, 3), player.coords)
    }

    @Test
    fun GameTestState.`the course turns away a follower`() = runDorgeshKaanTest { _ ->
        setVarp(FOLLOWER, PET.asRSCM(RSCMType.OBJ))
        startAt(CoordGrid(2721, 5241, 3))

        val messages = assertRefused(CABLE)

        assertEquals(listOf("You can't take a follower across these obstacles."), messages)
    }

    private fun GameTestScope.setVarp(varp: String, value: Int) {
        VarPlayerIntMapSetter.set(player, varp, value)
    }

    private fun GameTestScope.crossRoute(start: CoordGrid, route: List<Pair<String, CoordGrid>>) {
        startAt(start)
        for ((loc, coords) in route) {
            crossNear(loc, coords)
        }
    }

    private fun GameTestScope.takePart(parts: DorgeshKaanParts, heavy: Boolean): List<String> {
        player.withProtectedAccess { with(parts) { takePart(heavy) } }
        return gameMessages() + advanceCollecting(1)
    }

    private fun GameTestScope.talk(
        parts: DorgeshKaanParts,
        turgall: Npc,
        vararg options: Int,
    ): Talk {
        val messages = mutableListOf<String>()
        val lines = mutableListOf<String>()
        fun collect() {
            messages += gameMessages()
            lines += client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        }
        val choices = options.iterator()
        player.withProtectedAccess { with(parts) { talk(turgall) } }
        collect()
        repeat(MAX_DIALOGUE_STEPS) {
            val coroutine = player.activeCoroutine
            if (coroutine == null || !coroutine.isSuspended) {
                player.ifClose()
                advance()
                collect()
                return Talk(messages, lines)
            }
            if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                advance()
                collect()
                return@repeat
            }
            val input =
                if (player.ui.containsModal("interface.chatmenu")) {
                    check(choices.hasNext()) { "Out of options at a menu: $lines" }
                    ResumePauseButtonInput("component.chatmenu:options", choices.next())
                } else {
                    val parent =
                        CHAT_PARENTS.first { player.ui.containsModal("interface.$it") }
                    ResumePauseButtonInput("component.$parent:continue", -1)
                }
            coroutine.resumeWith(input)
            collect()
        }
        error("The dialogue did not end: $lines")
    }

    private fun GameTestScope.spawnTurgall(): Npc {
        val type = checkNotNull(ServerCacheManager.getNpc(TURGALL.asRSCM(RSCMType.NPC)))
        return spawnNpc(TURGALL_TILE, type)
    }

    private fun GameTestScope.giveJob() {
        setVarbit(PART_HEAVY, 1)
        setVarbit(PART_DELICATE, 2)
    }

    private fun GameTestScope.wearGrapple() {
        player.righthand = InvObj(CROSSBOW, 1)
        player.quiver = InvObj(GRAPPLE, GRAPPLES)
    }

    private fun GameTestScope.slotOf(obj: String): Int {
        val id = obj.asRSCM(RSCMType.OBJ)
        return player.inv.indices.first { player.inv[it]?.id == id }
    }

    private fun GameTestScope.xp(stat: String): Int = player.statMap.getFineXP(stat)

    private fun GameTestState.runDorgeshKaanTest(
        body: GameTestScope.(DorgeshKaanTestDeps) -> Unit
    ) =
        runInjectedGameTest(
            DorgeshKaanTestDeps::class,
            AgilityModule(),
            AgilityCourseScript::class,
            TurgallScript::class,
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
            setAgility(70)
            player.hat = InvObj(LIT_HELMET, 1)
            for (stat in listOf(RANGED, STRENGTH)) {
                player.statMap.setBaseLevel(stat, 70.toByte())
                player.statMap.setCurrentLevel(stat, 70.toByte())
            }
            blockMarks()
            body(deps)
        }

    private companion object {
        const val CABLE = "loc.dorgesh_cablebalance_start"
        const val ROPE = "loc.dorgesh_caves_rope_swing"
        const val LADDER = "loc.dorgesh_caves_monkeybars_end"
        const val WALL = "loc.dorgesh_cave_juttingwall"
        const val TUNNEL = "loc.dorgesh_caves_pipe_agility2"
        const val PYLON = "loc.dorgesh_pylon_grapple_base"
        const val TURGALL = "npc.dorgesh_male_engineer"
        const val SPANNER = "obj.dorgesh_spanner"
        const val POWERBOX = "obj.dorgesh_powerstation_powerbox"
        const val POWERBOX_BROKEN = "obj.dorgesh_powerstation_powerbox_broken"
        const val FUSE = "obj.dorgesh_powerstation_fuse"
        const val FUSE_BROKEN = "obj.dorgesh_powerstation_fuse_broken"
        const val CROSSBOW = "obj.xbows_crossbow_bronze"
        const val GRAPPLE = "obj.xbows_grapple_tip_bolt_mithril_rope"
        const val GRAPPLES = 50
        const val LIT_HELMET = "obj.cave_goblin_mining_helmet_lit"
        const val FOLLOWER = "varp.follower_obj"
        const val PET = "obj.skillpetagility"
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val LAPS = "varp.agility_laps_dorgesh_kaan"
        const val PART_HEAVY = "varbit.dorgesh_agility_part_heavy"
        const val PART_DELICATE = "varbit.dorgesh_agility_part_delicate"
        const val RETURNING = "varbit.dorgesh_agility_course_direction"
        const val RANGED = "stat.ranged"
        const val STRENGTH = "stat.strength"

        const val MAX_DIALOGUE_STEPS = 60
        const val WHAT_HELP = 1
        const val OKAY = 1
        const val GOODBYE = 4

        const val AGILITY_ROUTE_XP = 1590
        const val AGILITY_LEG_BONUS = 12160
        const val PYLON_XP = 180
        const val GRAPPLE_LEG_BONUS = 5710

        const val GRAPPLE_BREAKS = "Your grapple breaks as you land."
        const val GREETING = "Oh! Hello, surface-dweller. Do you think you could help me?"

        val TURGALL_TILE = CoordGrid(2741, 5241, 2)
        val CHAT_PARENTS = listOf("chat_left", "chat_right", "messagebox")

        val AGILITY_NORTH =
            listOf(
                LADDER to CoordGrid(2708, 5192, 3),
                CABLE to CoordGrid(2720, 5195, 3),
                TUNNEL to CoordGrid(2721, 5206, 3),
                WALL to CoordGrid(2721, 5214, 3),
                ROPE to CoordGrid(2728, 5215, 3),
                LADDER to CoordGrid(2740, 5215, 3),
                ROPE to CoordGrid(2729, 5229, 3),
                CABLE to CoordGrid(2721, 5231, 3),
            )

        val GRAPPLE_SOUTH =
            listOf(
                PYLON to CoordGrid(2710, 5242, 3),
                PYLON to CoordGrid(2704, 5224, 3),
                PYLON to CoordGrid(2703, 5212, 3),
            )
    }
}
