package org.rsmod.content.other.special.attacks.melee

import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.mapMultiway
import org.rsmod.api.player.mapMultiway
import org.rsmod.api.route.RayCastValidator
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.map.CoordGrid

class MeleeAreaTargets @Inject constructor(
    private val hunt: Hunt,
    private val areas: AreaChecker,
    private val rayCast: RayCastValidator,
    private val npcValidators: Set<NpcAttackValidateHook>,
) {
    internal fun collect(
        source: Player,
        primary: PathingEntity,
        radius: Int,
        limit: Int,
        centre: CoordGrid = primary.coords,
        inArea: (PathingEntity) -> Boolean,
    ): List<PathingEntity> {
        val result = mutableListOf(primary)
        if (!source.mapMultiway(areas) || !primary.inMultiway()) return result
        for (npc in hunt.findNpcs(centre, radius, HuntVis.Off)) {
            if (result.size >= limit) break
            if (npc === primary || !inArea(npc) || !npc.isValidTarget()) continue
            if (!npc.visType.hasOp(InteractionOp.Op2.slot) || !npc.mapMultiway(areas)) continue
            if (!hasSight(source, npc)) continue
            if (npcValidators.any { it.validate(source, npc) is NpcAttackValidateResult.Deny }) continue
            result += npc
        }
        // Existing PvP hooks return Pass outside their own area; absence of Deny does not
        // authorize hitting a bystander. Keep secondary targets NPC-only until a positive
        // PvP eligibility API exists. The primary target already passed the combat script.
        return result
    }

    private fun PathingEntity.inMultiway(): Boolean = when (this) {
        is Npc -> mapMultiway(areas)
        is Player -> mapMultiway(areas)
    }

    private fun hasSight(source: Player, target: PathingEntity): Boolean =
        source.coords.level == target.coords.level && rayCast.hasLineOfSight(
            source.coords, target.coords, source.size, source.size, target.size, target.size,
        )
}
