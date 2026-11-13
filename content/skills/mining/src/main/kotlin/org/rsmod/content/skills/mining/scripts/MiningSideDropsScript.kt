package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningSideDropsScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.skill != "stat.mining") {
                return@onEvent
            }
            if (product.source !is SkillingProductSource.Mining) {
                return@onEvent
            }
            rollCrystalShard()
        }
    }

    private fun SkillingActionCompleteEvent.rollCrystalShard() {
        if (MapSquareKey.from(player.coords).id != TRAHAEARN_REGION) {
            return
        }
        if (random.of(65) != 0) {
            return
        }
        if (player.invAdd(player.inv, "obj.prif_crystal_shard").success) {
            player.mes("You have obtained a crystal shard!")
        }
    }

    private companion object {
        private const val TRAHAEARN_REGION = 13250
    }
}
