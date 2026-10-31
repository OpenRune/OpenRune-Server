package org.rsmod.content.generic.locs.water

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerDeathPreDropHook
import org.rsmod.api.death.PlayerDeathStorageHook
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.plugin.PlayerDeathScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class WaterSourceScriptTest {
    @Test
    fun GameTestState.`well places filled bucket in first free slot`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.well", "obj.bucket_empty", 5)
            assertMessageSent("You fill the bucket from the well.")
            advance(1)
            assertEquals(id("obj.bucket_water"), player.inv[0]?.id)
            assertNull(player.inv[5])
        }

    @Test
    fun GameTestState.`well refuses jug without consuming it`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.well", "obj.jug_empty", 5)
            assertMessageSent("If I drop my jug down there, I don't think I'm likely to get it back.")
            advance(1)
            assertEquals(id("obj.jug_empty"), player.inv[5]?.id)
        }

    @Test
    fun GameTestState.`tap refuses fishbowl but fills vial in first free slot`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.kebos_farming_guild_watertap", "obj.fishbowl_empty", 5)
            assertMessageSent("Nothing interesting happens.")
            advance(1)
            assertEquals(id("obj.fishbowl_empty"), player.inv[5]?.id)
            use(interactions, "loc.kebos_farming_guild_watertap", "obj.vial_empty", 7)
            advance(1)
            assertEquals(id("obj.vial_water"), player.inv[0]?.id)
            assertNull(player.inv[7])
        }

    @Test
    fun GameTestState.`watering can continuation refills each actual mixed-dose can`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[7] = InvObj(id("obj.watering_can_3"), 1)
            player.inv[20] = InvObj(id("obj.watering_can_7"), 1)
            use(interactions, "loc.fountain", "obj.watering_can_0", 0)
            assertMessageSent("You fill the watering can from the fountain.")
            advance(1)
            assertEquals(id("obj.watering_can_8"), player.inv[0]?.id)
            assertEquals(id("obj.watering_can_3"), player.inv[7]?.id)
            assertEquals(id("obj.watering_can_7"), player.inv[20]?.id)
            advance(4)
            assertEquals(id("obj.watering_can_8"), player.inv[7]?.id)
            assertEquals(id("obj.watering_can_7"), player.inv[20]?.id)
            advance(4)
            assertEquals(id("obj.watering_can_8"), player.inv[20]?.id)
        }

    @Test
    fun GameTestState.`well refills shared Gricoller tank and full copies do not keep the refill queue alive`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[12] = InvObj(id("obj.zeah_wateringcan"), 1)
            VarPlayerIntMapSetter.set(player, "varbit.zeah_wateringcan_charges", 0)
            use(interactions, "loc.well", "obj.zeah_wateringcan", 6)
            advance(1)
            assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
            assertEquals(id("obj.zeah_wateringcan"), player.inv[6]?.id)
            assertEquals(id("obj.zeah_wateringcan"), player.inv[12]?.id)
            advance(5)
            VarPlayerIntMapSetter.set(player, "varbit.zeah_wateringcan_charges", 999)
            advance(10)
            assertEquals(999, player.vars["varbit.zeah_wateringcan_charges"])
        }

    @Test
    fun GameTestState.`buckets do not automatically continue`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[7] = InvObj(id("obj.bucket_empty"), 1)
            use(interactions, "loc.fountain", "obj.bucket_empty", 0)
            advance(9)
            assertEquals(id("obj.bucket_water"), player.inv[0]?.id)
            assertEquals(id("obj.bucket_empty"), player.inv[7]?.id)
        }

    @Test
    fun GameTestState.`sink fills cup and rejects beer unchanged`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.sink", "obj.cup_empty", 5)
            advance(1)
            assertEquals(id("obj.cup_water"), player.inv[0]?.id)
            assertNull(player.inv[5])
            use(interactions, "loc.sink", "obj.beer", 7)
            assertMessageSent("Nothing interesting happens.")
            advance(1)
            assertEquals(id("obj.beer"), player.inv[7]?.id)
        }

    @Test
    fun GameTestState.`walking interrupts watering can continuation`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[7] = InvObj(id("obj.watering_can_0"), 1)
            use(interactions, "loc.fountain", "obj.watering_can_0", 0)
            player.moveGameClick(COORDS.translateZ(-4))
            advance(9)
            assertEquals(id("obj.watering_can_8"), player.inv[0]?.id)
            assertEquals(id("obj.watering_can_0"), player.inv[7]?.id)
            assertEquals(COORDS.translateZ(-4), player.coords)
        }

    @Test
    fun GameTestState.`death cancels watering can continuation through respawn`() =
        runInjectedGameTest(
            LocUInteractions::class,
            DeathHooks(),
            WaterSourceScript::class,
            PlayerHitScript::class,
            PlayerDeathScript::class,
        ) { interactions ->
            player.clearInv()
            player.setCurrentLevel("stat.hitpoints", 5)
            player.inv[7] = InvObj(id("obj.watering_can_0"), 1)
            player.inv[20] = InvObj(id("obj.watering_can_0"), 1)
            use(interactions, "loc.fountain", "obj.watering_can_0", 0)
            player.queueHit(1, HitType.Typeless, 5, NoopPlayerHitModifier)
            advance(1)
            assertEquals(0, player.hitpoints)
            advance(18)
            assertTrue(player.hitpoints > 0)
            assertTrue(player.coords != COORDS.translateZ(-1))
            assertEquals(1, player.inv.count("obj.watering_can_8"))
            assertEquals(2, player.inv.count("obj.watering_can_0"))
            advance(8)
            assertEquals(1, player.inv.count("obj.watering_can_8"))
            assertEquals(2, player.inv.count("obj.watering_can_0"))
        }

    @Test
    fun GameTestState.`well fills watering can without moving it into an earlier hole`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.well", "obj.watering_can_0", 6)
            assertMessageSent("You fill the watering can from the well.")
            advance(1)
            assertEquals(id("obj.watering_can_8"), player.inv[6]?.id)
            assertNull(player.inv[0])
        }

    @Test
    fun GameTestState.`stale clicked slot refills nothing and starts no continuation`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[10] = InvObj(id("obj.watering_can_0"), 1)
            use(interactions, "loc.well", "obj.watering_can_0", 6, held = "obj.logs")
            advance(5)
            assertEquals(id("obj.logs"), player.inv[6]?.id)
            assertEquals(id("obj.watering_can_0"), player.inv[10]?.id)
        }

    @Test
    fun GameTestState.`well preserves refused containers with captured punctuation`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            val refusals =
                listOf(
                    "obj.vial_empty" to "vial down there,",
                    "obj.bowl_empty" to "bowl down there",
                    "obj.water_skin0" to "waterskin down there",
                    "obj.fishbowl_empty" to "fishbowl down there",
                )
            for ((item, text) in refusals) {
                player.clearInv()
                use(interactions, "loc.well", item, 6)
                assertMessageSent("If I drop my $text I don't think I'm likely to get it back.")
                advance(1)
                assertEquals(id(item), player.inv[6]?.id)
                assertEquals(1, player.inv.count(item))
            }
        }

    @Test
    fun GameTestState.`pump fills bucket into first free slot without source in message`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            use(interactions, "loc.fai_falador_waterpump", "obj.bucket_empty", 5)
            assertMessageSent("You fill the bucket.")
            advance(1)
            assertEquals(id("obj.bucket_water"), player.inv[0]?.id)
            assertNull(player.inv[5])
        }

    @Test
    fun GameTestState.`pump keeps cans in place and continues with source-free message`() =
        runInjectedGameTest(LocUInteractions::class, null, WaterSourceScript::class) { interactions ->
            player.clearInv()
            player.inv[10] = InvObj(id("obj.watering_can_0"), 1)
            use(interactions, "loc.fai_falador_waterpump", "obj.watering_can_0", 11)
            assertMessageSent("You fill the watering can.")
            advance(1)
            assertEquals(id("obj.watering_can_8"), player.inv[11]?.id)
            assertEquals(id("obj.watering_can_0"), player.inv[10]?.id)
            assertNull(player.inv[0])
            advance(4)
            assertEquals(id("obj.watering_can_8"), player.inv[10]?.id)
            assertNull(player.inv[0])
        }

    private class DeathHooks : AbstractModule() {
        override fun configure() {
            Multibinder.newSetBinder(binder(), PlayerDeathHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathCleanupHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathDropHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathPreDropHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerRespawnHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathStorageHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerGraveHook::class.java)
        }
    }

    private fun GameTestScope.use(
        interactions: LocUInteractions,
        source: String,
        item: String,
        slot: Int,
        held: String = item,
    ) {
        val loc = placeMapLoc(COORDS, source)
        player.teleport(COORDS.translateZ(-1))
        val type = checkNotNull(ServerCacheManager.getItem(id(item)))
        player.inv[slot] = InvObj(id(held), 1)
        player.withProtectedAccess {
            interactions.interactOp(this, loc, loc, checkNotNull(ServerCacheManager.getObject(loc.id)), type, inv, slot)
        }
    }

    private fun id(item: String): Int = item.asRSCM(RSCMType.OBJ)

    private companion object {
        val COORDS = CoordGrid(0, 51, 54, 44, 36)
    }
}
