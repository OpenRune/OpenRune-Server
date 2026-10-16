package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.GiantsFoundryEnums
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OrePackScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ORE_PACK) { open() }
    }

    private fun ProtectedAccess.open() {
        val ores = ores()
        val rolls = IntArray(ores.size)
        repeat(ORE_COUNT) { rolls[roll(ores)]++ }
        val noted = ores.map { (ore, _) -> noted(ore) }
        // UNVERIFIED: only the rolled ores the inventory doesn't already hold need a space (guess).
        val needed = noted.indices.count { rolls[it] > 0 && !inv.contains(noted[it]) }
        val missing = needed - (inv.freeSpace() + 1)
        if (missing > 0) {
            val spaces = if (missing == 1) "free inventory space" else "$missing free inventory spaces"
            mes("You will need another $spaces to open the pack.")
            return
        }
        invDel(inv, ORE_PACK, 1)
        mes("You open the pack and find $ORE_COUNT ores.")
        for (i in ores.indices) {
            if (rolls[i] > 0) {
                invAdd(inv, noted[i], rolls[i])
            }
        }
    }

    // UNVERIFIED: the weights in `giants_foundry_ore_pack_weights` are the wiki's counts from
    // 1913 opened packs, not the game's own table (wiki description).
    private fun ores(): List<Pair<ItemServerType, Int>> =
        GiantsFoundryEnums.giants_foundry_ore_pack_weights
            .mapNotNull { (ore, weight) -> weight?.let { ore to it } }
            .sortedByDescending { it.second }

    private fun roll(ores: List<Pair<ItemServerType, Int>>): Int {
        var pick = random.of(1, ores.sumOf { it.second })
        for ((index, ore) in ores.withIndex()) {
            pick -= ore.second
            if (pick <= 0) {
                return index
            }
        }
        return 0
    }

    private fun noted(ore: ItemServerType): String {
        val cert =
            ServerCacheManager.getItem(ore.certlink)
                ?: error("No noted form of ${ore.internalName}")
        return cert.internalName
    }

    private companion object {
        const val ORE_PACK = "obj.giants_foundry_ore_pack"
        const val ORE_COUNT = 30
    }
}
