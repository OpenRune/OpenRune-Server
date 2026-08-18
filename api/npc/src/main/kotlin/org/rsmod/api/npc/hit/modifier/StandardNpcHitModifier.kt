package org.rsmod.api.npc.hit.modifier

import jakarta.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.min
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

public class StandardNpcHitModifier @Inject constructor(private val eventBus: EventBus) :
    NpcHitModifier {
    override fun HitBuilder.modify(target: Npc) {
        target.publishEvent(this)
        target.applyStyleImmunity(this)
        target.applyFlatArmour(this)
    }

    private fun Npc.publishEvent(hit: HitBuilder) {
        val event = NpcHitEvents.Modify(this, hit)
        eventBus.publish(event)
    }

    private fun Npc.applyStyleImmunity(hit: HitBuilder) {
        val immune =
            when (hit.type) {
                HitType.Typeless -> false
                HitType.Melee -> vars["varn.immune_melee"] == 1
                HitType.Ranged -> vars["varn.immune_ranged"] == 1
                HitType.Magic -> vars["varn.immune_magic"] == 1
            }
        if (immune) {
            hit.damage = 0
        }
    }

    private fun Npc.applyFlatArmour(hit: HitBuilder) {
        val armour = vars["varn.flat_armour"]

        if (armour > 0) {
            val capped = min(armour.absoluteValue, hit.damage)
            vars["varn.flat_armour"] -= capped
            hit.damage -= capped
        }

        if (armour < 0) {
            vars["varn.flat_armour"] = 0
            hit.damage += armour.absoluteValue
        }
    }
}
