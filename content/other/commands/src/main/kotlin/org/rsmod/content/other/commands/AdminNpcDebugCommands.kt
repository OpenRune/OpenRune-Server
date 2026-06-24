package org.rsmod.content.other.commands

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AdminNpcDebugCommands
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onCommand("npcanim", "Play animation on nearest npc", ::npcAnim) {
            invalidArgs = "Use as ::npcanim seqId [radius] (ex: ::npcanim 7018 or ::npcanim 7018 12)"
        }
        onCommand("nanim", "Play animation on nearest npc", ::npcAnim) {
            invalidArgs = "Use as ::nanim seqId [radius] (ex: ::nanim 7018 or ::nanim 7018 12)"
        }
        onCommand("npcspot", "Play spotanim on nearest npc", ::npcSpotanim) {
            invalidArgs =
                "Use as ::npcspot spotanimId [height] [radius] (ex: ::npcspot 1216 96 or ::npcspot 1216 96 2)"
        }
        onCommand("nspot", "Play spotanim on nearest npc", ::npcSpotanim) {
            invalidArgs =
                "Use as ::nspot spotanimId [height] [radius] (ex: ::nspot 1216 96 or ::nspot 1216 96 2)"
        }
        onCommand("npcproj", "Fire projectile from nearest npc to you", ::npcProjectile) {
            invalidArgs =
                "Use as ::npcproj travelSpotanimId [impactSpotanimId] [radius] [projanimId] (ex: ::npcproj 1202 1203)"
        }
        onCommand("nproj", "Fire projectile from nearest npc to you", ::npcProjectile) {
            invalidArgs =
                "Use as ::nproj travelSpotanimId [impactSpotanimId] [radius] [projanimId] (ex: ::nproj 1202 1203)"
        }
    }

    private fun npcAnim(cheat: Cheat) =
        with(cheat) {
            val seqId =
                parseRequiredIntArg(
                    index = 0,
                    label = "seqId",
                    min = 0,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val type = ServerCacheManager.getAnim(seqId)
            if (type == null) {
                player.mes("That seq does not exist: $seqId")
                return@with
            }
            val radius =
                parseOptionalIntArg(
                    index = 1,
                    label = "radius",
                    default = DEFAULT_NPC_TARGET_RADIUS,
                    min = 0,
                    max = MAX_NPC_TARGET_RADIUS,
                )
                    ?: return@with
            val npc = nearestNpc(player, radius)
            if (npc == null) {
                player.mes("No visible NPC found within $radius tiles.")
                return@with
            }

            val priority = type.priority.coerceIn(0, 254)
            npc.anim(type.internalName, priority = priority)
            player.mes(
                "NPC anim: `${npc.name}` (${npc.id}) seq=$seqId${mappingSuffix(RSCMType.SEQ, seqId)} priority=$priority"
            )
            logger.debug { "NPC anim: npc=${npc.name} (${npc.id}), seq=$seqId, type=$type" }
        }

    private fun npcSpotanim(cheat: Cheat) =
        with(cheat) {
            val spotanimId =
                parseRequiredIntArg(
                    index = 0,
                    label = "spotanimId",
                    min = 0,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val height =
                parseOptionalIntArg(
                    index = 1,
                    label = "height",
                    default = 0,
                    min = 0,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val radius =
                parseOptionalIntArg(
                    index = 2,
                    label = "radius",
                    default = DEFAULT_NPC_TARGET_RADIUS,
                    min = 0,
                    max = MAX_NPC_TARGET_RADIUS,
                )
                    ?: return@with
            val npc = nearestNpc(player, radius)
            if (npc == null) {
                player.mes("No visible NPC found within $radius tiles.")
                return@with
            }

            val slot = 0
            npc.spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM,spotanimId), height = height, slot = slot)
            player.mes(
                "NPC spotanim: `${npc.name}` (${npc.id}) spot=$spotanimId${mappingSuffix(RSCMType.SPOTANIM, spotanimId)} height=$height slot=$slot"
            )
            logger.debug {
                "NPC spotanim: npc=${npc.name} (${npc.id}), spot=$spotanimId, height=$height, slot=$slot"
            }
        }

    private fun npcProjectile(cheat: Cheat) =
        with(cheat) {
            val travelSpotanimId =
                parseRequiredIntArg(
                    index = 0,
                    label = "travelSpotanimId",
                    min = 0,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val impactSpotanimId =
                parseOptionalIntArg(
                    index = 1,
                    label = "impactSpotanimId",
                    default = NO_IMPACT_SPOTANIM,
                    min = NO_IMPACT_SPOTANIM,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val radius =
                parseOptionalIntArg(
                    index = 2,
                    label = "radius",
                    default = DEFAULT_NPC_TARGET_RADIUS,
                    min = 0,
                    max = MAX_NPC_TARGET_RADIUS,
                )
                    ?: return@with
            val projanimId =
                parseOptionalIntArg(
                    index = 3,
                    label = "projanimId",
                    default = NO_PROJECTILE_TYPE,
                    min = NO_PROJECTILE_TYPE,
                    max = RAW_VISUAL_ID_MAX,
                )
                    ?: return@with
            val npc = nearestNpc(player, radius)
            if (npc == null) {
                player.mes("No visible NPC found within $radius tiles.")
                return@with
            }
            val projanimType =
                if (projanimId == NO_PROJECTILE_TYPE) {
                    debugProjectileType()
                } else {
                    ServerCacheManager.getProjectile(projanimId)
                        ?: run {
                            player.mes(
                                "That projectile type does not exist: $projanimId${mappingSuffix(RSCMType.PROJANIM, projanimId)}"
                            )
                            return@with
                        }
                }

            val projectile =
                worldRepo.projAnimSourced(
                    source = npc,
                    target = player,
                    spotanim = SpotanimType(travelSpotanimId),
                    type = projanimType,
                )
            if (impactSpotanimId != NO_IMPACT_SPOTANIM) {
                player.spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM,impactSpotanimId), delay = projectile.clientCycles)
            }

            val impact =
                if (impactSpotanimId == NO_IMPACT_SPOTANIM) {
                    "none"
                } else {
                    "$impactSpotanimId${mappingSuffix(RSCMType.SPOTANIM, impactSpotanimId)}"
                }
            player.mes(
                "NPC projectile: `${npc.name}` (${npc.id}) travel=$travelSpotanimId${mappingSuffix(RSCMType.SPOTANIM, travelSpotanimId)} impact=$impact type=${projectileTypeName(projanimId)} delay=${projectile.serverCycles}/${projectile.clientCycles}"
            )
            logger.debug {
                "NPC projectile: npc=${npc.name} (${npc.id}), travel=$travelSpotanimId, impact=$impactSpotanimId, projanim=$projanimId, proj=$projectile"
            }
        }

    private fun nearestNpc(player: Player, radius: Int): Npc? {
        val zoneRadius = (radius + ZONE_TILE_SIZE - 1) / ZONE_TILE_SIZE
        return npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius)
            .filter { it.isVisible }
            .filter { it.coords.level == player.coords.level }
            .filter { it.coords.chebyshevDistance(player.coords) <= radius }
            .minByOrNull { it.coords.chebyshevDistance(player.coords) }
    }

    private fun Cheat.parseRequiredIntArg(
        index: Int,
        label: String,
        min: Int,
        max: Int,
    ): Int? {
        val raw = args.getOrNull(index)
        val value = raw?.toIntOrNull()
        if (value == null || value !in min..max) {
            player.mes("$label must be an integer from $min to $max.")
            return null
        }
        return value
    }

    private fun Cheat.parseOptionalIntArg(
        index: Int,
        label: String,
        default: Int,
        min: Int,
        max: Int,
    ): Int? {
        val raw = args.getOrNull(index) ?: return default
        val value = raw.toIntOrNull()
        if (value == null || value !in min..max) {
            player.mes("$label must be an integer from $min to $max.")
            return null
        }
        return value
    }

    private fun mappingSuffix(type: RSCMType, id: Int): String =
        runCatching { RSCM.getReverseMapping(type, id) }
            .getOrNull()
            ?.let { " ($it)" }
            ?: ""

    private fun projectileTypeName(projanimId: Int): String =
        if (projanimId == NO_PROJECTILE_TYPE) {
            "debug"
        } else {
            "$projanimId${mappingSuffix(RSCMType.PROJANIM, projanimId)}"
        }

    private companion object {
        private const val DEFAULT_NPC_TARGET_RADIUS = 12
        private const val MAX_NPC_TARGET_RADIUS = 64
        private const val NO_IMPACT_SPOTANIM = -1
        private const val NO_PROJECTILE_TYPE = -1
        private const val RAW_VISUAL_ID_MAX = 0xFFFF
        private const val ZONE_TILE_SIZE = 8

        private fun debugProjectileType() =
            ProjAnimType(
                startHeight = 43,
                endHeight = 31,
                delay = 30,
                angle = 15,
                progress = 11,
                stepMultiplier = 5,
            )
    }
}
