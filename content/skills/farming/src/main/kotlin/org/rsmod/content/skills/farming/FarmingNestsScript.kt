package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.BirdNestEnums.bird_nest_reward_names
import org.rsmod.api.enums.FarmingWysonEnums.farming_wyson_nest_rewards
import org.rsmod.api.enums.FarmingWysonEnums.farming_wyson_seed_counts
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.utils.format.addArticle
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingNestsScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(WYSON_NEST) { search(it.slot, it.obj) }
        onPlayerQueue("queue.farming_nest_search") { searchNext() }
    }

    private fun ProtectedAccess.search(slot: Int, expected: InvObj) {
        if (inv[slot] != expected) return
        var roll = this@FarmingNestsScript.random.of(TOTAL_WEIGHT)
        val reward = REWARDS.entries.first { roll -= it.value; roll < 0 }.key
        val count = farming_wyson_seed_counts[reward] ?: checkNotNull(farming_wyson_seed_counts.default)
        val result = player.invTransaction(inv) {
            val target = select(inv)
            delete(target, expected.id, 1, slot)
            add(target, EMPTY_NEST_ID, 1, slot = slot)
            add(target, reward.id, count)
        }
        if (!result.success) {
            mes("You don't have enough inventory space.")
            return
        }
        spam("You take ${noun(reward)} out of the bird's nest.")
        if (inv.indices.any { inv[it]?.id == WYSON_NEST_ID }) {
            weakQueue("queue.farming_nest_search", SEARCH_INTERVAL)
        }
    }

    private fun ProtectedAccess.searchNext() {
        val slot = inv.indices.firstOrNull { inv[it]?.id == WYSON_NEST_ID } ?: return
        search(slot, checkNotNull(inv[slot]))
    }

    private fun noun(item: ItemServerType): String =
        bird_nest_reward_names[item]?.takeIf { it.isNotEmpty() } ?:
            if (" - " in item.examine) {
                item.examine.substringBefore(" - ").replaceFirstChar { it.lowercaseChar() }
            } else item.name.lowercase().addArticle()

    private companion object {
        // UNVERIFIED: auto-queue delay and slot order (guess); mirrors the bird nest search.
        private const val SEARCH_INTERVAL = 2
        private const val WYSON_NEST = "obj.bird_nest_decentseeds_jan2019"
        private val WYSON_NEST_ID by lazy { WYSON_NEST.asRSCM() }
        private val EMPTY_NEST_ID by lazy { "obj.bird_nest_empty".asRSCM() }
        private val REWARDS by lazy { farming_wyson_nest_rewards.filterValuesNotNull().backing }
        private val TOTAL_WEIGHT by lazy { REWARDS.values.sum() }
    }
}
