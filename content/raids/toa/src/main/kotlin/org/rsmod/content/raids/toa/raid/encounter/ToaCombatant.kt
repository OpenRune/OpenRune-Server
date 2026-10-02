package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.floor
import kotlin.math.min
import org.rsmod.game.entity.Npc

internal class ToaCombatant(private val room: ToaEncounter, val npc: Npc, private val spec: Spec) {
    class Spec(
        val type: String,
        val defenceCap: Int? = null,
        val pointMultiplier: Double = 1.0,
        val pathScaled: Boolean = true,
    ) {
        internal val typeId: Int by lazy { type.asRSCM(RSCMType.NPC) }
    }

    var defenceCap: Int? = spec.defenceCap

    var damageFactor: Double = 1.0
        private set

    private var raidFactor = 1.0

    val pointMultiplier: Double
        get() = spec.pointMultiplier

    private val random
        get() = room.raid.deps.random

    fun scale(partySize: Int) {
        val type = npc.type
        val raidFactor = 1.0 + room.raid.settings.raidLevel * RAID_LEVEL_FACTOR
        val pathLevel = if (spec.pathScaled) room.pathLevel else 0
        val levelFactor =
            if (pathLevel > 0) PATH_LEVEL_FIRST + (pathLevel - 1) * PATH_LEVEL_EACH else 0.0
        val extra = (partySize - 1).coerceAtLeast(0)
        val first = min(extra, TEAM_HP_FIRST_PLAYERS)
        val teamFactor = 1.0 + first * TEAM_HP_FIRST + (extra - first) * TEAM_HP_REST

        val hp = roundToTen(type.hitpoints * raidFactor * teamFactor * (1.0 + levelFactor))
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp
        val defence = floor(type.defence * raidFactor).toInt()
        npc.baseDefenceLvl = defence
        npc.defenceLvl = defence
        val attack = floor(type.attack * raidFactor).toInt()
        npc.baseAttackLvl = attack
        npc.attackLvl = attack
        val ranged = floor(type.ranged * raidFactor).toInt()
        npc.baseRangedLvl = ranged
        npc.rangedLvl = ranged
        val magic = floor(type.magic * raidFactor).toInt()
        npc.baseMagicLvl = magic
        npc.magicLvl = magic

        this.raidFactor = raidFactor
        damageFactor = min(MAX_DAMAGE_FACTOR, raidFactor + levelFactor)
    }

    fun holdDefenceFloor() {
        val cap = defenceCap ?: return
        val floor = floor((npc.type.defence - cap) * raidFactor).toInt()
        if (npc.defenceLvl < floor) npc.defenceLvl = floor
    }

    fun maxHit(base: Int): Int = floor(base * damageFactor).toInt()

    fun rollScaled(base: Int): Int = random.of(0, maxHit(base))

    fun rollScaled(min: Int, base: Int): Int = random.of(min, maxHit(base).coerceAtLeast(min))

    private fun roundToTen(value: Double): Int = ((value + 5.0) / 10.0).toInt() * 10

    private companion object {
        const val RAID_LEVEL_FACTOR = 0.004
        const val TEAM_HP_FIRST_PLAYERS = 2
        const val TEAM_HP_FIRST = 0.9
        const val TEAM_HP_REST = 0.6
        const val PATH_LEVEL_FIRST = 0.08
        const val PATH_LEVEL_EACH = 0.05
        const val MAX_DAMAGE_FACTOR = 2.5
    }
}
