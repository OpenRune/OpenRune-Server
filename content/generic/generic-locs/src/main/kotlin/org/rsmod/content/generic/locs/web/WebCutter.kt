package org.rsmod.content.generic.locs.web

import dev.openrune.types.ItemServerType
import dev.openrune.util.WeaponCategory
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.map.CoordGrid

@Singleton
class WebCutter @Inject constructor(private val locRepo: LocRepository) {

    suspend fun useItem(
        access: ProtectedAccess,
        web: BoundLocInfo,
        item: ItemServerType,
        slashedLoc: String,
    ) {
        if (!WebInteraction.isEligible(item)) return
        cut(access, web, slashedLoc, usedItem = item)
    }

    /**
     * Runs the web-cutting interaction. [slashedLoc] is swapped in on success when set; [onCut]
     * runs after a successful cut or an aranea-boots wade-through.
     */
    suspend fun cut(
        access: ProtectedAccess,
        web: BoundLocInfo,
        slashedLoc: String? = null,
        usedItem: ItemServerType? = null,
        moveThroughOnBoots: Boolean = true,
        onCut: suspend ProtectedAccess.() -> Unit = {},
    ): Boolean = with(access) {
        arriveDelay()
        faceLoc(web)

        if (usedItem == null && WebInteraction.hasAraneaBoots(this)) {
            if (moveThroughOnBoots) {
                wadeThroughWeb(web)
            } else {
                WebInteraction.wade(this)
            }
            onCut()
            return true
        }

        val item = usedItem ?: WebInteraction.bestSlashItem(this)
        if (item == null) {
            mes(WebInteraction.NO_BLADE_MESSAGE)
            return false
        }

        val overrideWeapon = player.righthand?.id != item.id
        val previousOverride = player.appearance.weaponOverride
        if (overrideWeapon) {
            player.appearance.weaponOverride = InvObj(item, 1)
        }
        try {
            anim(WebInteraction.slashAnimation(item))
            delay(2)
            if (random.of(100) >= slashChance(WebInteraction.slashBonus(item), item)) {
                mes(FAIL_MESSAGE)
                return false
            }

            mes(SUCCESS_MESSAGE)
            if (slashedLoc != null) {
                cutWeb(web, slashedLoc)
            }
            onCut()
            return true
        } finally {
            if (overrideWeapon) {
                player.appearance.weaponOverride = previousOverride
            }
        }
    }

    suspend fun passThrough(access: ProtectedAccess, web: BoundLocInfo) = with(access) {
        arriveDelay()
        faceLoc(web)
        approach(web)
        cross(web)
    }

    private fun cutWeb(web: BoundLocInfo, slashedLoc: String) {
        // `change` leaves the static map loc's collision behind, keeping the slashed web blocked.
        locRepo.del(web, WEB_RESPAWN_TICKS)
        locRepo.add(web.coords, slashedLoc, WEB_RESPAWN_TICKS, web.angle, web.shape)
    }

    private suspend fun ProtectedAccess.wadeThroughWeb(web: BoundLocInfo) {
        approach(web)
        WebInteraction.wade(this)
        cross(web)
    }

    private suspend fun ProtectedAccess.approach(web: BoundLocInfo) {
        val normal = wallNormal(web)
        if (normal != null) {
            // Stand on the player's side of the wall before crossing.
            val front = web.coords.translate(normal)
            val along =
                (coords.x - web.coords.x) * normal.xOff +
                    (coords.z - web.coords.z) * normal.zOff
            val side = if (along > 0) front else web.coords
            moveCardinalTo(side)
            return
        }
        if (coords == web.coords) return
        val dx = web.coords.x - coords.x
        val dz = web.coords.z - coords.z
        if (dx == 0 || dz == 0) return
        val xNeighbour = web.coords.translate(if (dx > 0) -1 else 1, 0)
        val zNeighbour = web.coords.translate(0, if (dz > 0) -1 else 1)
        val candidates =
            if (abs(dx) >= abs(dz)) {
                listOf(xNeighbour, zNeighbour)
            } else {
                listOf(zNeighbour, xNeighbour)
            }
        val neighbour = candidates.firstOrNull { !mapBlocked(it) } ?: candidates.first()
        moveCardinalTo(neighbour)
    }

    private suspend fun ProtectedAccess.cross(web: BoundLocInfo) {
        val normal = wallNormal(web)
        if (normal != null) {
            // Wall webs cross to the other side; ground webs step over the web tile.
            val front = web.coords.translate(normal)
            val along =
                (coords.x - web.coords.x) * normal.xOff +
                    (coords.z - web.coords.z) * normal.zOff
            val side = if (along > 0) front else web.coords
            val opposite = if (side == web.coords) front else web.coords
            val waypoints = if (coords == side) listOf(opposite) else listOf(side, opposite)
            forcedWalk(waypoints, crossTiles = waypoints.size)
            return
        }
        val destination =
            if (coords == web.coords) {
                web.coords.translate(web.angle.toDirection())
            } else {
                val dx = web.coords.x - coords.x
                val dz = web.coords.z - coords.z
                val direction =
                    when {
                        dx > 0 -> Direction.East
                        dx < 0 -> Direction.West
                        dz > 0 -> Direction.North
                        else -> Direction.South
                    }
                web.coords.translate(direction)
            }
        val waypoints =
            if (coords == web.coords) {
                listOf(destination)
            } else {
                listOf(web.coords, destination)
            }
        forcedWalk(waypoints, crossTiles = waypoints.size)
    }

    private suspend fun ProtectedAccess.moveCardinalTo(target: CoordGrid) {
        if (coords == target) return
        val dx = target.x - coords.x
        val dz = target.z - coords.z
        if (dx != 0 && dz != 0) {
            val xStep = coords.translate(if (dx > 0) 1 else -1, 0)
            val zStep = coords.translate(0, if (dz > 0) 1 else -1)
            val candidates =
                if (abs(dx) >= abs(dz)) {
                    listOf(xStep, zStep)
                } else {
                    listOf(zStep, xStep)
                }
            val step = candidates.firstOrNull { !mapBlocked(it) } ?: candidates.first()
            forcedWalk(listOf(step), crossTiles = 1)
        }
        if (coords != target) {
            forcedWalk(listOf(target), crossTiles = 2)
        }
    }

    private fun wallNormal(web: BoundLocInfo): Direction? =
        if (web.shape == LocShape.WallStraight) web.angle.toDirection() else null

    private fun LocAngle.toDirection(): Direction =
        when (this) {
            LocAngle.West -> Direction.West
            LocAngle.North -> Direction.North
            LocAngle.East -> Direction.East
            LocAngle.South -> Direction.South
        }

    private fun slashChance(slashBonus: Int, weapon: ItemServerType): Int {
        if (weapon.name.contains("wilderness sword", ignoreCase = true)) return 100
        if (WebInteraction.isKnife(weapon)) return 50
        val chance =
            when {
                slashBonus >= 100 -> 100
                slashBonus >= 20 -> 50 + (slashBonus - 20) * 50 / 80
                else -> 20
            }
        return if (weapon.weaponCategory == WeaponCategory.SlashSword) maxOf(chance, 50) else chance
    }

    private companion object {
        private const val WEB_RESPAWN_TICKS = 50

        private const val FAIL_MESSAGE = "You fail to cut through it."
        private const val SUCCESS_MESSAGE = "You slash the web apart."
    }
}
