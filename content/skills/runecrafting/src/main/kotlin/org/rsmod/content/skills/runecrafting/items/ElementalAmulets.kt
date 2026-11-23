package org.rsmod.content.skills.runecrafting.items

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.advanceRunecraftingXp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ElementalAmulets @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    private data class Recipe(
        val altar: String,
        val gem: String,
        val amulet: String,
        val rune: String,
        val spotanim: String?,
        val level: Int,
        val xp: Double,
    )

    private val recipes =
        listOf(
            Recipe(
                "loc.air_altar",
                "obj.air_diamond",
                "obj.amulet_of_air",
                "obj.airrune",
                null,
                18,
                50.0,
            ),
            Recipe(
                "loc.water_altar",
                "obj.water_sapphire",
                "obj.amulet_of_water",
                "obj.waterrune",
                "spotanim.spotanim_amulet_of_water",
                19,
                75.0,
            ),
            Recipe(
                "loc.earth_altar",
                "obj.earth_emerald",
                "obj.amulet_of_earth",
                "obj.earthrune",
                "spotanim.spotanim_amulet_of_earth",
                20,
                100.0,
            ),
            Recipe(
                "loc.fire_altar",
                "obj.fire_ruby",
                "obj.amulet_of_fire",
                "obj.firerune",
                "spotanim.spotanim_amulet_of_fire",
                21,
                125.0,
            ),
        )

    override fun ScriptContext.startup() {
        for (recipe in recipes) {
            onOpLocU(recipe.altar, recipe.gem) { createAmulet(recipe) }
            onOpLocU(recipe.altar, "obj.amulet_of_magic") { createAmulet(recipe) }
            for (altar in recipes) {
                onOpLocU(altar.altar, recipe.amulet) { createElementalAmulet() }
            }
        }
    }

    private suspend fun ProtectedAccess.createAmulet(recipe: Recipe) {
        if (player.baseRunecraftingLvl < recipe.level) {
            mesbox("You need Runecrafting level ${recipe.level} to make this amulet.")
            return
        }
        if (
            !inv.contains("obj.amulet_of_magic") ||
                !inv.contains(recipe.gem) ||
                inv.count(recipe.rune) < RUNES_PER_AMULET
        ) {
            mes("You need an amulet of magic, the matching gem and $RUNES_PER_AMULET runes.")
            return
        }

        anim("seq.amulet_of_air_create")
        recipe.spotanim?.let { spotanim(it, height = 100) }
        delay(CREATE_DELAY)

        if (invDel(inv, "obj.amulet_of_magic", 1).failure) return
        if (invDel(inv, recipe.gem, 1).failure) return
        if (invDel(inv, recipe.rune, RUNES_PER_AMULET).failure) return
        if (invAdd(inv, recipe.amulet, 1).success) {
            advanceRunecraftingXp(recipe.xp, xpMods)
        }
    }

    private suspend fun ProtectedAccess.createElementalAmulet() {
        if (player.baseRunecraftingLvl < ELEMENTAL_LEVEL) {
            mesbox("You need Runecrafting level $ELEMENTAL_LEVEL to make this amulet.")
            return
        }
        if (
            recipes.any {
                !inv.contains(it.amulet) || inv.count(it.rune) < RUNES_PER_ELEMENTAL
            }
        ) {
            mes(
                "You need an amulet of air, water, earth and fire and " +
                    "$RUNES_PER_ELEMENTAL of each matching rune."
            )
            return
        }

        anim("seq.amulet_of_air_create")
        spotanim("spotanim.spotanim_elemental_amulet", height = 100)
        delay(CREATE_DELAY)

        for (recipe in recipes) {
            if (invDel(inv, recipe.amulet, 1).failure) return
            if (invDel(inv, recipe.rune, RUNES_PER_ELEMENTAL).failure) return
        }
        if (invAdd(inv, "obj.elemental_amulet", 1).success) {
            advanceRunecraftingXp(ELEMENTAL_XP, xpMods)
        }
    }

    private companion object {
        const val RUNES_PER_AMULET = 100
        const val RUNES_PER_ELEMENTAL = 500
        const val ELEMENTAL_LEVEL = 30
        const val ELEMENTAL_XP = 175.0
        const val CREATE_DELAY = 3
    }
}
