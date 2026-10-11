package org.rsmod.content.slayer.requirements

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.NpcAttackOverride
import org.rsmod.api.combat.commons.npc.NpcAttackOverrideHook
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.table.slayer.SlayerGearRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getOrNull

internal object SlayerGear {
    const val ENGAGEMENT_RESET_TICKS = 20

    private val drains: Map<Int, List<Pair<String, Int>>> =
        SlayerGearRow.all().associate { row ->
            val percents = row.drains?.backing.orEmpty()
            row.rowId to
                percents.mapNotNull { (stat, percent) ->
                    percent?.let { stat.internalName to it }
                }
        }

    fun isProtected(worn: Array<InvObj?>, gear: SlayerGearRow): Boolean =
        worn.any { obj ->
            val type = getOrNull(obj) ?: return@any false
            type.id == gear.item?.id || (gear.group != null && type.contentGroup == gear.group)
        }

    fun startsEngagement(
        lastTargetUid: Int,
        lastAttackClock: Int,
        targetUid: Int,
        clock: Int,
    ): Boolean = lastTargetUid != targetUid || clock - lastAttackClock > ENGAGEMENT_RESET_TICKS

    fun drainAmounts(gear: SlayerGearRow, currentLevel: (String) -> Int): List<Pair<String, Int>> =
        drains.getValue(gear.rowId).map { (stat, percent) ->
            stat to currentLevel(stat) * percent / PERCENT
        }

    fun attack(
        gear: SlayerGearRow,
        protected: Boolean,
        engagementStart: Boolean,
        baseHitpoints: Int,
    ): NpcAttackOverride? {
        if (gear.firstHitOnly == true && !engagementStart) {
            return null
        }
        if (protected) {
            return if (gear.firstHitOnly == true) NpcAttackOverride.Fixed(0) else null
        }
        val penetration = gear.penetration ?: 0
        val fixedHit = gear.fixedHit
        val hitpointsPercent = gear.hitpointsPercent
        val maxHit = gear.maxHit
        return when {
            fixedHit != null -> NpcAttackOverride.Fixed(fixedHit, penetration)
            hitpointsPercent != null ->
                NpcAttackOverride.Fixed(baseHitpoints * hitpointsPercent / PERCENT, penetration)
            maxHit != null -> NpcAttackOverride.MaxHit(maxHit, gear.guaranteed == true)
            else -> null
        }
    }

    private const val PERCENT = 100
}

internal class SlayerGearAttackHook @Inject constructor() : NpcAttackOverrideHook {
    override fun override(npc: Npc, target: Player): NpcAttackOverride? {
        val gear = SlayerRules.gear(npc) ?: return null
        val protected = SlayerGear.isProtected(target.worn.objs, gear)
        val engagementStart = gear.firstHitOnly == true && beginsEngagement(npc, target)
        if (!protected) {
            for ((stat, amount) in SlayerGear.drainAmounts(gear) { target.stat(it) }) {
                if (amount > 0) target.statSub(stat, amount, 0)
            }
        }
        if (protected && engagementStart) {
            gear.repelMessage?.let(target::mes)
        }
        return SlayerGear.attack(gear, protected, engagementStart, target.baseHitpointsLvl)
    }

    private fun beginsEngagement(npc: Npc, target: Player): Boolean {
        val clock = npc.currentMapClock
        val targetUid = target.uid.packed
        val starts =
            SlayerGear.startsEngagement(npc.engagedTarget, npc.engagedClock, targetUid, clock)
        npc.engagedTarget = targetUid
        npc.engagedClock = clock
        return starts
    }

    private var Npc.engagedTarget: Int by intVarn("varn.slayer_engaged_target")
    private var Npc.engagedClock: Int by intVarn("varn.slayer_engaged_clock")
}
