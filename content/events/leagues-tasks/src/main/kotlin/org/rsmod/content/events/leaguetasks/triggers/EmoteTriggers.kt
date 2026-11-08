package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerEmoteEvent
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.world.WorldType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EmoteTriggers
@Inject
constructor(
    private val progress: LeagueTaskProgress,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByEmote: Map<String, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Emote).groupBy { checkNotNull(it.emote) }

    override fun ScriptContext.startup() {
        onEvent<PlayerEmoteEvent> {
            val rows = rowsByEmote[emote] ?: return@onEvent
            progress.record(player, rows.filter { player.isNear(it) })
        }
    }

    private fun Player.isNear(row: LeagueTaskRequirement): Boolean {
        if (row.npcs.isEmpty() && row.locs.isEmpty()) {
            return true
        }
        val zone = ZoneKey.from(coords)
        val nearNpc =
            row.npcs.isNotEmpty() &&
                npcRepo.findAll(zone, zoneRadius = 1).any { npc ->
                    (npc.type.id in row.npcs || npc.visType.id in row.npcs) &&
                        isWithinDistance(npc, NEAR_DISTANCE)
                }
        if (nearNpc) {
            return true
        }
        return row.locs.isNotEmpty() && nearbyLocs(zone).any { loc ->
            loc.id in row.locs && isWithinDistance(loc, NEAR_DISTANCE)
        }
    }

    private fun nearbyLocs(zone: ZoneKey): Sequence<BoundLocInfo> = sequence {
        for (dx in -1..1) {
            for (dz in -1..1) {
                val near = ZoneKey(x = zone.x + dx, z = zone.z + dz, level = zone.level)
                for (loc in locRepo.findAll(near)) {
                    val type = ServerCacheManager.getObject(loc.id) ?: continue
                    yield(BoundLocInfo(loc, type))
                }
            }
        }
    }

    private companion object {
        const val NEAR_DISTANCE = 5
    }
}
