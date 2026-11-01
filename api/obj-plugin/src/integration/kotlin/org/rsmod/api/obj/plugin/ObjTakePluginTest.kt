package org.rsmod.api.obj.plugin

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.objs.OpObjV2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.net.rsprot.handlers.OpObjHandler
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.map.collision.add
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class ObjTakePluginTest {
    @Test
    fun GameTestState.`a close pickup moves exactly the count to the inv`() =
        runTakeTest { deps ->
            val obj = deps.repo.add(stack, GROUND, duration = 100, receiver = player, count = 7)

            deps.opTake(player, obj)
            advance(ticks = 3)

            assertEquals(7, player.count(stack))
            assertEquals(0, deps.onGround(stack))
        }

    @Test
    fun GameTestState.`a far pickup moves exactly the count to the inv`() =
        runTakeTest { deps ->
            val obj = deps.repo.add(stack, GROUND, duration = 100, receiver = player, count = 7)
            deps.blockFloor()
            player.teleport(GROUND.translateX(1))

            deps.opTake(player, obj)
            advance(ticks = 5)

            assertEquals(7, player.count(stack))
            assertEquals(0, deps.onGround(stack))
        }

    @Test
    fun GameTestState.`a pickup with a full inv leaves the obj on the ground`() =
        runTakeTest { deps ->
            val obj = deps.repo.add(stack, GROUND, duration = 100, receiver = player, count = 7)
            player.fillInv(InvObj(filler, 1))

            deps.opTake(player, obj)
            advance(ticks = 3)

            assertEquals(0, player.count(stack))
            assertEquals(7, deps.onGround(stack))
        }

    @Test
    fun GameTestState.`a far pickup keeps the obj when the inv fills during the delay`() =
        runTakeTest { deps ->
            val obj = deps.repo.add(stack, GROUND, duration = 100, receiver = player, count = 7)
            deps.blockFloor()
            player.teleport(GROUND.translateX(1))

            deps.opTake(player, obj)
            advanceUntil({ player.isDelayed })
            player.fillInv(InvObj(filler, 1))
            advance(ticks = 5)

            assertEquals(0, player.count(stack))
            assertEquals(7, deps.onGround(stack))
        }

    class Deps
    @Inject
    constructor(
        val repo: ObjRepository,
        private val opObj: OpObjHandler,
        private val collision: CollisionFlagMap,
    ) {
        /* An obj on a blocked tile is taken from the adjacent tile after `delay(1)`. */
        fun blockFloor() {
            collision.add(GROUND, CollisionFlag.LOC)
        }

        fun opTake(player: Player, obj: Obj) {
            opObj.handle(player, OpObjV2(obj.type, obj.x, obj.z, false, op = 3, subop = 0))
        }

        fun onGround(type: ItemServerType): Int =
            repo.findAll(GROUND).filter { it.type == type.id }.sumOf { it.count }
    }

    private fun GameTestState.runTakeTest(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, ObjTakePlugin::class) { deps ->
            player.placeAt(GROUND)
            player.clearAllInvs()
            // Owner-only floor objs are only visible to a player with an observer id.
            player.observerUUID = 1L
            body(deps)
        }

    private val GameTestScope.stack
        get() = firstObjType { it.isStackable && it.cost > 0 }

    private val GameTestScope.filler
        get() = firstObjType { !it.isStackable && it.cost > 0 }

    private companion object {
        val GROUND = CoordGrid(0, 50, 50, 10, 10)
    }
}
