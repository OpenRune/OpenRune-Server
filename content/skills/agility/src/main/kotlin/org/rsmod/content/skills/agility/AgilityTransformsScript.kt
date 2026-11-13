package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.invtx.transfer
import org.rsmod.api.player.back
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.agility.AgilityForms.holdsAt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The forms of `dbtable.agility_transforms`. Larry's Tuxedo-time puts a player who carries a
 * clockwork suit into the penguin suit, which keeps their hands empty and lasts while they stay
 * near the penguins; a worn greegree turns its wearer into its monkey on Ape Atoll. Every form is
 * kept each tick, so it comes back after a logout and goes once its player leaves its mapsquares
 * or takes the greegree off. In the penguin suit the player swims in the Penguin course's pool and
 * sleds down its run. The penguins' secret lair and the course's door are here too.
 */
class AgilityTransformsScript @Inject constructor(private val players: PlayerList) :
    PluginScript() {
    private val swim by lazy { stance(SWIM_READY, SWIM_WALK, SWIM_BACKWARD, SWIM_WALK) }
    private val slide by lazy { stance(SLIDE_READY, SLIDE_WALK, SLIDE_WALK, SLIDE_RUN) }

    override fun ScriptContext.startup() {
        onOpNpc3(LARRY_ZOO) { tuxedoTime(it.npc) }
        onOpNpc3(LARRY_ICEBERG) { tuxedoTime(it.npc) }
        onOpLoc1(SECRET_LAIR) { enter(SECRET_LAIR_ENTRY, "You enter the secret lair.") }
        onOpLoc1(COURSE_DOOR) { enter(COURSE_ENTRY, "You enter the agility course.") }
        onPlayerQueue(SUIT_OFF) { suitOff() }
        onEvent<HeldEquipEvents.WearposChange> { keepHandsEmpty(player, wearpos) }
        onEvent<GameLifecycle.LateCycle> { players.forEach(::keepForm) }
    }

    private suspend fun ProtectedAccess.tuxedoTime(larry: Npc) {
        if (player.inPenguinSuit) {
            return
        }
        if (!startedColdWar()) {
            // UNVERIFIED: wording guessed.
            mes("You need to have started Cold War to do that.")
            return
        }
        if (SUITS.none { inv.count(it) > 0 }) {
            // UNVERIFIED: wording guessed.
            mes("You need a clockwork suit for Larry to put you in.")
            return
        }
        if (player.righthand != null || player.lefthand != null || player.back != null) {
            // UNVERIFIED: wording guessed; the wiki says the weapon, shield and cape slots must be
            // empty.
            mes("You need to empty your hands and take off your cape to fit in the suit.")
            return
        }
        // UNVERIFIED: the suit stays in the inventory; live may take it while it is worn.
        larry.anim(LARRY_CAST)
        anim(INTO_PENGUIN)
        spotanim(INTO_PENGUIN_SPOT)
        soundSynth(INTO_PENGUIN_SYNTH, delay = 10)
        delay(INTO_PENGUIN_TICKS)
        player.inPenguinSuit = true
        player.transmog = AgilityForms.penguinSuit.npc
        rebuildAppearance()
        mes("You shrink and Larry puts you in the penguin suit.")
        // TODO(content): the penguin emotes the clockwork suit unlocks.
    }

    private suspend fun ProtectedAccess.enter(dest: CoordGrid, message: String) {
        mes(message)
        fadeOverlay(0, FADE_CLEAR, 0, FADE_OPAQUE, FADE_CYCLES)
        delay(ENTER_TICKS)
        telejump(dest)
        fadeOverlay(0, FADE_OPAQUE, 0, FADE_CLEAR, FADE_CYCLES)
        closeFadeOverlay()
    }

    private fun ProtectedAccess.startedColdWar(): Boolean {
        val quest = AgilityCoursesRow.getRow(PENGUIN_COURSE).quest ?: return true
        val key = RSCM.getReverseMapping(RSCMType.DBROW, quest.rowId).substringAfter('.')
        return QuestRequirements.hasCompleted(player, key) ||
            QuestRequirements.isOnQuest(player, key)
    }

    private fun ProtectedAccess.suitOff() {
        mes("The spell wears off as you climb out of the suit, growing back to normal size.")
        resetAnim()
        rebuildAppearance()
    }

    private fun keepHandsEmpty(player: Player, wearpos: Wearpos) {
        if (!player.inPenguinSuit || wearpos != Wearpos.RightHand && wearpos != Wearpos.LeftHand) {
            return
        }
        val held = player.worn[wearpos.slot] ?: return
        val result =
            player.invTransaction(player.worn, player.inv) {
                val worn = select(player.worn)
                val inv = select(player.inv)
                transfer(worn, wearpos.slot, held.count, inv, untransform = true)
            }
        if (result.success) {
            player.mes("You can't hold anything in your hands in the penguin suit.")
            player.rebuildAppearance()
        }
    }

    private fun keepForm(player: Player) {
        if (!player.inPenguinSuit && player.transmog == null && player.righthand == null) {
            return
        }
        if (player.inPenguinSuit) {
            keepPenguinSuit(player)
        }
        keepGreegreeForm(player)
    }

    private fun keepPenguinSuit(player: Player) {
        val suit = AgilityForms.penguinSuit
        if (!suit.holdsAt(player.coords)) {
            takeSuitOff(player)
            return
        }
        if (player.transmog?.id != suit.npc.id) {
            player.transmog = suit.npc
            player.rebuildAppearance()
        }
        val stance =
            when {
                player.coords.within(suit.swimArea) -> swim
                player.coords.within(suit.slideArea) -> slide
                else -> null
            }
        val current = player.bas
        if (current === stance || stance == null && current !== swim && current !== slide) {
            return
        }
        player.bas = stance
        player.rebuildAppearance()
    }

    private fun takeSuitOff(player: Player) {
        player.inPenguinSuit = false
        player.transmog = null
        if (player.bas === swim || player.bas === slide) {
            player.bas = null
        }
        player.anim(OUT_OF_PENGUIN)
        player.spotanim(OUT_OF_PENGUIN_SPOT)
        player.soundSynth(OUT_OF_PENGUIN_SYNTH, delay = 10)
        player.rebuildAppearance()
        player.queue(SUIT_OFF, SUIT_OFF_TICKS)
    }

    // UNVERIFIED: no spotanim or sound plays as a greegree takes effect.
    private fun keepGreegreeForm(player: Player) {
        val current = player.transmog
        if (current != null && !AgilityForms.isWornForm(current.id)) {
            return
        }
        val form =
            player.righthand
                ?.let { AgilityForms.wornBy(it.id) }
                ?.takeIf { it.holdsAt(player.coords) }
                ?.npc
        if (current?.id == form?.id) {
            return
        }
        player.transmog = form
        player.rebuildAppearance()
    }

    private fun stance(ready: String, walk: String, back: String, run: String): BasType =
        BasType(
            readyAnim = ready.asRSCM(RSCMType.SEQ),
            turnOnSpot = walk.asRSCM(RSCMType.SEQ),
            walkForward = walk.asRSCM(RSCMType.SEQ),
            walkBack = back.asRSCM(RSCMType.SEQ),
            walkLeft = walk.asRSCM(RSCMType.SEQ),
            walkRight = walk.asRSCM(RSCMType.SEQ),
            running = run.asRSCM(RSCMType.SEQ),
        )

    private fun CoordGrid.within(corners: List<CoordGrid>): Boolean {
        val (southWest, northEast) = corners.takeIf { it.size == 2 } ?: return false
        return level == southWest.level &&
            x in southWest.x..northEast.x &&
            z in southWest.z..northEast.z
    }

    private companion object {
        const val PENGUIN_COURSE = "dbrow.agility_course_penguin"
        const val LARRY_ZOO = "npc.peng_larry_zoo"
        const val LARRY_ICEBERG = "npc.peng_larry_ice"
        const val SECRET_LAIR = "loc.peng_aval_l"
        const val COURSE_DOOR = "loc.peng_base_double_door_mid_agility"
        const val SUIT_OFF = "queue.agility_penguin_suit_off"

        const val LARRY_CAST = "seq.human_caststrike"
        const val INTO_PENGUIN = "seq.peng_human_transmogrify"
        const val INTO_PENGUIN_SPOT = "spotanim.peng_transmogrify"
        const val INTO_PENGUIN_SYNTH = "synth.human_into_penguin"
        const val OUT_OF_PENGUIN = "seq.peng_human_detransmogrify"
        const val OUT_OF_PENGUIN_SPOT = "spotanim.peng_detransmogrify"
        const val OUT_OF_PENGUIN_SYNTH = "synth.penguin_into_human"

        const val SWIM_READY = "seq.peng_swim_ready"
        const val SWIM_WALK = "seq.peng_swim_walk"
        const val SWIM_BACKWARD = "seq.peng_swim_walk_backward"
        const val SLIDE_READY = "seq.peng_slide_ready"
        const val SLIDE_WALK = "seq.peng_slide_walk"
        const val SLIDE_RUN = "seq.peng_slide_run"

        const val INTO_PENGUIN_TICKS = 7
        const val SUIT_OFF_TICKS = 3
        const val ENTER_TICKS = 3
        const val FADE_CLEAR = 255
        const val FADE_OPAQUE = 0
        const val FADE_CYCLES = 50

        val SUITS = listOf("obj.peng_suit_unwound", "obj.peng_suit_wound")
        val SECRET_LAIR_ENTRY = CoordGrid(2658, 10371, 0)
        val COURSE_ENTRY = CoordGrid(2643, 4033, 1)

        var Player.inPenguinSuit by boolVarBit("varbit.peng_transmog")
    }
}
