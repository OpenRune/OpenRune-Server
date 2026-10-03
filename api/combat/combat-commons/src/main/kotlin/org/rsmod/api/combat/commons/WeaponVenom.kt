package org.rsmod.api.combat.commons

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.api.mechanics.toxins.impl.NpcVenom
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

public object WeaponVenom {
    private val serpentHelms: List<String> = listOf("obj.serpentine_helm_charged", "obj.serpentine_helm_charged_cyan", "obj.serpentine_helm_charged_red")

    public fun attach(hit: Hit, source: Player, target: PathingEntity, random: GameRandom) {
        val guaranteed = target is Npc && serpentHelms.any { source.worn[Wearpos.Hat.slot]?.id == it.asRSCM() }
        hit.impactEffects.add { actualDamage ->
            if (actualDamage > 0 && (guaranteed || random.of(4) == 0)) {
                when (target) {
                    is Npc -> NpcVenom.tryVenom(target)
                    is Player -> PlayerVenom.tryVenom(target)
                }
            }
        }
    }
}
