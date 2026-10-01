package org.rsmod.content.bosses.doom

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.VarExpr
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder

internal object DoomShield {
    const val PHASE = "shielded"
    const val ENTER_EXT = "doom.shield_enter"
    const val EXIT_EXT = "doom.shield_exit"
    const val END_ABILITY = "end_shield"

    const val TRIGGER_HP = 0.75
    const val TRIGGER_ATTACKS = 2
    const val POOL_HP = 500
    const val RESIST_MESSAGE = "<col=a53fff>The demonic shield resists your attack!</col>"

    private const val ENTRY_DELAY = 2
    private const val CHARGE_TICKS = 17
    private const val END_DELAY = 1

    private val chargeLoop: Effect =
        sequence(
            anim(DoomCharge.CHARGE_LOOP_SEQ),
            spotanim(DoomCharge.CHARGE_UP_SPOTANIM, slot = DoomCharge.CHARGE_UP_SLOT),
        )

    private val startCharge: Effect =
        sequence(
            headbar(DoomCharge.CHARGE_BAR, fromPercent = 0, toPercent = 100, cycles = CHARGE_TICKS * DoomCharge.CYCLES_PER_TICK),
            setVarn(DoomVarns.CHARGE_END, VarExpr.Now + CHARGE_TICKS),
            setVarn(DoomVarns.CHARGE_LOOP_FROM, VarExpr.Now + 1),
        )

    val restartCharge: Effect = sequence(anim(DoomCharge.CANCEL_SEQ), startCharge)

    fun start(delve: DoomDelve): Effect =
        sequence(
            anim(DoomCharge.CHARGE_SEQ),
            wait(ENTRY_DELAY),
            transitionTo(PHASE),
            external(ENTER_EXT, params = delve),
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_SHIELD),
            setVarn(DoomVarns.GUARANTEED_HIT, 1),
            chargeLoop,
            startCharge,
        )

    /** Every shielded tick: fire once the charge deadline is reached, else keep the charge-up loop going. */
    fun chargeTick(delve: DoomDelve): Effect =
        whenever(
            varnExpired(DoomVarns.CHARGE_END),
            then = fire(delve),
            otherwise = whenever(varnExpired(DoomVarns.CHARGE_LOOP_FROM), chargeLoop),
        )

    private fun fire(delve: DoomDelve): Effect =
        sequence(
            setVarn(DoomVarns.CHARGE_END, 0),
            setVarn(DoomVarns.CHARGE_LOOP_FROM, 0),
            clearHeadbar(DoomCharge.CHARGE_BAR),
            DoomCharge.beam(delve),
            after(END_DELAY, run(END_ABILITY)),
        )

    fun end(delve: DoomDelve): Effect =
        whenever(
            InPhase(PHASE),
            sequence(
                transitionTo(if (delve.hasBurrow) DoomCar.ENTRY_PHASE else FIGHT_PHASE),
                setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_NONE),
                setVarn(DoomVarns.GUARANTEED_HIT, 0),
                setVarn(DoomVarns.CHARGE_END, 0),
                setVarn(DoomVarns.CHARGE_LOOP_FROM, 0),
                clearHeadbar(DoomCharge.CHARGE_BAR),
                external(EXIT_EXT, params = delve),
                if (delve.hasBurrow) run(DoomCar.ENTER_ABILITY) else nextAttackIn(0),
            ),
        )
}

@Singleton
internal class DoomShields
@Inject
constructor(
    private val deps: BossDeps,
    private val hpBar: BossHpBarScript,
    private val larvae: DoomLarvae,
) {
    fun register() {
        deps.extensionRegistry.register(DoomShield.ENTER_EXT) { ext -> enter(ext.npc, ext.target, ext.params as DoomDelve) }
        deps.extensionRegistry.register(DoomShield.EXIT_EXT) { ext -> exit(ext.npc, ext.target, ext.params as DoomDelve) }
    }

    fun capHit(boss: Npc, hit: HitBuilder) {
        if (!isShielded(boss)) return
        hit.damage = min(hit.damage, max(boss.hitpoints - 1, 0))
    }

    fun absorbsDeath(boss: Npc): Boolean = boss.hitpoints > 0 || isShielded(boss)

    fun onHit(boss: Npc) {
        if (!isShielded(boss) || boss.vars[DoomVarns.CHARGE] != DoomVarns.CHARGE_SHIELD || boss.hitpoints > 1) return
        boss.vars[DoomVarns.CHARGE] = DoomVarns.CHARGE_SHIELD_BROKEN
        boss.vars[DoomVarns.CHARGE_END] = 0
        boss.vars[DoomVarns.CHARGE_LOOP_FROM] = 0
        val player = deps.encounter(boss).lastTarget ?: return
        hpBar.onUpdate(player, boss, currentHp = 0)
        deps.worldQueues.add(1) {
            if (boss.isSlotAssigned && isShielded(boss)) deps.runAbility(boss, player, DoomShield.END_ABILITY)
        }
    }

    private fun enter(boss: Npc, player: Player, delve: DoomDelve) {
        boss.vars[DoomVarns.SHIELD_STASHED_HP] = boss.hitpoints
        boss.baseHitpointsLvl = DoomShield.POOL_HP
        boss.hitpoints = DoomShield.POOL_HP
        refreshHud(boss, player)
        larvae.startShieldStream(boss, player, delve)
    }

    private fun exit(boss: Npc, player: Player, delve: DoomDelve) {
        boss.baseHitpointsLvl = delve.hitpoints
        boss.hitpoints = boss.vars[DoomVarns.SHIELD_STASHED_HP]
        boss.vars[DoomVarns.SHIELD_STASHED_HP] = 0
        refreshHud(boss, player)
    }

    private fun refreshHud(boss: Npc, player: Player) {
        if (!player.isSlotAssigned) return
        hpBar.onUpdate(player, boss)
        hpBar.onRecolour(player, boss)
    }

    private fun isShielded(boss: Npc): Boolean = deps.encounter(boss).currentPhaseName == DoomShield.PHASE
}
