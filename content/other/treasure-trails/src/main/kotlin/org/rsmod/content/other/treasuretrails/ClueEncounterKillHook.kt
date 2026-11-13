package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.vars.VarPlayerIntMapSetter

class ClueEncounterKillHook @Inject constructor(private val encounters: ClueEncounters) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        val npc = context.npc
        if (!encounters.isOwnedEncounter(hero, npc)) return
        if (encounters.alive(hero).any { it.uid != npc.uid }) return
        val trail =
            TrailFlow.heldTrails(hero).firstOrNull { encounters.belongsTo(it.step, npc) } ?: return
        VarPlayerIntMapSetter.set(hero, TrailFlow.ENCOUNTER_DEFEATED, trail.step.rowId)
        if (trail.tier.difficulty == TrailTiers.MASTER) MasterVars.setReady(hero, true)
        if (trail.step.kind == ClueKind.Emote) {
            encounters.summonUri(hero, trail.tier)
        }
    }
}
