package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.floor
import kotlin.math.min
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

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

    val pointMultiplier: Double
        get() = spec.pointMultiplier

    private val random
        get() = room.raid.deps.random

    fun scale(partySize: Int) {
        val raidLevel = room.raid.settings.raidLevel
        val pathLevel = if (spec.pathScaled) room.pathLevel.coerceIn(0, MAX_PATH_LEVEL) else 0

        val hp = scaledHitpoints(npc.type.hitpoints, raidLevel, pathLevel, partySize)
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp

        val raidFactor = 1.0 + raidLevel * RAID_LEVEL_FACTOR
        val levelFactor =
            if (pathLevel > 0) PATH_LEVEL_FIRST + (pathLevel - 1) * PATH_LEVEL_EACH else 0.0
        damageFactor = min(MAX_DAMAGE_FACTOR, raidFactor + levelFactor)
    }

    fun holdDefenceFloor() {
        val cap = defenceCap ?: return
        val floor = npc.type.defence - cap
        if (npc.defenceLvl < floor) npc.defenceLvl = floor
    }

    fun rollAccuracy(target: Player, style: HitType, meleeAttackType: MeleeAttackType?): Boolean {
        val deps = room.raid.deps
        val type = npc.visType
        val (attackRoll, defenceRoll) =
            when (style) {
                HitType.Melee ->
                    deps.nvpMeleeAccuracy.computeAttackRoll(npc, type) to
                        deps.nvpMeleeAccuracy.computeDefenceRoll(target, meleeAttackType)
                HitType.Ranged ->
                    deps.nvpRangedAccuracy.computeAttackRoll(npc, type) to
                        deps.nvpRangedAccuracy.computeDefenceRoll(target)
                HitType.Magic,
                HitType.Dragonfire,
                HitType.DragonfireMetal,
                HitType.WyvernIce ->
                    deps.nvpMagicAccuracy.computeAttackRoll(npc, type) to
                        deps.nvpMagicAccuracy.computeDefenceRoll(target)
                HitType.Typeless -> return true
            }
        val raidLevel = room.raid.settings.raidLevel
        val raidAttackRoll = attackRoll * (RAID_ROLL_DIVISOR + raidLevel) / RAID_ROLL_DIVISOR
        val hitChance = AccuracyOperations.calculateHitChance(raidAttackRoll, defenceRoll)
        return AccuracyFormulae.isSuccessfulHit(hitChance, deps.random)
    }

    fun maxHit(base: Int): Int = floor(base * damageFactor).toInt()

    fun rollScaled(base: Int): Int = random.of(0, maxHit(base))

    fun rollScaled(min: Int, base: Int): Int = random.of(min, maxHit(base).coerceAtLeast(min))

    private fun scaledHitpoints(base: Int, raidLevel: Int, pathLevel: Int, partySize: Int): Int {
        var hp = base + base * (raidLevel * RAID_HP_PERCENT_PER_10_LEVELS / 10) / 100
        if (pathLevel > 0) {
            hp = hp * (100 + PATH_HP_PERCENT_BASE + PATH_HP_PERCENT_EACH * pathLevel) / 100
        }
        val size = partySize.coerceIn(1, MAX_PARTY_SIZE)
        if (size > 1) {
            var tenths = if (size == 2) TEAM_HP_TENTHS_SECOND else TEAM_HP_TENTHS_SECOND * 2
            if (size > 3) tenths += TEAM_HP_TENTHS_REST * (size - 3)
            hp = hp * (10 + tenths) / 10
        }
        if (hp <= HP_ROUND_FIVE_ABOVE) return hp
        val step = if (hp > HP_ROUND_TEN_ABOVE) 10 else 5
        return (hp + step / 2) / step * step
    }

    private companion object {
        const val RAID_LEVEL_FACTOR = 0.004
        const val RAID_ROLL_DIVISOR = 250
        const val PATH_LEVEL_FIRST = 0.08
        const val PATH_LEVEL_EACH = 0.05
        const val MAX_PATH_LEVEL = 6
        const val MAX_DAMAGE_FACTOR = 2.5

        const val RAID_HP_PERCENT_PER_10_LEVELS = 4
        const val PATH_HP_PERCENT_BASE = 3
        const val PATH_HP_PERCENT_EACH = 5
        const val MAX_PARTY_SIZE = 8
        const val TEAM_HP_TENTHS_SECOND = 9
        const val TEAM_HP_TENTHS_REST = 6
        const val HP_ROUND_TEN_ABOVE = 300
        const val HP_ROUND_FIVE_ABOVE = 100
    }
}
