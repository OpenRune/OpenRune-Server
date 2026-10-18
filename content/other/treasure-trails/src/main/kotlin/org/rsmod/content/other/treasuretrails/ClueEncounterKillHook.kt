package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.other.treasuretrails.TrailFlow.advance

class ClueEncounterKillHook

@Inject
constructor(
    private val encounters: ClueEncounters,
    private val protectedAccess: ProtectedAccessLauncher,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        val npc = context.npc
        if (!encounters.isOwnedEncounter(hero, npc)) return
        if (encounters.alive(hero).any { it.uid != npc.uid }) return
        val trail =
            TrailFlow.heldTrails(hero).firstOrNull { encounters.belongsTo(it.step, npc) } ?: return
        if (trail.step.kind == ClueKind.Emote) {
            encounters.summonUri(hero, trail.tier)
            return
        }
        protectedAccess.launch(hero) {
            val given = advance(trail.tier) ?: return@launch
            val casket = given.id == trail.tier.casketObj.id
            val text = if (casket) "You've found a casket!" else "You've found another clue!"
            startDialogue { objbox(given.symbol(), text) }
        }
    }
}
