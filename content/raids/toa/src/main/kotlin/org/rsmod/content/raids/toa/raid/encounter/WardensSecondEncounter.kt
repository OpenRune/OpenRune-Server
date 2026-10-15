package org.rsmod.content.raids.toa.raid.encounter

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.bosses.runtime.forceNext
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.Camera
import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenEnrage
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenFinalAbilities
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenGod
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenPhantoms
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardenSiphons
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardensSlams
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class WardensSecondEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    internal var warden: Npc? = null
        private set

    private var phantom: Npc? = null

    internal val amascut: Npc?
        get() = phantom

    internal val slams = WardensSlams(this)
    internal val siphons = WardenSiphons(this)
    internal val phantoms = WardenPhantoms(this)
    internal val enrage = WardenEnrage(this)

    internal var siphonDue = false
        private set

    internal val god: WardenGod
        get() = raid.wardensRestored ?: WardenGod.TUMEKEN

    override val fight =
        ToaFight(
            boss = { warden },
            combatants =
                WardenGod.entries.map { ToaCombatant.Spec(it.phaseThree, pathScaled = false) } +
                    WardenPhantoms.Kind.entries.map {
                        ToaCombatant.Spec(it.npc, pathScaled = false, scaleHitpoints = false)
                    },
            firstAttackDelay = FIRST_SLAM_DELAY,
            attackRate = { slamRate },
            death = ToaDeath(anim = DEATH_SEQ, dead = null, modelDelay = DEATH_MODEL_DELAY),
        )

    override val loot: ToaBossLoot
        get() =
            ToaBossLoot(
                trackerNpc = god.phaseThree,
                book = ToaBook("obj.toa_book_wardens", "varbit.toa_book_wardens"),
            )

    private val deathNpcs = ArrayList<Npc>()

    internal val slamRate: Int
        get() =
            when {
                raid.isActive(ToaInvocationKey.Insanity) -> INSANITY_SLAM_RATE
                raid.isActive(ToaInvocationKey.Overclocked2) -> OVERCLOCKED_2_SLAM_RATE
                raid.isActive(ToaInvocationKey.Overclocked) -> OVERCLOCKED_SLAM_RATE
                else -> SLAM_RATE
            }

    internal val fastSlams: Boolean
        get() = slamRate <= FAST_SLAM_RATE

    override fun arrival(): Arrival = Arrival(coords(ARRIVAL_TILE), Direction.South)

    override fun onBuilt() {
        spawnRoom()
    }

    override fun onReset() {
        spawnRoom()
    }

    override fun onFightStart(boss: Npc) {
        combatantOf(boss)?.scale(teamSize, P3_BASE_HP)
    }

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        if (boss.hitpoints > 0) phantoms.tick(targets)
        checkEnrage(boss)
        if (siphonDue || siphons.active) return
        val threshold = siphonThreshold(boss) ?: return
        if (boss.hitpoints <= threshold) siphonDue = true
    }

    override fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc === warden && siphons.active) return NpcAttackValidateResult.Deny(WARDEN_BUSY)
        return NpcAttackValidateResult.Pass
    }

    internal fun modifyWardenHit(npc: Npc, hit: HitBuilder) {
        if (npc !== warden || enrage.active) return
        if (siphons.active) {
            hit.damage = 0
            return
        }
        val threshold = max(siphonThreshold(npc) ?: enrageThreshold(npc), 1)
        hit.damage = min(hit.damage, max(npc.hitpoints - threshold, 0))
    }

    internal fun keepRotation() {
        val boss = warden ?: return
        if (boss.hitpoints <= 0 || enrage.active) return
        deps.bossDeps.forceNext(boss, slams.next().ability)
    }

    internal fun checkEnrage(npc: Npc) {
        if (npc !== warden || enrage.active || siphons.active || npc.hitpoints <= 0) return
        if (siphons.launched < SIPHON_THRESHOLDS.size || npc.hitpoints > enrageThreshold(npc)) return
        val target = targets().firstOrNull() ?: return
        deps.bossDeps.runAbility(npc, target, WardenFinalAbilities.ENRAGE)
    }

    private fun enrageThreshold(boss: Npc): Int = floor(boss.baseHitpointsLvl * ENRAGE_FRACTION).toInt()

    internal fun debugSiphon(): String? {
        if (siphons.launched >= SIPHON_THRESHOLDS.size) return "All siphon sets have run."
        siphonDue = true
        return null
    }

    internal fun debugHp(percent: Int): String? {
        val boss = warden ?: return "No Warden."
        boss.hitpoints = boss.baseHitpointsLvl * percent.coerceIn(1, 100) / 100
        hpBar?.update()
        return null
    }

    internal fun siphonLaunched() {
        siphonDue = false
    }

    private fun siphonThreshold(boss: Npc): Int? {
        val fraction = SIPHON_THRESHOLDS.getOrNull(siphons.launched) ?: return null
        return floor(boss.baseHitpointsLvl * fraction).toInt()
    }

    override val completeMessageDelay = DEATH_MODEL_DELAY

    override fun onFightComplete() {
        siphons.reset()
        schedule(MUSIC_DELAY) {
            for (player in players) {
                player.midiJingle(COMPLETE_JINGLE)
                player.midiSong(COMPLETE_MUSIC)
            }
        }
        schedule(DEATH_MODEL_DELAY) { for (player in players) Camera.camReset(player) }
        schedule(DEATH_MODEL_DELAY) { spawnDeathNpcs() }
        schedule(PHANTOM_CLEAR_DELAY) {
            phantoms.clear()
            phantom?.let(::despawn)
            phantom = null
        }
        schedule(CRYSTAL_DELAY) {
            deps.locRepo.add(
                coords(CRYSTAL_TILE),
                CRYSTAL,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun spawnDeathNpcs() {
        for ((index, seq) in DEATH_OFFSET_SEQS.withIndex()) {
            val tile = coords(WARDEN_TILE.translate(0, -index * DEATH_NPC_STEP))
            deathNpcs += spawn(god.deathNpc, tile, Direction.North).also { it.anim(seq) }
        }
    }

    private fun spawnRoom() {
        siphons.reset()
        slams.reset()
        enrage.reset()
        phantoms.clear()
        for (npc in deathNpcs) despawn(npc)
        deathNpcs.clear()
        siphonDue = false
        warden?.let(::despawn)
        phantom?.let(::despawn)
        warden =
            spawn(god.phaseThree, coords(WARDEN_TILE), Direction.North).also {
                it.ignoreCombatInteractions = true
            }
        phantom = spawn(PHANTOM, coords(PHANTOM_TILE), Direction.North)
    }

    companion object {
        const val CRYSTAL = "loc.toa_teleport_crystal_continue_wardens"
        const val PHANTOM = "npc.toa_amascut_p3"
        const val P3_BASE_HP = 880
        private val CRYSTAL_TILE = CoordGrid(3936, 5154, 1)
        private val ARRIVAL_TILE = CoordGrid(3936, 5162, 1)
        private val WARDEN_TILE = CoordGrid(3934, 5152, 1)
        private val PHANTOM_TILE = CoordGrid(3932, 5127, 1)
        private val SIPHON_THRESHOLDS = listOf(0.8, 0.6, 0.4, 0.2)
        private const val ENRAGE_FRACTION = 0.05
        private const val WARDEN_BUSY = "You can't attack the warden right now."
        private const val COMPLETE_JINGLE = "jingle.grumpy"
        private const val COMPLETE_MUSIC = "midi.toa_laid_to_rest"
        private const val MUSIC_DELAY = 2
        private const val CRYSTAL_DELAY = 13
        private const val PHANTOM_CLEAR_DELAY = 3
        private const val DEATH_MODEL_DELAY = 1
        private const val DEATH_NPC_STEP = 5
        private const val DEATH_SEQ = "seq.npc_wardens_death01"
        private val DEATH_OFFSET_SEQS = (1..5).map { "seq.spotanim_wardens01_death02_offset0$it" }
        private const val FIRST_SLAM_DELAY = 6
        private const val SLAM_RATE = 7
        private const val OVERCLOCKED_SLAM_RATE = 6
        private const val OVERCLOCKED_2_SLAM_RATE = 4
        private const val INSANITY_SLAM_RATE = 3
        private const val FAST_SLAM_RATE = 4
    }
}
