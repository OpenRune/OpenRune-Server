package org.rsmod.content.bosses.bryophyta

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class Bryophyta
@Inject
constructor(
    deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val instances: InstanceManager,
) : BossPluginScript(deps) {

    private var Player.bryophytaKills by intVarp("varp.total_bryophyta_kills")

    private val growthlings = IdentityHashMap<Npc, MutableList<Npc>>()
    private val growthlingOwner = IdentityHashMap<Npc, Npc>()

    private val growthlingsAlive = Condition.Custom { npc, _ -> hasLivingGrowthlings(npc) }

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onLethal = { npc -> onBossKilled(npc) },
            onModifyHit = { onBossHit(npc, hit) },
        )

        deps.extensionRegistry.register(SUMMON_ROLL) { _, npc, target, _ ->
            trySummonGrowthlings(npc, target, npc.hitpoints)
        }
        deps.extensionRegistry.register(POISON) { _, npc, target, _ ->
            if (target.members && !PlayerPoison.isPoisoned(target)) {
                PlayerPoison.tryPoison(target, source = npc)
            }
        }

        val growthlingType =
            ServerCacheManager.getNpc(GROWTHLING.asRSCM(RSCMType.NPC))
                ?: error("Missing npc type: $GROWTHLING")
        onOpNpcU(GROWTHLING) {
            val npc = it.npc
            if (npc.hitpoints <= 0) return@onOpNpcU
            if (!isAxeOrSecateurs(it.objType)) {
                player.mes(TOO_STRONG_MESSAGE)
                return@onOpNpcU
            }
            player.mes(PRUNE_MESSAGE, ChatType.Spam)
            npc.hitpoints = 0
            npc.queueDeath()
        }
        onModifyNpcHit(growthlingType) { onGrowthlingHit(npc, hit) }

        onEvent<NpcStateEvents.Delete> {
            if (npc.isType(GROWTHLING)) {
                onGrowthlingDeleted(npc)
            } else if (npc.isType(BOSS_NPC)) {
                growthlings.remove(npc)?.forEach { growthlingOwner.remove(it) }
            }
        }
    }

    override val spec =
        boss(BOSS_NPC) {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.giant_update_moss_attack")
                    hit {
                        damage(Accuracy(Roll(0..16)))
                        type(Melee)
                        onHit(external(POISON))
                    }
                }

            val magic =
                ability("magic") {
                    anim("seq.giant_basic_spell")
                    sound("synth.earthblast_cast_and_fire")
                    projectile {
                        spotanim = "spotanim.earthblast_travel"
                        impact = "spotanim.earthblast_impact"
                        hit {
                            damage(Accuracy(Roll(0..10)))
                            type(Magic)
                            onHit(external(POISON))
                        }
                    }
                }

            phase("combat", lockMovement = true) {
                attackRate = ATTACK_RATE
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(magic, weight = 1, requires = Condition.Not(WithinMeleeRange))
                }
                every(ATTACK_RATE, external(SUMMON_ROLL))
            }

            incoming {
                rule(growthlingsAlive) {
                    cap(0, Melee)
                    cap(0, Ranged)
                    cap(0, Magic)
                }
            }
        }

    private fun onBossHit(npc: Npc, hit: HitBuilder) {
        if (!hit.isFromPlayer) return
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        if (hit.damage <= 0) return
        trySummonGrowthlings(npc, attacker, npc.hitpoints - hit.damage)
    }

    private fun trySummonGrowthlings(boss: Npc, target: Player, expectedHitpoints: Int) {
        if (boss.hitpoints <= 0 || expectedHitpoints <= 0) return
        if (expectedHitpoints > boss.baseHitpointsLvl * 3 / 4) {
            return
        }
        if (hasLivingGrowthlings(boss)) {
            return
        }
        val engagingBoss = (target.interaction as? InteractionNpc)?.target === boss
        if (!engagingBoss) {
            return
        }
        if (deps.mapClock.cycle < boss.vars[GROWTHLING_COOLDOWN]) {
            return
        }
        if (!deps.random.randomBoolean(SUMMON_CHANCE)) {
            return
        }
        summonGrowthlings(boss, target)
    }

    private fun summonGrowthlings(boss: Npc, target: Player) {
        val tiles = growthlingTiles(boss)
        if (tiles.isEmpty()) return
        val spawned = growthlings.getOrPut(boss) { mutableListOf() }
        repeat(GROWTHLING_COUNT) {
            val tile = deps.random.pickOrNull(tiles) ?: return@repeat
            tiles.remove(tile)
            val growthling = deps.spawnOwnedNpc(boss, GROWTHLING, tile) ?: return@repeat
            growthling.mode = NpcMode.OpPlayer2
            spawned += growthling
            growthlingOwner[growthling] = boss
            growthling.opPlayer2(target, aiPlayerInteractions)
            deps.worldRepo.spotanimMap(SpotanimType(SMOKE_SPOTANIM.asRSCM(RSCMType.SPOTANIM)), tile)
            deps.worldRepo.soundArea(boss.coords, "synth.smokepuff", radius = 5)
        }
        boss.vars[GROWTHLING_COOLDOWN] = deps.mapClock.cycle + GROWTHLING_COOLDOWN_TICKS
    }

    private fun growthlingTiles(boss: Npc): MutableList<CoordGrid> {
        val bossSouthWest = boss.coords
        val bossNorthEast = bossSouthWest.translate(boss.size - 1, boss.size - 1)
        val candidates = mutableListOf<CoordGrid>()
        for (dx in -GROWTHLING_RADIUS..GROWTHLING_RADIUS) {
            for (dz in -GROWTHLING_RADIUS..GROWTHLING_RADIUS) {
                val tile = bossSouthWest.translate(dx, dz)
                val overBoss =
                    tile.x in bossSouthWest.x..bossNorthEast.x &&
                        tile.z in bossSouthWest.z..bossNorthEast.z
                if (!overBoss) {
                    candidates += tile
                }
            }
        }
        val walkable = candidates.filterNot { deps.collision.isWalkBlocked(it) }
        return (walkable.ifEmpty { candidates }).toMutableList()
    }

    private fun onGrowthlingHit(growthling: Npc, hit: HitBuilder) {
        if (!hit.isFromPlayer) return
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        if (isAxeOrSecateurs(hit.righthandType())) {
            hit.damage = growthling.hitpoints
            attacker.mes(PRUNE_MESSAGE, ChatType.Spam)
            return
        }
        if (growthling.hitpoints - hit.damage <= 0) {
            hit.damage = (growthling.hitpoints - 1).coerceAtLeast(0)
        }
        if (growthling.vars[GROWTHLING_WARNED] == 0) {
            growthling.vars[GROWTHLING_WARNED] = 1
            attacker.mes(CUT_MESSAGE)
        }
    }

    private fun onGrowthlingDeleted(growthling: Npc) {
        val boss = growthlingOwner.remove(growthling) ?: return
        val list = growthlings[boss] ?: return
        list.remove(growthling)
        if (list.none { it.isSlotAssigned && it.hitpoints > 0 } && boss.isSlotAssigned && boss.hitpoints > 0) {
            boss.vars[GROWTHLING_COOLDOWN] = deps.mapClock.cycle + GROWTHLING_COOLDOWN_TICKS
        }
    }

    private fun hasLivingGrowthlings(boss: Npc): Boolean {
        val list = growthlings[boss] ?: return false
        list.removeIf { !it.isSlotAssigned || it.hitpoints <= 0 }
        return list.isNotEmpty()
    }

    private fun onBossKilled(npc: Npc) {
        val player = killer(npc) ?: return
        player.bryophytaKills += 1
        player.mes("Your Bryophyta kill count is: <col=ff0000>${player.bryophytaKills}</col>.")
    }

    private fun killer(npc: Npc): Player? {
        npc.findHero(deps.playerList)?.let { return it }
        val instanceId = instances.instanceForNpc(npc) ?: return null
        val session = instances.sessionForId(instanceId) ?: return null
        return deps.playerList.firstOrNull { it.uuid in session.occupants }
    }

    private fun isAxeOrSecateurs(type: ItemServerType?): Boolean {
        if (type == null) return false
        if (type.internalName == BLESSED_AXE || type.internalName == ZOMBIE_AXE) return false
        if (type.isContentType("content.woodcutting_axe")) return true
        return type.internalName == SECATEURS || type.internalName == MAGIC_SECATEURS
    }

    private companion object {
        private const val BOSS_NPC = "npc.gb_mossgiant"
        private const val GROWTHLING = "npc.gb_growthling"
        private const val SECATEURS = "obj.secateurs"
        private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
        private const val BLESSED_AXE = "obj.blessed_axe"
        private const val ZOMBIE_AXE = "obj.zombie_axe"
        private const val SMOKE_SPOTANIM = "spotanim.smokepuff_large"
        private const val SUMMON_ROLL = "bryophyta.summon_roll"
        private const val POISON = "bryophyta.poison"
        private const val GROWTHLING_COOLDOWN = "varn.bryophyta_growthling_cooldown"
        private const val GROWTHLING_WARNED = "varn.bryophyta_growthling_warned"

        private const val ATTACK_RATE = 6
        private const val SUMMON_CHANCE = 5
        private const val GROWTHLING_COUNT = 3
        private const val GROWTHLING_RADIUS = 3
        private const val GROWTHLING_COOLDOWN_TICKS = 20

        private const val CUT_MESSAGE = "Cut the growthling down with an axe or secateurs."
        private const val TOO_STRONG_MESSAGE = "The Growthling is too strong to be hurt by that!"
        private const val PRUNE_MESSAGE = "You prune the Growthling."
    }
}
