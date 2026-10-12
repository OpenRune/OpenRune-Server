package org.rsmod.content.areas.city.keldagrim

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.CartConductors
import org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.KeldagrimCitizens
import org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.RatPits
import org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.Taverns
import org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.shops.CrossbowShop
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class KeldagrimPurchasesInteractionTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previousPolicy)
    }

    private fun fixture() =
        InteractionFixture { objRepo, shops ->
            listOf(
                Taverns(objRepo),
                RatPits(objRepo),
                CartConductors(objRepo),
                CrossbowShop(shops, objRepo),
                KeldagrimCitizens(objRepo),
            )
        }

    @Test
    fun `the King's Axe barman sells each drink at its price`() {
        for ((options, drink, price) in KINGS_AXE_DRINKS) {
            val f = fixture()
            f.player.inv[0] = InvObj(COINS, 20)
            f.talkTo(KINGS_AXE_BARMAN)
            f.finish(options)
            assertEquals(20 - price, f.player.inv.count(COINS), drink)
            assertEquals(1, f.player.inv.count(drink), drink)
        }
    }

    @Test
    fun `the King's Axe barman serves nothing to a player short of coins`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 9)
        f.talkTo(KINGS_AXE_BARMAN)
        f.finish(listOf(3, 1))
        assertEquals(9, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WINE))
    }

    @Test
    fun `declining the King's Axe barman costs nothing`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 20)
        f.talkTo(KINGS_AXE_BARMAN)
        f.finish(listOf(1, 2))
        assertEquals(20, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test
    fun `a full pack drops the King's Axe beer instead of losing it`() {
        val f = fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(KINGS_AXE_BARMAN)
        f.finish(listOf(1, 1))
        assertEquals(4, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(1, f.groundCount(BEER))
    }

    @Test
    fun `the Laughing Miner and ratpit bars sell beer for 2 and stew for 20`() {
        for ((npc, menu) in mapOf(BARMAID to listOf(2, 3), RAUBORN to listOf(1, 2))) {
            val beer = fixture()
            beer.player.inv[0] = InvObj(COINS, 25)
            beer.talkTo(npc)
            beer.finish(listOf(menu[0], 1))
            assertEquals(23, beer.player.inv.count(COINS), npc)
            assertEquals(1, beer.player.inv.count(BEER), npc)

            val stew = fixture()
            stew.player.inv[0] = InvObj(COINS, 25)
            stew.talkTo(npc)
            stew.finish(listOf(menu[1], 1))
            assertEquals(5, stew.player.inv.count(COINS), npc)
            assertEquals(1, stew.player.inv.count(STEW), npc)

            val poor = fixture()
            poor.player.inv[0] = InvObj(COINS, 19)
            poor.talkTo(npc)
            poor.finish(listOf(menu[1], 1))
            assertEquals(19, poor.player.inv.count(COINS), npc)
            assertEquals(0, poor.player.inv.count(STEW), npc)

            val declined = fixture()
            declined.player.inv[0] = InvObj(COINS, 25)
            declined.talkTo(npc)
            declined.finish(listOf(menu[1], 2))
            assertEquals(25, declined.player.inv.count(COINS), npc)
            assertEquals(0, declined.player.inv.count(STEW), npc)
        }
    }

    @Test
    fun `Kjut sells a kebab for 1 coin and nothing without one`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 3)
        f.talkTo(KJUT)
        f.finish(listOf(1))
        assertEquals(2, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(KEBAB))

        val poor = fixture()
        poor.talkTo(KJUT)
        poor.finish(listOf(1))
        assertEquals(0, poor.player.inv.count(KEBAB))
    }

    @Test
    fun `Blandebir fills an empty pot with ale yeast for 25 coins`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 30)
        f.player.inv[1] = InvObj(POT, 1)
        f.talkTo(BLANDEBIR)
        f.finish(listOf(4, 1, 5))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(POT))
        assertEquals(1, f.player.inv.count(ALE_YEAST))
    }

    @Test
    fun `Blandebir takes nothing without a pot, enough coins or agreement`() {
        val cases =
            listOf(
                Triple(30, 0, listOf(4, 5)),
                Triple(24, 1, listOf(4, 5)),
                Triple(30, 1, listOf(4, 2, 5)),
            )
        for ((coins, pots, options) in cases) {
            val f = fixture()
            f.player.inv[0] = InvObj(COINS, coins)
            if (pots > 0) f.player.inv[1] = InvObj(POT, pots)
            f.talkTo(BLANDEBIR)
            f.finish(options)
            assertEquals(coins, f.player.inv.count(COINS))
            assertEquals(pots, f.player.inv.count(POT))
            assertEquals(0, f.player.inv.count(ALE_YEAST))
        }
    }

    @Test
    fun `toasting with Gauss empties exactly one drink`() {
        for (drink in listOf(BEER, STOUT)) {
            val f = fixture()
            f.player.inv[0] = InvObj(drink, 1)
            f.player.inv[1] = InvObj(drink, 1)
            f.talkTo(GAUSS)
            f.finish(emptyList())
            assertEquals(1, f.player.inv.count(drink), drink)
            assertEquals(1, f.player.inv.count(BEER_GLASS), drink)
        }

        val wine = fixture()
        wine.player.inv[0] = InvObj(WINE, 1)
        wine.talkTo(GAUSS)
        wine.finish(emptyList())
        assertEquals(1, wine.player.inv.count(WINE))
        assertEquals(0, wine.player.inv.count(BEER_GLASS))
    }

    @Test
    fun `a cart conductor sells an Ice Mountain ticket for 150 coins`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 200)
        f.talkTo(CONDUCTOR)
        f.finish(listOf(3, 2, 1))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TICKET))
    }

    @Test
    fun `the Tickets option sells the same ticket`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(CONDUCTOR, op = 3)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TICKET))
    }

    @Test
    fun `the ring of charos halves the ticket price`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.player.worn[12] = InvObj(CHAROS, 1)
        f.talkTo(CONDUCTOR)
        f.finish(listOf(3, 2, 3))
        assertEquals(25, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TICKET))
    }

    @Test
    fun `a conductor sells no ticket when poor, declined or the pack is full`() {
        val poor = fixture()
        poor.player.inv[0] = InvObj(COINS, 149)
        poor.talkTo(CONDUCTOR)
        poor.finish(listOf(3, 2, 1, 4))
        assertEquals(149, poor.player.inv.count(COINS))
        assertEquals(0, poor.player.inv.count(TICKET))

        val declined = fixture()
        declined.player.inv[0] = InvObj(COINS, 200)
        declined.talkTo(CONDUCTOR)
        declined.finish(listOf(3, 2, 2, 4))
        assertEquals(200, declined.player.inv.count(COINS))
        assertEquals(0, declined.player.inv.count(TICKET))

        val full = fixture()
        full.fillInventory()
        full.player.inv[0] = InvObj(COINS, 200)
        full.talkTo(CONDUCTOR)
        full.finish(listOf(3, 2, 1, 4))
        assertEquals(200, full.player.inv.count(COINS))
        assertEquals(0, full.player.inv.count(TICKET))
        assertEquals(0, full.groundCount(TICKET))
    }

    @Test
    fun `spending the last coins frees a slot for the ticket`() {
        val f = fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 150)
        f.talkTo(CONDUCTOR)
        f.finish(listOf(3, 2, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TICKET))
    }

    @Test
    fun `a conductor sells a White Wolf Mountain ticket for 100 coins after Fishing Contest`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 200)
        f.talkTo(CONDUCTOR)
        f.finish(listOf(3, 3, 1))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WHITE_WOLF_TICKET))
    }

    @Test
    fun `the ring of charos halves the White Wolf Mountain price`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.player.worn[12] = InvObj(CHAROS, 1)
        f.talkTo(CONDUCTOR, op = 3)
        f.finish(listOf(3))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WHITE_WOLF_TICKET))
    }

    @Test
    fun `a conductor sells no White Wolf Mountain ticket to a poor player`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 99)
        f.talkTo(CONDUCTOR)
        f.finish(listOf(3, 3, 1, 4))
        assertEquals(99, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WHITE_WOLF_TICKET))
    }

    @Test
    fun `White Wolf Mountain is not offered before Fishing Contest`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 200)
        f.talkTo(CONDUCTOR, op = 3)
        f.finish(listOf(3))
        assertEquals(200, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WHITE_WOLF_TICKET))
    }

    @Test
    fun `Smokin' Joe hands over a rat pits manual after Ratcatchers`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.talkTo(SMOKIN_JOE)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(RATPITS_MANUAL))
    }

    @Test
    fun `a full pack drops Smokin' Joe's manual instead of losing it`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.fillInventory()
        f.talkTo(SMOKIN_JOE)
        f.finish(listOf(2, 2, 2))
        assertEquals(0, f.player.inv.count(RATPITS_MANUAL))
        assertEquals(1, f.groundCount(RATPITS_MANUAL))
    }

    @Test
    fun `Smokin' Joe gives no second manual`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = fixture()
        f.player.inv[0] = InvObj(RATPITS_MANUAL, 1)
        f.talkTo(SMOKIN_JOE)
        f.finish(listOf(2, 2, 2))
        assertEquals(1, f.player.inv.count(RATPITS_MANUAL))
    }

    @Test
    fun `Smokin' Joe ignores a player before Ratcatchers`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = fixture()
        f.talkTo(SMOKIN_JOE)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(RATPITS_MANUAL))
    }

    @Test
    fun `Hirko sells one bolt pouch for 1500 coins`() {
        val f = fixture()
        f.player.inv[0] = InvObj(COINS, 2000)
        f.talkTo(HIRKO)
        f.finish(listOf(2, 1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BOLT_POUCH))
    }

    @Test
    fun `Hirko sells no pouch to an owner, a poor player or one who declines`() {
        val owner = fixture()
        owner.player.inv[0] = InvObj(COINS, 2000)
        owner.player.inv[1] = InvObj(BOLT_POUCH, 1)
        owner.talkTo(HIRKO)
        owner.finish(listOf(2))
        assertEquals(2000, owner.player.inv.count(COINS))
        assertEquals(1, owner.player.inv.count(BOLT_POUCH))

        val poor = fixture()
        poor.player.inv[0] = InvObj(COINS, 1499)
        poor.talkTo(HIRKO)
        poor.finish(listOf(2, 1))
        assertEquals(1499, poor.player.inv.count(COINS))
        assertEquals(0, poor.player.inv.count(BOLT_POUCH))

        val declined = fixture()
        declined.player.inv[0] = InvObj(COINS, 2000)
        declined.talkTo(HIRKO)
        declined.finish(listOf(2, 2))
        assertEquals(2000, declined.player.inv.count(COINS))
        assertEquals(0, declined.player.inv.count(BOLT_POUCH))
    }

    @Test
    fun `a full pack drops the bolt pouch instead of losing it`() {
        val f = fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 2000)
        f.talkTo(HIRKO)
        f.finish(listOf(2, 1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.groundCount(BOLT_POUCH))
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val BEER = "obj.beer"
        private const val STOUT = "obj.dwarven_stout"
        private const val WINE = "obj.jug_wine"
        private const val STEW = "obj.stew"
        private const val KEBAB = "obj.kebab"
        private const val POT = "obj.pot_empty"
        private const val ALE_YEAST = "obj.ale_yeast"
        private const val BEER_GLASS = "obj.beer_glass"
        private const val TICKET = "obj.dwarf_minecart_ticket_kelda_ice"
        private const val CHAROS = "obj.ring_of_charos_unlocked"
        private const val BOLT_POUCH = "obj.xbows_bolt_pouch"
        private const val WHITE_WOLF_TICKET = "obj.dwarf_minecart_ticket_kelda_whitewolf"
        private const val RATPITS_MANUAL = "obj.ratpits_manual"
        private const val SMOKIN_JOE = "npc.vc_smokin_joe"

        private const val KINGS_AXE_BARMAN = "npc.dwarf_city_barman_rich"
        private const val BARMAID = "npc.dwarf_city_barmaid_poor"
        private const val RAUBORN = "npc.vc_dwarf_rich1"
        private const val KJUT = "npc.dwarf_city_dwarf_man8"
        private const val BLANDEBIR = "npc.blandebir"
        private const val GAUSS = "npc.dwarf_city_dwarf_man6"
        private const val CONDUCTOR = "npc.dwarf_city_train_conductor1"
        private const val HIRKO = "npc.xbows_sales_dwarf_keldegrim"

        private val KINGS_AXE_DRINKS =
            listOf(
                Triple(listOf(1, 1), BEER, 1),
                Triple(listOf(2, 1), STOUT, 2),
                Triple(listOf(3, 1), WINE, 10),
            )

        @JvmStatic @BeforeAll fun cache() = InteractionFixture.loadCache()

        @JvmStatic @AfterAll fun restore() = InteractionFixture.restore()
    }
}
