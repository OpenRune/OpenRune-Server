package org.rsmod.content.bosses.royaltitans

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.NpcRangedAccuracyHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.spells.attack.NpcSpellHitHook
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.module.PluginModule

class RoyalTitansModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerRespawnHook>(RoyalTitansDeaths::class.java)
        addSetBinding<NpcRangedAccuracyHook>(RoyalTitansRangedAccuracy::class.java)
        addSetBinding<NpcSpellHitHook>(RoyalTitansElementalSpells::class.java)
    }
}

/**
 * Counts deaths on the scoreboard. The encounter keeps the normal unsafe death handling: items go
 * to a grave outside the tunnel, as the entrance warning says.
 *
 * This runs from the respawn hook rather than [org.rsmod.api.death.PlayerDeathHook] so it also
 * counts deaths for staff accounts, which skip the drop/hook pipeline.
 */
class RoyalTitansDeaths @Inject constructor(private val encounter: RoyalTitansEncounter) :
    PlayerRespawnHook {
    override fun respawnCoords(player: Player): CoordGrid? {
        encounter.countDeath(player)
        return null
    }
}

/** The titans are far more vulnerable to ranged attacks while they are out of melee reach. */
class RoyalTitansRangedAccuracy @Inject constructor(private val encounter: RoyalTitansEncounter) :
    NpcRangedAccuracyHook {
    override fun multiplier(player: Player, npc: Npc): Double =
        encounter.rangedAccuracyMultiplier(npc)
}
