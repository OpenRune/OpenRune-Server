package org.rsmod.content.minigames.sepulchre

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MysteriousStrangerTest {
    class StrangerTestDeps @Inject constructor(val objRepo: ObjRepository)

    private fun GameTestState.runStrangerTest(body: GameTestScope.(StrangerTestDeps) -> Unit) =
        runInjectedGameTest(
            StrangerTestDeps::class,
            null,
            MysteriousStrangerScript::class,
            SepulchreScript::class,
            SepulchreObstacleScript::class,
        ) { deps ->
            advance()
            player.observerUUID = player.uuid
            body(deps)
        }

    private fun GameTestScope.stranger(): Npc {
        val type = checkNotNull(ServerCacheManager.getNpc(STRANGER.asRSCM(RSCMType.NPC)))
        startAt(CoordGrid(2400, 5985, 0))
        return spawnNpc(CoordGrid(2400, 5986, 0), type)
    }

    private fun GameTestScope.talkTo(npc: Npc) {
        player.opNpc1(npc)
        advance(2)
        answer()
    }

    private fun GameTestScope.shardsOnFloor(deps: StrangerTestDeps): Int {
        val shard = SHARDS.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(player.coords).filter { it.type == shard }.sumOf { it.count }
    }

    @Test
    fun GameTestState.`the first talk explains the sepulchre and hands over five shards once`() =
        runStrangerTest {
            val npc = stranger()

            talkTo(npc)

            assertEquals(1, player.vars[TUTORIAL])
            assertEquals(5, player.inv.count(SHARDS))

            player.ifClose()
            advance(2)
            setVar(TUTORIAL, 0)
            talkTo(npc)
            assertEquals(5, player.inv.count(SHARDS))
        }

    @Test
    fun GameTestState.`a full pack gets the shards at the player's feet`() =
        runStrangerTest { deps ->
            player.fillInv()
            val pack = player.inv.objs.toList()
            val npc = stranger()

            talkTo(npc)

            assertEquals(pack, player.inv.objs.toList())
            assertEquals(5, shardsOnFloor(deps))
        }

    @Test
    fun GameTestState.`shards that would overflow the stack go to the floor`() =
        runStrangerTest { deps ->
            player.inv[0] = InvObj(SHARDS, Int.MAX_VALUE)
            val npc = stranger()

            talkTo(npc)

            assertEquals(Int.MAX_VALUE, player.inv.count(SHARDS))
            assertEquals(5, shardsOnFloor(deps))
        }

    private companion object {
        const val STRANGER = "npc.hallowed_lobby_npc"
        const val SHARDS = "obj.hallowed_teleport"
    }
}
