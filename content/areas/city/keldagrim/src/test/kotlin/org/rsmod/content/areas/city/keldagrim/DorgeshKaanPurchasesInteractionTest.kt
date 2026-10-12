package org.rsmod.content.areas.city.keldagrim

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs.DorgeshKaanMarket
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DorgeshKaanPurchasesInteractionTest {
    private fun fixture() =
        InteractionFixture { objRepo, shops -> listOf(DorgeshKaanMarket(shops, objRepo)) }

    @Test
    fun `every food stall sells its food for 10 coins`() {
        for ((stall, food) in FOOD_STALLS) {
            val f = fixture()
            f.player.inv[0] = InvObj(COINS, 25)
            f.talkTo(stall)
            f.finish(listOf(1))
            assertEquals(15, f.player.inv.count(COINS), stall)
            assertEquals(1, f.player.inv.count(food), stall)
        }
    }

    @Test
    fun `food stalls sell nothing to a poor player or one who declines`() {
        for ((stall, food) in FOOD_STALLS) {
            val poor = fixture()
            poor.player.inv[0] = InvObj(COINS, 9)
            poor.talkTo(stall)
            poor.finish(listOf(1))
            assertEquals(9, poor.player.inv.count(COINS), stall)
            assertEquals(0, poor.player.inv.count(food), stall)

            val declined = fixture()
            declined.player.inv[0] = InvObj(COINS, 25)
            declined.talkTo(stall)
            declined.finish(listOf(2))
            assertEquals(25, declined.player.inv.count(COINS), stall)
            assertEquals(0, declined.player.inv.count(food), stall)
        }
    }

    @Test
    fun `a full pack drops the food, except at Tindar's who refuses`() {
        for ((stall, food) in FOOD_STALLS) {
            val f = fixture()
            f.fillInventory()
            f.player.inv[0] = InvObj(COINS, 25)
            f.talkTo(stall)
            f.finish(listOf(1))
            if (stall == TINDAR) {
                assertEquals(25, f.player.inv.count(COINS), stall)
                assertEquals(0, f.groundCount(food), stall)
            } else {
                assertEquals(15, f.player.inv.count(COINS), stall)
                assertEquals(1, f.groundCount(food), stall)
            }
        }
    }

    @Test
    fun `Tindar sells when the last coins free a slot`() {
        val f = fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(TINDAR)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count("obj.dorgesh_crispy_froglegs"))
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val TINDAR = "npc.dorgesh_market_female1"

        private val FOOD_STALLS =
            mapOf(
                "npc.dorgesh_market_male3" to "obj.dorgesh_frog_burger",
                "npc.dorgesh_market_female2" to "obj.dorgesh_bat_shish",
                "npc.dorgesh_market_male2" to "obj.dorgesh_frog_spawn_gumbo",
                TINDAR to "obj.dorgesh_crispy_froglegs",
                "npc.dorgesh_market_male1" to "obj.dorgesh_green_gloop_soup",
                "npc.dorgesh_market_female3" to "obj.dorgesh_wall_beast_fingers",
            )

        @JvmStatic @BeforeAll fun cache() = InteractionFixture.loadCache()

        @JvmStatic @AfterAll fun restore() = InteractionFixture.restore()
    }
}
