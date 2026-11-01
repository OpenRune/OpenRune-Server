package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import dev.or2.central.account.Rights
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.PlayerDeath
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathHandlingResolver
import org.rsmod.api.game.process.player.PlayerRegionProcessor
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.hook.GroundItemDropResolver
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class FoundryLifecycleTest {
    @Test
    fun `actual death captures items only after storing tutorial preform`() {
        val f = Fixture()
        repeat(4) { f.player.inv[it] = InvObj("obj.dragon_scimitar", 1) }
        val dropped = mutableListOf<Obj>()
        val repo = mockk<ObjRepository>()
        every { repo.add(capture(dropped), any(), any()) } returns true
        val prices = mockk<MarketPrices>()
        every { prices[any<dev.openrune.types.ItemServerType>()] } returns null
        val config = mockk<ServerConfig>(relaxed = true)
        every { config.gameplay.gravestones.enabled } returns false
        val drops = PlayerDeathDrops(MapClock(), repo, prices, GroundItemDropResolver(emptySet()), emptySet(), config)
        val death = PlayerDeath(
            MapClock(), drops, PlayerDeathHandlingResolver(emptySet()),
            setOf(FoundryDeathPreDropHook(f.events)), emptySet(), emptySet(), emptySet(), mockk(relaxed = true),
        )
        // Exercise the actual mutation pipeline, not the preview or a manually ordered hook.
        val handle = PlayerDeath::class.java.declaredMethods.single { it.name.startsWith("handleDeathDrops") }
        handle.isAccessible = true
        handle.invoke(death, f.player, f.player.coords.packed)
        f.assertStored()
        assertEquals(1, dropped.count { it.type == "obj.dragon_scimitar".asRSCM() })
        assertTrue(dropped.none { it.type == PREFORM.asRSCM() })
        assertEquals(3, f.player.inv.count("obj.dragon_scimitar"))

        val admin = Fixture()
        admin.player.modLevel = Rights.ADMINISTRATOR
        handle.invoke(death, admin.player, admin.player.coords.packed)
        admin.assertStored()
    }

    @Test
    fun `validation denied casts and invalid destinations leave carried preform and hud`() {
        val f = Fixture()
        val validator = PlayerTeleportValidator(emptySet())
        assertNull(validator.validate(f.player, TeleportType.Standard, mockk()))
        f.assertCarried()
        val denied = mockk<PlayerTeleportValidateHook>()
        every { denied.validate(any(), any(), any()) } returns "Not now."
        val collision = CollisionFlagMap()
        val access = f.access(PlayerTeleportValidator(setOf(denied)), collision)
        access.telejump(FOUNDRY_OUTSIDE)
        f.assertCarried()
        assertEquals(FOUNDRY_INSIDE, f.player.coords)
        f.access(validator, collision).telejump(FOUNDRY_OUTSIDE)
        f.assertCarried()
        assertEquals(FOUNDRY_INSIDE, f.player.coords)
    }

    @Test
    fun `actual successful exempt departure stores once but walking does not`() {
        val f = Fixture()
        val regions = PlayerRegionProcessor(mockk(relaxed = true), f.events)
        f.player.coords = CoordGrid(FOUNDRY_INSIDE.x + 1, FOUNDRY_INSIDE.z, 0)
        regions.process(f.player)
        f.assertCarried()
        val collision = CollisionFlagMap()
        collision.allocateIfAbsent(FOUNDRY_OUTSIDE.x, FOUNDRY_OUTSIDE.z, 0)
        f.access(PlayerTeleportValidator(emptySet()), collision).telejump(FOUNDRY_OUTSIDE, TeleportType.Exempt)
        assertEquals(FOUNDRY_OUTSIDE, f.player.coords)
        repeat(2) { regions.process(f.player) }
        f.assertStored()
    }

    @Test
    fun `committed weapon and shield replacements never strand a pocket preform`() {
        for (replacement in listOf("obj.bronze_sword", "obj.bronze_2h_sword", "obj.wooden_shield")) {
            val f = Fixture()
            f.player.inv[0] = InvObj(replacement, 1)
            for (slot in 1 until 28) f.player.inv[slot] = InvObj("obj.black_bead", 1)
            val result = HeldEquipOp(f.events).equip(f.player, 0, f.player.inv)
            assertTrue(result is HeldEquipResult.Success, "$replacement: $result")
            f.assertStored()
            assertEquals(27, f.player.inv.count("obj.black_bead"))
            assertTrue(f.player.worn.any { it?.id == replacement.asRSCM() })
        }
    }

    @Test
    fun `rejected two hand replacement preserves preform and every inventory item`() {
        val f = Fixture()
        f.player.worn[Wearpos.LeftHand.slot] = InvObj("obj.wooden_shield", 1)
        f.player.inv[0] = InvObj("obj.bronze_2h_sword", 1)
        for (slot in 1 until 28) f.player.inv[slot] = InvObj("obj.black_bead", 1)
        val before = f.player.inv.objs.toList()
        assertTrue(HeldEquipOp(f.events).equip(f.player, 0, f.player.inv) is HeldEquipResult.Fail.NotEnoughInvSpace)
        f.assertCarried()
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals("obj.wooden_shield".asRSCM(), f.player.worn[Wearpos.LeftHand.slot]?.id)
    }

    @Test
    fun `movement and equipment for players without preforms do not create storage`() {
        val f = Fixture()
        f.player.worn.fillNulls()
        f.player.inv[0] = InvObj("obj.bronze_sword", 1)
        HeldEquipOp(f.events).equip(f.player, 0, f.player.inv)
        f.player.coords = FOUNDRY_OUTSIDE
        f.player.pendingTelejump = true
        PlayerRegionProcessor(mockk(relaxed = true), f.events).process(f.player)
        assertFalse(f.player.preformStored)
    }

    private class Fixture {
        val events = EventBus()
        val player = Player().apply {
            client = mockk(relaxed = true)
            uuid = 987L
            observerUUID = uuid
            slotId = 1
            assignUid()
            coords = FOUNDRY_INSIDE
            lastKnownNormalCoord = coords
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            worn[Wearpos.RightHand.slot] = InvObj(PREFORM, 1)
            sleepingGiantsTutorial = SleepingGiants.TUTORIAL_PREFORM_TAKEN
            preformQuality = 120
            preformCompletion = 420
            preformTemperature = 600
            statMap.setBaseLevel("stat.attack", 99)
            statMap.setBaseLevel("stat.defence", 99)
        }

        init {
            with(FoundryAccessScript(mockk(), events)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            player.resumePreform(events)
        }

        fun access(validator: PlayerTeleportValidator, collision: CollisionFlagMap) = ProtectedAccess(
            player, GameCoroutine("foundry-lifecycle-test"),
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { events }, getCollision = { collision },
                getAreaChecker = { mockk() }, getTeleportValidator = { validator },
            ),
        )

        fun assertCarried() {
            assertTrue(player.holdsPreform())
            assertFalse(player.preformStored)
            assertTrue(player.ui.containsOverlay(HUD))
            assertEquals(1, player.worn.count(PREFORM) + player.inv.count(PREFORM))
        }

        fun assertStored() {
            assertTrue(player.preformStored)
            assertEquals(0, player.worn.count(PREFORM) + player.inv.count(PREFORM))
            assertFalse(player.ui.containsOverlay(HUD))
            assertEquals(SleepingGiants.TUTORIAL_PREFORM_TAKEN, player.sleepingGiantsTutorial)
            assertEquals(120, player.preformQuality)
            assertEquals(420, player.preformCompletion)
            assertEquals(600, player.preformTemperature)
        }
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
