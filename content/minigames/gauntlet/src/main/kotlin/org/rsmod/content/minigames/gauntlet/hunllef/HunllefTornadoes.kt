package org.rsmod.content.minigames.gauntlet.hunllef

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.sign
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType

@Singleton
class HunllefTornadoes @Inject constructor(private val deps: BossDeps) {
    fun registerExtensions() {
        deps.extensionRegistry.register(CHASE_EXT) { _, tornado, target, params ->
            chase(tornado, target, params as Boolean)
        }
    }

    private fun chase(tornado: Npc, player: Player, corrupted: Boolean) {
        var paused = false
        deps.repeatTick(
            ticks = LIFETIME_TICKS,
            onTick = { _ ->
                if (!tornado.isSlotAssigned || player.hitpoints <= 0) return@repeatTick false
                if (paused) {
                    paused = false
                    return@repeatTick true
                }
                if (tornado.coords == player.coords) {
                    strike(tornado, player, corrupted)
                    paused = true
                } else {
                    stepToward(tornado, player)
                }
                true
            },
        )
    }

    private fun stepToward(tornado: Npc, player: Player) {
        val dx = (player.coords.x - tornado.coords.x).sign
        val dz = (player.coords.z - tornado.coords.z).sign
        PathingEntityCommon.teleport(tornado, deps.collision, tornado.coords.translate(dx, dz))
    }

    private fun strike(tornado: Npc, player: Player, corrupted: Boolean) {
        val tiers = HunllefDamage.armourTiers(player, corrupted)
        val damage = HunllefDamage.rollTornado(corrupted, tiers)
        player.spotanim(if (corrupted) HIT_SPOTANIM_HM else HIT_SPOTANIM, height = HIT_HEIGHT)
        player.queueHit(tornado, 1, HitType.Typeless, damage, deps.playerHitModifier)
    }

    companion object {
        const val CHASE_EXT = "hunllef_tornado_chase"
        const val LIFETIME_TICKS = 20
        private const val HIT_HEIGHT = 0
        private const val HIT_SPOTANIM = "spotanim.crystal_hunllef_crystals_hit"
        private const val HIT_SPOTANIM_HM = "spotanim.crystal_hunllef_crystals_hit_hm"
    }
}
