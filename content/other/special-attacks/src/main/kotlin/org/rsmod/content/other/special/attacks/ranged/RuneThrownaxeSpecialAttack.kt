package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.floor
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class RuneThrownaxeSpecialAttack
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locs: LocRepository,
    private val world: WorldRepository,
    private val speeds: WeaponSpeeds,
    private val random: GameRandom,
    private val attackValidateHooks: Set<NpcAttackValidateHook>,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged(
            "obj.rune_thrownaxe",
            RuneThrownaxe(
                manager = manager,
                npcRepo = npcRepo,
                locs = locs,
                world = world,
                speeds = speeds,
                random = random,
                attackValidateHooks = attackValidateHooks,
            ),
        )
    }

    private class RuneThrownaxe(
        private val manager: SpecialAttackManager,
        private val npcRepo: NpcRepository,
        private val locs: LocRepository,
        private val world: WorldRepository,
        private val speeds: WeaponSpeeds,
        private val random: GameRandom,
        private val attackValidateHooks: Set<NpcAttackValidateHook>,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            if (target.visType.id in WEAK_DOOR_IDS) {
                knockDownSupports(target, attack)
                return true
            }
            return shoot(target, attack)
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = shoot(target, attack)

        private fun ProtectedAccess.shoot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            manager.setNextAttackDelay(this, speeds.actual(player) + 1)
            anim("seq.human_throw")
            spotanim(
                spot = LAUNCH_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = LAUNCH_HEIGHT,
            )
            val proj = manager.spawnProjectile(this, target, TRAVEL_SPOTANIM, TRAVEL_PROJANIM)
            val hit = rollChainhit(target, attack)
            manager.giveCombatXp(this, target, attack, hit.damage)
            manager.queueRangedHit(
                this,
                target,
                null,
                hit.damage,
                proj.clientCycles,
                proj.serverCycles,
            )
            if (target is Npc && hit.accurate) {
                fireChain(
                    primary = target,
                    origin = target,
                    excluded = setOf(target),
                    attack = attack,
                    chainStart = proj.endTime,
                    remaining = MAX_TARGETS - 1,
                )
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.fireChain(
            primary: Npc,
            origin: Npc,
            excluded: Set<Npc>,
            attack: CombatAttack.Ranged,
            chainStart: Int,
            remaining: Int,
        ) {
            if (remaining <= 0 || !mapMultiway()) {
                return
            }
            val cost = manager.getSpecialEnergyRequirement(RUNE_THROWAXE) ?: return
            // The primary hit is deducted after this returns, so reserve it before each bounce.
            if (!manager.hasSpecialEnergy(this, cost * 2)) {
                return
            }
            val candidates = candidatesNear(primary, excluded)
            if (candidates.isEmpty()) {
                return
            }
            val next = candidates[random.of(candidates.size)]
            manager.takeSpecialEnergy(this, cost)
            val raw =
                ProjAnim.fromBoundsToNpc(
                    origin.bounds(),
                    next,
                    TRAVEL_SPOTANIM_ID,
                    TRAVEL_PROJANIM,
                )
            val chained =
                raw.copy(startTime = chainStart, endTime = chainStart + (raw.endTime - raw.startTime))
            world.projAnim(chained)
            val (serverDelay, clientDelay) = chained.durations
            val hit = rollChainhit(next, attack)
            manager.giveCombatXp(this, next, attack, hit.damage)
            manager.queueRangedHit(this, next, null, hit.damage, clientDelay, serverDelay)
            if (hit.accurate) {
                fireChain(
                    primary = primary,
                    origin = next,
                    excluded = excluded + next,
                    attack = attack,
                    chainStart = chained.endTime,
                    remaining = remaining - 1,
                )
            }
        }

        private fun ProtectedAccess.candidatesNear(primary: Npc, excluded: Set<Npc>): List<Npc> =
            npcRepo
                .findAll(ZoneKey.from(primary.coords), zoneRadius = 1)
                .filter { it !in excluded }
                .filter { it.isValidTarget() }
                .filter { it.visType.hasOp(InteractionOp.Op2.slot) }
                .filter { centreDistance(primary, it) <= CHAIN_RADIUS }
                .filter { canAttack(player, it) }
                .toList()

        private fun canAttack(player: Player, npc: Npc): Boolean =
            attackValidateHooks.none { it.validate(player, npc) is NpcAttackValidateResult.Deny }

        private fun ProtectedAccess.rollChainhit(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Chainhit {
            val accurate =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            val damage = if (accurate) random.of(chainhitMaxHit(player) + 1) else 0
            return Chainhit(accurate, damage)
        }

        private fun chainhitMaxHit(player: Player): Int {
            val axe = ServerCacheManager.getItem(RUNE_THROWAXE_ID) ?: return 0
            val strength = axe.param(params.ranged_strength)
            val effectiveLevel = player.rangedLvl + STANCE_BONUS
            return floor(0.5 + (effectiveLevel * (strength + 64)) / 640.0).toInt()
        }

        private suspend fun ProtectedAccess.knockDownSupports(
            target: Npc,
            attack: CombatAttack.Ranged,
        ) {
            manager.setNextAttackDelay(this, speeds.actual(player) + 1)
            anim("seq.human_throw")
            spotanim(
                spot = LAUNCH_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = LAUNCH_HEIGHT,
            )
            val spawn = findLadderRoomSpawn()
            if (spawn != null) {
                val proj = manager.spawnProjectile(this, spawn, TRAVEL_SPOTANIM, TRAVEL_PROJANIM)
                val hit = rollChainhit(spawn, attack)
                manager.giveCombatXp(this, spawn, attack, hit.damage)
                manager.queueRangedHit(
                    this,
                    spawn,
                    null,
                    hit.damage,
                    proj.clientCycles,
                    proj.serverCycles,
                )
                mes("The rune thrownaxe bounces off a Dagannoth spawn!")
                manager.stopCombat(this)
                return
            }
            val props =
                npcRepo
                    .findAll(ZoneKey.from(CENTRE), zoneRadius = 1)
                    .filter { it.visType.id in WEAK_DOOR_IDS }
                    .toList()
            for (prop in props) {
                manager.spawnProjectile(this, prop, TRAVEL_SPOTANIM, TRAVEL_PROJANIM)
            }
            if (props.isEmpty()) {
                manager.spawnProjectile(this, target, TRAVEL_SPOTANIM, TRAVEL_PROJANIM)
            }
            delay(BOUNCE_DELAY)
            for (prop in props) {
                prop.hitpoints = 0
                prop.queueDeath()
                val door = locs.findAll(prop.coords).firstOrNull { it.id == DOOR_LOC } ?: continue
                val duration = prop.type.respawnRate.takeIf { it > 0 } ?: DOOR_RESPAWN_TICKS
                locs.del(door, duration)
            }
            mes("The rune thrownaxe ricochets between the door supports, knocking them all down!")
            manager.stopCombat(this)
        }

        private fun findLadderRoomSpawn(): Npc? {
            val spawns =
                npcRepo
                    .findAll(ZoneKey.from(CENTRE), zoneRadius = 1)
                    .filter { it.visType.id == LADDER_ROOM_SPAWN }
                    .filter { it.isValidTarget() }
                    .filter { it.coords.chebyshevDistance(CENTRE) <= SPAWN_INTERCEPT_RANGE }
                    .toList()
            return if (spawns.isEmpty()) null else spawns[random.of(spawns.size)]
        }

        private fun centreDistance(a: Npc, b: Npc): Int =
            centreOf(a).chebyshevDistance(centreOf(b))

        private fun centreOf(npc: Npc): CoordGrid =
            npc.coords.translate(npc.size / 2, npc.size / 2)

        private data class Chainhit(val accurate: Boolean, val damage: Int)

        private companion object {
            const val RUNE_THROWAXE = "obj.rune_thrownaxe"
            const val LAUNCH_SPOTANIM = "spotanim.rune_taxe_launch"
            const val TRAVEL_SPOTANIM = "spotanim.rune_taxe_travel"
            const val TRAVEL_PROJANIM = "projanim.thrown"
            const val LAUNCH_HEIGHT = 96
            const val MAX_TARGETS = 5
            const val CHAIN_RADIUS = 3
            const val STANCE_BONUS = 10
            const val BOUNCE_DELAY = 2
            const val DOOR_LOC = 8967
            const val DOOR_RESPAWN_TICKS = 50
            const val SPAWN_INTERCEPT_RANGE = 4

            val RUNE_THROWAXE_ID = RUNE_THROWAXE.asRSCM(RSCMType.OBJ)
            val TRAVEL_SPOTANIM_ID = TRAVEL_SPOTANIM.asRSCM(RSCMType.SPOTANIM)
            val LADDER_ROOM_SPAWN = "npc.dagganoth_critter".asRSCM(RSCMType.NPC)

            val CENTRE = CoordGrid(2545, 10143, 0)
            val WEAK_DOOR_IDS =
                setOf(
                    "npc.dagannoth_weak_door_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_west".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_falling_west".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_north".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_south".asRSCM(RSCMType.NPC),
                    "npc.dagannoth_weak_door_fallen_west".asRSCM(RSCMType.NPC),
                )
        }
    }
}
