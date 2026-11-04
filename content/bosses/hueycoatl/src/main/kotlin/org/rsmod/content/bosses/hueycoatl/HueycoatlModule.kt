package org.rsmod.content.bosses.hueycoatl

import jakarta.inject.Inject
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class HueycoatlModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathDropHook>(HueycoatlDropSuppressor::class.java)
        addSetBinding<PlayerDeathCleanupHook>(HueycoatlDeathCleanup::class.java)
        addSetBinding<PlayerDeathHook>(HueycoatlDeathGrave::class.java)
    }
}

internal class HueycoatlDropSuppressor @Inject constructor() : NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean = false

    override fun suppressesDropTable(context: NpcDeathKillContext): Boolean =
        context.npc.type.internalName in HUEY_NPCS
}

internal class HueycoatlDeathGrave @Inject constructor() : PlayerDeathHook {
    override val priority: Int = 1

    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (context.player.vars["varbit.huey_in_area"] == 0) return null
        return PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = !context.inInstance && !context.player.supplyPilesDisabled,
            untradeableHandling =
                if (context.inInstance) UntradeableHandling.KEEP else UntradeableHandling.DROP,
            grave = DeathGrave(coords = HueycoatlCoords.EXIT),
        )
    }
}

private var Player.supplyPilesDisabled by boolVarBit("varbit.gravestone_supplypiles_disabled")

internal class HueycoatlDeathCleanup @Inject constructor(
    private val fights: HueycoatlFightManager,
    private val worldStats: HueycoatlWorldStats,
) : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        if (player.vars["varbit.huey_in_area"] == 0) return
        fights.existingFightForPlayer(player)?.removeParticipant(player)
        worldStats.addDeath()
        VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 0)
        VarPlayerIntMapSetter.set(player, "varp.huey_perfect_streak", 0)
        VarPlayerIntMapSetter.set(
            player,
            "varp.huey_personal_deaths",
            player.vars["varp.huey_personal_deaths"] + 1,
        )
    }
}

private val HUEY_NPCS =
    setOf(
        HueycoatlNpcs.HEAD,
        HueycoatlNpcs.HEAD_DEFEATED,
        HueycoatlNpcs.HEAD_ENRAGED,
        HueycoatlNpcs.HEAD_INVULNERABLE,
        HueycoatlNpcs.HEAD_RESPAWN_PLACEHOLDER,
        HueycoatlNpcs.TAIL,
        HueycoatlNpcs.TAIL_BROKEN,
        HueycoatlNpcs.BODY,
        HueycoatlNpcs.BODY_BROKEN,
    )
