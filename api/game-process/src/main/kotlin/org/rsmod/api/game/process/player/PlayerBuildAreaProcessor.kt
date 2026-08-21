package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import org.rsmod.api.utils.map.BuildAreaUtils
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.WorldEntityList
import org.rsmod.map.zone.ZoneKey

public class PlayerBuildAreaProcessor
@Inject
constructor(private val worldEntities: WorldEntityList) {
    public fun process(player: Player) {
        player.processBuildAreaChange()
    }

    private fun Player.processBuildAreaChange() {
        // Players aboard a world entity keep the root build area anchored to the entity's
        // root-world position: their own coords are instance-land coords, but the client's
        // root map must follow the boat. No rebuild on embark; recentre only when the
        // entity nears the build-area boundary (mid-sail recentre).
        val worldEntity = worldEntities.findAt(coords)
        if (worldEntity != null) {
            val entityCoords = worldEntity.coords
            if (BuildAreaUtils.isOutsideOfBuildArea(entityCoords, buildArea)) {
                buildArea = BuildAreaUtils.calculateBuildArea(ZoneKey.from(entityCoords))
            }
            return
        }
        val rebuildBuildArea = BuildAreaUtils.requiresNewBuildArea(this)
        if (rebuildBuildArea) {
            enterBuildArea()
        }
    }

    private fun Player.enterBuildArea() {
        buildArea = BuildAreaUtils.calculateBuildArea(ZoneKey.from(coords))
    }
}
