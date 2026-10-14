package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Ore pack from Kovac's shop: 30 noted ores, each rolled on its own, mostly coal; Mining level
 * doesn't matter. https://oldschool.runescape.wiki/w/Ore_pack_(Giants%27_Foundry)
 */
class OrePackScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ORE_PACK) { open() }
    }

    private fun ProtectedAccess.open() {
        val rolls = IntArray(ORES.size)
        repeat(ORE_COUNT) { rolls[roll()]++ }
        val noted = ORES.map { (ore, _) -> noted(ore) }
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
        for (i in ORES.indices) {
            if (rolls[i] > 0) {
                invAdd(inv, noted[i], rolls[i])
            }
        }
    }

    private fun roll(): Int {
        var pick = random.of(1, ORES.sumOf { it.second })
        for ((index, ore) in ORES.withIndex()) {
            pick -= ore.second
            if (pick <= 0) {
                return index
            }
        }
        return 0
    }

    private fun noted(ore: String): String {
        val type = ServerCacheManager.getItem(ore.asRSCM(RSCMType.OBJ)) ?: error("Missing $ore")
        val cert = ServerCacheManager.getItem(type.certlink) ?: error("No noted form of $ore")
        return cert.internalName
    }

    private companion object {
        const val ORE_PACK = "obj.giants_foundry_ore_pack"
        const val ORE_COUNT = 30

        // UNVERIFIED: the ore weights, taken from crowd-sourced counts of opened packs, not the
        // game's own table (wiki description).
        val ORES =
            listOf(
                "obj.coal" to 46_854,
                "obj.iron_ore" to 5_803,
                "obj.mithril_ore" to 3_701,
                "obj.adamantite_ore" to 870,
                "obj.runite_ore" to 162,
            )
    }
}
