package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.game.MapClock
import org.rsmod.game.queue.WorldQueueList

/**
 * Everything a raid and its rooms need from the engine. [ToaRaid] and the encounters are plain
 * objects created at runtime, so Guice can't inject into them. [ToaRaidScript] injects this
 * holder once and hands it to every raid it starts, which then passes it to its rooms.
 *
 * Stateless, so no @Singleton is needed.
 */
class ToaRaidDeps
@Inject
constructor(
    val regionRepo: RegionRepository,
    val regions: RegionRegistry,
    val locRepo: LocRepository,
    val npcRepo: NpcRepository,
    val worldQueues: WorldQueueList,
    val mapClock: MapClock,
    val random: GameRandom,
    val launcher: ProtectedAccessLauncher,
)
