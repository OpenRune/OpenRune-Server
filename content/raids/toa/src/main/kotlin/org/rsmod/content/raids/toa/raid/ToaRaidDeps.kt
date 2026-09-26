package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.routefinder.collision.CollisionFlagMap

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
    val objRepo: ObjRepository,
    /** Loc animations, map spotanims and area sounds (room hazards). */
    val worldRepo: WorldRepository,
    /** For sending room NPCs into the engine's standard combat against a player (opPlayer2). */
    val aiInteractions: AiPlayerInteractions,
    /** Standard accuracy rolls for room NPCs with custom attacks (Crondis crocodiles). */
    val accuracy: AccuracyFormulae,
    val worldQueues: WorldQueueList,
    val mapClock: MapClock,
    val random: GameRandom,
    val launcher: ProtectedAccessLauncher,
    /** @Singleton script (safe to inject); shows boss-style progress bars such as the Crondis palm. */
    val bossHpBar: BossHpBarScript,
    /** Singleton binding from NetworkModule; used for the raid's extended NPC view (ToaNpcView.kt). */
    val network: NetworkService<Player>,
    /**
     * The standard player hit modifier (protection prayers and the like), the same binding BossDeps
     * injects. Boss attacks pass it to queueImpactHit so prayer is checked on impact.
     */
    val playerHitModifier: PlayerHitModifier,
    /** Walkable-tile checks for room hazards (Zebak's poison spread). */
    val collision: CollisionFlagMap,
    /**
     * Raw zone updates. Only for area sounds whose synth has no gameval name (WorldRepository's
     * soundArea takes a name); e.g. Zebak's synth_6590.
     */
    val zoneUpdates: ZoneUpdateMap,
)
