package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.types.MesAnimType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onDropTrigger
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.

/**
 * Sleeping Giants: Kovac shows the player the giants' foundry, they repair its tools in a private
 * copy of it, then make a first sword in the real foundry.
 * https://oldschool.runescape.wiki/w/Sleeping_Giants
 * https://oldschool.runescape.wiki/w/Transcript:Sleeping_Giants
 */
class SleepingGiantsQuest @Inject constructor(private val room: SleepingGiantsRoom) :
    QuestScript(
        SleepingGiants.QUEST,
        "varp.giants_foundry_main",
        // The quest dbrow's `stat_xp_awarded` is 60000 (tenths); the wiki gives 6,000.
        rewards { xp("stat.smithing", 6000.0) },
        // The reward scroll shows the preform's model (wiki File:Sleeping Giants reward scroll.png).
        ItemRewardDisplay(PREFORM, zoom = 400),
        questVarbit = "varbit.sleeping_giants",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == SleepingGiants.COMPLETE) {
            "Sleeping Giants end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at ${SleepingGiants.COMPLETE}."
        }

        onOpLoc1(ENTRANCE) { enterCave() }
        onOpNpc1(KOVAC_STRIKE) { strike(it.npc) }
        onOpNpc1(KOVAC_QUEST) {
            if (room.isInside(player)) talkInside(it.npc) else talkOutside(it.npc)
        }
        onOpNpc1(KOVAC_FOUNDRY) { talkInside(it.npc) }

        onOpLoc1(HAMMER_BROKEN) { repair(RepairTool.Hammer) }
        onOpLoc1(GRINDSTONE_BROKEN) { repair(RepairTool.Grindstone) }
        onOpLoc1(POLISHING_WHEEL_BROKEN) { repair(RepairTool.PolishingWheel) }
        val brokenTools =
            listOf(
                HAMMER_BROKEN to RepairTool.Hammer,
                GRINDSTONE_BROKEN to RepairTool.Grindstone,
                POLISHING_WHEEL_BROKEN to RepairTool.PolishingWheel,
            )
        for ((loc, tool) in brokenTools) {
            for (obj in REPAIR_MATERIALS) {
                onOpLocU(loc, obj) { repair(tool) }
            }
        }
        onOpLoc1(HAMMER_REPAIRED) { inspectRepaired(RepairTool.Hammer) }
        onOpLoc1(GRINDSTONE_REPAIRED) { inspectRepaired(RepairTool.Grindstone) }
        onOpLoc1(POLISHING_WHEEL_REPAIRED) { inspectRepaired(RepairTool.PolishingWheel) }

        onOpLoc1(CHEST_QUEST) { tryOpenBank() }
        onOpLoc2(CHEST_QUEST) { inspectChest() }
        onOpLoc1(CRATE_QUEST) { inspectCrate() }
        onOpLoc1(CRUCIBLE_QUEST) { inspectCrucible() }
        onOpLoc1(LAVA_POOL_QUEST) { inspectLavaPool() }
        onOpLoc1(JIG_QUEST) { inspectJig() }
        onOpLoc1(STORAGE_QUEST) { inspectStorage() }
        onOpLoc1(WATERFALL_QUEST) { inspectWaterfall() }

        onOpLoc1(CRATE_FULL) { searchCrate() }
        // sigmazx's Sleeping Giants video (youtube.com/watch?v=WnGuNtTJW0M, about 8:44).
        // UNVERIFIED: that the message comes from searching the crate; the click isn't shown.
        onOpLoc1(CRATE_EMPTY) { mes("There is nothing in the storage area to take.") }

        onPlayerQueueWithArgs<QuestPrompt>(QUEST_PROMPT_QUEUE) {
            when (val prompt = it.args) {
                is QuestPrompt.KovacStop -> kovacStop(prompt)
                QuestPrompt.SwordFinished -> questSwordFinished()
            }
        }
        onPlayerLogin { syncCompleted(player) }
        onPlayerLogout { room.logout(player) }
        onDropTrigger(DROP_TRIGGER) {
            if (room.onDropAttempt(player)) {
                // UNVERIFIED: the update gives no wording for the warning.
                player.mes(
                    "<col=ef1020>Anything you drop here will be lost when you leave. Drop it " +
                        "again if you're sure.</col>"
                )
                player.dropTrigger(DROP_TRIGGER)
            }
        }
        onEvent<HeldDropEvents.Drop> { room.onDropped(player) }
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Hill Giant</col> by the <col=800000>giant anvil</col> on " +
            "the <col=800000>Giants' Plateau</col>, east of <col=800000>Al Kharid</col>."

    // UNVERIFIED: no source transcribes the quest journal; written from the walkthrough.
    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "A <red>Hill Giant</red> called <red>Kovac</red> asked me for help. He showed me " +
                    "an ancient foundry his ancestors built beneath the <red>Giants' Plateau</red>."
            )
            objective(
                "Kovac wants the foundry's tools fixed: the <red>trip hammer</red> (an oak log, " +
                    "5 nails and a hammer), the <red>grindstone</red> (a chisel) and the " +
                    "<red>polishing wheel</red> (wool, 2 oak logs, 5 nails and a hammer)."
            ) {
                visibleWhen { stage(access.player) >= SleepingGiants.ENTERED }
                stageAtLeast(SleepingGiants.REPAIRED, "I repaired the foundry's tools.").strike()
            }
            objective(
                "Kovac wants to make a sword for an old friend. I should talk to him about how " +
                    "to make it."
            ) {
                visibleWhen { stage(access.player) >= SleepingGiants.REPAIRED }
                stageAtLeast(SleepingGiants.COMPLETE, "I made the sword.").strike()
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I helped Kovac, a giant who can talk, repair the tools of an ancient foundry " +
                    "his ancestors built beneath the Giants' Plateau."
            )
            line(
                "Together we made a sword for his old friend Obor. Kovac lets me use the foundry " +
                    "whenever I want."
            )
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    /** Players who have the quest completed (or treated as completed) see the empty crate. */
    private fun syncCompleted(player: Player) {
        if (
            QuestRequirements.hasCompleted(player, SleepingGiants.QUEST) &&
                player.sleepingGiantsTutorial < SleepingGiants.TUTORIAL_HANDED_IN
        ) {
            player.sleepingGiantsTutorial = SleepingGiants.TUTORIAL_HANDED_IN
        }
    }

    /* Starting out */

    private suspend fun ProtectedAccess.strike(npc: Npc) {
        startDialogue(npc) {
            lineByLine("Wait...", "Do not attack!") { chatNpc(neutral, it) }
            chatPlayer(chatshock, "What the... You can talk?!")
            lineByLine("Do...", "do not...", "flig... fight.") { chatNpc(neutral, it) }
            chatPlayer(chatshock, "You don't want to fight me?")
            lineByLine("Want...", "no, need...", "your help.") { chatNpc(neutral, it) }
            chatPlayer(quiz, "I must be dreaming. Giants can't talk!")
            chatNpc(neutral, "Please...")
            if (access.statBase(SMITHING) < REQUIRED_SMITHING) {
                // UNVERIFIED: the transcript has no branch for missing the Smithing requirement.
                mesbox("You need a Smithing level of $REQUIRED_SMITHING to start this quest.")
                return@startDialogue
            }
            if (!choice2("Yes.", true, "No.", false, title = "Start the Sleeping Giants quest?")) {
                chatPlayer(angry, "No way! I'm getting out of here. There's no chance I'd trust a giant!")
                chatNpc(neutral, "Please...")
                chatNpc(neutral, "help.")
                return@startDialogue
            }
            quest.setQuestStage(access, SleepingGiants.STARTED)
            chatPlayer(
                quiz,
                "Okay giant, I'll help you out, but first you need to tell me your name... if " +
                    "you even have one.",
            )
            chatNpcSpecific(KOVAC_NAME, KOVAC_OUTSIDE, neutral, "Kovac.")
            chatPlayer(
                quiz,
                "Kovac... alright then. My name is ${player.displayName}. So, Kovac, what do " +
                    "you need help with?",
            )
            followIn()
        }
        if (stage(player) == SleepingGiants.STARTED) {
            enterRoom(intro = true)
        }
    }

    // TODO(content): [verify:in_game] what Kovac says when spoken to after the player agreed
    // to help but before following him in; this replays his request to follow.
    private suspend fun ProtectedAccess.talkOutside(npc: Npc) {
        if (stage(player) != SleepingGiants.STARTED) {
            return
        }
        startDialogue(npc) { followIn() }
        enterRoom(intro = true)
    }

    private suspend fun Dialogue.followIn() {
        lineByLine("Please...", "follow in.") { chatKovacOutside(neutral, it) }
        chatPlayer(worried, "Hang on... is this a trick? Are you going to cook and eat me?!")
        chatKovacOutside(neutral, "No...<br> ")
        chatNpcSpecificNp(KOVAC_NAME, KOVAC_STRIKE, neutral, "No...<br>follow.")
    }

    /** The outside Kovac's name only changes once the quest stage has reached the client. */
    private suspend fun Dialogue.chatKovacOutside(mesanim: MesAnimType, text: String) =
        chatNpcSpecific(KOVAC_NAME, KOVAC_STRIKE, mesanim, text)

    /**
     * The cave: before the quest the player won't go in; until the tools are repaired it leads
     * to the quest's copy of the foundry, then to the real one.
     */
    private suspend fun ProtectedAccess.enterCave() {
        val stage = stage(player)
        when {
            QuestRequirements.hasCompleted(player, SleepingGiants.QUEST) ||
                stage >= SleepingGiants.REPAIRED -> enterFoundry()
            stage >= SleepingGiants.STARTED -> enterRoom(intro = stage < SleepingGiants.REPAIRING)
            else ->
                startDialogue {
                    chatPlayer(worried, "I don't think I want to disturb a cave of giants right now...")
                }
        }
    }

    /** Kovac's "follow" and the cave both lead into the quest's copy of the foundry. */
    private suspend fun ProtectedAccess.enterRoom(intro: Boolean) {
        val entered =
            room.enter(this, intro) {
                if (stage(player) < SleepingGiants.ENTERED) {
                    quest.setQuestStage(this, SleepingGiants.ENTERED)
                }
            }
        if (!entered) {
            mes("The foundry is too busy right now. Try again in a moment.")
            return
        }
        if (!intro) {
            return
        }
        val kovac = room.kovacOf(player) ?: return
        var ended = false
        try {
            startDialogue(kovac) { insideIntro(cutscene = true) }
            room.endIntro(this)
            ended = true
        } finally {
            if (!ended) {
                room.endIntroNow(this)
            }
        }
    }

    /* What is this? A workshop for giants? */

    private suspend fun ProtectedAccess.talkInside(npc: Npc) {
        startDialogue(npc) {
            when (val stage = stage(player)) {
                SleepingGiants.ENTERED -> insideIntro(cutscene = false)
                SleepingGiants.REPAIRING ->
                    if (RepairTool.entries.all { it.isRepaired(player) }) toolsRepaired()
                    else whatsLeft()
                SleepingGiants.REPAIRED,
                SleepingGiants.MAKING_SWORD ->
                    if (room.isInside(player)) toolsRepaired() else makeSword(npc)
                else -> {}
            }
        }
        if (stage(player) == SleepingGiants.MAKING_SWORD && room.isInside(player)) {
            room.moveToFoundry(this)
        }
    }

    private suspend fun Dialogue.insideIntro(cutscene: Boolean) {
        chatPlayer(chatshock, "Woah...<br>What is this place?")
        if (cutscene) {
            room.introWalkBack(access)
        }
        chatPlayer(quiz, "Did you build this?")
        lineByLine("No...", "Kovac...", "accep... ancestor build.") { chatNpc(neutral, it) }
        chatPlayer(chatshock, "Giants built this place?")
        lineByLine("Yes...", "long ago.") { chatNpc(neutral, it) }
        chatPlayer(quiz, "How? Giants don't build, do they? I thought they were brutes...")
        lineByLine("Before...", "giants differing.") { chatNpc(neutral, it) }
        lineByLine("Now...", "not.") { chatNpc(sad, it) }
        chatPlayer(confused, "Huh? I don't understand what you mean.")
        chatNpc(neutral, "Look.")
        room.kovacLooksAtTools(player)
        mesbox("Kovac looks out to the tools around you.")
        chatNpc(neutral, "Help fix.")
        chatPlayer(quiz, "You want me to help you by fixing those tools?")
        chatNpc(neutral, "Yes.")
        chatPlayer(quiz, "Okay, I'll help, but first you need to tell me what this place was used for?")
        lineByLine("Make...", "wheelpo... weapons.") { chatNpc(neutral, it) }
        chatPlayer(chatshock, "Giants made weapons here?")
        chatPlayer(
            quiz,
            "That's incredible! But why do you want to make weapons? Most giants I've come " +
                "across already have weapons?",
        )
        lineByLine("Make...", "giants...", "remember.") { chatNpc(neutral, it) }
        chatPlayer(quiz, "Remember what?")
        lineByLine("Giants...", "not just...", "killers.") { chatNpc(neutral, it) }
        lineByLine("Kovac want...", "help giants remember.") { chatNpc(neutral, it) }
        chatPlayer(
            neutral,
            "I see. To be honest, if I hadn't seen this place myself, I wouldn't have believed you.",
        )
        chatPlayer(neutral, "Alright, Kovac, I'll help you out. What do we need to do first?")
        quest.setQuestStage(access, SleepingGiants.REPAIRING)
        chatNpc(neutral, "We fix tools...")
    }

    // TODO(content): [verify:in_game] whether Kovac names only the tools still broken; the
    // transcript lists all three.
    private suspend fun Dialogue.whatsLeft() {
        chatPlayer(quiz, "What's left to fix, Kovac?")
        chatNpc(neutral, "Hmm...")
        for (tool in RepairTool.entries.filterNot { it.isRepaired(player) }) {
            chatNpc(neutral, "${tool.title}...")
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.toolsRepaired() {
        chatPlayer(neutral, "That's all the tools repaired. Well... as best as I can do.")
        lineByLine("Yes...", "Kovac begin to understand more now.") { chatNpc(neutral, it) }
        chatPlayer(neutral, "I've noticed your speech has been improving over time as well.")
        lineByLine("Kovac know what say...", "but sometime hard say proppel... properly.") {
            chatNpc(neutral, it)
        }
        chatPlayer(neutral, "That's alright, we'll figure it out together.")
        quest.setQuestStage(access, SleepingGiants.REPAIRED)
        chatNpc(neutral, "Kovac agree. Ready to make sword?")
        chatPlayer(happy, "Yes! I'm itching to get started.")
        lineByLine("Kovac get few things ready first...", "then we can start.") {
            chatNpc(neutral, it)
        }
        quest.setQuestStage(access, SleepingGiants.MAKING_SWORD)
    }

    /* Repairs */

    private enum class RepairTool(
        val title: String,
        val varbit: String,
        val oakLogs: Int,
        val nails: Int,
        val wool: Boolean,
        val hammer: Boolean,
        val chisel: Boolean,
    ) {
        Hammer("Trip hammer", "varbit.sleeping_giants_repair_hammer", 1, 5, false, true, false),
        Grindstone("Grindstone", "varbit.sleeping_giants_repair_grind", 0, 0, false, false, true),
        PolishingWheel(
            "Polishing wheel",
            "varbit.sleeping_giants_repair_polish",
            2,
            5,
            true,
            true,
            false,
        );

        fun state(player: Player): Int = player.foundryVar(varbit)

        fun isRepaired(player: Player): Boolean = state(player) == SleepingGiants.REPAIR_DONE
    }

    private suspend fun ProtectedAccess.repair(tool: RepairTool) {
        startDialogue {
            // TODO(content): [verify:in_game] whether the opening talk replays on every click;
            // here it plays until the first answer to the repair prompt.
            if (tool.state(player) == SleepingGiants.REPAIR_BROKEN) {
                repairIntro(tool)
            }
            val title =
                when (tool) {
                    RepairTool.Hammer -> "Repair the trip hammer?"
                    RepairTool.Grindstone -> "Repair the grindstone?"
                    RepairTool.PolishingWheel -> "Repair the polishing wheel?"
                }
            if (!choice2("Yes.", true, "No.", false, title = title)) {
                return@startDialogue
            }
            if (!access.hasMaterials(tool)) {
                when (tool) {
                    RepairTool.Hammer ->
                        doubleobjbox(
                            OAK_LOGS,
                            NAILS.first(),
                            "You will need an oak log, 5 nails and a hammer to repair the trip hammer.",
                        )
                    RepairTool.Grindstone ->
                        // UNVERIFIED: the missing-chisel message (guess)
                        objbox(CHISEL, "You will need a chisel to repair the grindstone.")
                    RepairTool.PolishingWheel ->
                        doubleobjbox(
                            WOOL,
                            OAK_LOGS,
                            "You will need some wool, 2 oak logs, 5 nails and a hammer to repair " +
                                "the polishing wheel.",
                        )
                }
                return@startDialogue
            }
            access.repairAnim(tool)
            if (!access.hasMaterials(tool)) {
                return@startDialogue
            }
            access.takeMaterials(tool)
            player.setFoundryVar(tool.varbit, SleepingGiants.REPAIR_DONE)
            if (tool == RepairTool.Grindstone) {
                access.delay(2)
            }
            repaired(tool)
        }
    }

    private suspend fun Dialogue.repairIntro(tool: RepairTool) {
        when (tool) {
            RepairTool.Hammer -> {
                room.kovacToTripHammer(player)
                chatPlayer(quiz, "I think this has seen better days. What was it?")
                lineByLine("Hmm...", "big hammer.") { chatKovac(neutral, it) }
                chatPlayer(quiz, "Right... and what did it do?")
                chatKovac(neutral, "Hit things.")
                chatPlayer(neutral, "Very perceptive, Kovac.")
                player.setFoundryVar(tool.varbit, SleepingGiants.REPAIR_INSPECTED)
                chatPlayer(neutral, "Let me take a look and see if I can fix it.")
                return
            }
            RepairTool.Grindstone -> {
                chatPlayer(neutral, "What was this meant to be?")
                lineByLine("Hmm...", "Kovac think it was for grinnin... grinding.") {
                    chatKovac(neutral, it)
                }
                chatPlayer(neutral, "I see. It looks like it's lost its teeth.")
                chatKovac(neutral, "Kovac conquer.")
                chatPlayer(confused, "Conquer? It's already broken! Hitting it more won't help.")
                lineByLine("Hmm...", "Kovac agree.") { chatKovac(neutral, it) }
                chatPlayer(neutral, "Right... now we're agreed, I'll see what I can do to fix it.")
            }
            RepairTool.PolishingWheel -> {
                chatPlayer(neutral, "Okay, what is this thing? It just looks like a wheel to me.")
                chatKovac(neutral, "Hmm...")
                lineByLine("Kovac ponders your question...", "...") { mesbox(it) }
                chatKovac(neutral, "Kovac think he know.")
                chatPlayer(quiz, "And?")
                lineByLine("This polishing wheel...", "make thing shiny.") { chatKovac(neutral, it) }
                chatPlayer(neutral, "Looks like it's lost its soft edge then.")
            }
        }
        player.setFoundryVar(tool.varbit, SleepingGiants.REPAIR_INSPECTED)
    }

    private suspend fun Dialogue.repaired(tool: RepairTool) {
        when (tool) {
            RepairTool.Hammer -> {
                chatPlayer(neutral, "That looks like it'll do the trick. What do you think?")
                lineByLine("Hmm...", "that look right.") { chatKovac(neutral, it) }
                chatPlayer(quiz, "What's it supposed to do?")
                hammerExplained()
            }
            RepairTool.Grindstone -> {
                chatPlayer(neutral, "Well that required a bit of elbow grease. It should be good to go now.")
                chatKovac(neutral, "Thank you small one...")
                chatPlayer(neutral, "Didn't want to try and help me?")
                lineByLine("Kovac did not know how...", "Kovac beggin... beginning to understand.") {
                    chatKovac(neutral, it)
                }
                chatPlayer(neutral, "Can you tell me what this is used for then?")
                grindstoneExplained()
            }
            RepairTool.PolishingWheel -> {
                chatPlayer(neutral, "Not my finest work, but I think it will do.")
                lineByLine("Hmm...", "Kovac agree.") { chatKovac(neutral, it) }
                chatPlayer(neutral, "So when would we use the polishing wheel?")
                polishingWheelExplained()
            }
        }
    }

    private suspend fun Dialogue.hammerExplained() {
        lineByLine("Kovac takes a long look at the trip hammer...", "...") { mesbox(it) }
        chatKovac(neutral, "Kovac think Kovac figure it out.")
        chatPlayer(neutral, "Go on.")
        lineByLine("Trip hammer power by water...", "Water spin wheel and raise hammer.") {
            chatKovac(neutral, it)
        }
        chatKovac(neutral, "Hammer fall and crump sword into shape.")
        chatPlayer(neutral, "That makes sense. Quite resourceful for giants to make something water powered.")
        chatKovac(neutral, "Mhm...")
    }

    private suspend fun Dialogue.grindstoneExplained() {
        lineByLine("Kovac takes a long look at the grindstone...", "...") { mesbox(it) }
        lineByLine("Grindstone important...", "We grind sword against stone...", "make sword sharp.") {
            chatKovac(neutral, it)
        }
        chatPlayer(neutral, "So we use it when making swords?")
        chatKovac(neutral, "Yes.")
    }

    private suspend fun Dialogue.polishingWheelExplained() {
        chatKovac(neutral, "Polishing sword impotan... important.")
        lineByLine("Create perfect killing edge...", "very good for sword.") { chatKovac(neutral, it) }
        chatPlayer(neutral, "That makes sense.")
    }

    // TODO(content): [verify:in_game] what inspecting a repaired tool says; this replays
    // Kovac's explanation from its repair.
    private suspend fun ProtectedAccess.inspectRepaired(tool: RepairTool) {
        startDialogue {
            when (tool) {
                RepairTool.Hammer -> hammerExplained()
                RepairTool.Grindstone -> grindstoneExplained()
                RepairTool.PolishingWheel -> polishingWheelExplained()
            }
        }
    }

    private suspend fun ProtectedAccess.repairAnim(tool: RepairTool) {
        when (tool) {
            RepairTool.PolishingWheel -> {
                delay(1)
                anim(LEATHER_CRAFTING_SEQ)
                delay(3)
                anim(PLOUGH_FIX_SEQ)
                delay(4)
            }
            RepairTool.Grindstone -> {
                delay(3)
                anim(PICKUP_FLOOR_SEQ)
                soundSynth(PUT_DOWN_SYNTH, delay = 15)
                delay(4)
                anim(GRINDSTONE_REPAIR_SEQ)
                delay(5)
            }
            RepairTool.Hammer -> {
                delay(1)
                anim(PLOUGH_FIX_SEQ)
                delay(4)
            }
        }
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        inv.contains(HAMMER) ||
            inv.contains(IMCANDO_HAMMER) ||
            player.worn[Wearpos.LeftHand.slot]?.isType(IMCANDO_HAMMER_OFFHAND) == true

    private fun ProtectedAccess.nailsType(count: Int): String? =
        NAILS.firstOrNull { inv.count(it) >= count }

    private fun ProtectedAccess.hasMaterials(tool: RepairTool): Boolean =
        inv.count(OAK_LOGS) >= tool.oakLogs &&
            (tool.nails == 0 || nailsType(tool.nails) != null) &&
            (!tool.wool || inv.contains(WOOL)) &&
            (!tool.hammer || hasHammer()) &&
            (!tool.chisel || inv.contains(CHISEL))

    private fun ProtectedAccess.takeMaterials(tool: RepairTool) {
        if (tool.oakLogs > 0) {
            invDel(inv, OAK_LOGS, tool.oakLogs)
        }
        if (tool.nails > 0) {
            invDel(inv, checkNotNull(nailsType(tool.nails)), tool.nails)
        }
        if (tool.wool) {
            invDel(inv, WOOL, 1)
        }
    }

    /* Inspecting the room's objects */

    private suspend fun Dialogue.chatKovac(mesanim: MesAnimType, text: String) =
        chatNpcSpecific(KOVAC_NAME, KOVAC_QUEST, mesanim, text)

    // UNVERIFIED: `shocked` for the lines that play chatshock, which no mesanim has (guess)
    private val Dialogue.chatshock: MesAnimType
        get() = shocked

    private suspend fun ProtectedAccess.inspectChest() {
        startDialogue {
            chatPlayer(quiz, "Kovac, where did you get this chest from?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "...")
            chatKovac(neutral, "chest store stuff.")
            mesbox("Kovac shrugs...")
            chatPlayer(neutral, "Alright then...")
        }
    }

    private suspend fun ProtectedAccess.inspectCrate() {
        startDialogue {
            chatPlayer(quiz, "What do you keep in this box?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "stuff.")
            chatPlayer(bored, "Very insightful...")
        }
    }

    private suspend fun ProtectedAccess.inspectCrucible() {
        startDialogue {
            chatPlayer(quiz, "What is this, Kovac?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "look like big bucket.")
            chatPlayer(quiz, "I can see that, but what's it used for?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "maybe we put things in.")
            chatKovac(neutral, "Kovac need think about it more.")
        }
    }

    private suspend fun ProtectedAccess.inspectLavaPool() {
        startDialogue {
            chatPlayer(worried, "Having all this lava here without a barrier is quite dangerous.")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "Kovac agree.")
            chatPlayer(quiz, "Should we try building something to keep us safe?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "no.")
            chatPlayer(quiz, "Why not?")
            chatKovac(neutral, "Just no swim in the hot juice.")
        }
    }

    private suspend fun ProtectedAccess.inspectJig() {
        startDialogue {
            chatPlayer(quiz, "What is this table used for, Kovac?")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "look like we put thing on it.")
            chatKovac(neutral, "Kovac not sure what yet.")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "Kovac think about it more.")
        }
    }

    private suspend fun ProtectedAccess.inspectStorage() {
        startDialogue {
            chatPlayer(worried, "This looks a little dangerous. Quite a perilous place to store something.")
            chatKovac(neutral, "Hmm...")
            chatKovac(neutral, "good place to cook food.")
            chatKovac(happy, "Hot meat very good.")
        }
    }

    private suspend fun ProtectedAccess.inspectWaterfall() {
        startDialogue {
            chatPlayer(neutral, "Not everyday you see a waterfall in a cave.")
            chatKovac(neutral, "Kovac drink from waterfall...")
            chatKovac(neutral, "Hydration impote... importent.")
            chatPlayer(neutral, "I agree, it's very important.")
        }
    }

    /* Make-A-Sword */

    private suspend fun ProtectedAccess.searchCrate() {
        startDialogue {
            lineByLine("You rummage through the box...", "You find a bunch of metal bars and old weapons!") {
                mesbox(it)
            }
            if (access.inv.freeSpace() < CRATE_ITEMS.sumOf { it.second }) {
                mesbox("You will need 20 free inventory slots to take the items from the crate.")
                return@startDialogue
            }
            if (!choice2("Yes.", true, "No.", false, title = "Take the items?")) {
                return@startDialogue
            }
            player.advanceTutorial(SleepingGiants.TUTORIAL_CRATE_SEARCHED)
            access.anim(TAKE_SEQ)
            for ((obj, count) in CRATE_ITEMS) {
                access.invAdd(access.inv, obj, count)
            }
            access.soundSynth(TAKE_SYNTH)
            doubleobjbox(
                "obj.bronze_battleaxe",
                "obj.iron_bar",
                "You take a selection of bars and weapons from the crate.",
            )
        }
    }

    private suspend fun Dialogue.makeSword(npc: Npc) {
        val tutorial = player.sleepingGiantsTutorial
        when {
            tutorial < SleepingGiants.TUTORIAL_COMMISSION -> commissionTalk()
            // TODO(content): [verify:in_game] Kovac's line before the crate is searched; this
            // repeats where the metal is.
            tutorial < SleepingGiants.TUTORIAL_CRATE_SEARCHED -> {
                lineByLine("Kovac has been connecti... collecting metals...", "They're in that box-crate by entry.") {
                    chatNpc(neutral, it)
                }
            }
            tutorial < SleepingGiants.TUTORIAL_CRUCIBLE_FULL ->
                if (player.crucibleState == 0) afterCrate() else crucibleNotFull()
            tutorial < SleepingGiants.TUTORIAL_MOULD_EXPLAINED -> crucibleFull()
            tutorial < SleepingGiants.TUTORIAL_MOULD_SET -> mouldHelp()
            tutorial < SleepingGiants.TUTORIAL_POUR_EXPLAINED -> mouldSet()
            // TODO(content): [verify:in_game] Kovac's line before the metal is poured; this
            // repeats his last instruction.
            tutorial < SleepingGiants.TUTORIAL_POURED -> {
                lineByLine("Pour the metal from the crucible into the mould...", "then take out the preform.") {
                    chatNpc(neutral, it)
                }
            }
            tutorial < SleepingGiants.TUTORIAL_PREFORM_TAKEN -> beforePickUp()
            else -> workingPreform(npc)
        }
    }

    private suspend fun Dialogue.commissionTalk() {
        chatPlayer(neutral, "Okay Kovac, I think I'm ready to make a sword.")
        lineByLine("Hmm...", "Kovac feel like things look correct...", "Kovac think you are right.") {
            chatNpc(neutral, it)
        }
        chatPlayer(quiz, "So what do I need to do to make a sword?")
        lineByLine("First...", "you will need a coliti... commission.") { chatNpc(neutral, it) }
        chatPlayer(quiz, "Where can I get one of those?")
        lineByLine("Kovac will supply them...", "Kovac already gotten one from old friend.") {
            chatNpc(neutral, it)
        }
        chatPlayer(neutral, "Okay, so what do I need to make?")
        player.commissionWord1 = CommissionWord.Flat.id
        player.commissionWord2 = CommissionWord.Broad.id
        player.commissionStart = (System.currentTimeMillis() / 1000).toInt()
        player.advanceTutorial(SleepingGiants.TUTORIAL_COMMISSION)
        chatNpc(neutral, "Kovac need a <col=ef1020>Flat Broad</col> sword.")
        chatPlayer(happy, "Great!")
        chatPlayer(confused, "How do I make that?")
        lineByLine("Kovac think we need to fill the...", "how's it called...", "the big stone bucket...") {
            chatNpc(neutral, it)
        }
        chatPlayer(quiz, "Oh, you mean a crucible?")
        lineByLine("Yes, thank you...", "Kovac think we need to fill the crucible with metal first.") {
            chatNpc(neutral, it)
        }
        lineByLine("Kovac has been connecti... collecting metals...", "They're in that box-crate by entry.") {
            chatNpc(neutral, it)
        }
        lineByLine("Use bars and weapons...", "Crucible can take both.") { chatNpc(neutral, it) }
        chatNpc(neutral, "Load metals into crucible until it looks full.")
        lineByLine("While you do that...", "Kovac think about next thing we need to do.") {
            chatNpc(neutral, it)
        }
    }

    private suspend fun Dialogue.afterCrate() {
        chatPlayer(neutral, "Alright, I've got the metal you said to take from the box. What now?")
        chatNpc(neutral, "You will need to fill the crucible...<br> <br> ")
        chatNpc(neutral, "You will need to fill the crucible...<br>Add metals, armour or weapon you find to fill it.")
        lineByLine("We stick to bronze and iron for this sword...", "We keep the first sword simple.") {
            chatNpc(neutral, it)
        }
    }

    private suspend fun Dialogue.crucibleNotFull() {
        chatPlayer(quiz, "I've added a few things to the crucible. Is that enough?")
        chatNpc(neutral, "No, we need more...")
        chatNpc(
            neutral,
            "28 metal bars worth. Weapon and armour can go into crucible too but some metal " +
                "lost when melted down.",
        )
    }

    private suspend fun Dialogue.crucibleFull() {
        chatPlayer(quiz, "Right, that's the crucible full. Can I make the sword yet?")
        lineByLine("No, not yet...", "Kovac think we need to set up a mould.") { chatNpc(neutral, it) }
        chatPlayer(quiz, "What's a mould used for?")
        lineByLine("Hmm...", "Kovac think mould will determine the type of sword to<br>be made.") {
            chatNpc(neutral, it)
        }
        chatNpc(neutral, "Remember, we need a <col=ef1020>Flat Broad</col> sword.")
        chatPlayer(quiz, "Okay, so how do I go about setting up a mould?")
        lineByLine("Kovac remember being told a sword is made from three<br>parts...", "a forte, a blade and a tip.") {
            chatNpc(neutral, it)
        }
        lineByLine("Use mould jig behind you...", "You can select part for each selecti... section.") {
            chatNpc(neutral, it)
        }
        lineByLine(
            "Each mould part is differing...",
            "They will suit different commissions better...",
            "Look for parts that match commission.",
        ) {
            chatNpc(neutral, it)
        }
        chatPlayer(quiz, "Right... I think I'm with you.")
        lineByLine("Look at mould jig...", "If you are still confused...", "come back to me.") {
            chatNpc(neutral, it)
        }
        player.advanceTutorial(SleepingGiants.TUTORIAL_MOULD_EXPLAINED)
    }

    private suspend fun Dialogue.mouldHelp() {
        chatPlayer(worried, "I'm sorry Kovac, but I'm not entirely sure what I'm meant to be doing.")
        chatNpc(neutral, "That is okay...")
        chatNpc(neutral, "Kovac not so sure of himself...")
        chatNpc(neutral, "but you help Kovac, so Kovac help you.")
        chatPlayer(happy, "Thanks Kovac. You're gentle for a giant.")
        chatPlayer(quiz, "So what was it I needed to do at the mould jig?")
        chatNpc(neutral, "We need to fill each part of the mould...")
        chatNpc(neutral, "Forte, blade and tip.")
        chatNpc(neutral, "We want to use mould parts that match collisi... commission Kovac give you.")
        chatNpc(neutral, "Remember, each part suit a differing sword better...")
        chatNpc(neutral, "Look for ones that match <col=ef1020>Flat and Broad</col>.")
    }

    private suspend fun Dialogue.mouldSet() {
        chatPlayer(
            quiz,
            "Right, the mould is setup and the crucible is full to the brim. We must be ready to pour.",
        )
        lineByLine("Hmm...", "let me look.") { chatNpc(neutral, it) }
        lineByLine("Kovac takes a look at what you've done...", "...") { mesbox(it) }
        lineByLine("Yes, all things look correct...", "Well done little one. We are ready.") {
            chatNpc(neutral, it)
        }
        lineByLine("Pour the metal from the crucible into the mould...", "then take out the preform.") {
            chatNpc(neutral, it)
        }
        lineByLine("Once you have done that...", "Kovac will tell how to work the preform.") {
            chatNpc(neutral, it)
        }
        player.advanceTutorial(SleepingGiants.TUTORIAL_POUR_EXPLAINED)
    }

    private suspend fun Dialogue.beforePickUp() {
        if (player.inv.contains(BUCKET_WATER) || player.worn[Wearpos.Hands.slot] != null) {
            chatPlayer(confused, "I'm not sure what to do next...")
            chatNpc(neutral, "Hmm...")
            chatNpc(neutral, "take the preform and put it in your hand. Then come see Kovac for help.")
            return
        }
        chatPlayer(worried, "The metal's too hot to pick up!")
        chatNpc(neutral, "Yes.")
        chatPlayer(quiz, "What do I do?")
        chatNpc(neutral, "Cool it down...")
        chatNpc(neutral, "What do you expect?")
        chatPlayer(quiz, "Ah, of course... How do I do that?")
        chatNpc(neutral, "Kovac do not know. Try pouring water on it.")
    }

    private suspend fun Dialogue.workingPreform(npc: Npc) {
        val finished =
            player.preformCompletion >= FoundryFormulas.SCALE || player.preformQuality == 0
        if (player.holdsPreform() && finished) {
            handIn()
            return
        }
        if (!player.holdsPreform()) {
            chatPlayer(confused, "I'm not sure what to do next...")
            chatNpc(neutral, "Hmm...")
            chatNpc(neutral, "take the preform and put it in your hand. Then come see Kovac for help.")
            return
        }
        // TODO(content): [verify:in_game] when the long explanation is given; here it's while
        // no progress has been made.
        if (player.preformCompletion == 0) {
            preformExplained()
            return
        }
        chatPlayer(confused, "I'm not sure which tool I need to use next.")
        val section = FoundryFormulas.section(player.preformCompletion, player.preformDifficulty)
        when (FoundryTool.forSeed(player.foundryVar(seedVarbit(section)))) {
            FoundryTool.Hammer -> {
                chatNpc(neutral, "Hmm...")
                chatNpc(neutral, "Hmm... you need to use the trip hammer.")
                chatNpc(neutral, "Make sure preform is nice and hot...")
                chatNpc(neutral, "Trip hammer needs a high temperature.")
            }
            FoundryTool.Grindstone -> {
                chatNpc(neutral, "Hmm...")
                chatNpc(neutral, "time to move over to the grindstone.")
                chatNpc(neutral, "The preform need to be warm...")
                chatNpc(neutral, "Fictio... friction from grindstone will heat the preform up as well.")
            }
            FoundryTool.PolishingWheel -> {
                chatNpc(neutral, "You are almost done...")
                chatNpc(neutral, "You just need to use polishing wheel to finish preform off.")
                chatNpc(neutral, "Preform need to be quite cool to work with polishing wheel.")
            }
        }
    }

    private suspend fun Dialogue.preformExplained() {
        chatPlayer(quiz, "Okay Kovac, I've got this huge burning hot sword. What do I do with it?")
        chatNpc(happy, "Now for fun part.")
        chatNpc(neutral, "Use tools around the foundry to reclin... refine the preform.")
        chatPlayer(quiz, "How do I know which tool I need to use?")
        chatNpc(neutral, "Hmm...")
        chatNpc(neutral, "Kovac not too sure. Kovac's gut feeling says use trip hammer first.")
        mesbox(
            "Progress on your sword can be tracked via the display on screen. You can track " +
                "which tool you need to use via the completion bar."
        )
        chatNpc(neutral, "This is very impoten... important...")
        chatNpc(neutral, "The preform needs to be within a certain heat range to work properly.")
        chatNpc(neutral, "If you use the wrong tool...")
        chatNpc(neutral, "or tool at wrong heat, we damage the preform quality.")
        chatNpc(neutral, "A sword with low quality is bad...")
        chatNpc(neutral, "Means less giants will want it, so Kovac give less for it.")
        chatNpc(
            neutral,
            "If you damage a preform too much, it will become junk and Kovac will have to throw " +
                "it away...",
        )
        chatNpc(neutral, "but this first attent... attempt, so do not worry about quality too much.")
        chatNpc(neutral, "You use trip hammer first. Trip hammer need preform to be quite hot.")
        mesbox("The temperature of the preform is tracked via the temperature bar.")
        mesbox(
            "To work a preform with a tool, the temperature needs to be within the matched " +
                "temperature range, or you'll damage the preform."
        )
        mesbox(
            "Any damage to the preform will lower its quality, tracked via the quality bar. If " +
                "there's no quality left in the preform it becomes unworkable, and must be throw " +
                "away."
        )
        chatNpc(neutral, "If the preform at wrong temperature...")
        chatNpc(neutral, "You can hot it up by dunking it in lava or cold it by holding it to waterfall.")
        chatPlayer(neutral, "Alright, I'll give it a go.")
        chatNpc(neutral, "Remember, try trip hammer first with hot preform.")
    }

    /**
     * The quest sword pays no foundry rewards: in sigmazx's video
     * (youtube.com/watch?v=WnGuNtTJW0M) chat goes straight to the quest completion, with no
     * coins, reputation or hand-in summary, and the XP drop is only the quest's 6,000.
     */
    private suspend fun Dialogue.handIn() {
        chatPlayer(happy, "Okay Kovac, I think I've finally finished the sword!")
        lineByLine("Hmm...", "let Kovac look.") { chatNpc(neutral, it) }
        val quality = player.preformQuality
        player.removePreform()
        access.closeHud()
        player.clearSword()
        player.advanceTutorial(SleepingGiants.TUTORIAL_HANDED_IN)
        access.anim(TAKE_SEQ)
        lineByLine("You pass the sword over to Kovac...", "He analyses your work...") { mesbox(it) }
        lineByLine("Hmm...", "Yes...", "Hmm...") { chatNpc(neutral, it) }
        lineByLine("This is defeanin... definitely a sword...", "Well done little one.") {
            chatNpc(neutral, it)
        }
        if (quality == 0) {
            chatPlayer(sad, "Sorry it's not very good. I'm still getting use to everything.")
            chatNpc(neutral, "That is okay. You do better than Kovac could...")
            chatNpc(neutral, "We still leanin... learning.")
            chatPlayer(quiz, "So what do we do with it now?")
        } else {
            chatPlayer(happy, "Great! So what do we do with it now?")
        }
        chatNpc(neutral, "Kovac take care of it.")
        chatPlayer(quiz, "Out of curiosity, who is this first blade for?")
        lineByLine("Kovac friend...", "Obor.") { chatNpc(neutral, it) }
        // Obor's kill count is `total_hillgiant_boss_kills` (the cache's hiscores row for Obor).
        if (vars[OBOR_KILLS] > 0) {
            chatPlayer(chatshock, "Obor? He's just going to use it to kill me!")
            lineByLine(
                "Obor good friend. Kovac know he only attack if you<br>trespass his cave...",
                "Maybe you should have some manners and knock first.",
            ) {
                chatNpc(neutral, it)
            }
            chatPlayer(
                confused,
                "Well, you're not wrong there... I normally just let myself in.",
            )
            chatNpc(neutral, "Yes, manner very important.")
        } else {
            chatPlayer(quiz, "Obor? Can't say I've heard of them.")
        }
        chatNpc(
            neutral,
            "Kovac think Obor show other giants how strong our swords are. Then every giant " +
                "want one.",
        )
        chatPlayer(happy, "That's a pretty clever marketing plan, I must admit.")
        chatNpc(neutral, "Kovac know what he do sometime...<br> <br> ")
        chatNpcSpecificNp(
            KOVAC_NAME,
            KOVAC_FOUNDRY_HEAD,
            neutral,
            "Kovac know what he do sometime...<br>Thank you for help, ${player.displayName}. You " +
                "free to use foundry whenever you want.",
        )
        quest.completeQuest(access)
    }

    /**
     * Kovac stops the player when a tool is used on the quest sword at the wrong time.
     * https://oldschool.runescape.wiki/w/Transcript:Sleeping_Giants
     */
    private suspend fun ProtectedAccess.kovacStop(stop: QuestPrompt.KovacStop) {
        startDialogue {
            chatNpcSpecific(KOVAC_NAME, KOVAC_FOUNDRY_HEAD, angry, "Stop!")
            chatPlayer(worried, "What did I do wrong?")
            chatNpcSpecific(KOVAC_NAME, KOVAC_FOUNDRY_HEAD, angry, "You're damaging the preform!")
            chatPlayer(quiz, "How?")
            chatNpcSpecific(KOVAC_NAME, KOVAC_FOUNDRY_HEAD, neutral, stop.reason)
            chatPlayer(sad, "Oh, my bad!")
        }
    }

    private companion object {
        const val ENTRANCE = "loc.giants_foundry_entrance"
        const val OBOR_KILLS = "varp.total_hillgiant_boss_kills"
        const val GRINDSTONE_REPAIR_SEQ = "seq.sg_grindstone_repair"
        const val LEATHER_CRAFTING_SEQ = "seq.human_leather_crafting"
        const val PLOUGH_FIX_SEQ = "seq.hosidius_plough_fix"
        const val PICKUP_FLOOR_SEQ = "seq.human_pickupfloor"
        const val PUT_DOWN_SYNTH = "synth.put_down"
        const val TAKE_SEQ = "seq.human_pickuptable"
        const val TAKE_SYNTH = "synth.pick2"
        const val KOVAC_NAME = "Kovac"
        const val KOVAC_STRIKE = "npc.giants_foundry_kovac_fake_attack"
        const val KOVAC_OUTSIDE = "npc.giants_foundry_kovac_multi_outside"
        const val KOVAC_QUEST = "npc.giants_foundry_kovac_quest"
        const val KOVAC_FOUNDRY = "npc.giants_foundry_kovac_1op"
        const val KOVAC_FOUNDRY_HEAD = "npc.giants_foundry_kovac_multi"

        const val SMITHING = "stat.smithing"
        const val REQUIRED_SMITHING = 15

        const val HAMMER_BROKEN = "loc.giants_foundry_trip_hammer_broken_quest"
        const val GRINDSTONE_BROKEN = "loc.giants_foundry_grindstone_broken_quest"
        const val POLISHING_WHEEL_BROKEN = "loc.giants_foundry_polishing_wheel_broken_quest"
        const val HAMMER_REPAIRED = "loc.giants_foundry_trip_hammer_quest"
        const val GRINDSTONE_REPAIRED = "loc.giants_foundry_grindstone_quest"
        const val POLISHING_WHEEL_REPAIRED = "loc.giants_foundry_polishing_wheel_quest"
        const val CHEST_QUEST = "loc.giants_foundry_chest_quest"
        const val CRATE_QUEST = "loc.giants_foundry_supply_box_quest"
        const val CRUCIBLE_QUEST = "loc.giants_foundry_crucible_empty_quest"
        const val LAVA_POOL_QUEST = "loc.giants_foundry_lava_pool_quest"
        const val JIG_QUEST = "loc.giants_foundry_jig_empty_quest"
        const val STORAGE_QUEST = "loc.giants_foundry_preform_storage_empty_quest"
        const val WATERFALL_QUEST = "loc.giants_foundry_waterfall_quest"
        const val CRATE_FULL = "loc.giants_foundry_supply_box"
        const val CRATE_EMPTY = "loc.giants_foundry_supply_box_empty"

        const val OAK_LOGS = "obj.oak_logs"
        const val WOOL = "obj.wool"
        const val CHISEL = "obj.chisel"
        const val HAMMER = "obj.hammer"
        const val IMCANDO_HAMMER = "obj.imcando_hammer"
        const val IMCANDO_HAMMER_OFFHAND = "obj.imcando_hammer_offhand"
        const val BUCKET_WATER = "obj.bucket_water"

        /**
         * Using a material on a broken tool starts its repair too: a June 2022 hotfix stopped
         * "using items on the Polishing Wheel" from fixing the trip hammer.
         * https://oldschool.runescape.wiki/w/Giants%27_Foundry (Changes)
         */
        // UNVERIFIED: that using a material plays the Repair op's dialogue (guess)
        val REPAIR_MATERIALS by lazy {
            listOf(OAK_LOGS, WOOL, CHISEL, HAMMER, IMCANDO_HAMMER) + NAILS
        }

        // UNVERIFIED: that nails other than steel nails work (wiki description)
        val NAILS =
            listOf(
                "obj.nails_bronze",
                "obj.nails_iron",
                "obj.nails",
                "obj.nails_black",
                "obj.nails_mithril",
                "obj.nails_adamant",
                "obj.nails_rune",
            )

        /**
         * 20 items, exactly 28 bars' worth (16 bronze, 12 iron), as seen in the inventory right
         * after taking them in two Sleeping Giants videos (Slayermusiq1,
         * youtube.com/watch?v=io6mQxi1ddI 4:09; sigmazx, youtube.com/watch?v=WnGuNtTJW0M 5:41).
         */
        val CRATE_ITEMS =
            listOf(
                "obj.bronze_bar" to 8,
                "obj.iron_bar" to 6,
                "obj.bronze_battleaxe" to 2,
                "obj.iron_longsword" to 2,
                "obj.bronze_platebody" to 1,
                "obj.iron_platebody" to 1,
            )
    }
}

/** Something the quest says while the sword is worked, queued from the refining timer. */
internal sealed interface QuestPrompt {
    /** Kovac stops the player; [reason] is his line from the transcript. */
    class KovacStop(val reason: String) : QuestPrompt

    data object SwordFinished : QuestPrompt

    companion object {
        private val NAMES =
            mapOf(
                FoundryTool.Hammer to "trip hammer",
                FoundryTool.Grindstone to "grindstone",
                FoundryTool.PolishingWheel to "polishing wheel",
            )

        fun wrongTool(needed: FoundryTool) =
            KovacStop("Using wrong tool. We should be using the ${NAMES.getValue(needed)}.")

        fun tooCold(tool: FoundryTool) =
            KovacStop(
                "The sword is too cold. We need to heat it up to use it on the ${NAMES.getValue(tool)}."
            )

        fun tooHot(tool: FoundryTool) =
            KovacStop(
                "The sword is too hot. We need to cool it down to use it on the ${NAMES.getValue(tool)}."
            )
    }
}

/**
 * A broken-up line is one box that gains a line per click; each frame is padded with blank
 * lines to the final line count, so the box keeps its size and chathead anim.
 */
private suspend fun lineByLine(vararg lines: String, show: suspend (String) -> Unit) {
    for (shown in 1..lines.size) {
        val hidden = lines.drop(shown).flatMap { it.split(LINE_BREAK) }.map { " " }
        show((lines.take(shown) + hidden).joinToString(LINE_BREAK))
    }
}

private const val LINE_BREAK = "<br>"

internal const val QUEST_PROMPT_QUEUE = "queue.giants_foundry_quest_prompt"

/** The player's line once the quest sword is done, or a tool is used on it after. */
internal suspend fun ProtectedAccess.questSwordFinished() {
    startDialogue {
        chatPlayer(neutral, "Looks like the sword is finished. I should take it to Kovac.")
    }
}
