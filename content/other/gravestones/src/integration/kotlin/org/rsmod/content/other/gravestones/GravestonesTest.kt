package org.rsmod.content.other.gravestones

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerGraveHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.death.plugin.StandardPvmDeathHook
import org.rsmod.api.player.SupplyItems
import org.rsmod.api.player.input.PlayerIdle
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class GravestonesTest {
    @Test
    fun GameTestState.`a death keeps every lost obj in the grave, most valuable first`() =
        runGraveTest { deps ->
            val lost = listOf(InvObj(cheap, 1), InvObj(pricey, 1), InvObj(stack, 500))
            val unheld = deps.graves.store(player, DEATH, DeathGrave(), lost)

            assertEquals(emptyList<InvObj>(), unheld)
            assertEquals(InvObj(pricey, 1), player.grave[0])
            assertEquals(lost.toSet(), player.grave.filterNotNull { true }.toSet())
            assertEquals(Gravestones.DURATION, player.graveDuration)
            assertNotNull(player.graveCoords)
        }

    @Test
    fun GameTestState.`a second death adds to the grave and stacks`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(stack, 100)))
            val coords = player.graveCoords
            deps.graves.store(player, DEATH.translate(5, 5), DeathGrave(), listOf(InvObj(stack, 50)))

            assertEquals(coords, player.graveCoords)
            assertEquals(150, player.grave.count(stack.internalName))
            assertEquals(1, player.grave.occupiedSpace())
        }

    @Test
    fun GameTestState.`a stack past MAX_VALUE hands back the overflow`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(stack, Int.MAX_VALUE - 10)))
            val unheld = deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(stack, 25)))

            assertEquals(Int.MAX_VALUE, player.grave.count(stack.internalName))
            assertEquals(listOf(InvObj(stack, 15)), unheld)
        }

    @Test
    fun GameTestState.`a full grave hands back its least valuable objs`() =
        runGraveTest { deps ->
            val distinct = firstObjTypes(GRAVE_SIZE + 2) { it.isStackable && it.cost > 0 }
            val lost = distinct.map { InvObj(it, 1) }
            val unheld = deps.graves.store(player, DEATH, DeathGrave(), lost)

            assertEquals(GRAVE_SIZE, player.grave.occupiedSpace())
            assertEquals(2, unheld.size)
            val kept = player.grave.filterNotNull { true }
            assertEquals(lost.toSet(), (kept + unheld).toSet())
            assertTrue(unheld.all { left -> kept.all { deps.graves.value(it) >= deps.graves.value(left) } })
        }

    @Test
    fun GameTestState.`take-all moves the free objs to the inv`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1), InvObj(stack, 40), InvObj(pricey, 1)))
            openGrave()

            player.ifButton(component(FREE_BUTTON))
            advance()

            assertEquals(1, player.inv.count(cheap.internalName))
            assertEquals(40, player.inv.count(stack.internalName))
            assertEquals(0, player.grave.count(cheap.internalName))
            assertEquals(1, player.grave.count(pricey.internalName))
        }

    @Test
    fun GameTestState.`take-all with a full inv moves and loses nothing`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            player.fillInv()
            openGrave()

            player.ifButton(component(FREE_BUTTON))
            advance()

            assertEquals(1, player.grave.count(cheap.internalName))
            assertEquals(0, player.inv.count(cheap.internalName))
        }

    @Test
    fun GameTestState.`two takes of one slot in a tick move it once`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1), InvObj(stack, 7)))
            openGrave()
            val slot = (0 until GRAVE_SIZE).first { player.grave[it]?.id == stack.id }

            player.ifButton(component(FREE_ITEMS), comsub = slot)
            player.ifButton(component(FREE_ITEMS), comsub = slot)
            advance()

            assertEquals(7, player.inv.count(stack.internalName))
            assertEquals(0, player.grave.count(stack.internalName))
        }

    @Test
    fun GameTestState.`a pay obj stays until the fee is paid`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(pricey, 1), InvObj(cheap, 1)))
            openGrave()

            player.ifButton(component(PAY_ITEMS), comsub = 0)
            advance()

            assertEquals(1, player.grave.count(pricey.internalName))
            assertEquals(0, player.inv.count(pricey.internalName))
        }

    @Test
    fun GameTestState.`paying takes the fee from the bank once`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(pricey, 1), InvObj(cheap, 1)))
            val fee = deps.graves.fee(player)
            assertTrue(fee > 0)
            val bank = player.invMap.getOrPut("inv.bank")
            bank[0] = InvObj("obj.coins", fee * 3)
            openGrave()

            player.ifButton(component(PAY_BUTTON))
            player.ifButton(component(PAY_BUTTON), op = IfButtonOp.Op1)
            advance()

            assertEquals(fee * 2, bank.count("obj.coins"))
            assertEquals(1, player.inv.count(pricey.internalName) + player.grave.count(pricey.internalName))
            assertTrue(player.feePaid)
            assertEquals(0, deps.graves.fee(player))
        }

    @Test
    fun GameTestState.`paying takes carried coins before the bank`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(pricey, 1)))
            val fee = deps.graves.fee(player)
            val bank = player.invMap.getOrPut("inv.bank")
            bank[0] = InvObj("obj.coins", fee)
            player.inv[0] = InvObj("obj.coins", fee + 5)
            openGrave()

            player.ifButton(component(PAY_BUTTON))
            advance()

            assertEquals(5, player.inv.count("obj.coins"))
            assertEquals(fee, bank.count("obj.coins"))
            assertTrue(player.feePaid)
        }

    @Test
    fun GameTestState.`Death's Coffer pays before carried coins`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(pricey, 1)))
            val fee = deps.graves.fee(player)
            player.deathCoffer = fee + 3
            player.inv[0] = InvObj("obj.coins", fee)
            openGrave()

            player.ifButton(component(PAY_BUTTON))
            advance()

            assertEquals(3, player.deathCoffer)
            assertEquals(fee, player.inv.count("obj.coins"))
            assertTrue(player.feePaid)
        }

    @Test
    fun GameTestState.`paying without the coins changes nothing`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(pricey, 1)))
            val fee = deps.graves.fee(player)
            val bank = player.invMap.getOrPut("inv.bank")
            bank[0] = InvObj("obj.coins", fee - 1)
            openGrave()

            player.ifButton(component(PAY_BUTTON))
            advance()

            assertEquals(fee - 1, bank.count("obj.coins"))
            assertFalse(player.feePaid)
            assertEquals(1, player.grave.count(pricey.internalName))
        }

    @Test
    fun GameTestState.`looting puts objs back where they were and re-equips worn ones`() =
        runGraveTest { deps ->
            val wearable = checkNotNull(ServerCacheManager.getItem("obj.leather_boots".asRSCM(RSCMType.OBJ)))
            val wornSlot = wearable.wearpos1
            player.inv[5] = InvObj(cheap, 1)
            player.worn[wornSlot] = InvObj(wearable, 1)
            val lost = listOf(InvObj(cheap, 1), InvObj(wearable, 1))
            deps.graves.store(player, DEATH, DeathGrave(), lost)
            player.inv.fillNulls()
            player.worn.fillNulls()
            player.setVarBit("varbit.settings_gravestone_autoequip", 1)
            openGrave()

            player.ifButton(component(FREE_BUTTON))
            advance()

            assertEquals(InvObj(cheap, 1), player.inv[5])
            assertEquals(InvObj(wearable, 1), player.worn[wornSlot])
            assertTrue(player.grave.isEmpty())
            assertNull(player.graveCoords)
            assertEquals(0, player.graveDuration)
        }

    @Test
    fun GameTestState.`kept worn objs go back on first so grave objs regain their slots`() =
        runGraveTest { deps ->
            val boots = checkNotNull(ServerCacheManager.getItem("obj.leather_boots".asRSCM(RSCMType.OBJ)))
            player.inv[0] = InvObj(cheap, 1)
            player.worn[boots.wearpos1] = InvObj(boots, 1)
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            player.inv.fillNulls()
            player.worn.fillNulls()
            player.inv[0] = InvObj(boots, 1)
            player.setVarBit("varbit.settings_gravestone_autoequip", 1)
            openGrave()

            player.ifButton(component(FREE_BUTTON))
            advance()

            assertEquals(InvObj(cheap, 1), player.inv[0])
            assertEquals(InvObj(boots, 1), player.worn[boots.wearpos1])
        }

    @Test
    fun GameTestState.`death drops send the lost objs to the grave, not the floor`() =
        runGraveTest { deps ->
            player.inv[0] = InvObj(pricey, 1)
            player.inv[1] = InvObj(cheap, 1)
            player.inv[2] = InvObj(stack, 9)
            val handling =
                PlayerDeathHandling(
                    keepCount = 1,
                    dropReceiver = player,
                    dropDuration = PlayerDeathDrops.DROP_DURATION_STANDARD,
                    revealDelay = PlayerDeathDrops.PVP_REVEAL_DELAY,
                    supplyPile = false,
                    untradeableHandling = UntradeableHandling.DROP,
                    grave = DeathGrave(),
                )
            val result =
                deps.drops.selectDrops(
                    carried = player.inv.filterNotNull { true },
                    rules = PlayerDeathDrops.DeathDropRules(),
                    handling = handling,
                )
            deps.drops.applyDrops(player, result, handling, DEATH)

            val held = player.inv.filterNotNull { true } + player.grave.filterNotNull { true }
            assertEquals(setOf(InvObj(pricey, 1), InvObj(cheap, 1), InvObj(stack, 9)), held.toSet())
            assertEquals(1, player.inv.occupiedSpace())
        }

    @Test
    fun GameTestState.`food forms a supply pile while the setting is on`() =
        runGraveTest { deps ->
            val held = standardDeath(deps, supplyPilesDisabled = false)

            assertEquals(setOf(InvObj(pricey, 1), InvObj(cheap, 1)), held.toSet())
        }

    @Test
    fun GameTestState.`food goes to the grave while the setting is off`() =
        runGraveTest { deps ->
            val held = standardDeath(deps, supplyPilesDisabled = true)

            assertEquals(setOf(InvObj(pricey, 1), InvObj(cheap, 1), InvObj(food, 1)), held.toSet())
        }

    @Test
    fun GameTestState.`an expired grave moves everything to Death's Office`() =
        runGraveTest { deps ->
            val lost = listOf(InvObj(pricey, 1), InvObj(cheap, 1), InvObj(stack, 40))
            deps.graves.store(player, DEATH, DeathGrave(), lost)

            deps.graves.expire(player)

            assertTrue(player.grave.isEmpty())
            assertEquals(lost.toSet(), player.deathOffice.filterNotNull { true }.toSet())
            assertNull(player.graveCoords)
            assertEquals(0, player.graveDuration)
        }

    @Test
    fun GameTestState.`the grave expires when its timer runs out`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            player.graveDuration = Gravestones.TIMER_INTERVAL

            advance(ticks = Gravestones.TIMER_INTERVAL * 2)

            assertNull(player.graveCoords)
            assertEquals(1, player.deathOffice.count(cheap.internalName))
        }

    @Test
    fun GameTestState.`the grave timer pauses while the player is idle`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            PlayerIdle.recordInput(player)
            advance(ticks = Gravestones.IDLE_CYCLES + 1)
            val idleFrom = player.graveDuration

            advance(ticks = Gravestones.TIMER_INTERVAL * 3)
            assertEquals(idleFrom, player.graveDuration)

            PlayerIdle.recordInput(player)
            advance(ticks = Gravestones.TIMER_INTERVAL * 2)
            assertTrue(player.graveDuration < idleFrom)
        }

    @Test
    fun GameTestState.`a second death prunes resources, extra unstackables and supplies`() =
        runGraveTest { deps ->
            val first =
                listOf(
                    InvObj(pricey, 1),
                    InvObj("obj.bones", 3),
                    InvObj(cheap, 35),
                    InvObj(food, 2),
                )
            deps.graves.store(player, DEATH, DeathGrave(), first)
            val graveTile = checkNotNull(player.graveCoords)

            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 2)))

            assertEquals(30, player.grave.count(cheap.internalName))
            assertEquals(1, player.grave.count(pricey.internalName))
            assertEquals(0, player.grave.count("obj.bones"))
            assertEquals(0, player.grave.count(food.internalName))
            assertEquals(3, player.deathOffice.count("obj.bones"))
            assertEquals(7, player.deathOffice.count(cheap.internalName))
            val floor = deps.objs.findAll(graveTile).filter { it.type == food.id }.sumOf { it.count }
            assertEquals(2, floor)
        }

    @Test
    fun GameTestState.`a grave of only resources and supplies disappears on an empty death`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj("obj.bones", 1), InvObj(food, 1)))

            deps.graves.store(player, DEATH, DeathGrave(), emptyList())

            assertNull(player.graveCoords)
            assertEquals(1, player.deathOffice.count("obj.bones"))
        }

    @Test
    fun GameTestState.`a Wilderness PvM death deletes food instead of keeping it`() =
        runGraveTest { deps ->
            player.inv[0] = InvObj(pricey, 1)
            player.inv[1] = InvObj(cheap, 1)
            player.inv[2] = InvObj(food, 1)
            val handling =
                PlayerDeathHandling(
                    keepCount = 1,
                    dropReceiver = player,
                    dropDuration = PlayerDeathDrops.DROP_DURATION_STANDARD,
                    revealDelay = PlayerDeathDrops.PVP_REVEAL_DELAY,
                    supplyPile = false,
                    untradeableHandling = UntradeableHandling.DROP,
                    grave = DeathGrave(relocate = true),
                    deleteSupplies = true,
                )
            val result =
                deps.drops.selectDrops(
                    carried = player.inv.filterNotNull { true },
                    rules = PlayerDeathDrops.DeathDropRules(),
                    handling = handling,
                )
            deps.drops.applyDrops(player, result, handling, DEATH)

            val held = player.inv.filterNotNull { true } + player.grave.filterNotNull { true }
            assertEquals(setOf(InvObj(pricey, 1), InvObj(cheap, 1)), held.toSet())
            assertEquals(0, deps.objs.findAll(DEATH).count { it.type == food.id })
        }

    @Test
    fun GameTestState.`a relocating death moves the existing grave`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            val moved = DEATH.translate(10, 10)

            deps.graves.store(player, moved, DeathGrave(relocate = true), listOf(InvObj(stack, 3)))

            assertEquals(moved, player.graveCoords)
            assertEquals(1, player.grave.count(cheap.internalName))
            assertEquals(3, player.grave.count(stack.internalName))
        }

    @Test
    fun GameTestState.`the grave's op opens it for its owner`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            val grave = checkNotNull(player.graveCoords)
            player.teleport(grave.translateX(1))
            val npc = checkNotNull(npcRepo.findAll(grave).firstOrNull())

            player.opNpc1(npc)
            advance(ticks = 3)

            assertTrue(player.ui.containsOverlay(Gravestones.INTERFACE))
        }

    @Test
    fun GameTestState.`the grave opens from five tiles away without walking up`() =
        runGraveTest { deps ->
            deps.graves.store(player, DEATH, DeathGrave(), listOf(InvObj(cheap, 1)))
            val grave = checkNotNull(player.graveCoords)
            val start = grave.translateX(5)
            player.teleport(start)
            val npc = checkNotNull(npcRepo.findAll(grave).firstOrNull())

            player.opNpc1(npc)
            advance(ticks = 3)

            assertTrue(player.ui.containsOverlay(Gravestones.INTERFACE))
            assertEquals(start, player.coords)
        }

    class Deps
    @Inject
    constructor(val graves: Gravestones, val drops: PlayerDeathDrops, val objs: ObjRepository)

    private class GraveHookModule : AbstractModule() {
        override fun configure() {
            Multibinder.newSetBinder(binder(), PlayerGraveHook::class.java)
                .addBinding()
                .to(Gravestones::class.java)
        }
    }

    private fun GameTestState.runGraveTest(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, GraveHookModule(), GravestoneScript::class) { deps ->
            player.placeAt(DEATH)
            player.clearAllInvs()
            // Owner-only floor drops need it; test players don't log in.
            player.observerUUID = 1L
            player.deathCoffer = 0
            // Past the first-death tutorial, which holds the grave's timer.
            player.metDeath = true
            body(deps)
        }

    /** Dies with a pricey obj, a cheap obj and a food; returns what the inv and grave hold. */
    private fun GameTestScope.standardDeath(deps: Deps, supplyPilesDisabled: Boolean): List<InvObj> {
        player.setVarBit("varbit.gravestone_supplypiles_disabled", if (supplyPilesDisabled) 1 else 0)
        player.inv[0] = InvObj(pricey, 1)
        player.inv[1] = InvObj(cheap, 1)
        player.inv[2] = InvObj(food, 1)
        val context =
            PlayerDeathContext(
                player = player,
                coords = DEATH,
                inWilderness = false,
                wildernessLevel = 0,
                inRevenantCaves = false,
                inInstance = false,
                isSkulled = false,
                hasProtectItem = false,
                recentPvpDamage = false,
                gamemode = 0,
                killer = null,
            )
        val handling = checkNotNull(StandardPvmDeathHook().handleDeath(context)).copy(keepCount = 1)
        val result = deps.drops.selectDrops(player, context, handling)
        deps.drops.applyDrops(player, result, handling, DEATH)
        return player.inv.filterNotNull { true } + player.grave.filterNotNull { true }
    }

    private fun GameTestScope.openGrave() {
        player.ifOpenOverlay(Gravestones.INTERFACE, "component.toplevel_osrs_stretch:floater")
        player.ifSetEvents(FREE_ITEMS, 0 until GRAVE_SIZE, IfEvent.Op1, IfEvent.Op10)
        player.ifSetEvents(PAY_ITEMS, 0 until GRAVE_SIZE, IfEvent.Op1, IfEvent.Op10)
    }

    private fun GameTestScope.firstObjTypes(count: Int, predicate: (ItemServerType) -> Boolean) =
        findObjType(predicate).take(count).toList().also { check(it.size == count) }

    private val GameTestScope.cheap: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 1 until 1000 }

    private val GameTestScope.pricey: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 200_000 until 900_000 }

    private val GameTestScope.food: ItemServerType
        get() = firstObjType { !it.isStackable && it.cost in 1 until 1000 && SupplyItems.isFoodOrPotion(it) }

    private val GameTestScope.stack: ItemServerType
        get() = firstObjType { it.isStackable && it.cost in 1 until 100 }

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private companion object {
        val DEATH = CoordGrid(0, 50, 50, 21, 18)
        const val GRAVE_SIZE = 120
        const val FREE_ITEMS = "component.gravestone_generic:freeitems"
        const val PAY_ITEMS = "component.gravestone_generic:payitems"
        const val FREE_BUTTON = "component.gravestone_generic:freebutton"
        const val PAY_BUTTON = "component.gravestone_generic:paybutton"
    }
}
