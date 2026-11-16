package org.rsmod.content.skills.herblore

import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class BrewBonus(val output: String, val message: String)

class CrushBonus(val extra: Int, val message: String)

// TODO: remove once feat/leagues-foundation (#456) is merged; its mutable SkillingProduct event
// (item, count, inputs) replaces these two hooks. Move MoonsHerbloreHook onto that event.
interface HerbloreHook {
    fun brewBonus(player: Player, potion: HerbloreFinishedRow): BrewBonus? = null

    fun crushBonus(player: Player, recipe: HerbloreCrushingRow): CrushBonus? = null
}

class HerbloreModule : PluginModule() {
    override fun bind() {
        newSetBinding<HerbloreHook>()
    }
}
