package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.agility.arena.BrimhavenArenaScript
import org.rsmod.content.skills.agility.arena.PirateJackieScript
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class AgilityRewardNpcsTest {
    private fun GameTestScope.spawn(npc: String, coords: CoordGrid): Npc {
        val type = checkNotNull(ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)))
        return spawnNpc(coords, type)
    }

    private fun GameTestScope.buyCape(coins: Int, fill: Boolean = false) {
        setAgility(99)
        startAt(BESIDE_IZZY)
        if (fill) {
            player.fillInv()
        }
        player.inv[0] = InvObj(COINS, coins)
        val izzy = spawn(IZZY, IZZY_TILE)
        player.opNpc1(izzy)
        converse(IZZY_SKILLCAPE, IZZY_BUY)
    }

    private fun GameTestScope.claimDiary() {
        startAt(BESIDE_JACKIE)
        val jackie = spawn(JACKIE, JACKIE_TILE)
        player.opNpc1(jackie)
        converse(JACKIE_DIARY)
    }

    @Test
    fun GameTestState.`izzy swaps 99,000 coins for the agility cape and hood`() =
        runAgilityTest(BrimhavenArenaScript::class) {
            buyCape(coins = CAPE_PRICE)

            assertEquals(0, player.inv.count(COINS))
            assertEquals(1, player.inv.count(CAPE))
            assertEquals(1, player.inv.count(HOOD))
            assertEquals(0, player.inv.count(TRIMMED_CAPE))
            assertEquals(2, player.inv.objs.count { it != null })
        }

    @Test
    fun GameTestState.`another 99 trims the cape`() =
        runAgilityTest(BrimhavenArenaScript::class) {
            player.setBaseLevel("stat.strength", 99)

            buyCape(coins = CAPE_PRICE)

            assertEquals(1, player.inv.count(TRIMMED_CAPE))
            assertEquals(0, player.inv.count(CAPE))
            assertEquals(1, player.inv.count(HOOD))
        }

    @Test
    fun GameTestState.`izzy keeps the cape from a player short of coins`() =
        runAgilityTest(BrimhavenArenaScript::class) {
            buyCape(coins = CAPE_PRICE - 1)

            assertEquals(CAPE_PRICE - 1, player.inv.count(COINS))
            assertEquals(0, player.inv.count(CAPE))
            assertEquals(0, player.inv.count(HOOD))
        }

    @Test
    fun GameTestState.`the cape needs room for its hood too`() =
        runAgilityTest(BrimhavenArenaScript::class) {
            buyCape(coins = CAPE_PRICE, fill = true)

            assertEquals(CAPE_PRICE, player.inv.count(COINS))
            assertEquals(0, player.inv.count(CAPE))
            assertEquals(0, player.inv.count(HOOD))
            assertEquals(28, player.inv.objs.count { it != null })
        }

    @Test
    fun GameTestState.`jackie hands out the easy diary reward once`() =
        runAgilityTest(PirateJackieScript::class) {
            setVarbit(EASY_DONE, EASY_COMPLETE)

            claimDiary()
            claimDiary()

            assertEquals(1, player.inv.count(GLOVES))
            assertEquals(1, player.inv.count(LAMP))
            assertEquals(2, player.inv.objs.count { it != null })
            assertEquals(1, player.vars[EASY_REWARD])
        }

    @Test
    fun GameTestState.`a full inventory keeps the easy diary reward back`() =
        runAgilityTest(PirateJackieScript::class) {
            setVarbit(EASY_DONE, EASY_COMPLETE)
            player.fillInv()
            val slots = player.inv.objs.toList()

            claimDiary()

            assertEquals(slots, player.inv.objs.toList())
            assertEquals(0, player.vars[EASY_REWARD])
        }

    private companion object {
        const val COINS = "obj.coins"
        const val CAPE = "obj.skillcape_agility"
        const val TRIMMED_CAPE = "obj.skillcape_agility_trimmed"
        const val HOOD = "obj.skillcape_agility_hood"
        const val CAPE_PRICE = 99_000
        const val IZZY = "npc.agilityarena_clerk"
        const val IZZY_SKILLCAPE = 4
        const val IZZY_BUY = 2
        const val JACKIE = "npc.agilityarena_tickettrader"
        const val JACKIE_DIARY = 4
        const val EASY_DONE = "varbit.atjun_easy_done"
        const val EASY_REWARD = "varbit.atjun_easy_reward"
        const val EASY_COMPLETE = 2
        const val GLOVES = "obj.atjun_gloves_easy"
        const val LAMP = "obj.atjun_easy_lamp"
        val IZZY_TILE = CoordGrid(2807, 3191, 0)
        val BESIDE_IZZY = CoordGrid(2808, 3191, 0)
        val JACKIE_TILE = CoordGrid(2809, 3192, 0)
        val BESIDE_JACKIE = CoordGrid(2809, 3191, 0)
    }
}
