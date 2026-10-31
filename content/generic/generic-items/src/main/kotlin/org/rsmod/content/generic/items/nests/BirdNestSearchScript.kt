package org.rsmod.content.generic.items.nests

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.BirdNestEnums.bird_nest_egg_rewards
import org.rsmod.api.enums.BirdNestEnums.bird_nest_reward_names
import org.rsmod.api.enums.BirdNestEnums.bird_nest_ring_rewards
import org.rsmod.api.enums.BirdNestEnums.bird_nest_seed_rewards
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

class BirdNestSearchScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    private val eggs by lazy { bird_nest_egg_rewards.filterValuesNotNull().backing }
    private val weighted by lazy {
        mapOf(
            item("obj.bird_nest_seeds_jan2019") to
                bird_nest_seed_rewards.filterValuesNotNull().backing,
            item("obj.bird_nest_ring") to bird_nest_ring_rewards.filterValuesNotNull().backing,
        )
    }
    private val searchable by lazy { eggs.keys + weighted.keys }
    private val searchableIds by lazy { searchable.map { it.id }.toSet() }
    private val emptyNest by lazy { "obj.bird_nest_empty".asRSCM() }

    private fun item(name: String) = checkNotNull(ServerCacheManager.getItem(name.asRSCM()))

    override fun ScriptContext.startup() {
        searchable.forEach { nest -> onOpHeld1(nest) { search(it.slot, it.obj) } }
        onPlayerQueue("queue.bird_nest_search") { searchNext() }
    }

    private fun ProtectedAccess.search(slot: Int, expected: InvObj) {
        if (inv[slot] != expected) return
        val nest = getInvObj(expected)
        val reward = rewardFor(nest) ?: return
        val result =
            player.invTransaction(inv) {
                val target = select(inv)
                delete(target, expected.id, 1, slot)
                add(target, emptyNest, 1, slot = slot)
                add(target, reward.id, 1)
            }
        if (!result.success) {
            mes("You don't have enough inventory space.")
            return
        }
        spam("You take ${noun(reward)} out of the bird's nest.")
        if (inv.indices.any { inv[it]?.id in searchableIds }) {
            weakQueue("queue.bird_nest_search", SEARCH_INTERVAL)
        }
    }

    private fun ProtectedAccess.searchNext() {
        val slot = inv.indices.firstOrNull { inv[it]?.id in searchableIds } ?: return
        search(slot, checkNotNull(inv[slot]))
    }

    private fun rewardFor(nest: ItemServerType): ItemServerType? {
        eggs[nest]?.let {
            return it
        }
        val weights = weighted[nest] ?: return null
        var roll = random.of(weights.values.sum())
        return weights.entries.first {
            roll -= it.value
            roll < 0
        }.key
    }

    // UNVERIFIED: ring and egg search wording (guess), so the item noun is used until captured.
    private fun noun(item: ItemServerType): String =
        bird_nest_reward_names[item]?.takeIf { it.isNotEmpty() }
            ?: if (" - " in item.examine) {
                item.examine.substringBefore(" - ").replaceFirstChar { it.lowercaseChar() }
            } else item.name.lowercase().addArticle()

    private companion object {
        // UNVERIFIED: auto-queue delay and slot order (guess); mirrors ItemPackScript.
        private const val SEARCH_INTERVAL = 2
    }
}
