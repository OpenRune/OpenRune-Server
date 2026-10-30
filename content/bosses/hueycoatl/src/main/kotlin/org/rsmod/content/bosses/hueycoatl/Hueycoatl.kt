package org.rsmod.content.bosses.hueycoatl

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class Hueycoatl @Inject constructor(
    private val manager: HueycoatlFightManager,
    private val entry: HueycoatlEntry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        entry.register(this)
        onGameStartup { manager.initializePublic() }
        validateHueycoatlCache()

        val headTypes =
            listOf(
                HueycoatlNpcs.HEAD,
                HueycoatlNpcs.HEAD_INVULNERABLE,
                HueycoatlNpcs.HEAD_DEFEATED,
                HueycoatlNpcs.HEAD_ENRAGED,
                HueycoatlNpcs.HEAD_RESPAWN_PLACEHOLDER,
            ).mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }
        val bodyTypes =
            listOf(HueycoatlNpcs.BODY, HueycoatlNpcs.BODY_BROKEN)
                .mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }
        val tailTypes =
            listOf(HueycoatlNpcs.TAIL, HueycoatlNpcs.TAIL_BROKEN)
                .mapNotNull { ServerCacheManager.getNpc(it.asRSCM(RSCMType.NPC)) }

        for (type in headTypes + bodyTypes + tailTypes) {
            onNpcQueue(type, "queue.death") {
                manager.fightForNpc(npc)?.onNpcDeath(npc)
            }
            onModifyNpcHit(type) {
                manager.fightForNpc(npc)?.modifyHit(this)
            }
            onNpcHit(type) {
                manager.fightForNpc(npc)?.recordHit(this)
            }
        }

        onPlayerLogout { manager.onLogout(player) }

        for (type in HueycoatlNpcs.LOBBY_NPCS.mapNotNull(::getNpcType)) {
            onEvent<NpcStateEvents.Spawn>(type.id.toLong()) { lockLobbyNpc(npc) }
        }
    }

    private fun lockLobbyNpc(npc: Npc) {
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.ignoreCombatInteractions = true
        npc.setHunt(0)
    }

    private fun validateHueycoatlCache() {
        for (varp in REQUIRED_VARPS) {
            requireNotNull(ServerCacheManager.getVarp(varp.asRSCM(RSCMType.VARP))) {
                "Missing Hueycoatl varp definition: $varp"
            }
        }
        for (name in ATTACK_HEAD_NPCS) {
            val type =
                requireNotNull(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))) {
                    "Missing Hueycoatl npc definition: $name"
                }
            for (param in ATTACK_BONUS_PARAMS) {
                requireNotNull(type.paramOrNull(param)) {
                    "Missing $param on $name"
                }
            }
        }
        val tail =
            requireNotNull(ServerCacheManager.getNpc(HueycoatlNpcs.TAIL.asRSCM(RSCMType.NPC))) {
                "Missing Hueycoatl tail definition"
            }
        for (param in TAIL_HITMARK_PARAMS) {
            requireNotNull(tail.paramOrNull(param)) { "Missing $param on the Hueycoatl tail" }
        }
    }

    private fun getNpcType(name: String) =
        ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))
}

private val REQUIRED_VARPS =
    listOf(
        "varp.huey_best_time",
        "varp.huey_perfect_streak",
        "varp.huey_personal_deaths",
    )

private val ATTACK_HEAD_NPCS =
    listOf(
        HueycoatlNpcs.HEAD,
        HueycoatlNpcs.HEAD_INVULNERABLE,
        HueycoatlNpcs.HEAD_ENRAGED,
    )

private val ATTACK_BONUS_PARAMS =
    listOf(params.attack_melee, params.attack_ranged, params.attack_magic)

private val TAIL_HITMARK_PARAMS =
    listOf(params.hitmark_lit, params.hitmark_tint, params.hitmark_max)
