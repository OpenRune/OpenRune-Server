package org.rsmod.api.player.death

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.HitmarkTypeGroup
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

public val DEATH_CAUSE_ATTR: AttributeKey<DeathCause> = AttributeKey()

public sealed class DeathCause {
    public data class ByPlayer(val killer: Player) : DeathCause()

    public data class ByNpc(val typeName: String, val typeId: Int) : DeathCause()

    public data object Poison : DeathCause()

    public data object Venom : DeathCause()

    public data object Disease : DeathCause()

    public data object Unknown : DeathCause()
}

public fun DeathCause.describe(): String =
    when (this) {
        is DeathCause.ByPlayer -> "You were killed by ${killer.displayName}."
        is DeathCause.ByNpc ->
            if (typeName.isNotBlank() && typeName != "unknown") {
                "You were killed by $typeName (id $typeId)."
            } else {
                "You were killed by an npc (id $typeId)."
            }
        DeathCause.Poison -> "You died from poison."
        DeathCause.Venom -> "You died from venom."
        DeathCause.Disease -> "You died from disease."
        DeathCause.Unknown -> "You died from unknown causes."
    }

public fun Player.recordDeathCause(cause: DeathCause) {
    attr[DEATH_CAUSE_ATTR] = cause
}

public fun Hit.resolveDeathCause(npcSource: Npc?, playerSource: Player?): DeathCause {
    if (playerSource != null) {
        return DeathCause.ByPlayer(playerSource)
    }
    if (npcSource != null) {
        val typeName = RSCM.getReverseMapping(RSCMType.NPC, npcSource.visType.id) ?: "unknown"
        return DeathCause.ByNpc(typeName, npcSource.visType.id)
    }
    resolveToxinCause()?.let { return it }
    return DeathCause.Unknown
}

private fun Hit.resolveToxinCause(): DeathCause? =
    when {
        matchesHitmarkGroup(hitmark_groups.poison_damage) -> DeathCause.Poison
        matchesHitmarkGroup(hitmark_groups.venom) -> DeathCause.Venom
        matchesHitmarkGroup(hitmark_groups.disease) -> DeathCause.Disease
        else -> null
    }

private fun Hit.matchesHitmarkGroup(group: HitmarkTypeGroup): Boolean {
    val lit = group.lit.asRSCM(RSCMType.HITMARK)
    val tint = group.tint?.asRSCM(RSCMType.HITMARK)
    return hitmark.self == lit || (tint != null && hitmark.self == tint)
}
