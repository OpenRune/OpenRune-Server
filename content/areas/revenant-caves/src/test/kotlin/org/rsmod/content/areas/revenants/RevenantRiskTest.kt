package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.config.constants
import org.rsmod.api.death.*
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.protect.*
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.areas.wilderness.WildernessDeathHook
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.*
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
internal class RevenantRiskTest {
    @Test fun `Wilderness death drops all weapon ether except Craw activation even when weapon protected`() {
        for (kind in RevenantCharges.weapons) {
            val p = player(); p.worn[Wearpos.RightHand.slot] = RevenantCharges.withCount(InvObj(kind.empty), 1013)
            val c = context(p); val result = drops().selectDrops(p, c, WildernessDeathHook().handleDeath(c)!!)
            assertEquals(kind.empty.asRSCM(), result.kept.single().id)
            assertEquals(if (kind.empty == "obj.wild_cave_bow_uncharged")13 else 1013, result.lostTradeable.single { it.id == RevenantCharges.ETHER.asRSCM() }.count)
            assertEquals(1013, RevenantCharges.count(p.worn[Wearpos.RightHand.slot]!!), "A preview cannot mutate charges")
        }
    }

    @Test fun `bracelet and every artefact are never protected including notes and instance rules`() {
        for (item in listOf(RevenantCharges.withCount(InvObj(RevenantCharges.EMPTY_BRACELET), 73)) + AncientArtefact.entries.flatMap { listOf(InvObj(it.item), InvObj("obj.cert_${it.item.removePrefix("obj.")}")) }) {
            for (instance in listOf(false, true)) {
                val p = player(); p.inv[0] = item
                val c = context(p).copy(inInstance = instance)
                val r = drops().selectDrops(p, c, WildernessDeathHook().handleDeath(c)!!)
                assertTrue(r.kept.isEmpty(), item.toString()); assertTrue(r.lostTradeable.isNotEmpty(), item.toString())
                if (RevenantCharges.kind(item) == RevenantCharges.bracelet) {
                    assertEquals(73, r.lostTradeable.single { it.id == RevenantCharges.ETHER.asRSCM() }.count)
                    assertTrue(r.lostTradeable.any { it.id == RevenantCharges.EMPTY_BRACELET.asRSCM() })
                }
            }
        }
    }

    @Test fun `outside Wilderness retains protected weapon charges but bracelet is still lost`() {
        val p = player(); val kind = RevenantCharges.weapons.first(); p.inv[0] = RevenantCharges.withCount(InvObj(kind.empty), 1004)
        val c = context(p).copy(inWilderness = false, inRevenantCaves = false)
        val r = drops().selectDrops(p, c, PlayerDeathHandling(3, p, 6000, 0, false, UntradeableHandling.DROP))
        assertEquals(1004, RevenantCharges.count(r.kept.single())); assertTrue(r.lostTradeable.isEmpty())
    }

    @Test fun `admission survives exit and ordinary Wilderness PvM but is lost on cave or Wilderness PvP death`() {
        for ((cave, pvp) in listOf(false to false, true to false, false to true, true to true)) {
            val p = player(); p.revenantAdmission = 3; val killer = if (pvp)player() else null
            val c = context(p).copy(inRevenantCaves = cave, killer = killer)
            val r = drops().selectDrops(p, c, WildernessDeathHook().handleDeath(c)!!)
            assertEquals(3, p.revenantAdmission)
            assertEquals(if (pvp)100000 else 0, r.lostTradeable.filter { it.id == "obj.coins".asRSCM() }.sumOf { it.count })
            drops().prepareCompletion(c)(); assertEquals(if (cave || pvp)2 else 3, p.revenantAdmission)
        }
    }

    @Test fun `Forinthry skull carries death risk and Surge transfer captures worn state before clearing`() {
        val victim = player(); val killer = player(); victim.skullIcon = constants.skullicon_forinthry_surge
        assertTrue(victim.hasSkullDeathPenalty())
        victim.worn[Wearpos.Front.slot] = InvObj("obj.wild_cave_amulet"); killer.worn[Wearpos.Front.slot] = InvObj("obj.wild_cave_amulet")
        ProtectedAccessLauncher.withProtectedAccess(victim, ProtectedAccessContextFactory.empty()) { vars["varp.forinthry_surge_expiration"] = 1000 }
        val finish = drops().prepareCompletion(context(victim).copy(killer = killer))
        victim.worn.fillNulls(); finish()
        assertEquals(constants.skullicon_forinthry_surge, killer.skullIcon)
        assertTrue(killer.vars["varp.forinthry_surge_expiration"] > killer.currentMapClock)
    }
    private fun drops(): PlayerDeathDrops = PlayerDeathDrops(MapClock(100), mock(ObjRepository::class.java), mock(MarketPrices::class.java), mock(GroundItemDropResolver::class.java), setOf(RevenantDeathHook()))
    private fun context(p: Player) = PlayerDeathContext(p, p.coords, true, 25, true, false, false, false, false, 0, null)
    private fun player() = Player().apply {
        currentMapClock = 100; coords = CoordGrid(3197, 10056)
        inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28))
        worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
