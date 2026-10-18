package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.repo.obj.ObjRepository

/** A key step's npc drops its key for the killer while they hold the clue and lack the key. */
class ClueKeyDropHook @Inject constructor(private val objRepo: ObjRepository) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        for (trail in TrailFlow.heldTrails(hero)) {
            val target = ClueSteps.target(hero, trail.step) as? ClueTarget.OpenWithKey ?: continue
            if (target.row.npcs.none { it.id == context.npc.type.id }) continue
            if (TrailTiers.owns(hero, target.row.key)) continue
            objRepo.add(target.row.key, context.dropCoords, KEY_DURATION, receiver = hero)
        }
    }

    private companion object {
        const val KEY_DURATION = 200
    }
}
