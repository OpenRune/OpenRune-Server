package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.*

internal class ForestryCraftingProvider @Inject constructor(private val xp: XpModifiers) : CraftingRecipeProvider {
    override val products = listOf(
        CraftingProduct(CraftingSection.COMBINING, ForestryContainers.HARNESS, inputs = listOf(Material("obj.forestry_log_brace", 1), Material("obj.rope", 2), Material("obj.adamantite_bar", 3), Material("obj.nails", 45)), level = 1, xp = 0.0, ticks = listOf(0), tools = emptyList(), actionName = "make this harness"),
    ) + listOf("obj.needle", "obj.costumeneedle").map { tool ->
        CraftingProduct(CraftingSection.COMBINING, "obj.forestry_clothes_pouch", inputs = listOf(Material("obj.forestry_clothes_pouch_blueprint", 1), Material("obj.leather", 1)) + if (tool == "obj.needle") listOf(Material("obj.thread", 1)) else emptyList(), level = 50, xp = 470.0, ticks = listOf(3), tools = listOf(tool), anims = listOf("seq.human_leather_crafting"), actionName = "make this pouch")
    }
    override suspend fun create(access: ProtectedAccess, product: CraftingProduct) = with(access) {
        val harness = product.output == ForestryContainers.HARNESS
        val woodcutting = if (harness) 75 else 50
        fun ready() = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && player.woodcuttingLvl >= woodcutting && (if (harness) player.smithingLvl >= 75 else player.craftingLvl >= 50) && product.inputs.all { inv.physicalCount(it.internal) >= it.count } && product.tools.all { inv.physicalCount(it) > 0 }
        if (!ready()) { mes(if (harness) "You need level 75 Smithing and Woodcutting, a log brace, 45 steel nails, two ropes and three adamantite bars." else "You need level 50 Crafting and Woodcutting, a blueprint, leather and a needle with thread."); return@with }
        val origin = coords
        val references = (product.inputs.map { it.internal } + product.tools).flatMap { type -> inv.indices.filter { inv[it]?.id == type.asRSCM() }.map { it to inv[it] } }
        if (!harness) { anim("seq.human_leather_crafting"); try { delay(3) } finally { resetAnim() } }
        if (!ready() || coords != origin || references.any { (slot, item) -> inv[slot] !== item }) return@with
        val tx = player.invTransaction(inv) { val i = select(inv); for (material in product.inputs) delete(i, material.internal.asRSCM(), material.count); add(i, product.output.asRSCM(), 1) }
        if (tx.failure) { mes("You need room for the finished item."); return@with }
        if (!harness) statAdvance("stat.crafting", 47.0 * xp.get(player, "stat.crafting"))
        mes(if (harness) "You create a sturdy harness." else "You create a clothes pouch.")
    }
}
