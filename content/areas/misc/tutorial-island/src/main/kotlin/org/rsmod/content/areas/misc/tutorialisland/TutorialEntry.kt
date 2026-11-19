package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.input.StringDialogInput
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialEntry
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val guide: TutorialGuide,
    private val rules: TutorialRules,
    private val eventBus: EventBus,
    private val playerList: PlayerList,
) : PluginScript() {

    private var Player.newAccount by boolVarBit("varbit.new_player_account")

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.PrepareLogin> { prepareLogin() }
        onPlayerLogin { login() }
        onEvent<StringDialogInput> { lookupName() }
        onIfModalButton("component.tutorial_displayname:set_name") { setName() }
        onIfModalButton("component.tutorial_displayname:suggestion1") { selectSuggestion(0) }
        onIfModalButton("component.tutorial_displayname:suggestion2") { selectSuggestion(1) }
        onIfModalButton("component.tutorial_displayname:suggestion3") { selectSuggestion(2) }
        onIfClose("interface.tutorial_displayname") { closeName(player) }
        onIfModalButton("component.player_design:gender_male") { setBodyType(0, 0) }
        onIfModalButton("component.player_design:gender_female") { setBodyType(1, 1) }
        onIfModalButton("component.player_design:confirm") { confirmDesign(closed = false) }
        onIfClose("interface.player_design") {
            protectedAccess.launch(player) { confirmDesign(closed = true) }
        }
        onIfClose("interface.tutorial_player_experience") { closeExperience(player) }
        onPlayerSoftTimer(TUTORIAL_TIMER) { tick() }
    }

    private fun SessionStateEvent.PrepareLogin.prepareLogin() {
        if (!player.newAccount) {
            return
        }
        if (player.tutorialStage == 0) {
            player.coords = TutorialCoords.START
            player.tutorialStage = TutorialStage.START
            player.displayNameStatus = 1
        }
    }

    private fun SessionStateEvent.Login.login() {
        if (!player.inTutorial) {
            return
        }
        if (player.tutorialStage <= TutorialStage.START) {
            player.beginName()
        } else {
            guide.apply(player, player.tutorialStage)
            player.softTimer(TUTORIAL_TIMER, 1)
        }
    }

    private fun Player.beginName() {
        protectedAccess.launch(this) {
            if (player.tutorialStage > TutorialStage.START) {
                return@launch
            }
            player.ifOpenFullOverlay("interface.tutorial_overlay", eventBus)
            player.ifSetHide("component.tutorial_overlay:noclick", true)
            ifOpenMainModal("interface.tutorial_displayname")
            player.ifSetText(
                "component.tutorial_displayname:status",
                "Please look up a name to see whether it is available.",
            )
            player.runClientScript(
                NAME_ALLOW_INPUT,
                NAME_LOOK_UP.asRSCM(RSCMType.COMPONENT),
                NAME_SET.asRSCM(RSCMType.COMPONENT),
                NAME_TEXT.asRSCM(RSCMType.COMPONENT),
                NAME_HIGHLIGHT.asRSCM(RSCMType.COMPONENT),
                NAME_INPUT.asRSCM(RSCMType.COMPONENT),
                true,
            )
        }
    }

    private fun StringDialogInput.lookupName() {
        if (player.tutorialStage != TutorialStage.START) {
            return
        }
        val name = text.trim()
        player.attr[CANDIDATE_NAME] = name
        if (!validName(name)) {
            sendNameStatus(player, name, available = false, invalid = true)
            return
        }
        val taken = playerList.any { it !== player && it.displayName.equals(name, ignoreCase = true) }
        sendNameStatus(player, name, available = !taken)
    }

    private fun sendNameStatus(
        player: Player,
        name: String,
        available: Boolean,
        invalid: Boolean = false,
    ) {
        if (invalid) {
            player.displayNameStatus = 1
            player.ifSetText(
                "component.tutorial_displayname:status",
                "Sorry, the display name <col=ffffff>$name</col> is <col=ff0000>not valid</col>.",
            )
            return
        }
        if (available) {
            player.displayNameStatus = 4
            player.ifSetText(
                "component.tutorial_displayname:status",
                "Great! The display name <col=ffffff>$name</col> is <col=00ff00>available</col>!" +
                    "<br>You may set this name now, or enter another to look up.",
            )
            player.ifSetHide("component.tutorial_displayname:suggestions", true)
            return
        }
        player.displayNameStatus = 1
        player.ifSetText(
            "component.tutorial_displayname:status",
            "Sorry, the display name <col=ffffff>$name</col> is <col=ff0000>not available</col>." +
                "<br>Try clicking one of our suggestions, instead:",
        )
        player.ifSetHide("component.tutorial_displayname:suggestions", false)
        val suggestions = suggestions(name)
        player.attr[SUGGESTIONS] = suggestions
        player.ifSetText("component.tutorial_displayname:suggestion1", suggestions[0])
        player.ifSetText("component.tutorial_displayname:suggestion2", suggestions[1])
        player.ifSetText("component.tutorial_displayname:suggestion3", suggestions[2])
    }

    private suspend fun ProtectedAccess.selectSuggestion(index: Int) {
        val suggestions = player.attr[SUGGESTIONS] ?: return
        player.attr[CANDIDATE_NAME] = suggestions.getOrNull(index) ?: return
        player.displayNameStatus = 4
        setName()
    }

    private suspend fun ProtectedAccess.setName() {
        if (player.tutorialStage != TutorialStage.START) {
            return
        }
        val name = player.attr[CANDIDATE_NAME] ?: player.displayName.ifBlank { player.username }
        if (!validName(name)) {
            player.ifSetText("component.tutorial_displayname:status", "Please look up a name first.")
            return
        }
        if (playerList.any { it !== player && it.displayName.equals(name, ignoreCase = true) }) {
            player.ifSetText("component.tutorial_displayname:status", "That name is not available.")
            return
        }
        player.displayName = name
        player.displayNameStatus = 5
        player.hasDisplayNameTransmitter = true
        player.ifSetText("component.tutorial_displayname:status", "Requesting...")
        player.runClientScript(NAME_SET_COMPLETE)
        ifCloseSub("interface.tutorial_displayname")
        player.attr.remove(CANDIDATE_NAME)
        player.attr.remove(SUGGESTIONS)
        openDesign()
    }

    private fun closeName(player: Player) {
        if (player.tutorialStage != TutorialStage.START || player.hasDisplayNameTransmitter) {
            return
        }
        player.hasDisplayNameTransmitter = true
        player.displayNameStatus = 5
        protectedAccess.launch(player) {
            if (player.ui.getModalOrNull("interface.tutorial_displayname") != null) {
                ifCloseSub("interface.tutorial_displayname")
            }
            openDesign()
        }
    }

    private fun ProtectedAccess.openDesign() {
        player.playerDesignBodyType = player.appearance.bodyType
        player.transmitPronouns = player.appearance.pronoun
        ifOpenMainModal("interface.player_design")
    }

    private fun ProtectedAccess.setBodyType(bodyType: Int, pronoun: Int) {
        player.playerDesignBodyType = bodyType
        player.transmitPronouns = pronoun
        player.appearance.bodyType = bodyType
        player.appearance.pronoun = pronoun
    }

    private suspend fun ProtectedAccess.confirmDesign(closed: Boolean) {
        if (player.tutorialStage != TutorialStage.START || player.attr[DESIGN_DONE] == true) {
            return
        }
        player.attr[DESIGN_DONE] = true
        player.appearance.bodyType = player.playerDesignBodyType
        player.appearance.pronoun = player.transmitPronouns
        if (!closed && player.ui.getModalOrNull("interface.player_design") != null) {
            ifCloseSub("interface.player_design")
        }
        ifOpenMainModal("interface.tutorial_player_experience")
        val input = coroutine.pause(ResumePauseButtonInput::class)
        if (input.isComponentType("component.tutorial_player_experience:content")) {
            player.adventurePaths =
                if (input.subcomponent == ADVENTURE_PATHS_EXPERIENCED) {
                    ADVENTURE_PATHS_OPTED_OUT
                } else {
                    ADVENTURE_PATHS_PARTICIPATING
                }
        } else {
            player.adventurePaths = ADVENTURE_PATHS_PARTICIPATING
        }
        guide.advance(player, TutorialStage.EXPERIENCE_SELECTED)
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun closeExperience(player: Player) {
        if (player.tutorialStage != TutorialStage.START) {
            return
        }
        player.adventurePaths = ADVENTURE_PATHS_PARTICIPATING
        guide.advance(player, TutorialStage.EXPERIENCE_SELECTED)
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun PlayerTimerEvent.Soft.tick() {
        if (!player.inTutorial) {
            return
        }
        rules.nextStage(player)?.let { guide.advance(player, it) }
        val ticks = player.attr.getOrPut(HINT_TICKS) { 0 } + 1
        if (ticks >= HINT_REPEAT_TICKS) {
            guide.show(player, player.tutorialStage)
            player.attr[HINT_TICKS] = 0
        } else {
            player.attr[HINT_TICKS] = ticks
        }
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun validName(name: String): Boolean {
        if (name.length !in 1..12) {
            return false
        }
        if (name != name.trim()) {
            return false
        }
        return name.all { it.isLetterOrDigit() || it == ' ' } && !name.contains("  ")
    }

    private fun suggestions(name: String): List<String> {
        val base = name.filter { it.isLetterOrDigit() }.take(8).ifBlank { "Adventurer" }
        return listOf(
            (base + "1").take(12),
            (base + "27").take(12),
            (base + "x").take(12),
        )
    }

    private companion object {
        private const val TUTORIAL_TIMER = "timer.tutorial_island"
        private const val HINT_REPEAT_TICKS = 50
        private const val ADVENTURE_PATHS_PARTICIPATING = 1
        private const val ADVENTURE_PATHS_OPTED_OUT = 2
        private const val ADVENTURE_PATHS_EXPERIENCED = 2
        private val HINT_TICKS = AttributeKey<Int>(temp = true)
        private val CANDIDATE_NAME = AttributeKey<String>(temp = true)
        private val SUGGESTIONS = AttributeKey<List<String>>(temp = true)
        private val DESIGN_DONE = AttributeKey<Boolean>(temp = true)
        private val NAME_ALLOW_INPUT =
            "clientscript.[clientscript,tutorial_displayname_allow_input]".asRSCM(RSCMType.CLIENTSCRIPT)
        private val NAME_SET_COMPLETE =
            "clientscript.[clientscript,script4145]".asRSCM(RSCMType.CLIENTSCRIPT)
        private const val NAME_LOOK_UP = "component.tutorial_displayname:look_up_name"
        private const val NAME_SET = "component.tutorial_displayname:set_name"
        private const val NAME_TEXT = "component.tutorial_displayname:name_text"
        private const val NAME_HIGHLIGHT = "component.tutorial_displayname:name_highlight"
        private const val NAME_INPUT = "component.tutorial_displayname:name"
    }
}
