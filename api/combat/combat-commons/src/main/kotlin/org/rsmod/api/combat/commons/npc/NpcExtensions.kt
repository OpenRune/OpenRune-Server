package org.rsmod.api.combat.commons.npc

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcMode
import kotlin.math.max
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

private var Npc.lastCombat: Int by intVarn("varn.lastcombat")
private var Npc.aggressivePlayer by typePlayerUidVarn("varn.aggressive_player")
private var Npc.attackingPlayer by typePlayerUidVarn("varn.attacking_player")

public fun Npc.canRetaliate(): Boolean {
    if (actionDelay + constants.combat_activecombat_delay < currentMapClock) {
        return true
    }
    return mode != NpcMode.OpPlayer2 && mode != NpcMode.ApPlayer2 && mode != NpcMode.PlayerEscape
}

public fun Npc.queueCombatRetaliate(source: Player, delay: Int = 1) {
    queue("queue.com_retaliate_player", delay)
    aggressivePlayer = source.uid
    lastCombat = max(lastCombat, currentMapClock)
}

public fun Npc.combatDefaultRetaliateOp(interactions: AiPlayerInteractions) {
    if (!canRetaliate()) {
        return
    }
    val target = interactions.resolvePlayer(aggressivePlayer) ?: return
    attackingPlayer = target.uid
    actionDelay = currentMapClock + (attackRate() / 2)
    retaliate(target, interactions, ap = false)
}

public fun Npc.combatDefaultRetaliateAp(interactions: AiPlayerInteractions) {
    if (!canRetaliate()) {
        return
    }
    val target = interactions.resolvePlayer(aggressivePlayer) ?: return
    attackingPlayer = target.uid
    actionDelay = currentMapClock + (attackRate() / 2)
    retaliate(target, interactions, ap = true)
}

private fun Npc.retaliate(target: Player, interactions: AiPlayerInteractions, ap: Boolean) {
    when {
        hitpoints <= param(params.retreat) -> {
            playerEscape(target)
        }
        visType.wanderRange > 0 && !target.isWithinDistance(spawnCoords, aggressionRange()) -> {
            playerEscape(target)
        }
        ap -> {
            apPlayer2(target, interactions)
        }
        else -> {
            opPlayer2(target, interactions)
        }
    }
}

public fun Npc.combatPlayDefendAnim(clientDelay: Int = 0) {
    val defendAnim = visType.paramOrNull(params.defend_anim)
    if (defendAnim != null) {
        anim(RSCM.getReverseMapping(RSCMType.SEQ,defendAnim.id), delay = clientDelay)
    }
}

public fun Npc.combatPlayDefendSpot(ammo: ItemServerType?, clientDelay: Int) {
    val type =
        ammo?.let { id -> ServerCacheManager.getItems().values.firstOrNull { it.id == id.id } }
            ?: return
    if (!type.isCategoryType("category.javelin")) {
        return
    }
    spotanim("spotanim.ballista_special", delay = clientDelay, height = 146)
}

public fun Npc.attackRate(): Int = visType.param(params.attackrate)

public fun Npc.aggressionRange(): Int = visType.maxRange + visType.attackRange

public fun Npc.resolveCombatXpMultiplier(): Double = combatXpMultiplier / 1000.0
