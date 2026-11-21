package org.rsmod.content.quest.area.varrock.dragonslayer

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.DragonfireElvarg
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.Roll
import org.rsmod.api.bosses.dsl.WithinMeleeRange
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.roll
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionRegistry
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.config.Constants
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.ELVARGS_HEAD
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_CRANDOR
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_ELVARG_SLAIN
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.foundSecretDoor
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Elvarg's fight: melee up close, dragonfire at any range, and a 10% prayer drain per breath. */
class Elvarg @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec =
        boss(ELVARG) {
            stats(attackRate = ATTACK_RATE)
            val melee =
                ability("melee") {
                    anim(MELEE_SEQ)
                    hit {
                        damage(0..MELEE_MAX).roll()
                        type(Melee)
                    }
                }
            val dragonfire =
                ability("dragonfire") {
                    anim(BREATH_SEQ)
                    projectile(
                        spotanim = BREATH_PROJECTILE,
                        travel = BREATH_TRAVEL,
                        hit =
                            Effect.Hit(
                                damage = Roll(0..DRAGONFIRE_MAX),
                                type = DragonfireElvarg,
                                penetration = FULL_PENETRATION,
                                onHit = external(PRAYER_DRAIN),
                                onHitEvenOnMiss = true,
                            ),
                    )
                }
            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = MELEE_WEIGHT, requires = WithinMeleeRange)
                    +random(dragonfire, weight = BREATH_WEIGHT)
                }
            }
        }

    companion object {
        const val ELVARG = "npc.elvarg"
        const val ATTACK_RATE = 4
        const val MELEE_MAX = 8
        const val DRAGONFIRE_MAX = 70
        const val MELEE_WEIGHT = 3
        const val BREATH_WEIGHT = 1
        const val FULL_PENETRATION = 100
        const val PRAYER_DRAIN = "dragonslayer.elvarg_prayer_drain"
        const val MELEE_SEQ = "seq.dragon_attack"
        const val BREATH_SEQ = "seq.dragon_firebreath_all_attack"
        const val BREATH_PROJECTILE = "spotanim.dragon_ranged_fire_attack"
        const val BREATH_TRAVEL = "projanim.dragonfire"

        /** Every breath takes 10% of the target's remaining prayer, rounded down. */
        fun drainPrayer(player: Player) {
            val drain = player.stat(PRAYER) / PRAYER_DRAIN_DIVISOR
            if (drain > 0) {
                player.statSub(PRAYER, drain, 0)
            }
        }

        private const val PRAYER = "stat.prayer"
        private const val PRAYER_DRAIN_DIVISOR = 10
    }
}

/** The lair: the stalagmite wall into Elvarg's chamber and the secret wall to Karamja. */
class DragonSlayerLair
@Inject
constructor(
    private val quest: DragonSlayerQuest,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val bossExtensions: BossExtensionRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        bossExtensions.register(Elvarg.PRAYER_DRAIN) { _, _, target, _ -> Elvarg.drainPrayer(target) }
        onOpLoc1(ARENA_WALL) { climbArenaWall(it.loc) }
        onOpLoc1(SECRET_DOOR) { secretDoor(it.loc) }
    }

    private suspend fun ProtectedAccess.climbArenaWall(wall: BoundLocInfo) {
        arriveDelay()
        val stage = quest.stage(player)
        if (stage == STAGE_ELVARG_SLAIN) {
            mes("You have already slain the dragon. Now you just need to return to Oziach for")
            mes("your reward!")
            return
        }
        if (stage >= STAGE_COMPLETE) {
            mes("You have already slain Elvarg the dragon.")
            return
        }
        if (stage < STAGE_STARTED) {
            mes("You can't see a way over the wall from here.")
            return
        }
        val entering = coords.x < wall.coords.x
        val dest = CoordGrid(wall.coords.x + if (entering) 1 else -1, coords.z, coords.level)
        anim(CLIMB_SEQ)
        exactMove(coords, dest, delay1 = 0, delay2 = CLIMB_CYCLES, dir = if (entering) Constants.em_face_east else Constants.em_face_west)
        delay(CLIMB_TICKS)
        if (entering) {
            breatheOnEntry()
        }
    }

    /** Elvarg greets everyone who climbs into her chamber with a breath of fire. */
    private suspend fun ProtectedAccess.breatheOnEntry() {
        val elvarg = elvargNear(player.coords) ?: return
        elvarg.facePlayer(player)
        elvarg.anim(Elvarg.BREATH_SEQ)
        val cap =
            DragonfireProtection.resolveMaxHit(
                player,
                DragonfireProtection.DragonfireType.Elvarg,
                Elvarg.DRAGONFIRE_MAX,
            )
        val damage = if (cap <= 0) 0 else random.of(cap + 1)
        spotanim(ENTRY_IMPACT, delay = ENTRY_IMPACT_DELAY, height = ENTRY_IMPACT_HEIGHT)
        queueHit(elvarg, ENTRY_HIT_DELAY, HitType.Typeless, damage, modifier = NoopPlayerHitModifier)
        Elvarg.drainPrayer(player)
    }

    private fun elvargNear(coords: CoordGrid): Npc? =
        npcRepo.findAll(ZoneKey.from(coords), zoneRadius = 3).firstOrNull {
            it.isType(Elvarg.ELVARG) && it.hitpoints > 0
        }

    private suspend fun ProtectedAccess.secretDoor(door: BoundLocInfo) {
        arriveDelay()
        val fromCrandor = coords.z >= door.coords.z
        if (fromCrandor) {
            if (player.foundSecretDoor == 0) {
                player.foundSecretDoor = 1
                mes("You found a secret door.")
                mes("You remember where the secret door is for future reference.")
            }
        } else if (player.foundSecretDoor == 0 && !quest.isComplete(player)) {
            mes("The door is securely locked.")
            soundSynth(LOCKED_SOUND)
            return
        }
        val across = if (fromCrandor) door.coords.translateZ(-1) else door.coords
        soundSynth(SECRET_DOOR_SOUND)
        locRepo.change(door, SECRET_DOOR_OPEN, SECRET_DOOR_TICKS)
        forcedWalk(across, 1)
    }

    private companion object {
        const val ARENA_WALL = "loc.dragon_slayer_qip_stalagtite_jump"
        const val SECRET_DOOR = "loc.dragonsecretdoor"
        const val SECRET_DOOR_OPEN = "loc.dragonsecretdoor_open"
        const val SECRET_DOOR_TICKS = 3
        const val SECRET_DOOR_SOUND = "synth.nicedoor_open"
        const val LOCKED_SOUND = "synth.locked"
        const val CLIMB_SEQ = "seq.human_jump_hurdle"
        const val CLIMB_CYCLES = 30
        const val CLIMB_TICKS = 2
        const val ENTRY_IMPACT = "spotanim.firebreath_attack"
        const val ENTRY_IMPACT_DELAY = 30
        const val ENTRY_IMPACT_HEIGHT = 100
        const val ENTRY_HIT_DELAY = 2
    }
}

/** Only players who are fighting their way through the quest may attack Elvarg. */
class ElvargAttackHook @Inject constructor(private val quest: DragonSlayerQuest) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(Elvarg.ELVARG)) {
            return NpcAttackValidateResult.Pass
        }
        return when (quest.stage(player)) {
            in STAGE_STARTED..STAGE_CRANDOR -> NpcAttackValidateResult.Pass
            STAGE_ELVARG_SLAIN ->
                NpcAttackValidateResult.Deny(
                    "You have already slain the dragon. Now you just need to return to Oziach for " +
                        "your reward!"
                )
            else -> NpcAttackValidateResult.Deny("You have already slain Elvarg.")
        }
    }
}

/** Elvarg falls: the killer cuts off her head as proof, and the quest moves on. */
class ElvargKillHook
@Inject
constructor(
    private val quest: DragonSlayerQuest,
    private val locRepo: LocRepository,
    private val launcher: ProtectedAccessLauncher,
) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        val hero = context.hero
        if (!npc.isType(Elvarg.ELVARG) || quest.stage(hero) != STAGE_CRANDOR) {
            return
        }
        val corner = npc.coords
        launcher.launch(hero) { cutOffHead(corner, CORPSE_ANGLE) }
    }

    private suspend fun ProtectedAccess.cutOffHead(corner: CoordGrid, angle: LocAngle) {
        val body = locRepo.add(corner, FULL_BODY, CORPSE_TICKS, angle, LocShape.CentrepieceStraight)
        faceSquare(corner.translate(CORPSE_CENTRE, CORPSE_CENTRE))
        anim(REMOVE_HEAD_START)
        soundSynth(CUT_HEAD_START)
        delay(CUT_TICKS)
        anim(REMOVE_HEAD_END)
        soundSynth(CUT_HEAD_END)
        locRepo.del(body, Int.MAX_VALUE)
        locRepo.add(corner, HEADLESS_BODY, CORPSE_TICKS, angle, LocShape.CentrepieceStraight)
        quest.setStage(this, STAGE_ELVARG_SLAIN)
        mes("You cut off Elvarg's head as proof of your victory!")
        if (invAdd(inv, ELVARGS_HEAD, 1).success) {
            midiJingle(VICTORY_JINGLE)
        }
    }

    private companion object {
        const val FULL_BODY = "loc.dragon_slayer_qip_elvarg_full_body"
        const val HEADLESS_BODY = "loc.dragon_slayer_qip_elvarg_body_no_head"
        const val REMOVE_HEAD_START = "seq.dragon_slayer_qip_remove_head_part1"
        const val REMOVE_HEAD_END = "seq.dragon_slayer_qip_remove_head_part2"
        const val CUT_HEAD_START = "synth.dragonslayer_cutoffhead1"
        const val CUT_HEAD_END = "synth.dragonslayer_cutoffhead2"
        const val VICTORY_JINGLE = "jingle.dragonslayer_victory"
        const val CORPSE_TICKS = 50
        const val CUT_TICKS = 3
        const val CORPSE_CENTRE = 2
        val CORPSE_ANGLE = LocAngle.West
    }
}
