package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.config.refs.params
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahMeleeReachTest {
    private val reach = ZulrahMeleeReach()

    @Test
    fun `halberds and all scythe variants gain shoreline reach against every form`() {
        for (weapon in LONG_WEAPONS) {
            val player = player(weapon)
            val nativeRange = getInvObj(InvObj(weapon)).param(params.attackrange)
            for (form in ZulrahCombatScript.BOSS_TYPES) {
                val boss = Npc(ZulrahEncounterManager.type(form), ANCHORS.first())
                assertEquals(3, reach.range(player, boss, nativeRange), "$weapon against $form")
            }
        }
    }

    @Test
    fun `native weapon ranges remain unchanged outside Zulrah`() {
        for (weapon in LONG_WEAPONS) {
            val player = player(weapon)
            val nativeRange = getInvObj(InvObj(weapon)).param(params.attackrange)
            for (symbol in listOf("npc.snakeboss_minion_melee", "npc.godwars_bandos_avatar")) {
                val target = Npc(ZulrahEncounterManager.type(symbol), ANCHORS.first())
                assertNull(reach.range(player, target, nativeRange), "$weapon against $symbol")
            }
        }
        assertEquals(2, getInvObj(InvObj("obj.noxious_halberd")).param(params.attackrange))
    }

    @Test
    fun `switching to ordinary melee or unarmed removes the extended reach`() {
        val player = player("obj.noxious_halberd")
        val boss = Npc(ZulrahEncounterManager.type(ZulrahCombatScript.BOSS_TYPES.first()), ANCHORS.first())
        assertEquals(3, reach.range(player, boss, 2))

        player.worn[Wearpos.RightHand.slot] = InvObj("obj.abyssal_whip")
        assertNull(reach.range(player, boss, 1))
        player.worn[Wearpos.RightHand.slot] = null
        assertNull(reach.range(player, boss, 1))
    }

    @Test
    fun `native routes from arena arrival reach every anchor with collision and line of sight`() {
        val collision = arenaCollision()
        val routes = RouteFactory(collision)
        val rays = RayCastValidator(collision)
        for (weapon in listOf("obj.noxious_halberd", "obj.scythe_of_vitur")) {
            val nativeRange = getInvObj(InvObj(weapon)).param(params.attackrange)
            for (form in ZulrahCombatScript.BOSS_TYPES) {
                for (anchor in ANCHORS) {
                    val player = player(weapon).apply { coords = ARRIVAL }
                    val boss = Npc(ZulrahEncounterManager.type(form), anchor)
                    val route = routes.create(player.avatar, boss.avatar)
                    assertTrue(route.success, "$weapon could not route to $form at $anchor")
                    val end = route.lastOrNull()?.let { CoordGrid(it.x, it.z, it.level) } ?: ARRIVAL
                    player.coords = end
                    assertFalse(
                        collision[end.x, end.z, end.level] and WALK_BLOCKERS != 0,
                        "Route ended on a blocked tile: $end",
                    )
                    val attackRange = checkNotNull(reach.range(player, boss, nativeRange))
                    assertTrue(player.isWithinDistance(boss, attackRange), "$anchor is out of reach from $end")
                    assertTrue(
                        rays.hasLineOfSight(end, anchor, destWidth = boss.size, destLength = boss.size),
                        "Line of sight blocked from $end to $anchor",
                    )
                }
            }
        }
    }

    @Test
    fun `shoreline reach does not bypass projectile blocking obstacles or planes`() {
        val collision = CollisionFlagMap()
        val player = player("obj.noxious_halberd").apply { coords = CoordGrid(2268, 3070) }
        val boss = Npc(ZulrahEncounterManager.type(ZulrahCombatScript.BOSS_TYPES.first()), ANCHORS.first())
        for (x in 2260..2278) for (z in 3068..3080) collision[x, z, 0] = 0
        collision[2268, 3072, 0] = CollisionFlag.BLOCK_WALK
        val rays = RayCastValidator(collision)
        assertTrue(rays.hasLineOfSight(player.coords, boss.coords, destWidth = 5, destLength = 5))

        collision[2268, 3072, 0] = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC_PROJ_BLOCKER
        assertFalse(rays.hasLineOfSight(player.coords, boss.coords, destWidth = 5, destLength = 5))
        player.coords = CoordGrid(2268, 3070, 1)
        assertFalse(player.isWithinDistance(boss, checkNotNull(reach.range(player, boss, 2))))
    }

    private fun player(weapon: String): Player =
        Player().apply {
            worn = Inventory.create("inv.worn")
            worn[Wearpos.RightHand.slot] = InvObj(weapon)
        }

    private fun arenaCollision(): CollisionFlagMap {
        val collision = CollisionFlagMap()
        val builder = GameMapBuilder()
        for (squareZ in 47..48) {
            val square = MapSquareKey(35, squareZ)
            val id = (35 shl 8) or squareZ
            val map = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, id, 0))))
            val locs = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, id, 1))))
            GameMapDecoder.putMaps(collision, square, map)
            GameMapDecoder.putLocs(builder, collision, square, map, locs)
        }
        return collision
    }

    companion object {
        private lateinit var cache: Cache
        private val ARRIVAL = CoordGrid(2268, 3069)
        private val ANCHORS = listOf(
            CoordGrid(2266, 3073), CoordGrid(2266, 3062),
            CoordGrid(2276, 3071), CoordGrid(2256, 3071),
        )
        private val LONG_WEAPONS = listOf(
            "obj.bronze_halberd", "obj.rune_halberd", "obj.dragon_halberd", "obj.noxious_halberd",
            "obj.scythe_of_vitur", "obj.scythe_of_vitur_uncharged",
            "obj.scythe_of_vitur_or", "obj.scythe_of_vitur_uncharged_or",
            "obj.scythe_of_vitur_bl", "obj.scythe_of_vitur_uncharged_bl",
        )
        private const val WALK_BLOCKERS = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        @JvmStatic
        @BeforeAll
        fun loadCache() {
            cache = ServerCacheManager.init(240)
        }

        @JvmStatic
        @AfterAll
        fun closeCache() {
            cache.close()
        }
    }
}
