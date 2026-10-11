package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.*

/** Native recipes share the Crafting click registry; linked inputs and output commit together. */
internal class ContainerCraftingScript @Inject constructor(private val store: ContainerStore, private val xp: XpModifiers) : CraftingRecipeProvider {
    override val products = recipes.sortedByDescending { it.level } + listOf("obj.slayer_herb_sack", "obj.slayer_herb_sack_open").map { form ->
        recipe(if (form.endsWith("_open")) "obj.slayer_herb_sack_silk_open" else "obj.slayer_herb_sack_silk", 1, 0, listOf(form to 1, "obj.slayer_spider_silk" to 1), ticks = 1, tools = emptyList())
    }
    override suspend fun create(access: ProtectedAccess, product: CraftingProduct) { with(access) { createContainer(product) } }

    private suspend fun ProtectedAccess.createContainer(product: CraftingProduct) {
        val costume = inv.physicalCount("obj.costumeneedle") > 0
        val input = product.inputs.filterNot { costume && it.internal == "obj.thread" }
        fun ready(): Boolean = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && player.craftingLvl >= product.level && input.all { inv.physicalCount(it.internal) >= it.count } && (!product.consumesThread || costume || inv.physicalCount("obj.thread") > 0) && (product.tools.all { tool -> if (tool == "obj.needle") costume || inv.physicalCount(tool) > 0 else inv.physicalCount(tool) > 0 } && (product.tools.isEmpty() || costume == (inv.physicalCount("obj.costumeneedle") > 0)))
        if (!ready()) { mes("You need level ${product.level} Crafting, the materials and a needle with thread."); return }
        val origin = coords
        val materialRefs = input.flatMap { material -> inv.indices.filter { inv[it]?.id == material.internal.asRSCM() }.map { it to inv[it] } }
        if (product.ticksAt(0) > 0) {
            product.anims.firstOrNull()?.let { anim(it) }
            try { delay(product.ticksAt(0)) } finally { if (product.anims.isNotEmpty()) resetAnim() }
        }
        if (!ready() || coords != origin || materialRefs.any { (slot, obj) -> inv[slot] !== obj }) return
        val target = ContainerCatalog.byId[product.output.asRSCM()]!!
        val migrated = if (target.family == "gem_sack") input.mapNotNull { ContainerCatalog.byId[it.internal.asRSCM()] }.filter { it.family in setOf("low_gems", "gem_bag") }.distinctBy { it.family } else emptyList()
        val merged = store.contents(player, target).toMutableMap()
        for (source in migrated) for ((item, count) in store.contents(player, source)) {
            val amount = (merged[item] ?: 0).toLong() + count
            if (amount > target.limit(item.toInt()) || item.toInt() !in target.accepted) { mes("Empty some gems before combining these containers."); return }
            merged[item] = amount.toInt()
        }
        val threadUses = player.vars["varbit.crafting_thread_uses"].takeIf { it > 0 } ?: 5
        val useThread = product.consumesThread && !costume
        val result = player.invTransaction(inv) {
            val i = select(inv)
            for (material in input) delete(i, material.internal.asRSCM(), material.count)
            if (useThread && threadUses == 1) delete(i, "obj.thread".asRSCM(), 1)
            add(i, target.id, 1)
        }
        if (result.failure) { mes("You don't have enough inventory space."); return }
        if (useThread) VarPlayerIntMapSetter.set(player, "varbit.crafting_thread_uses", if (threadUses == 1) 5 else threadUses - 1)
        for (source in migrated) store.clear(player, source)
        if (migrated.isNotEmpty()) player.attr[target.stateKey] = merged
        if (product.xp > 0) statAdvance("stat.crafting", product.xp / 10.0 * xp.get(player, "stat.crafting"))
        publish(org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent(player, org.rsmod.api.player.events.skilling.SkillingActionContext.Product(skill = "stat.crafting", item = product.output, count = 1, experienceGranted = product.xp / 10.0, source = org.rsmod.api.player.events.skilling.SkillingProductSource.Crafting)))
        mes("You create a ${dev.openrune.ServerCacheManager.getItem(target.id)!!.name.lowercase()}.")
    }

    internal companion object {
        val recipes = listOf(
            recipe("obj.gem_pouch", 12, 25, listOf("obj.bolt_of_linen" to 1, "obj.silk" to 1, "obj.thread" to 1)),
            recipe("obj.gem_satchel", 39, 50, listOf("obj.gem_pouch" to 1, "obj.bolt_of_canvas" to 1, "obj.silk" to 1, "obj.thread" to 1)),
            recipe("obj.gem_tote", 81, 200, listOf("obj.gem_satchel" to 1, "obj.bolt_of_cotton" to 1, "obj.silk" to 1, "obj.thread" to 1)),
            recipe("obj.gem_tote", 81, 275, listOf("obj.bolt_of_cotton" to 1, "obj.bolt_of_canvas" to 1, "obj.bolt_of_linen" to 1, "obj.silk" to 3, "obj.thread" to 3)),
            recipe("obj.gem_sack", 81, 300, listOf("obj.gem_tote" to 1, "obj.gem_bag" to 1, "obj.immaculate_mole_skin" to 1, "obj.thread" to 1)),
            recipe("obj.hg_furpouch_small", 35, 20, listOf("obj.hunting_fur_jaguar_perfect" to 4), 3).copy(consumesThread = true, anims = listOf("seq.human_leather_crafting")),
            recipe("obj.hg_furpouch_med", 50, 40, listOf("obj.hg_furpouch_small" to 1, "obj.hunting_fur_leopard_perfect" to 3), 3).copy(consumesThread = true, anims = listOf("seq.human_leather_crafting")),
            recipe("obj.hg_furpouch_large", 65, 60, listOf("obj.hg_furpouch_med" to 1, "obj.hunting_fur_tiger_perfect" to 3), 3).copy(consumesThread = true, anims = listOf("seq.human_leather_crafting")),
            recipe("obj.hg_meatpouch_small", 35, 30, listOf("obj.hunting_fennecfox_fur" to 4), 3).copy(consumesThread = true, anims = listOf("seq.human_leather_crafting")),
            recipe("obj.hg_meatpouch_large", 65, 60, listOf("obj.hg_meatpouch_small" to 1, "obj.hunting_antelopesun_fur" to 3), 3).copy(consumesThread = true, anims = listOf("seq.human_leather_crafting")),
        ).flatMap { base ->
            var variants = listOf(base)
            for (material in base.inputs.filter { it.internal in setOf("obj.gem_pouch", "obj.gem_satchel", "obj.gem_tote", "obj.gem_bag") }) {
                variants += variants.map { p -> p.copy(inputs = p.inputs.map { if (it.internal == material.internal) Material("${it.internal}_open", it.count) else it }) }
            }
            variants.flatMap { p -> listOf(p.copy(inputs = p.inputs.filterNot { it.internal == "obj.thread" }, tools = listOf("obj.costumeneedle")), p) }
        }
        private fun recipe(output: String, level: Int, xp: Int, materials: List<Pair<String, Int>>, ticks: Int = 0, tools: List<String> = listOf("obj.needle")) = CraftingProduct(CraftingSection.COMBINING, output, inputs = materials.map { Material(it.first, it.second) }, level = level, xp = xp * 10.0, ticks = listOf(ticks), tools = tools, actionName = "make this container")
    }
}
