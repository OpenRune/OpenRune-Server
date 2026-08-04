package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningSideDropsScript
@Inject
constructor(
    private val random: GameRandom,
    private val objRepo: ObjRepository,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.skill != "stat.mining") {
                return@onEvent
            }
            val source = product.source as? SkillingProductSource.Mining ?: return@onEvent
            rollClueGeode(source)
            rollCrystalShard()
        }
    }

    private fun SkillingActionCompleteEvent.rollClueGeode(source: SkillingProductSource.Mining) {
        var chance = source.rockData.clueBaseChance
        if (chance <= 0) {
            return
        }
        if ("obj.ring_of_wealth_i" in player.worn && player.coords.isInWilderness(areaChecker)) {
            chance /= 2
        }
        if (random.of(chance) != 0) {
            return
        }
        val geode = random.pick(CLUE_GEODES)
        if (player.invAddOrDrop(objRepo, geode)) {
            player.mes("You find a clue geode!")
        } else {
            player.mes("A clue geode falls to the ground as your inventory is too full.")
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

        private val CLUE_GEODES =
            listOf(
                "obj.mining_clue_geode_beginner",
                "obj.mining_clue_geode_easy",
                "obj.mining_clue_geode_medium",
                "obj.mining_clue_geode_hard",
                "obj.mining_clue_geode_elite",
            )
    }
}
