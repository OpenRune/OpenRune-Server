package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BirdNestSearchScript @Inject constructor(private val storage: PlayerItemStorage) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((nest, egg) in eggs) onOpHeld1(nest) { search(it.inventory, it.slot, nest, egg) }
        onOpHeld1(RING) { search(it.inventory, it.slot, RING, pick(rings)) }
        onOpHeld1(SEEDS) { search(it.inventory, it.slot, SEEDS, pick(seeds)) }
        onOpHeld1(OLD_SEEDS) { search(it.inventory, it.slot, OLD_SEEDS, pick(seeds.take(16))) }
    }

    private fun ProtectedAccess.pick(table: List<Pair<String, Int>>): String {
        var roll = random.of(table.sumOf { it.second })
        for ((item, weight) in table) {
            if (roll < weight) return item
            roll -= weight
        }
        error("Nest reward roll exceeded its table")
    }

    private fun ProtectedAccess.search(inventory: Inventory, slot: Int, nest: String, reward: String) {
        if (inventory !== inv || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        val current = inventory[slot] ?: return
        if (current.id != nest.asRSCM()) return
        val transfer = storage.planTransfer(player, inventory, reward, 1, incoming = true)
        val result = player.invTransaction(inventory, autoCommit = false) {
            val selected = select(inventory)
            delete(selected, current.id, 1, slot = slot)
            add(selected, "obj.bird_nest_empty".asRSCM(), 1)
            if (transfer.amount == 0) add(selected, reward.asRSCM(), 1)
        }
        if (result.failure || !transfer.valid()) {
            mes("You need more inventory space to search this nest.")
            return
        }
        result.commitAll()
        transfer.commit()
        mes("You search the nest and find something inside.")
    }

    internal companion object {
        const val RING = "obj.bird_nest_ring"
        const val SEEDS = "obj.bird_nest_seeds_jan2019"
        const val OLD_SEEDS = "obj.bird_nest_seeds"
        val eggs = listOf("red", "blue", "green").associate { "obj.bird_nest_egg_$it" to "obj.bird_egg_$it" }
        val rings = listOf("gold" to 35, "sapphire" to 40, "emerald" to 15, "ruby" to 9, "diamond" to 1)
            .map { "obj.${it.first}_ring" to it.second }
        val seeds = listOf(
            "acorn" to 214, "apple_tree_seed" to 170, "banana_tree_seed" to 108,
            "orange_tree_seed" to 85, "willow_seed" to 135, "teak_seed" to 4,
            "curry_tree_seed" to 68, "maple_seed" to 54, "pineapple_tree_seed" to 42,
            "mahogany_seed" to 4, "papaya_tree_seed" to 34, "palm_tree_seed" to 22,
            "calquat_tree_seed" to 17, "yew_seed" to 27, "magic_tree_seed" to 5,
            "spirit_tree_seed" to 11, "celastrus_tree_seed" to 3, "redwood_tree_seed" to 2,
            "dragonfruit_tree_seed" to 6,
        ).map { "obj.${it.first}" to it.second }
    }
}
