package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.other.treasuretrails.TrailFlow.advance

class ClueMonsterKillHook
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val objRepo: ObjRepository,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val hero = context.hero
        val npcId = context.npc.type.id
        val trail =
            TrailFlow.heldTrails(hero).firstOrNull { trail ->
                val target = ClueSteps.target(hero, trail.step) as? ClueTarget.KillForKey
                target != null && target.row.npcs.any { it.id == npcId }
            } ?: return
        protectedAccess.launch(hero) {
            advance(trail.tier) { slot, obj ->
                val held = inv[slot] ?: return@advance false
                if (invDel(inv, TrailTiers.type(held.id).symbol(), 1, slot).failure) {
                    return@advance false
                }
                objRepo.add(obj, context.dropCoords, DROP_DURATION, receiver = hero)
                true
            }
        }
    }

    private companion object {
        const val DROP_DURATION = 200
    }
}
