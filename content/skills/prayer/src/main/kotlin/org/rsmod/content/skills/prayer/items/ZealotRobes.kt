package org.rsmod.content.skills.prayer.items

import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import kotlin.random.Random

internal object ZealotRobes {
    private const val PIECE_SAVE_CHANCE = 0.0125
    private const val MAX_SAVE_CHANCE = 0.05

    private const val HELM = "obj.shades_prayer_helm"
    private const val TOP = "obj.shades_prayer_top"
    private const val BOTTOM = "obj.shades_prayer_bottom"
    private const val BOOTS = "obj.shades_prayer_boots"

    fun Player.saveChance(): Double {
        var chance = 0.0
        if (hat.isType(HELM)) chance += PIECE_SAVE_CHANCE
        if (torso.isType(TOP)) chance += PIECE_SAVE_CHANCE
        if (legs.isType(BOTTOM)) chance += PIECE_SAVE_CHANCE
        if (feet.isType(BOOTS)) chance += PIECE_SAVE_CHANCE
        return chance.coerceAtMost(MAX_SAVE_CHANCE)
    }

    fun Player.shouldConsume(baseConsumeChance: Double = 1.0): Boolean {
        val cappedConsumeChance = baseConsumeChance.coerceIn(0.0, 1.0)
        if (cappedConsumeChance <= 0.0) {
            return false
        }
        val finalConsumeChance = cappedConsumeChance * (1.0 - saveChance())
        return Random.nextDouble() < finalConsumeChance
    }

    fun Player.countConsumed(total: Int, baseConsumeChance: Double = 1.0): Int {
        if (total <= 0) {
            return 0
        }
        var consumed = 0
        repeat(total) {
            if (shouldConsume(baseConsumeChance)) {
                consumed++
            }
        }
        return consumed
    }
}
