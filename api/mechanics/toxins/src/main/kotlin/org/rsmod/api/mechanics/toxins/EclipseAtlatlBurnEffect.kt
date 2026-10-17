package org.rsmod.api.mechanics.toxins

import jakarta.inject.Inject
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Eclipse moon armour's set effect: wiki-verified 20% chance for a successful Eclipse atlatl hit
 * to inflict a stack of Burn on the target, while the full set (helm/chestplate/tassets/atlatl)
 * is worn. This is what feeds the Eclipse special's own burn-consumption bonus - without this, the
 * special's "consume remaining burn damage" mechanic never has anything to consume from a solo
 * player's own attacks (it can still trigger off a teammate's Burning claws/Arkan blade/Scorching
 * bow burn, per the wiki, but that's not a normal solo case).
 *
 * Deliberately scoped to *normal* attacks only, matching the special attack's own design: the
 * special consumes burn rather than re-applying it, so a solo player alternates ordinary attacks
 * (building burn via this passive) with occasional specials (cashing it in) - having the special
 * also roll this chance would make it self-consuming and pointless.
 */
public class EclipseAtlatlBurnEffect
@Inject
constructor(
    private val burns: BurnEffectService,
    private val random: GameRandom,
) {
    public fun rollOnHit(source: Player, target: PathingEntity, damage: Int) {
        if (damage <= 0) return
        val wearingFullSet =
            EquipmentChecks.isEclipseMoonSet(
                helm = source.hat,
                top = source.torso,
                legs = source.legs,
                weapon = source.righthand,
            )
        if (!wearingFullSet) return
        if (!random.randomBoolean(BURN_CHANCE_DENOMINATOR)) return
        burns.apply(source, target)
    }

    private companion object {
        const val BURN_CHANCE_DENOMINATOR: Int = 5
    }
}
