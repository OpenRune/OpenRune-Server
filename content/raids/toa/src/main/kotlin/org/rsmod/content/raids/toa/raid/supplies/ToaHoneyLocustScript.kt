package org.rsmod.content.raids.toa.raid.supplies

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.other.consumables.ActivityConsumable
import org.rsmod.content.other.consumables.ConsumableActivityAccess
import org.rsmod.content.other.consumables.ConsumableDelayState
import org.rsmod.content.other.consumables.ConsumableType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaHoneyLocustScript
@Inject
constructor(private val activityAccess: ConsumableActivityAccess) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(LOCUST) { eat(it.slot) }
    }

    private fun ProtectedAccess.eat(slot: Int) {
        if (!ConsumableDelayState.canConsume(access = this, type = ConsumableType.FOOD)) return
        val food = ActivityConsumable(type = ConsumableType.FOOD, restoresHitpoints = true)
        val refusal = activityAccess.refusal(player, food)
        if (refusal != null) {
            mes(refusal)
            return
        }
        if (invDel(inv, LOCUST, count = 1, slot = slot).failure) return

        anim(EAT_ANIM)
        soundSynth(EAT_SOUND)
        mes("You eat the honey locust.")
        statBoost(HITPOINTS, constant = HEAL, percent = 0)
        statHeal(PRAYER, constant = PRAYER_CONSTANT, percent = QUARTER)
        for (stat in COMBAT_STATS) {
            statHeal(stat, constant = STAT_CONSTANT, percent = QUARTER)
        }
        ConsumableDelayState.recordConsumption(
            access = this,
            type = ConsumableType.FOOD,
            consumeDelay = EAT_DELAY,
            combatDelay = EAT_DELAY,
        )
    }

    private companion object {
        const val LOCUST = "obj.toa_honey_locust"

        const val HEAL = 20
        const val PRAYER_CONSTANT = 7
        const val STAT_CONSTANT = 8
        const val QUARTER = 25

        const val EAT_DELAY = 3
        const val EAT_SOUND = "synth.dom_burrow_slam"
        const val EAT_ANIM = "seq.human_eat"

        const val HITPOINTS = "stat.hitpoints"
        const val PRAYER = "stat.prayer"
        val COMBAT_STATS =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")
    }
}
