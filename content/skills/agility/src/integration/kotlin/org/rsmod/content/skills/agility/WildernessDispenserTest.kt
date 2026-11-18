package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.table.agility.AgilityDispenserLootRow
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.lootingbag.LootingBag
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.cert
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WildernessDispenserTest {
    private val GameTestScope.bag: Inventory
        get() = LootingBag.contents(player)

    private fun GameTestScope.dispenser(): BoundLocInfo {
        val dispenser = placeMapLoc(DISPENSER_TILE, DISPENSER, LocShape[10], LocAngle[1])
        startAt(BESIDE_DISPENSER)
        return dispenser
    }

    private fun GameTestScope.tag(dispenser: BoundLocInfo): List<String> {
        player.opLoc1(dispenser)
        return converse()
    }

    private fun GameTestScope.pay(dispenser: BoundLocInfo, vararg options: Int): List<String> {
        player.opLoc2(dispenser)
        return converse(*options)
    }

    private fun GameTestScope.redeem(dispenser: BoundLocInfo): List<String> {
        player.opLoc3(dispenser)
        return converse()
    }

    private fun GameTestScope.finishedLap(paid: Boolean, streak: Int = 1) {
        setVarbit(TICKET_AVAILABLE, 1)
        if (paid) {
            setVarbit(FEE_PAID, 1)
            setVarbit(REWARD_AVAILABLE, 1)
            setVarbit(STREAK, streak)
        }
    }

    private fun GameTestScope.queueLoot(clue: Boolean = false) {
        random.next = if (clue) 0 else 1
        random.then = 0
        random.then = 4
        random.then = 0
        random.then = 0
    }

    private fun GameTestScope.assertUnclaimed() {
        assertEquals(1, player.vars[TICKET_AVAILABLE])
        assertEquals(1, player.vars[REWARD_AVAILABLE])
        assertEquals(0, player.inv.count(TICKET))
    }

    @Test
    fun GameTestState.`paying takes the fee once and gives a looting bag`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 300_000)

            val paid = pay(dispenser, YES)
            val again = pay(dispenser)

            assertEquals(150_000, player.inv.count(COINS))
            assertEquals(1, player.vars[FEE_PAID])
            assertEquals(1, player.inv.count(LOOTING_BAG))
            assertEquals(0, player.vars[PAYMENT_WARNING])
            assertTrue(PAID in paid && BAG_GIVEN in paid) { "$paid" }
            assertTrue(ALREADY_PAID in again) { "$again" }
        }

    @Test
    fun GameTestState.`paying without enough coins changes nothing`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 149_999)

            val messages = pay(dispenser, YES)

            assertEquals(149_999, player.inv.count(COINS))
            assertEquals(0, player.vars[FEE_PAID])
            assertEquals(0, player.inv.count(LOOTING_BAG))
            assertTrue(NO_COINS in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`declining the payment changes nothing`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 150_000)

            pay(dispenser, NO)

            assertEquals(150_000, player.inv.count(COINS))
            assertEquals(0, player.vars[FEE_PAID])
            assertEquals(0, player.inv.count(LOOTING_BAG))
        }

    @Test
    fun GameTestState.`paying without asking again skips the prompt next time`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 300_000)

            pay(dispenser, YES_ALWAYS)
            setVarbit(FEE_PAID, 0)
            pay(dispenser)

            assertEquals(1, player.vars[PAYMENT_WARNING])
            assertEquals(0, player.inv.count(COINS))
            assertEquals(1, player.vars[FEE_PAID])
            assertEquals(1, player.inv.count(LOOTING_BAG))
        }

    @Test
    fun GameTestState.`paying with a full inventory is refused before the prompt`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.fillInv()
            player.inv[0] = InvObj(COINS, 150_000)

            val messages = pay(dispenser)

            assertEquals(150_000, player.inv.count(COINS))
            assertEquals(0, player.vars[FEE_PAID])
            assertTrue(NO_FREE_SLOT in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`an owned looting bag is not given again`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 150_000)
            player.inv[1] = InvObj(LOOTING_BAG_OPEN, 1)

            val messages = pay(dispenser, YES)

            assertEquals(0, player.inv.count(COINS))
            assertEquals(0, player.inv.count(LOOTING_BAG))
            assertEquals(1, player.inv.count(LOOTING_BAG_OPEN))
            assertTrue(BAG_GIVEN !in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`coins gone at the payment prompt pay nothing`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 150_000)

            player.opLoc2(dispenser)
            advanceUntil({ player.ui.containsModal(MENU) }, timeoutTicks = 6)
            player.inv[0] = null
            val messages = converse(YES)

            assertEquals(0, player.vars[FEE_PAID])
            assertEquals(0, player.inv.count(LOOTING_BAG))
            assertEquals(0, player.inv.occupiedSpace())
            assertTrue(NO_COINS in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`a pack filled at the payment prompt gets the bag on the floor`() =
        runAgilityTest(WildernessDispenserScript::class) { deps ->
            val dispenser = dispenser()
            player.inv[0] = InvObj(COINS, 300_000)

            player.opLoc2(dispenser)
            advanceUntil({ player.ui.containsModal(MENU) }, timeoutTicks = 6)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(BEER, 1)
            }
            converse(YES)

            assertEquals(150_000, player.inv.count(COINS))
            assertEquals(player.inv.size - 1, player.inv.count(BEER))
            assertEquals(1, player.vars[FEE_PAID])
            assertEquals(1, groundCount(deps, LOOTING_BAG))
        }

    @Test
    fun GameTestState.`an unpaid lap gives a ticket once`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            finishedLap(paid = false)

            val first = tag(dispenser)
            val second = tag(dispenser)

            assertEquals(1, player.inv.count(TICKET))
            assertEquals(1, player.inv.objs.count { it != null })
            assertEquals(listOf(TICKET_GIVEN), first)
            assertEquals(listOf(NO_LAP), second)
        }

    @Test
    fun GameTestState.`a paid lap puts its loot in the open bag once`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            finishedLap(paid = true)
            queueLoot()

            val messages = tag(dispenser)
            tag(dispenser)

            assertEquals(4, bag.count(noted(ANGLERFISH)))
            assertEquals(1, bag.count(noted(ADAMANT_FULL_HELM)))
            assertEquals(2, bag.objs.count { it != null })
            assertEquals(1, player.inv.count(TICKET))
            assertEquals(1, SUPPLIES.sumOf { player.inv.count(it) })
            assertEquals(3, player.inv.objs.count { it != null })
            assertEquals(0, player.vars[TICKET_AVAILABLE])
            assertEquals(0, player.vars[REWARD_AVAILABLE])
            assertEquals(TICKET_GIVEN, messages.first())
            val award = messages.single { it.startsWith("You have been awarded") }
            assertTrue(award.startsWith(AWARD_START) && award.endsWith(AWARD_END)) { award }
        }

    @Test
    fun GameTestState.`two tags in one tick claim the loot once`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            finishedLap(paid = true)
            queueLoot()

            player.opLoc1(dispenser)
            player.opLoc1(dispenser)
            converse()

            assertEquals(4, bag.count(noted(ANGLERFISH)))
            assertEquals(1, bag.count(noted(ADAMANT_FULL_HELM)))
            assertEquals(2, bag.objs.count { it != null })
            assertEquals(1, player.inv.count(TICKET))
            assertEquals(1, SUPPLIES.sumOf { player.inv.count(it) })
            assertEquals(3, player.inv.occupiedSpace())
        }

    @Test
    fun GameTestState.`a paid lap waits for an open looting bag`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG, 1)
            finishedLap(paid = true)
            queueLoot()

            val messages = tag(dispenser)

            assertUnclaimed()
            assertEquals(1, player.inv.objs.count { it != null })
            assertEquals(0, bag.objs.count { it != null })
            assertTrue(NO_OPEN_BAG in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`a full looting bag keeps the loot back`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            repeat(bag.size) { bag[it] = InvObj(BEER, 1) }
            finishedLap(paid = true)
            queueLoot()

            val messages = tag(dispenser)

            assertUnclaimed()
            assertEquals(bag.size, bag.count(BEER))
            assertTrue(BAG_FULL in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`a bag with room for the resource alone takes neither roll`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            repeat(bag.size - 1) { bag[it] = InvObj(BEER, 1) }
            finishedLap(paid = true)
            queueLoot()

            tag(dispenser)

            assertUnclaimed()
            assertEquals(0, bag.count(noted(ANGLERFISH)))
            assertEquals(0, bag.count(noted(ADAMANT_FULL_HELM)))
            assertEquals(bag.size - 1, bag.objs.count { it != null })
        }

    @Test
    fun GameTestState.`a resource stack that would overflow keeps the loot back`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            bag[0] = InvObj(noted(ANGLERFISH), Int.MAX_VALUE)
            finishedLap(paid = true)
            queueLoot()

            tag(dispenser)

            assertUnclaimed()
            assertEquals(Int.MAX_VALUE, bag.count(noted(ANGLERFISH)))
            assertEquals(0, bag.count(noted(ADAMANT_FULL_HELM)))
        }

    @Test
    fun GameTestState.`a clue takes the supply's place in the last free slot`() =
        runAgilityTest(WildernessDispenserScript::class) { deps ->
            val dispenser = dispenser()
            player.inv[0] = InvObj(TICKET, 5)
            repeat(25) { player.inv[it + 1] = InvObj(BEER, 1) }
            player.inv[26] = InvObj(LOOTING_BAG_OPEN, 1)
            finishedLap(paid = true)
            queueLoot(clue = true)

            tag(dispenser)

            assertEquals(6, player.inv.count(TICKET))
            assertEquals(1, player.inv.count(CLUE))
            assertEquals(0, SUPPLIES.sumOf { player.inv.count(it) + groundCount(deps, it) })
            assertEquals(4, bag.count(noted(ANGLERFISH)))
        }

    @Test
    fun GameTestState.`a full inventory drops the ticket and leaves out the extra`() =
        runAgilityTest(WildernessDispenserScript::class) { deps ->
            val dispenser = dispenser()
            player.fillInv()
            player.inv[27] = InvObj(LOOTING_BAG_OPEN, 1)
            val slots = player.inv.objs.toList()
            finishedLap(paid = true)
            queueLoot()

            val messages = tag(dispenser)

            assertEquals(slots, player.inv.objs.toList())
            assertEquals(1, groundCount(deps, TICKET))
            assertEquals(4, bag.count(noted(ANGLERFISH)))
            assertEquals(1, bag.count(noted(ADAMANT_FULL_HELM)))
            assertEquals(0, SUPPLIES.sumOf { groundCount(deps, it) })
            val award = messages.single { it.startsWith("You have been awarded") }
            assertEquals(AWARD_NO_EXTRA, award)
        }

    @Test
    fun GameTestState.`a ticket that would overflow the stack goes to the floor`() =
        runAgilityTest(WildernessDispenserScript::class) { deps ->
            val dispenser = dispenser()
            player.inv[0] = InvObj(TICKET, Int.MAX_VALUE)
            finishedLap(paid = false)

            tag(dispenser)

            assertEquals(Int.MAX_VALUE, player.inv.count(TICKET))
            assertEquals(1, groundCount(deps, TICKET))
        }

    @Test
    fun GameTestState.`redeeming pays every ticket at the rate for the amount`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(TICKET, 11)
            val before = agilityXp()

            val messages = redeem(dispenser)

            assertEquals(0, player.inv.count(TICKET))
            assertEquals(before + 11 * 2100, agilityXp())
            val redeemed =
                "You redeem 11 Wilderness agility tickets and have been awarded 2,310 Agility XP!"
            assertEquals(listOf(redeemed), messages)
        }

    @Test
    fun GameTestState.`redeeming one ticket says so`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(TICKET, 1)

            val messages = redeem(dispenser)

            val redeemed =
                "You redeem 1 Wilderness agility ticket and have been awarded 200 Agility XP!"
            assertEquals(listOf(redeemed), messages)
        }

    @Test
    fun GameTestState.`redeeming without tickets does nothing`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            val before = agilityXp()

            val messages = redeem(dispenser)

            assertEquals(before, agilityXp())
            assertTrue(NO_TICKETS in messages) { "$messages" }
        }

    @Test
    fun GameTestState.`redeeming a full stack of tickets takes every one`() =
        runAgilityTest(WildernessDispenserScript::class) {
            val dispenser = dispenser()
            player.inv[0] = InvObj(TICKET, Int.MAX_VALUE)
            val before = agilityXp()

            redeem(dispenser)

            assertEquals(0, player.inv.count(TICKET))
            assertTrue(agilityXp() > before)
        }

    @Test
    fun GameTestState.`the streak picks the loot bracket`() = runAgilityTest {
        val rows = AgilityDispenserLootRow.all()
        val brackets = STREAKS.map { WildernessDispenser.bracket(rows, it).firstLap }

        assertEquals(listOf(1, 1, 16, 16, 31, 31, 61, 61), brackets)
    }

    @Test
    fun GameTestState.`a paid lap counts towards the streak and says so`() = runAgilityTest {
        setVarbit(FEE_PAID, 1)
        setVarbit(STREAK, 15)

        val messages = runWildernessLap()

        assertEquals(16, player.vars[STREAK])
        assertEquals(1, player.vars[TICKET_AVAILABLE])
        assertEquals(1, player.vars[REWARD_AVAILABLE])
        val streak = messages.indexOf(STREAK_16)
        assertTrue(streak > 0 && "lap count is" in messages[streak - 1]) { "$messages" }
    }

    @Test
    fun GameTestState.`an unpaid lap gives no streak`() = runAgilityTest {
        val messages = runWildernessLap()

        assertEquals(0, player.vars[STREAK])
        assertEquals(1, player.vars[TICKET_AVAILABLE])
        assertEquals(0, player.vars[REWARD_AVAILABLE])
        assertTrue(messages.none { "reward streak" in it }) { "$messages" }
    }

    @Test
    fun GameTestState.`a lap over an untagged one keeps the flags and the streak`() =
        runAgilityTest {
            finishedLap(paid = true, streak = 20)
            setVarbit(START_WARNING, 1)

            runWildernessLap()

            assertEquals(21, player.vars[STREAK])
            assertEquals(1, player.vars[TICKET_AVAILABLE])
            assertEquals(1, player.vars[REWARD_AVAILABLE])
            assertEquals(1, player.vars[FEE_PAID])
        }

    @Test
    fun GameTestState.`declining the start prompt stays at the pipe`() = runAgilityTest {
        finishedLap(paid = true)
        setAgility(99)
        startAt(WILDERNESS_START)
        val before = agilityXp()

        player.opLoc1(locNear(PIPE, WILDERNESS_START))
        converse(NO)

        assertEquals(WILDERNESS_START, player.coords)
        assertEquals(before, agilityXp())
        assertEquals(0, player.vars[START_WARNING])
    }

    @Test
    fun GameTestState.`starting without asking again crosses the pipe`() = runAgilityTest {
        finishedLap(paid = false)
        setAgility(99)
        startAt(WILDERNESS_START)

        player.opLoc1(locNear(PIPE, WILDERNESS_START))
        converse(YES_ALWAYS)

        assertEquals(PIPE_END, player.coords)
        assertEquals(1, player.vars[START_WARNING])
        assertEquals(1, player.vars[TICKET_AVAILABLE])
    }

    private fun GameTestScope.runWildernessLap(): List<String> {
        setAgility(99)
        blockMarks()
        startAt(WILDERNESS_START)
        advance()
        return crossAll(WILDERNESS_LAP)
    }

    private fun GameTestScope.groundCount(deps: AgilityTestDeps, obj: String): Int {
        val type = obj.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(player.coords).filter { it.type == type }.sumOf { it.count }
    }

    private fun noted(obj: String): String = cert(obj).internalName

    private companion object {
        const val YES = 1
        const val YES_ALWAYS = 2
        const val NO = 3
        const val MENU = "interface.chatmenu"
        const val DISPENSER = "loc.wildy_agility_pillar"
        const val PIPE = "loc.obstical_pipe2"
        const val TICKET = "obj.wildy_agility_token"
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val CLUE = "obj.trail_medium_emote_exp1"
        const val LOOTING_BAG = "obj.looting_bag"
        const val LOOTING_BAG_OPEN = "obj.looting_bag_open"
        const val ANGLERFISH = "obj.blighted_anglerfish"
        const val ADAMANT_FULL_HELM = "obj.adamant_full_helm"
        const val FEE_PAID = "varbit.wildy_agility_deposit"
        const val TICKET_AVAILABLE = "varbit.wildy_agility_token_available"
        const val REWARD_AVAILABLE = "varbit.wildy_agility_reward_available"
        const val STREAK = "varbit.wildy_agility_reward_lap_new"
        const val START_WARNING = "varbit.wildy_agility_start_warning"
        const val PAYMENT_WARNING = "varbit.wildy_agility_payment_warning"
        const val PAID = "You have paid 150,000 coins to receive a reward after each lap."
        const val BAG_GIVEN =
            "You received <col=ef1020>a Looting bag</col> from the Agility dispenser."
        const val ALREADY_PAID =
            "You have already paid 150,000 coins to receive a reward after each lap."
        const val NO_COINS = "You need 150,000 coins to pay to receive additional rewards."
        const val NO_FREE_SLOT = "You need at least 1 free inventory slot before depositing coins."
        const val TICKET_GIVEN =
            "You received <col=ef1020>a Wilderness agility ticket</col> from the Agility " +
                "dispenser."
        const val NO_LAP =
            "You need to complete one full lap to receive a ticket and pay 150,000 coins to " +
                "receive a reward after each lap."
        const val NO_OPEN_BAG =
            "You must have an open looting bag in your inventory before trying to claim loot."
        const val BAG_FULL =
            "You must have free space in your looting bag before trying to claim loot."
        const val NO_TICKETS = "You do not have any Wilderness agility tickets to redeem."
        const val STREAK_16 = "Your Wilderness Agility reward streak is: <col=ff0000>16</col>"
        const val AWARD_START =
            "You have been awarded <col=ef1020>4 x Blighted anglerfish</col> and " +
                "<col=ef1020>1 x Adamant full helm</col>, and an extra <col=ef1020>"
        const val AWARD_END = "</col> from the Agility dispenser."
        const val AWARD_NO_EXTRA =
            "You have been awarded <col=ef1020>4 x Blighted anglerfish</col> and " +
                "<col=ef1020>1 x Adamant full helm</col> from the Agility dispenser."
        val DISPENSER_TILE = CoordGrid(3005, 3936, 0)
        val BESIDE_DISPENSER = CoordGrid(3005, 3935, 0)
        val WILDERNESS_START = CoordGrid(3004, 3937, 0)
        val PIPE_END = CoordGrid(3004, 3950, 0)
        val STREAKS = listOf(1, 15, 16, 30, 31, 60, 61, 500)
        val SUPPLIES =
            listOf(
                "obj.blighted_anglerfish",
                "obj.blighted_mantaray",
                "obj.blighted_karambwan",
                "obj.blighted_4dose2restore",
            )
        val WILDERNESS_LAP =
            listOf(
                "loc.obstical_pipe2",
                "loc.obstical_ropeswing2",
                "loc.steppingstone1",
                "loc.wilderness_log_balance1",
                "loc.wildclimbingrock",
            )
    }
}
