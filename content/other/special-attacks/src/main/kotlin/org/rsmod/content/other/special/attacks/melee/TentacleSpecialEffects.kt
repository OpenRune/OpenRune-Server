package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.impl.NpcPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

internal object TentacleSpecialEffects {
    fun attach(hit: Hit, source: Player, target: PathingEntity, rng: GameRandom) {
        val sourceUid = source.uid.packed
        val targetUid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
        hit.impactEffects.add {
            val valid = when (target) {
                is Npc -> target.isSlotAssigned && target.uid.packed == targetUid && target.hitpoints > 0
                is Player -> target.isSlotAssigned && target.uid.packed == targetUid && target.stat("stat.hitpoints") > 0
            }
            if (valid && source.isSlotAssigned && source.uid.packed == sourceUid) {
                when (target) {
                    is Npc -> {
                        val resistance = target.visType.paramOrNull(params.freeze_resistance) ?: 0
                        if (rng.of(100) >= resistance) CombatEffects.freeze(target, 8)
                        if (rng.of(2) == 0) NpcPoison.tryPoison(target, PlayerPoison.severityForInitialDamage(4))
                    }
                    is Player -> {
                        CombatEffects.freeze(target, 8)
                        if (rng.of(2) == 0) PlayerPoison.tryPoison(target, initialDamage = 4)
                    }
                }
            }
        }
    }
}
