package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.FarmingEnums.farming_nest_reward_names
import org.rsmod.api.enums.FarmingEnums.farming_ring_nest_rewards
import org.rsmod.api.enums.FarmingSeedEnums.farming_seed_nest_rewards
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
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingNestsScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        SEARCHABLE.forEach { nest -> onOpHeld1(nest) { search(it.slot, it.obj) } }
        onPlayerQueue("queue.farming_nest_search") { searchNext() }
    }

    private fun ProtectedAccess.search(slot: Int, expected: InvObj) {
        if (inv[slot] != expected) return
        val nest = getInvObj(expected).internalName
        val rewards = REWARDS[nest]
        val reward = if (rewards != null) {
            var roll = this@FarmingNestsScript.random.of(TOTAL_WEIGHTS.getValue(nest))
            rewards.entries.first { roll -= it.value; roll < 0 }.key
        } else {
            val egg = when (nest) {
                "obj.bird_nest_egg_red" -> "obj.bird_egg_red"
                "obj.bird_nest_egg_green" -> "obj.bird_egg_green"
                "obj.bird_nest_egg_blue" -> "obj.bird_egg_blue"
                else -> return
            }
            checkNotNull(ServerCacheManager.getItem(egg.asRSCM()))
        }
        val count = if (nest == "obj.bird_nest_decentseeds_jan2019") {
            checkNotNull(farming_wyson_seed_counts[reward])
        } else 1
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
        if (inv.indices.any { inv[it]?.id in SEARCHABLE_IDS }) {
            weakQueue("queue.farming_nest_search", SEARCH_INTERVAL)
        }
    }

    private fun ProtectedAccess.searchNext() {
        val slot = inv.indices.firstOrNull { inv[it]?.id in SEARCHABLE_IDS } ?: return
        search(slot, checkNotNull(inv[slot]))
    }

    // shortcut: ring/egg wording is unobserved; use the item noun until those searches are captured.
    private fun noun(item: ItemServerType): String =
        farming_nest_reward_names[item]?.takeIf { it.isNotEmpty() } ?:
            if (" - " in item.examine) {
                item.examine.substringBefore(" - ").replaceFirstChar { it.lowercaseChar() }
            } else item.name.lowercase().addArticle()

    private companion object {
        // shortcut: cadence/order are unobserved; queue delay 2 and lowest slot first mirror ItemPackScript, replace after a multi-nest capture.
        private const val SEARCH_INTERVAL = 2
        private val SEARCHABLE = setOf(
            "obj.bird_nest_seeds_jan2019",
            "obj.bird_nest_decentseeds_jan2019",
            "obj.bird_nest_ring",
            "obj.bird_nest_egg_red",
            "obj.bird_nest_egg_green",
            "obj.bird_nest_egg_blue",
        )
        private val SEARCHABLE_IDS by lazy { SEARCHABLE.map { it.asRSCM() }.toSet() }
        private val EMPTY_NEST_ID by lazy { "obj.bird_nest_empty".asRSCM() }
        private val REWARDS by lazy {
            mapOf(
                "obj.bird_nest_seeds_jan2019" to farming_seed_nest_rewards.filterValuesNotNull().backing,
                "obj.bird_nest_decentseeds_jan2019" to farming_wyson_nest_rewards.filterValuesNotNull().backing,
                "obj.bird_nest_ring" to farming_ring_nest_rewards.filterValuesNotNull().backing,
            )
        }
        private val TOTAL_WEIGHTS by lazy { REWARDS.mapValues { (_, rewards) -> rewards.values.sum() } }
    }
}
