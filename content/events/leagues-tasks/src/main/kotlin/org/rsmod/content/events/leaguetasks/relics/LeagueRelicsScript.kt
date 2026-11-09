package org.rsmod.content.events.leaguetasks.relics

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.content.events.leaguetasks.tutorial.LeagueMenu
import org.rsmod.content.events.leaguetasks.tutorial.LeagueTutorial
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The client draws the relic grid itself; only `clickzones` and `confirm_button` reach the server. */
class LeagueRelicsScript
@Inject
constructor(
    private val relics: LeagueRelics,
    private val tasks: LeagueTasks,
    private val tutorial: LeagueTutorial,
    private val eventBus: EventBus,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.league_side_panel:relics_button") {
            if (!tutorial.takesMenuButton(player, LeagueMenu.Relics)) {
                player.openRelics()
            }
        }
        onIfModalButton("component.league_relics:clickzones") { player.viewRelic(it.comsub) }
        onIfModalButton("component.league_relics:confirm_button") { confirmRelic() }
    }

    private fun Player.openRelics() {
        ifOpenMainModal("interface.league_relics", eventBus, colour = -1, transparency = -3)
        ifSetEvents("component.league_relics:clickzones", 0..23, IfEvent.Op1)
        ifSetEvents("component.league_relics:league_menu_frame", 10..20, IfEvent.Op1)
        ifSetEvents("component.league_relics:toggles", -1..-1, IfEvent.ScriptTrigger)
    }

    private fun Player.viewRelic(slot: Int) {
        val tier = relics.tierAt(this, slot) ?: return
        val relic = relics.relicAt(this, slot) ?: return
        VarPlayerIntMapSetter.set(this, "varbit.league_relic_last_viewed", slot)
        runClientScript(
            "clientscript.[clientscript,league_relic_expanded_view]".asRSCM(RSCMType.CLIENTSCRIPT),
            "component.league_relics:view_all".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:view_all_scrollbar".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:view_one".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:loading".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:icon".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:name".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:description_header".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:description".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:select_button".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:select_back".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:confirm".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:confirm_frame".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:confirm_text".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:confirm_button".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:confirm_cancel".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:passive_header".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:passive_description".asRSCM(RSCMType.COMPONENT),
            "component.league_relics:relic_available_header".asRSCM(RSCMType.COMPONENT),
            viewState(tier, relic),
            relic.struct,
            tier.passivesStruct,
            relic.description,
            tier.passives,
        )
    }

    /** UNVERIFIED: the state for a relic whose tier already has another pick. */
    private fun Player.viewState(tier: LeagueRelics.RelicTier, relic: LeagueRelics.Relic): Int {
        val selected = relics.selected(this, tier)
        return when {
            selected == relic.key -> VIEW_SELECTED
            selected == 0 && relics.isTierUnlocked(this, tier) -> VIEW_AVAILABLE
            else -> VIEW_LOCKED
        }
    }

    private suspend fun ProtectedAccess.confirmRelic() {
        val slot = vars["varbit.league_relic_last_viewed"]
        val tier = relics.tierAt(player, slot) ?: return
        val relic = relics.relicAt(player, slot) ?: return
        if (relics.selected(player, tier) != 0 || !relics.isTierUnlocked(player, tier)) {
            return
        }
        relics.select(player, tier, relic)
        player.ifCloseModal("interface.league_relics", eventBus)
        player.soundSynth("synth.league_unlock_1", delay = 8)
        player.soundSynth("synth.league_unlock_2", delay = 44)
        player.soundSynth("synth.league_unlock_3", delay = 80)
        player.soundSynth("synth.league_unlock_4", delay = 104)
        val female = player.appearance.bodyType == constants.bodytype_b
        anim(if (female) "seq.human_relic_unlock_female" else "seq.human_relic_unlock")
        relics.unlockSpotanim(player)?.let { spotanim(it, height = UNLOCK_SPOTANIM_HEIGHT) }
        delay(UNLOCK_MESSAGE_DELAY)
        val league = tasks.leagueOf(player) ?: return
        mes("Congratulations, you've unlocked a new Relic: ${league.messageColour}${relic.name}</col>.")
        ClientScripts.notificationDisplay(
            player,
            "Relic Unlocked!",
            "You have unlocked the <col=ffffff>${relic.name}</col> Relic.",
            league.notificationColour,
        )
        player.midiJingle("jingle.dt2_whisperer")
        eventBus.publish(LeagueRelicUnlockedEvent(player, tier, relic))
    }

    private companion object {
        const val VIEW_LOCKED = 0
        const val VIEW_SELECTED = 1
        const val VIEW_AVAILABLE = 2
        const val UNLOCK_SPOTANIM_HEIGHT = 92
        const val UNLOCK_MESSAGE_DELAY = 5
    }
}
