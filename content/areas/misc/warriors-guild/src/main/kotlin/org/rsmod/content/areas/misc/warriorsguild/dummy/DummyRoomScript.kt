package org.rsmod.content.areas.misc.warriorsguild.dummy

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.aconverted.SynthType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.fx.MeleeAnimationAndSound
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.WarguildDummiesRow
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.dummyRound
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Ajjat's dummy room: one dummy at a time springs out of its hole (the controlled dummy out of two)
 * for 4-15 ticks, and each player gets one swing at it per appearance.
 */
class DummyRoomScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val conRepo: ControllerRepository,
    private val attackTypes: AttackTypes,
    private val attackStyles: AttackStyles,
    private val random: GameRandom,
) : PluginScript() {
    private var current: WarguildDummiesRow? = null
    private var ticksLeft = 0
    private var round = 0

    override fun ScriptContext.startup() {
        onGameStartup {
            val controller = Controller(CONTROLLER, ROOM_CENTRE)
            conRepo.add(controller, Int.MAX_VALUE)
            controller.aiTimer(1)
        }
        onAiConTimer(CONTROLLER) { tick() }
        for (dummy in WarguildDummiesRow.all()) {
            onOpLoc1(RSCM.getReverseMapping(RSCMType.LOC, dummy.up.id)) { hit(dummy) }
        }
        onOpLoc1("loc.warguild_info_scroll_dummy") { ifOpenMainModal("interface.warguild_dummy") }
        onOpLoc1("loc.warguild_info_scroll_catapult") {
            ifOpenMainModal("interface.warguild_defence")
        }
    }

    private fun tick() {
        if (ticksLeft == 0) {
            val dummy = WarguildDummiesRow.all().random()
            current = dummy
            ticksLeft = random.of(MIN_TICKS, MAX_TICKS)
            round++
            for (hole in holes(dummy.hole.id)) locRepo.change(hole, dummy.up, ticksLeft - 1)
            for (up in holes(dummy.up.id)) worldRepo.locAnim(up, "seq.warguild_dummy_spring_up")
        } else if (ticksLeft == FALL_TICK) {
            val dummy = current
            if (dummy != null) {
                for (up in holes(dummy.up.id)) worldRepo.locAnim(up, "seq.warguild_dummy_spring_down")
            }
        } else if (ticksLeft == 1) {
            current = null
        }
        ticksLeft--
    }

    private fun holes(type: Int): List<LocInfo> =
        ROOM_ZONES.flatMap { zone -> locRepo.findAll(zone).filter { it.id == type }.toList() }

    private suspend fun ProtectedAccess.hit(dummy: WarguildDummiesRow) {
        if (current?.rowId != dummy.rowId) return
        if (player.dummyRound == round) {
            mes("You've already hit a dummy this turn.")
            return
        }
        val type = attackTypes.get(player)
        val style = attackStyles.get(player)
        if (type == null || !type.isMelee || style == null) {
            mes("You can only use melee weapons in this minigame.")
            return
        }
        player.dummyRound = round
        playAttackFx()
        delay(1)
        val rightStyle = dummy.attackStyle == 0 || dummy.attackStyle == style.id
        val rightType = dummy.attackType == 0 || dummy.attackType == type.id
        if (rightStyle && rightType) {
            statAdvance("stat.attack", ATTACK_XP)
            TokenLedger.credit(player, TOKENS)
            mes("You whack the dummy successfully!")
            return
        }
        mes("You whack the dummy with the wrong attack style.")
        spotanim("spotanim.stunned", height = STUN_HEIGHT)
        soundSynth(STUN_SYNTH)
        player.frozen = true
        player.routeDestination.clear()
        player.timer(FREEZE_TIMER, STUN_TICKS)
    }

    private fun ProtectedAccess.playAttackFx() {
        val stance = CombatStance[player.vars["varp.com_mode"]]
        val fx = MeleeAnimationAndSound.from(stance)
        val weapon = getOrNull(player.righthand)
        val seq = weapon?.paramOrNull(fx.animParam) ?: SequenceServerType(fx.defaultAnim.asRSCM())
        val synth = weapon?.paramOrNull(fx.soundParam) ?: SynthType(fx.defaultSound.asRSCM())
        anim(RSCM.getReverseMapping(RSCMType.SEQ, seq.id))
        player.soundSynth(synth)
    }

    private companion object {
        const val CONTROLLER = "controller.warguild_dummies"
        const val MIN_TICKS = 4
        const val MAX_TICKS = 15
        const val FALL_TICK = 2
        const val ATTACK_XP = 15.0
        const val TOKENS = 2
        const val STUN_TICKS = 3
        const val STUN_HEIGHT = 100
        const val FREEZE_TIMER = "timer.combat_freeze"
        const val STUN_SYNTH = "synth.stunned"

        val ROOM_CENTRE = CoordGrid(2857, 3552, 0)
        val ROOM_ZONES =
            listOf(
                ZoneKey.from(CoordGrid(2855, 3549, 0)),
                ZoneKey.from(CoordGrid(2856, 3549, 0)),
                ZoneKey.from(CoordGrid(2855, 3552, 0)),
                ZoneKey.from(CoordGrid(2856, 3552, 0)),
            )
    }
}
