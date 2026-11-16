package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.misc.user.MoveMinimapClick
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.map.CoordGrid
import sun.misc.Unsafe

@ResourceLock("ServerCacheManager")
class CutsceneMoveClickTest {
    @Test fun `world and minimap clicks cannot replace a route during a cutscene`() {
        for (minimap in listOf(false, true)) {
            val player = Player()
            val original = RouteRequestCoord(CoordGrid(3200, 3200), clientRequest = true)
            player.routeRequest = original
            VarPlayerIntMapSetter.set(player, "varbit.cutscene_status", 1)

            if (minimap) {
                uninitialized<MoveMinimapClickHandler>().handle(
                    player, MoveMinimapClick(3210, 3210, 0, 0, 0, 0, 0, 0)
                )
            } else {
                uninitialized<MoveGameClickHandler>().handle(player, MoveGameClick(3210, 3210, 0))
            }

            assertSame(original, player.routeRequest)
        }
    }

    private inline fun <reified T> uninitialized(): T {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return (field.get(null) as Unsafe).allocateInstance(T::class.java) as T
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
