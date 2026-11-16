package org.rsmod.api.npc.hit.modifier

import jakarta.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.min
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.isStyleImmuneTo
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitBuilder

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
        if (isStyleImmuneTo(hit.type)) {
            hit.damage = 0
        }
    }

    private fun Npc.applyFlatArmour(hit: HitBuilder) {
        val armour = vars["varn.flat_armour"]
        if (hit.damage <= 0) return

        if (armour > 0) {
            hit.damage -= min(armour, hit.damage)
        } else if (armour < 0) {
            hit.damage += armour.absoluteValue
        }
    }
}
