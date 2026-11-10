package org.rsmod.api.player.stat

import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Player

/**
 * Plays the level-up spot animation on this avatar and the launch and burst sounds nearby.
 *
 * [spotanim] can select the standard, level 99, or maxed effect. This only sends the shared
 * effects so it can also be used for an avatar without its own client session.
 */
public fun Player.levelUpFireworks(
    world: WorldRepository,
    spotanim: String = "spotanim.levelup_anim",
) {
    this.spotanim(spotanim, height = 124)
    world.soundArea(coords, "synth.firework", radius = 1)
    world.soundArea(coords, "synth.burst", delay = 25, radius = 1)
}
