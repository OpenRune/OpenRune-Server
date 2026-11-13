package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.ui.ifCloseModals
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingSpellInventoryTest {
    @Test
    fun `full inventory fills ordinary vessels cans and seedlings once without consuming noted items`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj("obj.bucket_empty")
        player.inv[1] = InvObj("obj.clay")
        player.inv[2] = InvObj("obj.water_skin2")
        player.inv[3] = InvObj("obj.watering_can_3")
        player.inv[4] = InvObj(chain.dry)
        player.inv[5] = InvObj("obj.zeah_wateringcan")
        player.inv[6] = InvObj("obj.cert_bucket_empty", Int.MAX_VALUE)
        for (slot in 7..27) player.inv[slot] = InvObj("obj.spade")
        assertTrue(FarmingHumidify.hasTarget(player))
        FarmingHumidify.fill(player)
        assertEquals("obj.bucket_water".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.softclay".asRSCM(), player.inv[1]?.id)
        assertEquals("obj.water_skin4".asRSCM(), player.inv[2]?.id)
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[3]?.id)
        assertEquals(chain.wet.id, player.inv[4]?.id)
        assertEquals("obj.zeah_wateringcan".asRSCM(), player.inv[5]?.id)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertEquals(Int.MAX_VALUE, player.inv[6]?.count)
        assertEquals("obj.cert_bucket_empty".asRSCM(), player.inv[6]?.id)
        for (slot in 7..27) assertEquals("obj.spade".asRSCM(), player.inv[slot]?.id)
        assertFalse(FarmingHumidify.hasTarget(player))
        val after = player.inv.toList()
        FarmingHumidify.fill(player)
        assertEquals(after, player.inv.toList())
    }

    @Test
    fun `a lone Gricoller can refills even when no ordinary vessel needs replacing`() {
        val player = player()
        player.inv[0] = InvObj("obj.zeah_wateringcan")
        assertTrue(FarmingHumidify.hasTarget(player))
        FarmingHumidify.fill(player)
        assertEquals("obj.zeah_wateringcan".asRSCM(), player.inv[0]?.id)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertFalse(FarmingHumidify.hasTarget(player))
    }

    @Test
    fun `noted vessels and watered saplings alone do not allow another charged cast`() {
        val player = player()
        player.inv[0] = InvObj("obj.cert_bucket_empty", 100)
        player.inv[1] = InvObj(FarmingSaplings.chains.first().wet)
        player.inv[2] = InvObj("obj.watering_can_8")
        assertFalse(FarmingHumidify.hasTarget(player))
    }

    @Test
    fun `tome confirmation cannot consume a second copy after unlocking`() {
        val player = player()
        for (slot in 0..27) player.inv[slot] = InvObj("obj.spade")
        player.inv[0] = InvObj("obj.fossil_mine_ultrasoil_book")
        player.inv[1] = InvObj("obj.fossil_mine_ultrasoil_book")
        VarPlayerIntMapSetter.set(player, "varp.fossil_perm_transmit", 7)
        assertTrue(FarmingAshTomeScript.unlock(player, 0))
        assertEquals(null, player.inv[0])
        assertEquals(1, player.vars[FarmingSpellsScript.ULTRA_UNLOCK])
        assertEquals(7, player.vars["varp.fossil_perm_transmit"] and 7)
        assertFalse(FarmingAshTomeScript.unlock(player, 1))
        assertEquals("obj.fossil_mine_ultrasoil_book".asRSCM(), player.inv[1]?.id)
        for (slot in 2..27) assertEquals("obj.spade".asRSCM(), player.inv[slot]?.id)
    }

    @Test
    fun `vanished selected tome cannot consume a different inventory copy`() {
        val player = player()
        player.inv[0] = InvObj("obj.spade")
        player.inv[1] = InvObj("obj.fossil_mine_ultrasoil_book")
        assertFalse(FarmingAshTomeScript.unlock(player, 0))
        assertEquals(0, player.vars[FarmingSpellsScript.ULTRA_UNLOCK])
        assertEquals("obj.spade".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.fossil_mine_ultrasoil_book".asRSCM(), player.inv[1]?.id)
    }

    @Test
    fun `paid Fertile Soil publishes committed patch once and consumes exact runes and ash with a full inventory`() {
        for (ultra in listOf(false, true)) {
            val f = SpellFixture()
            f.giveRunes(casts = 2)
            f.give(FarmingSpellsScript.ASH, 3)
            if (ultra) f.variable(FarmingSpellsScript.ULTRA_UNLOCK, 1)
            f.variable(FarmingSpellsScript.SOIL_WARNING, 1)
            f.fill()
            val spades = f.count("obj.spade")
            f.onFertile = {
                assertEquals(if (ultra) Compost.ULTRA else Compost.SUPER, f.state().compost)
                assertEquals(if (ultra) 1 else 3, f.count(FarmingSpellsScript.ASH))
                for (requirement in f.spell.objReqs) {
                    assertEquals(requirement.count, f.count(requirement.obj.internalName))
                }
            }
            f.cast()
            assertEquals(listOf(FarmingFertileSoilEvent(f.player, f.patch)), f.fertiles)
            assertEquals(spades, f.count("obj.spade"))
            val after = f.player.inv.toList()
            val xp = f.xp()
            f.cast()
            assertEquals(after, f.player.inv.toList())
            assertEquals(xp, f.xp())
            assertEquals(1, f.fertiles.size)
        }
    }

    @Test
    fun `Fertile Soil without complete runes level or spellbook never treats or consumes ash`() {
        for (failure in listOf("missing", "partial", "level", "spellbook")) {
            val f = SpellFixture()
            if (failure != "missing") f.giveRunes()
            f.variable(FarmingSpellsScript.ULTRA_UNLOCK, 1)
            f.give(FarmingSpellsScript.ASH, 2)
            when (failure) {
                "partial" -> f.removeRune()
                "level" -> f.player.statMap.setCurrentLevel("stat.magic", 1)
                "spellbook" -> f.variable("varbit.spellbook", 0)
            }
            val before = f.player.inv.toList()
            val xp = f.xp()
            f.cast()
            assertEquals(PatchState().empty(), f.state(), failure)
            assertEquals(before, f.player.inv.toList(), failure)
            assertEquals(xp, f.xp(), failure)
            assertTrue(f.fertiles.isEmpty(), failure)
        }
    }

    @Test
    fun `Fertile Soil refuses weeds dead grown and every existing compost without charging or publishing`() {
        val crop = FarmingCrops.all.first { it.kind == PatchKind.HERB }
        val planted = PatchState(weeds = 3, cropIndex = crop.index)
        val states = listOf(
            PatchState(),
            planted.copy(health = Health.DEAD),
            planted.copy(stage = crop.stages),
        ) + listOf(Compost.NORMAL, Compost.SUPER, Compost.ULTRA).map {
            PatchState().empty().copy(compost = it)
        }
        for (state in states) {
            val f = SpellFixture()
            f.state(state)
            f.giveRunes()
            f.give(FarmingSpellsScript.ASH, 2)
            f.variable(FarmingSpellsScript.ULTRA_UNLOCK, 1)
            val before = f.player.inv.toList()
            val xp = f.xp()
            f.cast()
            assertEquals(state, f.state())
            assertEquals(before, f.player.inv.toList())
            assertEquals(xp, f.xp())
            assertTrue(f.fertiles.isEmpty())
        }
    }

    @Test
    fun `ordinary paid compost never publishes a Fertile Soil event or allows a subsequent charged cast`() {
        val f = SpellFixture()
        f.give("obj.bucket_compost")
        f.ordinaryCompost()
        assertEquals(Compost.NORMAL, f.state().compost)
        assertEquals(0, f.count("obj.bucket_compost"))
        assertEquals(1, f.count("obj.bucket_empty"))
        assertTrue(f.fertiles.isEmpty())
        f.giveRunes()
        val before = f.player.inv.toList()
        val xp = f.xp()
        f.cast()
        assertEquals(before, f.player.inv.toList())
        assertEquals(xp, f.xp())
        assertTrue(f.fertiles.isEmpty())
    }

    @Test
    fun `insufficient ash warning cancels without charge but an explicit supercompost cast commits once`() {
        for (option in listOf(1, 3)) {
            val f = SpellFixture()
            f.variable(FarmingSpellsScript.ULTRA_UNLOCK, 1)
            f.giveRunes()
            f.give(FarmingSpellsScript.ASH)
            val before = f.player.inv.toList()
            val xp = f.xp()
            f.cast()
            assertEquals(before, f.player.inv.toList())
            assertEquals(PatchState().empty(), f.state())
            assertTrue(f.fertiles.isEmpty())
            f.choose(option)
            assertEquals(1, f.count(FarmingSpellsScript.ASH))
            if (option == 1) {
                assertEquals(Compost.SUPER, f.state().compost)
                assertEquals(listOf(FarmingFertileSoilEvent(f.player, f.patch)), f.fertiles)
                for (requirement in f.spell.objReqs) assertEquals(0, f.count(requirement.obj.internalName))
            } else {
                assertEquals(before, f.player.inv.toList())
                assertEquals(xp, f.xp())
                assertEquals(PatchState().empty(), f.state())
                assertTrue(f.fertiles.isEmpty())
            }
        }
    }

    @Test
    fun `stale soil vanished runes and interruption during ash warning cannot publish or consume a cast`() {
        for (failure in listOf("soil", "runes", "interrupt")) {
            val f = SpellFixture()
            f.variable(FarmingSpellsScript.ULTRA_UNLOCK, 1)
            f.giveRunes()
            f.give(FarmingSpellsScript.ASH)
            val xp = f.xp()
            f.cast()
            when (failure) {
                "soil" -> f.state(PatchState().empty().copy(compost = Compost.NORMAL))
                "runes" -> f.removeRune()
            }
            val state = f.state()
            val inventory = f.player.inv.toList()
            if (failure == "interrupt") f.interrupt() else f.choose(1)
            assertEquals(state, f.state(), failure)
            assertEquals(inventory, f.player.inv.toList(), failure)
            assertEquals(xp, f.xp(), failure)
            assertTrue(f.fertiles.isEmpty(), failure)
        }
    }

    @Test
    fun `Geomancy charges once before its four tick cast and reopening pays for a new cast`() {
        val f = SpellFixture(FarmingSpellsScript.GEOMANCY)
        f.giveRunes(casts = 2)
        f.fill()
        val before = f.player.inv.toList()
        val state = f.state()
        val xp = f.xp()
        val cost = f.spell.objReqs.associate { it.obj.id to it.count }
        fun remaining(casts: Int) = before.map { obj ->
            obj?.let {
                val count = it.count - (cost[it.id] ?: 0) * casts
                if (count == 0) null else it.copy(count = count)
            }
        }
        repeat(2) { cast ->
            f.close()
            f.cast()
            val paid = remaining(cast + 1)
            val expectedXp = xp.first + ((cast + 1) * f.spell.castXp * 10).toInt()
            assertEquals(paid, f.player.inv.toList())
            assertEquals(expectedXp to xp.second, f.xp())
            assertFalse(FarmingGeomancy.INTERFACE in f.player.ui)
            repeat(3) {
                f.advance()
                assertFalse(FarmingGeomancy.INTERFACE in f.player.ui)
            }
            f.advance()
            assertTrue(FarmingGeomancy.INTERFACE in f.player.ui)
            assertEquals(paid, f.player.inv.toList())
            assertEquals(expectedXp to xp.second, f.xp())
            assertEquals(state, f.state())
        }
        f.close()
        val paid = f.player.inv.toList()
        val paidXp = f.xp()
        f.cast()
        repeat(4) { f.advance() }
        assertFalse(FarmingGeomancy.INTERFACE in f.player.ui)
        assertEquals(paid, f.player.inv.toList())
        assertEquals(paidXp, f.xp())
        assertEquals(state, f.state())
    }

    @Test
    fun `Geomancy refuses partial runes low level and wrong spellbook without opening or payment`() {
        for (failure in listOf("partial", "level", "spellbook")) {
            val f = SpellFixture(FarmingSpellsScript.GEOMANCY)
            f.giveRunes()
            when (failure) {
                "partial" -> f.removeRune()
                "level" -> f.player.statMap.setCurrentLevel("stat.magic", 1)
                "spellbook" -> f.variable("varbit.spellbook", 0)
            }
            val before = f.player.inv.toList()
            val state = f.state()
            val xp = f.xp()
            f.cast()
            repeat(4) { f.advance() }
            assertFalse(FarmingGeomancy.INTERFACE in f.player.ui, failure)
            assertEquals(before, f.player.inv.toList(), failure)
            assertEquals(xp, f.xp(), failure)
            assertEquals(state, f.state(), failure)
        }
    }

    private class SpellFixture(component: String = FarmingSpellsScript.FERTILE) {
        private val events = EventBus()
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int): Int = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int): Int = maxInclusive
            override fun randomDouble(): Double = 0.999
        }
        private val modifiers = XpModifiers(emptySet())
        private val farming = FarmingService(
            modifiers, random, events, FarmingRewardTestSupport.rewards(random, events),
            FarmingEquipment(random), farmingSiteAccess(),
        )
        private val spells = initialized(MagicSpellRegistry())
        val spell = spells.allSpells().single {
            it.component.packed == component.asRSCM(RSCMType.COMPONENT)
        }
        private val runes = MagicRuneManager(
            initialized(FakeRuneRepository()), initialized(ComboRuneRepository()),
            initialized(CompactRuneRepository()), initialized(UnlimitedRuneRepository()),
            initialized(StaffSubstituteRepository()), initialized(RuneSubstituteRepository()),
            emptySet(),
        )
        private val script = FarmingSpellsScript(
            farming, spells, runes, modifiers, FarmingGeomancy(farming), WorldRepository(ZoneUpdateMap()),
        )
        private val compost = FarmingCompostService(farming, modifiers)
        val patch = FarmingPatches.all.single { it.kind == PatchKind.HERB && it.site == "west_port_phasmatys" }
        val fertiles = mutableListOf<FarmingFertileSoilEvent>()
        var onFertile: (FarmingFertileSoilEvent) -> Unit = {}
        private val coroutine = GameCoroutine("farming-spell-inventory-test")
        private var result: Result<Unit>? = null
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events }, getAlignment = { TextAlignment() }, getRandom = { random },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 806L
            slotId = 1
            assignUid()
            coords = patch.coords.first()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            for (stat in listOf("stat.magic", STAT_FARMING)) {
                statMap.setXP(stat, 13_034_431)
                statMap.setBaseLevel(stat, 99)
                statMap.setCurrentLevel(stat, 99)
            }
        }

        init {
            variable("varp.farming_clock", Int.MAX_VALUE)
            variable("varbit.spellbook", checkNotNull(spell.spellbook).varValue)
            state(PatchState().empty())
            events.subscribeUnbound(FarmingFertileSoilEvent::class.java) {
                assertEquals(this@SpellFixture.player, player)
                assertEquals(this@SpellFixture.patch, patch)
                assertTrue(state().compost != Compost.NONE)
                onFertile(this)
                fertiles += this
            }
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }

        private fun <T : Any> initialized(repository: T): T = repository.apply {
            javaClass.declaredMethods.single {
                it.name.substringBefore('$') == "init" && it.parameterCount == 0
            }.apply { isAccessible = true }.invoke(this)
        }

        fun variable(name: String, value: Int) = VarPlayerIntMapSetter.set(player, name, value)
        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun state(state: PatchState) = variable(patch.varp, state.pack())
        fun xp(): Pair<Int, Int> =
            player.statMap.getFineXP("stat.magic") to player.statMap.getFineXP(STAT_FARMING)
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun give(obj: String, count: Int = 1) {
            val slot = player.inv.indices.first { player.inv[it] == null }
            player.inv[slot] = InvObj(obj, count)
        }
        fun giveRunes(casts: Int = 1) {
            for (requirement in spell.objReqs) give(requirement.obj.internalName, requirement.count * casts)
        }
        fun removeRune() {
            val rune = spell.objReqs.first().obj.id
            val slot = player.inv.indices.first { player.inv[it]?.id == rune }
            val obj = checkNotNull(player.inv[slot])
            player.inv[slot] = if (obj.count == 1) null else obj.copy(count = obj.count - 1)
        }
        fun fill() {
            for (slot in player.inv.indices) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade")
        }
        fun cast() = start {
            if (spell.component.packed == FarmingSpellsScript.GEOMANCY.asRSCM(RSCMType.COMPONENT)) {
                assertTrue(events.publish(this, IfOverlayButton(spell.component, -1, null, IfButtonOp.Op1)))
                return@start
            }
            val type = checkNotNull(ServerCacheManager.getObject(patch.locs.first().asRSCM(RSCMType.LOC)))
            val loc = BoundLocInfo(player.coords, LocEntity(type.id, 10, 0), 2, 1, 1, 0)
            assertTrue(events.publish(this, LocTDefaultEvents.Op(loc, loc, type, null, -1, spell.component)))
        }
        fun close() = player.ifCloseModals(events)
        fun advance() {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            if (!coroutine.isIdle) coroutine.advance()
            result?.getOrThrow()
        }
        fun ordinaryCompost() {
            start {
                val slot = player.inv.indices.first { player.inv[it]?.id == "obj.bucket_compost".asRSCM() }
                with(compost) { apply(patch, "obj.bucket_compost", slot) }
            }
            repeat(20) {
                if (coroutine.isIdle) {
                    checkNotNull(result).getOrThrow()
                    return
                }
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
                result?.getOrThrow()
            }
            error("Compost action did not finish")
        }
        fun choose(option: Int) {
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
            coroutine.resumeWith(ResumePauseButtonInput("component.chatmenu:options", option))
            checkNotNull(result).getOrThrow()
        }
        fun interrupt() {
            assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
            coroutine.cancel()
            assertTrue(result?.exceptionOrNull() is CancellationException)
        }
        private fun start(action: suspend ProtectedAccess.() -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@SpellFixture.result = result }
            })
            result?.getOrThrow()
        }
    }

    private fun player(): Player = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        VarPlayerIntMapSetter.set(this, "varp.farming_growth_offset", 32)
        VarPlayerIntMapSetter.set(this, "varp.farming_sapling_clock", Int.MAX_VALUE)
    }

    companion object {
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
