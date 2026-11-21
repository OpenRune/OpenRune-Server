package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.events.PlayerTimerEvent
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.input.StringDialogInput
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenFullOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfToplevelButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.quest.manager.Quest
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
    private val exit: TutorialExit,
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
        for (part in DESIGN_PARTS) {
            onIfModalButton("component.player_design:${part}_left") {
                TutorialDesign.cycle(player, part, -1)
            }
            onIfModalButton("component.player_design:${part}_right") {
                TutorialDesign.cycle(player, part, 1)
            }
        }
        onIfModalButton("component.player_design:confirm") { confirmDesign(closed = false) }
        onIfClose("interface.player_design") {
            protectedAccess.launch(player) { confirmDesign(closed = true) }
        }
        onIfClose("interface.tutorial_player_experience") { closeExperience(player) }
        onTabOpened("stone11", TutorialStage.SETTINGS_REQUESTED, TutorialStage.SETTINGS_OPENED)
        onTabOpened("stone1", TutorialStage.SHRIMP_CAUGHT, TutorialStage.SKILLS_VIEWED)
        onIfToplevelButton("component.toplevel_osrs_stretch:stone2") {
            when {
                player.tutorialStage == TutorialStage.QUEST_GUIDE_TALKED ->
                    guide.advance(player, TutorialStage.QUEST_LIST_OPENED)
                player.tutorialStage == TutorialStage.LEFT_ISLAND &&
                    player.adventurePaths == 1 ->
                    protectedAccess.launch(player) { with(exit) { calloutJon() } }
            }
        }
        onTabOpened("stone4", TutorialStage.DAGGER_EQUIP_TASK, TutorialStage.WORN_OPENED)
        onTabOpened("stone8", TutorialStage.ACCOUNT_GUIDE_TALKED, TutorialStage.ACCOUNT_MENU_OPENED)
        onTabOpened("stone5", TutorialStage.PRAYER_GUIDE_TALKED, TutorialStage.PRAYER_BOOK_OPENED)
        onTabOpened("stone6", TutorialStage.MAGIC_GUIDE_TALKED, TutorialStage.MAGIC_BOOK_OPENED)
        onIfToplevelButton("component.toplevel_osrs_stretch:stone3") {
            when (player.tutorialStage) {
                TutorialStage.NET_GIVEN -> guide.advance(player, TutorialStage.INVENTORY_VIEWED)
                TutorialStage.COMBAT_TAB_OPENED ->
                    guide.advance(player, TutorialStage.COMBAT_TAB_CLOSED)
            }
        }
        onIfToplevelButton("component.toplevel_osrs_stretch:stone0") {
            if (player.tutorialStage == TutorialStage.SWORD_EQUIPPED) {
                openCombatTab(player)
                guide.advance(player, TutorialStage.COMBAT_TAB_OPENED)
            }
        }
        onPlayerSoftTimer(TUTORIAL_TIMER) { tick() }
    }

    private fun ScriptContext.onTabOpened(
        stone: String,
        stage: Int,
        next: Int,
    ) {
        onIfToplevelButton("component.toplevel_osrs_stretch:$stone") {
            if (player.tutorialStage == stage) {
                guide.advance(player, next)
            }
        }
    }

    private fun reconcileInterfaces(player: Player) {
        val stage = player.tutorialStage
        val open =
            when (stage) {
                TutorialStage.WORN_OPENED ->
                    player.ui.containsModal("interface.equipment") ||
                        player.ui.containsModal("interface.equipment_side")
                TutorialStage.MINING_GUIDE_BAR -> player.ui.containsModal("interface.smithing")
                TutorialStage.LADDER_UP ->
                    player.ui.containsOverlay("interface.bankside") ||
                        player.ui.containsModal("interface.bankmain")
                else -> return
            }
        if (player.attr[INTERFACE_STAGE] != stage) {
            player.attr[INTERFACE_STAGE] = stage
            player.attr[INTERFACE_WAS_OPEN] = open
            return
        }
        if (player.attr[INTERFACE_WAS_OPEN] == true) {
            if (!open) {
                player.attr[INTERFACE_WAS_OPEN] = false
            }
            return
        }
        if (!open) {
            return
        }
        when (stage) {
            TutorialStage.WORN_OPENED -> {
                player.tutorialCombatLogin = 1
                guide.advance(player, TutorialStage.EQUIPMENT_STATS_OPENED)
            }
            TutorialStage.MINING_GUIDE_BAR -> {
                guide.advance(player, TutorialStage.ANVIL_OPENED)
                guide.showSmithingMenuOpened(player)
                TutorialHighlights.smithingDagger(player)
            }
            TutorialStage.LADDER_UP -> guide.advance(player, TutorialStage.BANK_OPENED)
        }
    }

    private fun openCombatTab(player: Player) {
        VarPlayerIntMapSetter.set(player, "varp.com_mode", 1)
        player.comStance = 2
    }

    private fun SessionStateEvent.PrepareLogin.prepareLogin() {
        if (!player.newAccount) {
            return
        }
        if (player.tutorialStage == 0) {
            player.coords = TutorialCoords.START
            player.tutorialStage = TutorialStage.START
            player.displayNameStatus = 1
            player.comStance = 0
        }
    }

    private fun SessionStateEvent.Login.login() {
        if (player.tutorialStage >= TutorialStage.COMPLETE && player.vars["varp.qp"] == 0) {
            player.repairQuestCompletion()
        }
        TutorialTabs.sync(player)
        if (!player.inTutorial) {
            return
        }
        seedTutorialBank(player)
        if (player.tutorialStage <= TutorialStage.START) {
            player.hasDisplayNameTransmitter = false
            if (player.displayNameStatus < 5) {
                player.beginName()
            } else {
                player.beginDesign()
            }
        } else {
            player.hasDisplayNameTransmitter = true
            player.resumeTutorial()
        }
    }

    private fun Player.repairQuestCompletion() {
        val stage = tutorialStage
        protectedAccess.launch(this) {
            val quest = Quest.get("quest_learningtheropes") ?: return@launch
            player.tutorialStage = TutorialStage.COMPLETE - 1
            quest.completeQuest(this)
            player.tutorialStage = stage
            if (player.ui.containsModal("interface.questscroll")) {
                ifCloseSub("interface.questscroll")
            }
        }
    }

    private fun Player.resumeTutorial() {
        ifOpenFullOverlay("interface.tutorial_overlay", eventBus)
        ifSetHide("component.tutorial_overlay:noclick", true)
        guide.apply(this, tutorialStage)
        attr[LAST_RUN_STATE] = vars["varp.option_run"]
        softTimer(TUTORIAL_TIMER, 1)
    }

    private fun Player.beginDesign() {
        protectedAccess.launch(this) {
            if (player.tutorialStage > TutorialStage.START) {
                return@launch
            }
            player.ifOpenFullOverlay("interface.tutorial_overlay", eventBus)
            player.ifSetHide("component.tutorial_overlay:noclick", false)
            player.ifSetHide("component.tutorial_overlay:hint_layer", true)
            openDesign()
        }
    }

    private fun seedTutorialBank(player: Player) {
        if (player.tutorialStage >= TutorialStage.BANK_OPENED) {
            return
        }
        val bank = player.invMap.getOrPut("inv.bank")
        if (bank.count(TutorialObj.COINS) == 0) {
            player.invAdd(bank, TutorialObj.COINS, 25)
        }
    }

    private fun Player.beginName() {
        protectedAccess.launch(this) {
            if (player.tutorialStage > TutorialStage.START) {
                return@launch
            }
            player.ifOpenFullOverlay("interface.tutorial_overlay", eventBus)
            player.ifSetHide("component.tutorial_overlay:noclick", false)
            player.ifSetHide("component.tutorial_overlay:hint_layer", true)
            mesOverlay(
                "<col=0000ff>Setting your name</col><br>Before you get started, you'll need to " +
                    "set a display name. Please use the open interface to set one.",
            )
            ifOpenMainModal("interface.tutorial_displayname")
            player.ifSetText(
                "component.tutorial_displayname:status",
                "Please look up a name to see whether it is available.",
            )
            player.configureNameInput()
            player.softTimer(TUTORIAL_TIMER, 1)
        }
    }

    private fun Player.configureNameInput() {
        runClientScript(
            NAME_ALLOW_INPUT,
            NAME_LOOK_UP.asRSCM(RSCMType.COMPONENT),
            NAME_SET.asRSCM(RSCMType.COMPONENT),
            NAME_TEXT.asRSCM(RSCMType.COMPONENT),
            NAME_HIGHLIGHT.asRSCM(RSCMType.COMPONENT),
            NAME_INPUT.asRSCM(RSCMType.COMPONENT),
            1,
        )
    }

    private fun Player.retryNameInput() {
        if (tutorialStage != TutorialStage.START) {
            return
        }
        if (!ui.containsModal("interface.tutorial_displayname")) {
            return
        }
        val attempts = attr.getOrPut(NAME_INPUT_ATTEMPTS) { 0 }
        if (attempts >= NAME_INPUT_ATTEMPTS_MAX) {
            return
        }
        attr[NAME_INPUT_ATTEMPTS] = attempts + 1
        configureNameInput()
    }

    private fun StringDialogInput.lookupName() {
        if (player.tutorialStage != TutorialStage.START) {
            return
        }
        val name = text.trim()
        player.attr[CANDIDATE_NAME] = name
        player.displayNameStatus = 2
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
        val name = player.attr[CANDIDATE_NAME]
        if (name == null || player.displayNameStatus != 4) {
            player.ifSetText("component.tutorial_displayname:status", "Please look up a name first.")
            return
        }
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
        player.ifSetText("component.tutorial_displayname:status", "Requesting...")
        player.runClientScript(NAME_SET_COMPLETE)
        ifCloseSub("interface.tutorial_displayname")
        player.hasDisplayNameTransmitter = true
        player.ifSetHide("component.tutorial_overlay:noclick", true)
        player.attr.remove(CANDIDATE_NAME)
        player.attr.remove(SUGGESTIONS)
        openDesign()
    }

    private fun closeName(player: Player) {
        if (player.tutorialStage != TutorialStage.START || player.hasDisplayNameTransmitter) {
            return
        }
        protectedAccess.launch(player) {
            if (player.tutorialStage != TutorialStage.START ||
                player.hasDisplayNameTransmitter
            ) {
                return@launch
            }
            if (!player.ui.containsModal("interface.tutorial_displayname")) {
                ifOpenMainModal("interface.tutorial_displayname")
                player.configureNameInput()
                player.ifSetText(
                    "component.tutorial_displayname:status",
                    "Please look up a name to see whether it is available.",
                )
            }
        }
    }

    private suspend fun ProtectedAccess.openDesign() {
        player.playerDesignBodyType = player.appearance.bodyType
        player.transmitPronouns = player.appearance.pronoun
        player.ifSetHide("component.tutorial_overlay:noclick", false)
        mesOverlay(
            "<col=0000ff>Setting your appearance</col><br>Before you get started, you'll need to " +
                "set the appearance of your character. Please use the open interface to set " +
                "your appearance.",
        )
        ifOpenMainModal("interface.player_design")
    }

    private fun ProtectedAccess.setBodyType(bodyType: Int, pronoun: Int) {
        TutorialDesign.setGender(player, bodyType, pronoun)
    }

    private suspend fun ProtectedAccess.confirmDesign(closed: Boolean) {
        if (player.tutorialStage != TutorialStage.START || player.attr[DESIGN_DONE] == true) {
            return
        }
        player.attr[DESIGN_DONE] = true
        player.appearance.bodyType = player.playerDesignBodyType
        player.appearance.pronoun = player.transmitPronouns
        if (!closed && player.ui.containsModal("interface.player_design")) {
            ifCloseSub("interface.player_design")
        }
        player.ifSetHide("component.tutorial_overlay:noclick", true)
        mesOverlay(
            "<col=0000ff>Past Experience</col><br>Before you get started, please use the open " +
                "interface to select your experience with Old School RuneScape.",
        )
        player.attr[EXPERIENCE_CHOSEN] = false
        ifOpenMainModal("interface.tutorial_player_experience")
        player.ifSetEvents(
            "component.tutorial_player_experience:content",
            0..3,
            IfEvent.PauseButton,
        )
        player.ifSetHide("component.tutorial_overlay:noclick", false)
        val input = coroutine.pause(ResumePauseButtonInput::class)
        if (player.tutorialStage != TutorialStage.START) {
            return
        }
        if (!input.isComponentType("component.tutorial_player_experience:content")) {
            return
        }
        player.attr[EXPERIENCE_CHOSEN] = true
        if (player.ui.containsModal("interface.tutorial_player_experience")) {
            ifCloseSub("interface.tutorial_player_experience")
        }
        player.ifSetHide("component.tutorial_overlay:noclick", true)
        guide.advance(player, TutorialStage.EXPERIENCE_SELECTED)
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun closeExperience(player: Player) {
        if (player.tutorialStage != TutorialStage.START || player.attr[EXPERIENCE_CHOSEN] == true) {
            return
        }
        player.ifSetHide("component.tutorial_overlay:noclick", true)
        guide.advance(player, TutorialStage.EXPERIENCE_SELECTED)
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun PlayerTimerEvent.Soft.tick() {
        if (!player.inTutorial) {
            return
        }
        player.retryNameInput()
        checkRunToggle(player)
        reconcileInterfaces(player)
        rules.nextStage(player)?.let { next ->
            guide.advance(player, next)
            when (next) {
                TutorialStage.DAGGER_SMITHED -> TutorialHighlights.clearSmithingDagger(player)
                TutorialStage.DAGGER_EQUIPPED -> TutorialHighlights.clearItems(player)
                TutorialStage.SWORD_EQUIPPED -> {
                    player.comStance = 1
                    TutorialHighlights.clearItems(player)
                }
            }
        }
        if (player.tutorialStage == TutorialStage.WIND_STRIKE_RESOLVED) {
            protectedAccess.launch(player) {
                val quest = Quest.get("quest_learningtheropes")
                if (quest != null && !quest.isQuestCompleted(player)) {
                    quest.completeQuest(this)
                }
                TutorialHighlights.clearWindStrike(player)
                guide.advance(player, TutorialStage.COMPLETE)
            }
        }
        player.softTimer(TUTORIAL_TIMER, 1)
    }

    private fun checkRunToggle(player: Player) {
        val run = player.vars["varp.option_run"]
        val last = player.attr[LAST_RUN_STATE]
        if (last == null) {
            player.attr[LAST_RUN_STATE] = run
            return
        }
        if (run == last) {
            return
        }
        player.attr[LAST_RUN_STATE] = run
        if (player.tutorialStage == TutorialStage.QUEST_HOUSE_ENTERED) {
            player.attr[RUN_TOGGLED] = true
            guide.show(player, player.tutorialStage)
        }
    }

    private fun ProtectedAccess.mesOverlay(text: String) {
        player.openMesOverlay()
        player.runClientScript(MESOVERLAY, text)
    }

    private fun Player.openMesOverlay() {
        ifOpenOverlay("interface.mesoverlay", "component.chatbox:chatoverlay", eventBus)
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
        private const val NAME_INPUT_ATTEMPTS_MAX = 5
        private val MESOVERLAY =
            "clientscript.[clientscript,mesoverlay]".asRSCM(RSCMType.CLIENTSCRIPT)
        private val DESIGN_PARTS =
            listOf(
                "head",
                "jaw",
                "torso",
                "arms",
                "hands",
                "legs",
                "feet",
                "hair",
                "torso_col",
                "legs_col",
                "feet_col",
                "skin",
            )
        private val INTERFACE_STAGE = AttributeKey<Int>(temp = true)
        private val INTERFACE_WAS_OPEN = AttributeKey<Boolean>(temp = true)
        private val NAME_INPUT_ATTEMPTS = AttributeKey<Int>(temp = true)
        private val CANDIDATE_NAME = AttributeKey<String>(temp = true)
        private val SUGGESTIONS = AttributeKey<List<String>>(temp = true)
        private val DESIGN_DONE = AttributeKey<Boolean>(temp = true)
        private val EXPERIENCE_CHOSEN = AttributeKey<Boolean>(temp = true)
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
