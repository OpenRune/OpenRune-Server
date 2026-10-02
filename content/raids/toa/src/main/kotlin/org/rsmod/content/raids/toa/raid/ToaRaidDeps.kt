package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.content.other.consumables.potion.toa.ToaPotionEffect
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.routefinder.collision.CollisionFlagMap

class ToaRaidDeps
@Inject
constructor(
    val regionRepo: RegionRepository,
    val regions: RegionRegistry,
    val locRepo: LocRepository,
    val npcRepo: NpcRepository,
    val objRepo: ObjRepository,
    val worldRepo: WorldRepository,
    val aiInteractions: AiPlayerInteractions,
    val accuracy: AccuracyFormulae,
    val worldQueues: WorldQueueList,
    val mapClock: MapClock,
    val random: GameRandom,
    val launcher: ProtectedAccessLauncher,
    val bossHpBar: BossHpBarScript,
    val network: NetworkService<Player>,
    val playerHitModifier: PlayerHitModifier,
    val collision: CollisionFlagMap,
    val routeFactory: RouteFactory,
    val stepFactory: StepFactory,
    val deathDrops: PlayerDeathDrops,
    val marketPrices: MarketPrices,
    val eventBus: EventBus,
    val supplyEffects: ToaPotionEffect,
    val bossDeps: BossDeps,
    val playerList: PlayerList,
)
