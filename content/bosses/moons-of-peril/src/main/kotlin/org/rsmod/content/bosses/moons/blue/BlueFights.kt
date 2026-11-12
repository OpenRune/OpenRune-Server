package org.rsmod.content.bosses.moons.blue

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.npcAnimSpecific
import org.rsmod.api.player.output.npcSpotanimSpecific
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.content.bosses.moons.BlueHooks
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsFight
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.content.bosses.moons.blue.FrozenWeapons.FreezeResult
import org.rsmod.content.bosses.moons.blue.FrozenWeapons.ReleaseResult
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitType
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

@Singleton
class BlueFights
@Inject
constructor(
    private val deps: BossDeps,
    private val fights: MoonsFights,
    private val frozenWeapons: FrozenWeapons,
) : BlueHooks {
    private val moon = Moon.Blue
    private val icicles = arrayOfNulls<Npc>(BLOCK_OFFSETS.size)
    private val standIns = IdentityHashMap<Player, Npc>()
    private var freezeStartedAt = NEVER

    fun coldLock(event: NpcHitEvents.Modify) {
        val hit = event.hit
        if (!hit.isFromPlayer) return
        val player = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        val fight = fights.of(player) ?: return
        if (!fight.coldLocked) return
        fight.coldLocked = false
        hit.damage = 0
    }

    fun coldDecay(player: Player) {
        val cold = player.vars[MoonsFights.CONDITION]
        if (cold > 0) player.set(MoonsFights.CONDITION, (cold - COLD_DECAY).coerceAtLeast(0))
    }

    override fun onColdGain(fight: MoonsFight, before: Int, after: Int) {
        val player = fight.player
        when {
            before < COLD_WARNING_2 && after >= COLD_WARNING_2 -> player.mes(COLD_MESSAGE_2)
            before < COLD_WARNING_1 && after >= COLD_WARNING_1 -> player.mes(COLD_MESSAGE_1)
        }
        if (after < COLD_LOCK) return
        player.set(MoonsFights.CONDITION, after - COLD_LOCK_DROP)
        player.mes(COLD_LOCK_MESSAGE)
        player.spotanim(COLD_SPOTANIM, height = COLD_SPOTANIM_HEIGHT, slot = COLD_SPOTANIM_SLOT)
        fight.coldLocked = true
    }

    fun freezeStart() {
        freezeStartedAt = deps.mapClock.cycle
    }

    fun joined(player: Player) {
        val offset = deps.mapClock.cycle - freezeStartedAt
        if (freezeStartedAt == NEVER || offset !in 0 until FREEZE_TICKS || offset >= EXPLODE_TICK) return
        val fight = fights.of(player) ?: return
        arm(fight)
        if (offset < HIDE_TICK) {
            player.set(HIDE_NPC, 1)
            spawnStandIn(player, fade = false)
        }
        if (offset + DISARM_DELAY < EXPLODE_TICK) {
            deps.worldQueues.add(DISARM_DELAY) {
                if (fights.of(player) === fight) disarm(player)
            }
        }
    }

    fun freezeArm(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.subdued) return
        arm(fight)
    }

    private fun arm(fight: MoonsFight) {
        val player = fight.player
        val target = deps.random.of(0, BLOCK_OFFSETS.lastIndex)
        fight.freezeTarget = target
        fights.setPlayerFocus(player, target)
        player.set(WEAR_PREVENT, 1)
        player.set(AD_HP, BLOCK_HITPOINTS)
        player.set(ICICLE_STATE, 0)
    }

    fun icicleSpawned(npc: Npc, index: Int) {
        icicles[index] = npc
        npc.ignoreCombatInteractions = true
        npc.vars["varn.forced_max_hit_styles"] = FORCED_MAX_HIT_STYLES
        npc.anim(BLOCK_RISE_SEQ)
    }

    fun spawnStandIn(player: Player, fade: Boolean) {
        removeStandIn(player)
        if (fights.of(player)?.subdued != false) return
        val npc = Npc(STAND_IN_NPC, moon.southWest)
        deps.npcRepo.add(npc, STAND_IN_LIFESPAN)
        npc.ignoreCombatInteractions = true
        npc.faceSquare(player.coords)
        val spotanim = (if (fade) FAKE_FADE else FAKE_MAGIC).asRSCM(RSCMType.SPOTANIM)
        player.npcSpotanimSpecific(npc, spotanim)
        standIns[player] = npc
    }

    fun removeStandIn(player: Player) {
        val npc = standIns.remove(player) ?: return
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    fun disarm(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.moon != moon || fight.subdued || fight.freezeTarget < 0) return
        when (frozenWeapons.freeze(player)) {
            FreezeResult.Frozen -> disarmVisuals(player, fight)
            FreezeResult.NoWeapon -> {
                val damage = deps.random.of(NO_WEAPON_HIT.first, NO_WEAPON_HIT.last)
                hitPlayer(player, damage)
                player.mes(NO_WEAPON_MESSAGE)
            }
            FreezeResult.Denied -> {
                if (deps.mapClock.cycle - freezeStartedAt < DISARM_RETRY_LIMIT) {
                    deps.worldQueues.add(1) { if (fights.of(player) === fight) disarm(player) }
                }
            }
            FreezeResult.AlreadyHolding -> Unit
        }
    }

    private fun disarmVisuals(player: Player, fight: MoonsFight) {
        val block = icicles[fight.freezeTarget]
        player.anim(DISARM_SEQ)
        player.spotanim(DISARM_SPOTANIM, slot = DISARM_SPOTANIM_SLOT)
        player.spam(DISARM_MESSAGE)
        player.soundSynth(DISARM_SYNTH)
        val target = moon.southWest.translate(
            BLOCK_OFFSETS[fight.freezeTarget].first + 1,
            BLOCK_OFFSETS[fight.freezeTarget].second + 1,
        )
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = TELL_SPOTANIM.asRSCM(RSCMType.SPOTANIM),
                startHeight = TELL_START_HEIGHT,
                endHeight = TELL_END_HEIGHT,
                startTime = 0,
                endTime = TELL_END_TIME,
                angle = TELL_ANGLE,
                progress = TELL_PROGRESS,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = player.coords,
                endCoord = target,
            )
        )
        if (block != null && block.isSlotAssigned) {
            player.npcAnimSpecific(block, BLOCK_GLOW_SEQ.asRSCM(RSCMType.SEQ), TELL_END_TIME)
        }
    }

    private fun hitPlayer(player: Player, damage: Int) {
        val npc = fights.npc(moon) ?: return
        player.queueHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
    }

    fun punch(npc: Npc, attacker: Player, damage: Int): Int {
        val fight = fights.of(attacker) ?: return 0
        if (fight.moon != moon || fight.subdued) return 0
        val index = icicles.indexOfFirst { it === npc }
        if (index < 0 || index != fight.freezeTarget) return 0
        val remaining = attacker.vars[AD_HP]
        if (damage <= 0 || remaining <= 0 || attacker.vars[ICICLE_STATE] != 0) return 0
        val left = (remaining - damage).coerceAtLeast(0)
        attacker.set(AD_HP, left)
        showBlockHeadbar(npc, attacker, left)
        attacker.soundSynth(PUNCH_SYNTH)
        if (left <= 0) retrieveBlock(attacker, fight, npc)
        return damage
    }

    private fun showBlockHeadbar(npc: Npc, attacker: Player, left: Int) {
        val bar = ServerCacheManager.getHealthBar(BLOCK_HEADBAR.asRSCM(RSCMType.HEADBAR)) ?: return
        val fill = left * bar.segments / BLOCK_HITPOINTS
        npc.showHeadbar(Headbar.fromPlayerSource(bar.id, null, fill, fill, 0, 0, attacker.slotId))
    }

    private fun retrieveBlock(player: Player, fight: MoonsFight, block: Npc) {
        player.set(ICICLE_STATE, fight.freezeTarget + 1)
        block.anim(BLOCK_CRACK_SEQ)
        retrieve(player, fight)
    }

    private fun retrieve(player: Player, fight: MoonsFight) {
        if (fights.of(player) !== fight) return
        when (frozenWeapons.release(player)) {
            ReleaseResult.Kept -> deps.worldQueues.add(1) { retrieve(player, fight) }
            else -> {
                player.set(WEAR_PREVENT, 0)
                player.mes(RETRIEVE_MESSAGE)
                player.soundSynth(RETRIEVE_SYNTH)
                player.soundSynth(EQUIP_SYNTH, delay = RETRIEVE_EQUIP_DELAY)
                player.soundSynth(EQUIP_SYNTH)
            }
        }
    }

    fun iciclesExplode() {
        for (block in icicles) {
            if (block != null && block.isSlotAssigned) block.anim(BLOCK_EXPLODE_SEQ)
        }
    }

    fun freezeExplode(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.moon != moon || fight.subdued) return
        if (fight.freezeTarget >= 0 && player.vars[ICICLE_STATE] == 0) {
            hitPlayer(player, deps.random.of(FAIL_HIT.first, FAIL_HIT.last))
            player.mes(FAIL_MESSAGE)
        }
        if (frozenWeapons.release(player) != ReleaseResult.Kept) player.set(WEAR_PREVENT, 0)
    }

    fun freezeEnd(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.moon != moon) return
        if (frozenWeapons.release(player) != ReleaseResult.Kept) player.set(WEAR_PREVENT, 0)
        player.set(ICICLE_STATE, 0)
        player.set(AD_HP, 0)
        fight.freezeTarget = -1
    }

    fun freezeSelfDamage(player: Player) {
        val fight = fights.of(player) ?: return
        if (fight.subdued) return
        val tracker = fights.tracker(moon) ?: return
        val hitpoints = tracker.of(player)?.hitpoints ?: return
        val cost = (hitpoints + SELF_DAMAGE_OFFSET) / SELF_DAMAGE_DIVISOR
        tracker.set(player, (hitpoints - cost).coerceAtLeast(1))
    }

    fun hideMoon(npc: Npc) {
        val type = ServerCacheManager.getNpc(INVISIBLE_MOON.asRSCM(RSCMType.NPC)) ?: return
        npc.transmog(type, Int.MAX_VALUE)
    }

    fun iceFloor(wave: Int) {
        val corners =
            List(BLOCK_OFFSETS.size) {
                when (wave) {
                    CORNERS_WAVE -> true
                    in OPTIONAL_CORNERS_WAVES -> deps.random.of(0, 1) == 1
                    else -> false
                }
            }
        val tiles = iceFloorTiles(corners).map { (dx, dz) -> moon.southWest.translate(dx, dz) }.toSet()
        val spawn = FLOOR_SPAWN.asRSCM(RSCMType.SPOTANIM)
        val idle = FLOOR_IDLE.asRSCM(RSCMType.SPOTANIM)
        val attack = FLOOR_ATTACK.asRSCM(RSCMType.SPOTANIM)
        for (tile in tiles) {
            deps.worldRepo.spotanimMap(spotanimType(spawn), tile)
            deps.worldRepo.spotanimMap(spotanimType(idle), tile, delay = FLOOR_IDLE_DELAY)
            deps.worldRepo.spotanimMap(spotanimType(attack), tile, delay = FLOOR_ATTACK_DELAY)
        }
        for ((dx, dz) in BLOCK_OFFSETS) {
            val at = moon.southWest.translate(dx, dz)
            deps.worldRepo.soundArea(at, FLOOR_SYNTH, radius = FLOOR_SOUND_RADIUS)
            deps.worldRepo.soundArea(at, FLOOR_SYNTH_2, delay = FLOOR_SYNTH_2_DELAY, radius = FLOOR_SOUND_RADIUS)
        }
        deps.worldQueues.add(FLOOR_STRIKE) { floorStrike(tiles) }
    }

    private fun spotanimType(id: Int) = SpotanimType(id)

    private fun floorStrike(tiles: Set<CoordGrid>) {
        for (player in fights.playersIn(moon)) {
            if (player.coords in tiles) hitPlayer(player, deps.random.of(FLOOR_HIT.first, FLOOR_HIT.last))
        }
    }

    override fun left(player: Player) {
        frozenWeapons.release(player).let { result ->
            if (result != ReleaseResult.Kept) player.set(WEAR_PREVENT, 0) else player.set(WEAR_PREVENT, 1)
        }
        player.set(ICICLE_STATE, 0)
        player.set(AD_HP, 0)
        player.set(HIDE_NPC, 0)
        player.set(BRAZIER_LEFT, 0)
        player.set(BRAZIER_RIGHT, 0)
        fights.of(player)?.freezeTarget = -1
        removeStandIn(player)
    }

    fun loginNet(player: Player) {
        if (frozenWeapons.held(player) != null) {
            val message =
                when (frozenWeapons.releaseAtLogin(player)) {
                    ReleaseResult.ToInventory -> LOGIN_INVENTORY_MESSAGE
                    ReleaseResult.ToBank -> LOGIN_BANK_MESSAGE
                    ReleaseResult.Kept -> LOGIN_KEPT_MESSAGE
                    else -> null
                }
            message?.let(player::mes)
        }
        player.set(WEAR_PREVENT, 0)
    }

    fun onDeathRelease(player: Player) {
        if (frozenWeapons.held(player) != null) frozenWeapons.release(player)
    }

    companion object {
        private const val NEVER = -10_000
        private const val FREEZE_TICKS = 58
        private const val HIDE_TICK = 8
        private const val EXPLODE_TICK = 51
        private const val DISARM_DELAY = 4
        private const val DISARM_RETRY_LIMIT = 47
        private const val BLOCK_HITPOINTS = 72
        private const val FORCED_MAX_HIT_STYLES = 7
        private const val BLOCK_HEADBAR = "headbar.health_160"
        private const val COLD_DECAY = 2
        private const val COLD_WARNING_1 = 3
        private const val COLD_WARNING_2 = 9
        private const val COLD_LOCK = 18
        private const val COLD_LOCK_DROP = 16
        private const val COLD_MESSAGE_1 =
            "<col=00e6e6>The Blue Moon pierces your defence. The cold is starting to get in...</col>"
        private const val COLD_MESSAGE_2 =
            "<col=00e6e6>The Blue Moon pierces your defence. Your joints are starting to lock up...</col>"
        private const val COLD_LOCK_MESSAGE =
            "<col=00e6e6>Your joints lock up as you go to attack. The cold is too much to bear!</col>"
        private const val COLD_SPOTANIM = "spotanim.wyvern_skeleton_impact_breath"
        private const val COLD_SPOTANIM_HEIGHT = 60
        private const val COLD_SPOTANIM_SLOT = 2
        private const val NO_WEAPON_MESSAGE =
            "<col=00e6e6>With no weapon in hand, your hands begin to freeze over!</col>"
        private const val FAIL_MESSAGE =
            "<col=00e6e6>The cold bites into you as the ice finally shatters.</col>"
        private const val DISARM_MESSAGE =
            "<col=00e6e6>Your weapon freezes over. You're forced to let go!</col>"
        private const val RETRIEVE_MESSAGE = "You retrieve your weapon from within the cracked ice."
        private const val LOGIN_INVENTORY_MESSAGE =
            "Eyatlalli returns the weapon you left frozen in the Blue Moon's arena."
        private const val LOGIN_BANK_MESSAGE =
            "Eyatlalli has sent the weapon you left frozen in the Blue Moon's arena to your bank."
        private const val LOGIN_KEPT_MESSAGE =
            "Eyatlalli is still holding the weapon you left frozen in the Blue Moon's arena. " +
                "Make room in your inventory or bank and log in again."
        private const val STAND_IN_NPC = "npc.pmoon_boss_tracking_npc"
        private const val STAND_IN_LIFESPAN = 20
        private const val FAKE_MAGIC = "spotanim.pmoon_boss_fake_blue_magic"
        private const val FAKE_FADE = "spotanim.pmoon_boss_fake_blue_fadeout"
        private const val INVISIBLE_MOON = "npc.pmoon_blue_boss_invis"
        private const val DISARM_SEQ = "seq.human_killerwatt_electricshock"
        private const val DISARM_SPOTANIM = "spotanim.vfx_djinn_blue_magic_player_spotanim_01"
        private const val DISARM_SPOTANIM_SLOT = 2
        private const val TELL_SPOTANIM = "spotanim.vfx_djinn_blue_magic_projectile_01"
        private const val TELL_START_HEIGHT = 70
        private const val TELL_END_HEIGHT = 100
        private const val TELL_END_TIME = 90
        private const val TELL_ANGLE = 150
        private const val TELL_PROGRESS = 10
        private const val BLOCK_GLOW_SEQ = "seq.vfx_djinn_blue_ice_block_idle_02"
        private const val BLOCK_RISE_SEQ = "seq.vfx_djinn_blue_ice_block_spawn_01"
        private const val BLOCK_CRACK_SEQ = "seq.vfx_djinn_blue_ice_block_cracking_01"
        private const val BLOCK_EXPLODE_SEQ = "seq.vfx_djinn_blue_ice_block_explode_01"
        private const val DISARM_SYNTH = "synth.varl_pm_boss_blue_disarm_player_01"
        private const val PUNCH_SYNTH = "synth.varl_pm_boss_blue_weapon_ice_block_cracking_01"
        private const val RETRIEVE_SYNTH = "synth.varl_pm_boss_blue_weapon_ice_block_break"
        private const val EQUIP_SYNTH = "synth.equip_backpack"
        private const val RETRIEVE_EQUIP_DELAY = 20
        private const val FLOOR_SPAWN = "spotanim.vfx_djinn_ice_floor_spawn_01"
        private const val FLOOR_IDLE = "spotanim.vfx_djinn_ice_floor_idle_01"
        private const val FLOOR_ATTACK = "spotanim.vfx_djinn_ice_floor_attack_01"
        private const val FLOOR_IDLE_DELAY = 30
        private const val FLOOR_ATTACK_DELAY = 90
        private const val FLOOR_STRIKE = 3
        private const val FLOOR_SYNTH = "synth.varl_pm_boss_blue_ice_spike_spawn"
        private const val FLOOR_SYNTH_2 = "synth.varl_pm_boss_blue_ice_spike_fly_up"
        private const val FLOOR_SYNTH_2_DELAY = 70
        private const val FLOOR_SOUND_RADIUS = 8
        private const val CORNERS_WAVE = 3
        private const val SELF_DAMAGE_OFFSET = 8
        private const val SELF_DAMAGE_DIVISOR = 31
        const val WEAR_PREVENT = "varbit.pmoon_boss_blue_wearprevent"
        const val ICICLE_STATE = "varbit.pmoon_boss_icicle_state"
        const val AD_HP = MoonsFights.AD_HP
        const val HIDE_NPC = "varbit.pmoon_boss_hide_npc"
        const val BRAZIER_LEFT = "varbit.pmoon_boss_blue_brazier_lit_left"
        const val BRAZIER_RIGHT = "varbit.pmoon_boss_blue_brazier_lit_right"
        private val NO_WEAPON_HIT = 3..15
        private val FAIL_HIT = 1..28
        private val FLOOR_HIT = 1..10
        private val OPTIONAL_CORNERS_WAVES = setOf(2, 4)

        /** Block south-west corners relative to the Moon's south-west tile. */
        val BLOCK_OFFSETS = listOf(-4 to 0, 5 to -2, 3 to 7)

        private val PLUS =
            shape(
                listOf(0..1),
                listOf(0..1),
                listOf(-1..2),
                listOf(-3..-1, 2..4),
                listOf(-3..-1, 2..4),
                listOf(-1..2),
                listOf(0..1),
                listOf(0..1),
            )

        private val CORNERS =
            shape(
                listOf(-3..-1, 2..4),
                listOf(-3..-1, 2..4),
                listOf(-3..4),
                listOf(-1..-1, 2..2),
                listOf(-1..-1, 2..2),
                listOf(-3..4),
                listOf(-3..-1, 2..4),
                listOf(-3..-1, 2..4),
            )

        private const val SHAPE_TOP = 4

        private fun shape(vararg rows: List<IntRange>): List<Pair<Int, Int>> =
            rows.withIndex().flatMap { (row, ranges) ->
                ranges.flatMap { range -> range.map { dx -> dx to SHAPE_TOP - row } }
            }

        fun iceFloorTiles(corners: List<Boolean>): Set<Pair<Int, Int>> {
            val tiles = mutableSetOf<Pair<Int, Int>>()
            for ((index, block) in BLOCK_OFFSETS.withIndex()) {
                val shape = if (corners[index]) CORNERS else PLUS
                shape.mapTo(tiles) { (dx, dz) -> (block.first + dx) to (block.second + dz) }
            }
            return tiles
        }
    }
}
