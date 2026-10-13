package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBook
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossLoot
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaDeath
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class BabaEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    internal var baba: Npc? = null
        private set

    internal val boulders = BabaBoulders(this)
    internal val rockfall = BabaRockfall(this)
    internal val baboons = BabaBaboons(this)

    override val fight =
        ToaFight(
            boss = { baba },
            combatants = listOf(ToaCombatant.Spec(BabaNpcs.BABA)),
            music = BabaMidis.FIGHT,
            firstAttackDelay = FIRST_ATTACK_DELAY,
            attackRate = { ATTACK_RATE },
            death = ToaDeath(anim = BabaSeqs.DEATH, dead = null, modelDelay = DEATH_MODEL_DELAY),
        )

    override val completeMessageDelay = DEATH_MODEL_DELAY

    override val rewardNpc = BabaNpcs.FREED_APMEKEN

    override val loot =
        ToaBossLoot(
            trackerNpc = BabaNpcs.COFFIN,
            heroObj = BabaObjs.BANANA,
            book = ToaBook(BabaObjs.BOOK, BabaVarbits.BOOK_OWNED),
        )

    override fun onBuilt() {
        spawnBaba()
        baboons.spawnSarcophagi()
    }

    override fun onReset() {
        spawnBaba()
        baboons.clear()
        baboons.spawnSarcophagi()
    }

    override fun onFightStart(boss: Npc) {
        resetVarns(boss)
        boulders.clear()
        rockfall.clear()
        baboons.clear()
        baboons.spawnSarcophagi()
        boss.anim(BabaSeqs.SPAWN)
    }

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        boulders.tick()
        baboons.tick()
    }

    override fun onFightComplete() {
        baboons.clear()
        baboons.clearSarcophagi()
        boulders.clear()
        rockfall.clear()
    }

    override fun pickTarget(boss: Npc, targets: List<Player>): Player =
        lowestHpTarget(boss, targets)

    internal fun lowestHpTarget(boss: Npc, targets: List<Player>): Player =
        targets.minWith(
            compareBy<Player> { it.hitpoints }.thenBy { it.coords.chebyshevDistance(boss.coords) }
        )

    internal fun isRolling(boss: Npc): Boolean =
        deps.bossDeps.encounter(boss).currentPhaseName == BabaPhases.ROLLING

    internal fun retarget(boss: Npc) {
        if (boss !== baba) return
        val targets = targets()
        if (targets.isEmpty()) return
        boss.apPlayer2(pickTarget(boss, targets), deps.aiInteractions)
    }

    internal fun rockfallReady(boss: Npc): Boolean =
        boss.vars[BabaVarns.ROCKFALL_READY] <= deps.mapClock.cycle

    internal fun underBoss(boss: Npc): Boolean =
        targets().any {
            val dx = it.coords.x - boss.coords.x
            val dz = it.coords.z - boss.coords.z
            dx in 0 until BabaShock.BOSS_SIZE && dz in 0 until BabaShock.BOSS_SIZE
        }

    internal fun edgeTile(z: Int): CoordGrid {
        val x = deps.random.of(BabaRocks.EDGE_MIN_X, BabaRocks.EDGE_MAX_X)
        return coords(CoordGrid(x, z, BabaCoords.BABA.level))
    }

    internal fun rollReset(boss: Npc) {
        if (boss === baba) rockfall.clear()
    }

    internal fun modifyBabaHit(boss: Npc, hit: HitBuilder) {
        if (boss !== baba || !hit.isFromPlayer) return
        if (mustSurvive(boss) && !endsLastRoll(boss, hit) && hit.damage >= boss.hitpoints) {
            hit.damage = (boss.hitpoints - 1).coerceAtLeast(0)
        }
    }

    private fun mustSurvive(boss: Npc): Boolean =
        isRolling(boss) || boss.vars[BabaVarns.ROLLS] < ROLL_PHASES

    private fun lastRoll(boss: Npc): Boolean =
        isRolling(boss) && boss.vars[BabaVarns.ROLLS] == ROLL_PHASES - 1

    private fun endsLastRoll(boss: Npc, hit: HitBuilder): Boolean {
        if (!lastRoll(boss) || hit.type != HitType.Melee) return false
        if (!boss.isVisType(BabaNpcs.DIGGING)) return false
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.bossDeps.playerList) }
        return attacker != null && staticCoords(attacker.coords).x >= BabaRolling.TOP_ZONE_X
    }

    internal fun babaHit(boss: Npc, @Suppress("UNUSED_PARAMETER") hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== baba) return
        hpBar?.update()
    }

    /**
     * The hit clamp only sees her hitpoints when a hit is queued, so two hits in flight together
     * can still reach zero. She is put back on 1 unless the last roll has been ended by melee.
     */
    internal fun babaDeath(boss: Npc) {
        if (boss !== baba) return
        val lastRollEnded = lastRoll(boss) && boss.vars[BabaVarns.ROLL_EXIT] == 1
        if (mustSurvive(boss) && !lastRollEnded) {
            boss.hitpoints = 1
            hpBar?.update()
            return
        }
        complete()
    }

    internal fun nextWeakLane(boss: Npc): Int {
        val rows = boss.vars[BabaVarns.ROWS]
        val previous = boss.vars[BabaVarns.WEAK_LANE]
        val lane =
            if (rows == 0) {
                deps.random.of(1, BabaRolling.LANES - 2)
            } else {
                val spread = BabaRolling.WEAK_LANE_SPREAD
                deps.random
                    .of(previous - spread, previous + spread)
                    .coerceIn(0, BabaRolling.LANES - 1)
            }
        boss.vars[BabaVarns.WEAK_LANE] = lane
        boss.vars[BabaVarns.ROWS] = rows + 1
        return lane
    }

    internal fun boulderRow(boss: Npc) {
        if (boss !== baba) return
        boulders.spawnRow(nextWeakLane(boss))
    }

    internal fun crackedHit(npc: Npc, hit: HitBuilder) {
        if (!hit.isFromPlayer || hit.type != HitType.Ranged) return
        hit.damage = npc.hitpoints
        boulders.disarm(npc)
    }

    internal fun debugRoll(): String? {
        val boss = baba
        if (stage != ToaStage.STARTED || boss == null) return NOT_STARTED
        val percent = if (boss.vars[BabaVarns.ROLLS] == 0) FIRST_ROLL_PERCENT else SECOND_ROLL_PERCENT
        boss.hitpoints = (boss.baseHitpointsLvl * percent / 100).coerceAtLeast(1)
        hpBar?.update()
        return null
    }

    internal fun debugShockwave(stomp: Boolean): String? =
        runDebug(if (stomp) BabaAbilities.STOMP else BabaAbilities.SHOCKWAVE)

    internal fun debugDischarge(): String? {
        if (baboons.alive > 0) return "Baboons are already out."
        return runDebug(BabaAbilities.DISCHARGE)
    }

    internal fun debugRockfall(): String? = runDebug(BabaAbilities.ROCKFALL)

    private fun runDebug(ability: String): String? {
        val boss = baba
        if (stage != ToaStage.STARTED || boss == null) return NOT_STARTED
        val targets = targets().ifEmpty { return NOT_STARTED }
        deps.bossDeps.runAbility(boss, lowestHpTarget(boss, targets), ability)
        return null
    }

    private fun resetVarns(boss: Npc) {
        boss.vars[BabaVarns.ROLLS] = 0
        boss.vars[BabaVarns.ROWS] = 0
        boss.vars[BabaVarns.ROLL_EXIT] = 0
        boss.vars[BabaVarns.WEAK_LANE] = 0
        boss.vars[BabaVarns.AUTOS] = 0
        boss.vars[BabaVarns.THROW_AT] = 0
        boss.vars[BabaVarns.ROCKFALL_READY] = 0
        boss.vars[BabaVarns.LAST_SPECIAL] = 0
        boss.vars[BabaVarns.UNDER] = 0
    }

    private fun spawnBaba() {
        baba?.let(::despawn)
        val boss = spawn(BabaNpcs.BABA, coords(BabaCoords.BABA), Direction.West)
        boss.ignoreCombatInteractions = true
        boss.apRequiresLineOfSight = false
        baba = boss
        boulders.clear()
        rockfall.clear()
    }

    private companion object {
        const val ATTACK_RATE = 6
        const val FIRST_ATTACK_DELAY = 7
        const val DEATH_MODEL_DELAY = 4
        const val ROLL_PHASES = 2
        const val FIRST_ROLL_PERCENT = 65
        const val SECOND_ROLL_PERCENT = 32
        const val NOT_STARTED = "The fight isn't running."
    }
}
