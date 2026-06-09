package org.rsmod.content.skills.runecrafting.essencepouch

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ColossalPouchCraftEvents : PluginScript() {
    override fun ScriptContext.startup() {
        val pouchItems = EssencePouch.Tier.entries.flatMap { tier ->
            tier.items.toList()
        }

        pouchItems.forEach { pouch ->
            onOpHeldU(ABYSSAL_NEEDLE, pouch) { craftColossalPouch(it) }
            onOpHeldU(REGULAR_NEEDLE, pouch) { rejectRegularNeedle() }
        }
    }

    private suspend fun ProtectedAccess.craftColossalPouch(ev: HeldUEvents.Type) {
        if (isColossalPouchInteraction(ev)) {
            mes("You don't want to poke the pouch with a needle again. It was bad enough the first time.")
            return
        }

        if (EssencePouch.hasColossalPouch(player)) {
            mes("You already have a colossal pouch.")
            return
        }

        if (player.baseRunecraftingLvl < RUNECRAFT_LEVEL_REQ) {
            mes("You need level $RUNECRAFT_LEVEL_REQ Runecrafting to stitch these pouches together.")
            return
        }

        if (player.baseCraftingLvl < CRAFTING_LEVEL_REQ) {
            mes("You need level $CRAFTING_LEVEL_REQ Crafting to even attempt stitching these together.")
            return
        }

        if (EssencePouch.hasDegradedCraftPouch(player)) {
            mes("Your pouches must not be degraded before you can stitch them together.")
            return
        }

        if (!EssencePouch.hasRequiredCraftPouches(player)) {
            mes("You need a small, medium, large and giant pouch to stitch together.")
            return
        }

        if (!inv.contains(ABYSSAL_NEEDLE)) {
            return
        }

        if (!EssencePouch.craftPouchesAreEmpty(player)) {
            mes("Your pouches must be empty before you can stitch them together.")
            return
        }

        anim("seq.human_runecraft")
        delay(CRAFT_DELAY_CYCLES)

        for (tier in EssencePouch.craftablePouchTiers) {
            if (invDel(inv, tier.intactItem, 1).failure) {
                return
            }
            EssencePouch.resetTier(player, tier)
        }

        if (invDel(inv, ABYSSAL_NEEDLE, 1).failure) {
            return
        }

        if (invAdd(inv, EssencePouch.Tier.Colossal.intactItem, 1).failure) {
            return
        }

        statAdvance("stat.crafting", CRAFTING_XP)

        val capacity = EssencePouch.colossalCapacity(player.baseRunecraftingLvl)
        mes(
            "You attempt to stitch together all of the pouches. It's difficult, but you eventually " +
                "craft a strange, kind of gross, fleshy container. It seems capable of storing up to " +
                "$capacity essence at the moment.",
        )
    }

    private fun ProtectedAccess.rejectRegularNeedle() {
        mes("The needle doesn't seem to be able to work with this kind of... material.")
    }

    private fun isColossalPouchInteraction(ev: HeldUEvents.Type): Boolean {
        val colossal = EssencePouch.Tier.Colossal
        if (ev.first.isType(colossal.intactItem) || ev.second.isType(colossal.intactItem)) {
            return true
        }
        val degraded = colossal.degradedItem ?: return false
        return ev.first.isType(degraded) || ev.second.isType(degraded)
    }

    private companion object {
        const val ABYSSAL_NEEDLE = "obj.abyssal_needle"
        const val REGULAR_NEEDLE = "obj.needle"
        const val RUNECRAFT_LEVEL_REQ = 25
        const val CRAFTING_LEVEL_REQ = 56
        const val CRAFTING_XP = 1000.0
        const val CRAFT_DELAY_CYCLES = 3
    }
}
