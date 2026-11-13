package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.agility.arena.ArenaDispensers
import org.rsmod.content.skills.agility.arena.ArenaPlanks
import org.rsmod.content.skills.agility.arena.ArenaTrapScript
import org.rsmod.content.skills.agility.arena.BrimhavenArenaScript
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class BrimhavenArenaTest {
    class ArenaTestDeps
    @Inject
    constructor(
        val dispensers: ArenaDispensers,
        val objRepo: ObjRepository,
        val planks: ArenaPlanks,
        val locRepo: LocRepository,
    )

    private fun GameTestState.runArenaTest(body: GameTestScope.(ArenaTestDeps) -> Unit) =
        runInjectedGameTest(
            ArenaTestDeps::class,
            AgilityModule(),
            AgilityCourseScript::class,
            BrimhavenArenaScript::class,
            ArenaTrapScript::class,
            PlayerHitScript::class,
        ) { deps ->
            random.next = deps.dispensers.dispensers.indexOf(TAG_DISPENSER)
            advance()
            player.observerUUID = player.uuid
            for (placement in ARENA_PLACEMENTS) {
                placeMapLoc(
                    placement.coords,
                    placement.loc,
                    LocShape[placement.shape],
                    LocAngle[placement.angle],
                )
            }
            body(deps)
        }

    private fun GameTestScope.arenaLoc(loc: String, coords: CoordGrid): BoundLocInfo {
        val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
        val info = findLocs(coords).first { it.coords == coords && it.id == type.id }
        return BoundLocInfo(info, type)
    }

    private fun GameTestScope.click(loc: String, coords: CoordGrid, ticks: Int): List<String> {
        player.opLoc1(arenaLoc(loc, coords))
        return advanceCollecting(ticks)
    }

    private fun GameTestScope.setHitpoints(level: Int) {
        player.statMap.setBaseLevel("stat.hitpoints", level.toByte())
        player.statMap.setCurrentLevel("stat.hitpoints", level.toByte())
    }

    private fun GameTestScope.count(obj: String): Int = player.inv.count(obj)

    private fun GameTestScope.groundCount(deps: ArenaTestDeps, obj: String): Int {
        val type = obj.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(player.coords).filter { it.type == type }.sumOf { it.count }
    }

    @Test
    fun GameTestState.`izzy takes the fee and the ladder lets the player down`() =
        runArenaTest {
            startAt(CoordGrid(2807, 3192, 0))
            player.inv[0] = InvObj(COINS, 500)
            val izzy = spawnNpc(CoordGrid(2807, 3191, 0), npcType("npc.agilityarena_clerk"))
            player.opNpc3(izzy)
            val messages = advanceCollecting(ticks = 4)
            npcRepo.del(izzy, Int.MAX_VALUE)

            assertTrue("You give Cap'n Izzy the 200 coin entrance fee." in messages)
            assertEquals(300, count(COINS))
            assertEquals(1, player.vars[CAN_ENTER])

            startAt(CoordGrid(2809, 3193, 0))
            click(LADDER_DOWN, CoordGrid(2809, 3194, 0), ticks = 4)
            assertEquals(CoordGrid(2805, 9591, 3), player.coords)
            assertEquals(2, player.vars[HIGHLIGHTED])
        }

    @Test
    fun GameTestState.`the ladder keeps out a player who has not paid`() = runArenaTest {
        startAt(CoordGrid(2809, 3193, 0))
        click(LADDER_DOWN, CoordGrid(2809, 3194, 0), ticks = 4)
        assertEquals(CoordGrid(2809, 3193, 0), player.coords)
    }

    @Test
    fun GameTestState.`leaving keeps the player out for a minute`() = runArenaTest {
        startAt(CoordGrid(2805, 9589, 3))
        click("loc.agilityarena_ladderup", CoordGrid(2805, 9590, 3), ticks = 4)
        assertEquals(CoordGrid(2810, 3194, 0), player.coords)
        assertEquals(0, player.vars[CAN_ENTER])

        setVarbit(CAN_ENTER, 1)
        startAt(CoordGrid(2809, 3193, 0))
        click(LADDER_DOWN, CoordGrid(2809, 3194, 0), ticks = 4)
        assertEquals(CoordGrid(2809, 3193, 0), player.coords)

        advance(ticks = 100)
        click(LADDER_DOWN, CoordGrid(2809, 3194, 0), ticks = 4)
        assertEquals(CoordGrid(2805, 9591, 3), player.coords)
    }

    @Test
    fun GameTestState.`tagging the dispenser under the arrow pays once`() = runArenaTest {
        setAgility(76)
        startAt(TAG_TILE)
        val xp = agilityXp()

        val messages = click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertTrue("Your Agility Arena lap count is: <col=ff0000>1</col>." in messages)
        assertTrue(
            "<col=ef1020>${player.displayName}</col> was the first to tag the pillar!" in messages
        )
        assertEquals(1, count(TICKET))
        assertEquals(1, count(VOUCHER))
        assertEquals(xp + 2100, agilityXp())

        val again = click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertTrue(
            "You can only get one ticket at a time, wait till the arrow moves again." in again
        )
        assertEquals(1, count(TICKET))
        assertEquals(1, count(VOUCHER))
    }

    @Test
    fun GameTestState.`tags on one tick pay once`() = runArenaTest {
        startAt(TAG_TILE)
        val dispenser = arenaLoc(TAG_LOC, TAG_DISPENSER)
        repeat(3) { player.opLoc1(dispenser) }
        advanceCollecting(ticks = 3)
        assertEquals(1, count(TICKET))
        assertEquals(1, count(VOUCHER))
    }

    @Test
    fun GameTestState.`only the dispenser under the arrow pays`() = runArenaTest {
        startAt(CoordGrid(2762, 9590, 3))
        val messages = click(TAG_LOC, CoordGrid(2761, 9590, 3), ticks = 3)
        assertTrue(
            "You can only get a ticket when the flashing arrow is above the pillar." in messages
        )
        assertEquals(0, count(TICKET))
    }

    @Test
    fun GameTestState.`a full inventory drops the tag's ticket and voucher`() = runArenaTest {
        deps ->
        startAt(TAG_TILE)
        player.fillInv()
        val held = player.inv.occupiedSpace()

        click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertEquals(held, player.inv.occupiedSpace())
        assertEquals(1, groundCount(deps, TICKET))
        assertEquals(1, groundCount(deps, VOUCHER))
    }

    @Test
    fun GameTestState.`a full stack of tickets drops the new one`() = runArenaTest { deps ->
        startAt(TAG_TILE)
        player.inv[0] = InvObj(TICKET, Int.MAX_VALUE)

        click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertEquals(Int.MAX_VALUE, count(TICKET))
        assertEquals(1, groundCount(deps, TICKET))
        assertEquals(1, count(VOUCHER))
    }

    private fun GameTestScope.stepOnto(from: CoordGrid, onto: CoordGrid, ticks: Int) {
        startAt(from)
        player.moveGameClick(onto)
        advance(ticks)
    }

    @Test
    fun GameTestState.`spinning blades are dived across`() = runArenaTest {
        setAgility(99)
        val xp = agilityXp()
        random.next = 0
        stepOnto(CoordGrid(2780, 9557, 3), CoordGrid(2779, 9557, 3), ticks = 6)
        assertEquals(CoordGrid(2776, 9557, 3), player.coords)
        assertEquals(xp + 280, agilityXp())
    }

    @Test
    fun GameTestState.`spinning blades throw a failing player back and hurt`() =
        runArenaTest {
            setAgility(40)
            setHitpoints(93)
            val xp = agilityXp()
            random.next = 255
            stepOnto(CoordGrid(2783, 9571, 3), CoordGrid(2783, 9573, 3), ticks = 6)
            assertEquals(CoordGrid(2783, 9572, 3), player.coords)
            assertEquals(93 - 6, player.hitpoints)
            assertEquals(xp, agilityXp())
        }

    @Test
    fun GameTestState.`darts are ducked under`() = runArenaTest {
        setAgility(99)
        val xp = agilityXp()
        random.next = 0
        stepOnto(CoordGrid(2779, 9568, 3), CoordGrid(2778, 9568, 3), ticks = 12)
        assertEquals(CoordGrid(2776, 9568, 3), player.coords)
        assertEquals(xp + 300, agilityXp())
    }

    @Test
    fun GameTestState.`darts drain agility and hurt`() = runArenaTest {
        setAgility(77)
        setHitpoints(93)
        random.next = 255
        stepOnto(CoordGrid(2779, 9568, 3), CoordGrid(2778, 9568, 3), ticks = 6)
        assertEquals(CoordGrid(2779, 9568, 3), player.coords)
        assertEquals(75, player.statMap.getCurrentLevel("stat.agility").toInt())
        assertEquals(93 - 6, player.hitpoints)
    }

    @Test
    fun GameTestState.`floor spikes and pressure pads are dived across`() = runArenaTest {
        setAgility(99)
        val xp = agilityXp()
        stepOnto(CoordGrid(2772, 9553, 3), CoordGrid(2772, 9552, 3), ticks = 5)
        assertEquals(CoordGrid(2772, 9550, 3), player.coords)
        assertEquals(xp + 240, agilityXp())

        stepOnto(CoordGrid(2798, 9557, 3), CoordGrid(2799, 9557, 3), ticks = 5)
        assertEquals(CoordGrid(2801, 9557, 3), player.coords)
        assertEquals(xp + 240 + 260, agilityXp())
    }

    @Test
    fun GameTestState.`a rising saw blade throws the player off and hurts`() = runArenaTest {
        setHitpoints(95)
        startAt(CoordGrid(2788, 9579, 3))
        advance(ticks = 9)
        assertEquals(CoordGrid(2787, 9579, 3), player.coords)
        assertEquals(95 - 6, player.hitpoints)
    }

    private fun GameTestScope.layPlankBoards() {
        for (z in WEST_PLANK_ROWS) {
            for (x in 2765..2768) {
                placeMapLoc(CoordGrid(x, z, 3), PLANK_MIDDLE, LocShape.GroundDecor)
            }
        }
    }

    private fun GameTestScope.keepWholeWestPlank(deps: ArenaTestDeps, row: Int) {
        random.next = WEST_PLANK_ROWS.indexOf(row)
        deps.planks.reroll()
    }

    private fun GameTestScope.breaks(deps: ArenaTestDeps, start: CoordGrid): Boolean {
        var breaks = false
        player.withProtectedAccess { breaks = deps.planks.breaksUnder(this, start) }
        return breaks
    }

    @Test
    fun GameTestState.`each arrow move keeps one plank of each set whole`() = runArenaTest { deps ->
        layPlankBoards()
        setAgility(1)
        val broken = BROKEN_PLANK.asRSCM(RSCMType.LOC)
        repeat(10) {
            deps.planks.reroll()
            for (set in PLANK_STARTS) {
                assertEquals(1, set.count { !breaks(deps, it) })
            }
            val whole = WEST_PLANK_ROWS.single { !breaks(deps, CoordGrid(2770, it, 3)) }
            for (z in WEST_PLANK_ROWS) {
                val boards =
                    (2765..2768).map { x ->
                        deps.locRepo.findExact(CoordGrid(x, z, 3), LocShape.GroundDecor)?.id
                    }
                assertEquals(z != whole, broken in boards, "row $z")
            }
        }
    }

    @Test
    fun GameTestState.`a broken plank drops the player below the pillar and hurts`() =
        runArenaTest { deps ->
            setAgility(50)
            setHitpoints(93)
            keepWholeWestPlank(deps, row = 9556)
            startAt(CoordGrid(2770, 9557, 3))
            val xp = agilityXp()

            click("loc.agilityarena_plank", CoordGrid(2769, 9557, 3), ticks = 12)

            assertEquals(CoordGrid(2770, 9557, 0), player.coords)
            assertEquals(93 - 6, player.hitpoints)
            assertEquals(xp, agilityXp())
        }

    @Test
    fun GameTestState.`the whole plank is crossed`() = runArenaTest { deps ->
        setAgility(50)
        keepWholeWestPlank(deps, row = 9556)
        startAt(CoordGrid(2770, 9556, 3))
        val xp = agilityXp()

        click("loc.agilityarena_plank2", CoordGrid(2769, 9556, 3), ticks = 12)

        assertEquals(CoordGrid(2763, 9556, 3), player.coords)
        assertEquals(xp + 60, agilityXp())
    }

    @Test
    fun GameTestState.`99 agility crosses a broken plank`() = runArenaTest { deps ->
        setAgility(99)
        keepWholeWestPlank(deps, row = 9556)
        startAt(CoordGrid(2770, 9557, 3))

        assertFalse(breaks(deps, CoordGrid(2770, 9557, 3)))
        click("loc.agilityarena_plank", CoordGrid(2769, 9557, 3), ticks = 12)

        assertEquals(CoordGrid(2763, 9557, 3), player.coords)
    }

    @Test
    fun GameTestState.`a tag soon after a hop locks the dispensers`() = runArenaTest {
        setVarp(LAST_TAG, currentSecond() - 10)
        startAt(TAG_TILE)

        val messages = click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertTrue(HOP_LOCK in messages)
        assertEquals(0, count(TICKET))
        assertTrue(player.vars[TAG_LOCK] > currentSecond())

        setVarp(LAST_TAG, 0)
        val again = click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertTrue(HOP_LOCK in again)
        assertEquals(0, count(TICKET))
    }

    @Test
    fun GameTestState.`a tag a minute after the last one pays`() = runArenaTest {
        setVarp(LAST_TAG, currentSecond() - 120)
        startAt(TAG_TILE)

        click(TAG_LOC, TAG_DISPENSER, ticks = 3)

        assertEquals(1, count(TICKET))
        assertTrue(player.vars[LAST_TAG] >= currentSecond() - 1)
    }

    private fun GameTestScope.setVarp(varp: String, value: Int) {
        VarPlayerIntMapSetter.set(player, varp, value)
    }

    private fun currentSecond(): Int = (System.currentTimeMillis() / 1000).toInt()

    private fun npcType(npc: String) =
        checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))

    private companion object {
        const val COINS = "obj.coins"
        const val TICKET = "obj.agilityarena_ticket_new"
        const val VOUCHER = "obj.agilityarena_voucher"
        const val CAN_ENTER = "varbit.agilityarena_canenter"
        const val HIGHLIGHTED = "varbit.helper_agility_highlighted_course"
        const val LADDER_DOWN = "loc.agilityarena_ladderdown"
        const val TAG_LOC = "loc.agility_ticketpillar"
        const val PLANK_MIDDLE = "loc.agilityarena_plank_middle"
        const val BROKEN_PLANK = "loc.agilityarena_plank_broke1"
        const val LAST_TAG = "varp.agility_arena_last_tag"
        const val TAG_LOCK = "varp.agility_arena_tag_lock"
        const val HOP_LOCK = "You can't get another ticket so soon after changing worlds."
        val WEST_PLANK_ROWS = 9556..9558
        val PLANK_STARTS =
            listOf(
                WEST_PLANK_ROWS.map { CoordGrid(2770, it, 3) },
                (9589..9591).map { CoordGrid(2803, it, 3) },
            )
        val TAG_DISPENSER = CoordGrid(2772, 9590, 3)
        val TAG_TILE = CoordGrid(2773, 9590, 3)
    }
}
