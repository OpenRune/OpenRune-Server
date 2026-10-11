package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.events.skilling.*
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Stored and worn pieces cannot stack; conflicting equipment disables the corresponding slot. */
internal class ForestryOutfitXpMod @Inject constructor(private val store: ContainerStore) : StatXpMod("stat.woodcutting") {
    override fun Player.modifier(): Double {
        val kit = ForestryContainers.kit
        val stored = if (store.owns(this, kit) && store.count(this, kit, "obj.forestry_clothes_pouch".asRSCM()) > 0) store.contents(this, kit) else emptyMap()
        var bonus = 0.0; var effective = 0; var previousWornBonus = 0.0
        for ((index, slot) in listOf(0, 4, 7, 10).withIndex()) {
            val ids = ForestryContainers.outfits[index].map { it.asRSCM() }
            val item = worn[slot]
            val type = item?.let { ServerCacheManager.getItem(it.id) }
            val wearing = item?.id in ids
            if (wearing) previousWornBonus += (type?.paramOrNull(params.xpmod_percent) ?: 0) / 100.0
            val blocked = !wearing && type != null && blockedSetPrefixes.any { type.name.startsWith(it, ignoreCase = true) }
            if (wearing || !blocked && ids.any { (stored[it.toString()] ?: 0) > 0 }) { bonus += pieces[index]; effective++ }
        }
        // The fork's generic worn modifier treats native tenths of a percent as whole percent.
        // Correct only these four pieces here, while adding the stored/full-outfit mechanics.
        return bonus + (if (effective == 4) 0.005 else 0.0) - previousWornBonus
    }
    private companion object {
        val pieces = listOf(0.004, 0.008, 0.006, 0.002)
        val blockedSetPrefixes = listOf("Angler", "Spirit angler", "Carpenter", "Farmer", "Graceful", "Guild hunter", "Inquisitor", "Prospector", "Golden prospector", "Pyromancer", "Rogue", "Shayzien", "Smiths", "Void", "Elite void", "Zealot", "Bloodbark", "Swampbark", "Virtus", "Obsidian", "Crystal", "Ahrim", "Dharok", "Guthan", "Karil", "Torag", "Verac")
    }
}

internal class ForestryRationsScript @Inject constructor(private val store: ContainerStore, private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.source !is SkillingProductSource.Woodcutting || product.count <= 0 || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || player.runEnergy >= 9000) return@onEvent
            val kit = ForestryContainers.kit; val ration = "obj.forestry_ration".asRSCM()
            if (!store.owns(player, kit) || store.count(player, kit, ration) <= 0 || random.of(100) >= 30) return@onEvent
            store.update(player, kit, ration, -1)
            player.runEnergy = (player.runEnergy + 2000).coerceAtMost(10_000)
            UpdateRun.energy(player, player.runEnergy)
        }
    }
}
