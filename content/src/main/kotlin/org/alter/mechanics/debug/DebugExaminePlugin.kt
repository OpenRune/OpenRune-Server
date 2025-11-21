package org.alter.mechanics.debug

import org.alter.api.ext.*
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.GameObject
import org.alter.game.model.priv.Privilege
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ExamineEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

/**
 * Plugin for debug examine information (dev/admin only)
 * Shows detailed object and NPC information when examining
 */
class DebugExaminePlugin : PluginEvent() {

    override fun init() {
        // Handle examine events to show debug information (dev/admin only)
        on<ExamineEvent> {
            where {
                // Only show debug data for dev/admin
                (entityType == ExamineEntityType.OBJECT || entityType == ExamineEntityType.NPC) &&
                (player.privilege.powers.contains(Privilege.DEV_POWER) ||
                 player.privilege.powers.contains(Privilege.ADMIN_POWER))
            }
            then {
                when (entityType) {
                    ExamineEntityType.OBJECT -> {
                        // Find the object by searching nearby chunks
                        var obj: GameObject? = null
                        for (dx in -2..2) {
                            for (dz in -2..2) {
                                val checkTile = player.tile.transform(dx, dz)
                                val chunk = world.chunks.get(checkTile, createIfNeeded = false) ?: continue
                                obj = chunk.getEntities<GameObject>(
                                    checkTile,
                                    org.alter.game.model.EntityType.STATIC_OBJECT,
                                    org.alter.game.model.EntityType.DYNAMIC_OBJECT
                                ).firstOrNull { it.internalID == id }
                                if (obj != null) break
                            }
                            if (obj != null) break
                        }

                        if (obj != null) {
                            val rscmName = RSCM.getReverseMapping(RSCMType.LOCTYPES, obj.internalID) ?: "unknown"
                            player.message("Object: id=${id}, rscm=$rscmName, x=${obj.tile.x}, z=${obj.tile.z}, height=${obj.tile.height}, rot=${obj.rot}, type=${obj.type}")
                        } else {
                            // Object not found nearby, just show the ID
                            val rscmName = RSCM.getReverseMapping(RSCMType.LOCTYPES, id) ?: "unknown"
                            player.message("Object: id=${id}, rscm=$rscmName (not found in nearby area)")
                        }
                    }
                    ExamineEntityType.NPC -> {
                        // Find the NPC by searching nearby chunks
                        var npc: org.alter.game.model.entity.Npc? = null
                        for (dx in -2..2) {
                            for (dz in -2..2) {
                                val checkTile = player.tile.transform(dx, dz)
                                val chunk = world.chunks.get(checkTile, createIfNeeded = false) ?: continue
                                npc = chunk.getEntities<org.alter.game.model.entity.Npc>(
                                    checkTile,
                                    org.alter.game.model.EntityType.NPC
                                ).firstOrNull { it.id == id }
                                if (npc != null) break
                            }
                            if (npc != null) break
                        }

                        if (npc != null) {
                            val rscmName = RSCM.getReverseMapping(RSCMType.NPCTYPES, npc.id) ?: "unknown"
                            val npcDef = dev.openrune.ServerCacheManager.getNpc(npc.id)
                            player.message("NPC: id=${id}, rscm=$rscmName, x=${npc.tile.x}, z=${npc.tile.z}, height=${npc.tile.height}, combat=${npcDef?.combatLevel ?: -1}, size=${npcDef?.size ?: -1}")
                        } else {
                            // NPC not found nearby, just show the ID
                            val rscmName = RSCM.getReverseMapping(RSCMType.NPCTYPES, id) ?: "unknown"
                            player.message("NPC: id=${id}, rscm=$rscmName (not found in nearby area)")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

