package org.rsmod.content.quest.area.falador.knightssword

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KnightsSwordQuest :
    QuestScript(
        "quest_knightssword",
        "varp.squire",
        rewards { xp("stat.smithing", SMITHING_XP) },
        ItemRewardDisplay(BLURITE_SWORD, zoom = 400),
        questVarbit = "varbit.knights_sword_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "The Knight's Sword end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Squire<col=000080> in the courtyard of the " +
            "<col=800000>White Knights' Castle<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(player.player)
            strike(
                "I told the Squire I would help him to replace the sword he had lost. It could " +
                    "only be made by an Imcando Dwarf."
            )
            when (stage) {
                STAGE_STARTED ->
                    line(
                        "The Squire suggests I speak to <red>Reldo</red> in the <red>Varrock " +
                            "Palace Library</red> for information about the <red>Imcando " +
                            "Dwarves.</red>"
                    )
                STAGE_TOLD_BY_RELDO ->
                    line(
                        "Reldo couldn't give me much information about the <red>Imcando</red> " +
                            "except a few live on the <red>southern peninsula of Asgarnia,</red> " +
                            "they dislike strangers, and LOVE <red>redberry pies.</red>"
                    )
                STAGE_GAVE_PIE -> {
                    strike(FOUND_THURGO)
                    line("He will help me now I have gained his trust through <red>pie.</red>")
                }
                STAGE_THURGO_NEEDS_PICTURE -> {
                    strike(FOUND_THURGO)
                    line(
                        "<red>Thurgo</red> needs a <red>picture of the sword</red> before he can " +
                            "help."
                    )
                    line("I should probably ask the <red>Squire</red> about obtaining one.")
                }
                STAGE_LOOKING_FOR_PORTRAIT -> {
                    strike("$FOUND_THURGO Thurgo needed a picture of the sword to replace.")
                    if (player.inv.count(PORTRAIT) > 0) {
                        line(
                            "I now have a picture of the <red>Knight's Sword</red> - I should " +
                                "take it to <red>Thurgo</red> so that he can duplicate it."
                        )
                    } else {
                        line(
                            "The Squire told me about a <red>portrait</red> of Sir Vyvin's " +
                                "father which has a <red>picture of the sword</red> in <red>Sir " +
                                "Vyvin's room.</red>"
                        )
                    }
                }
                STAGE_GAVE_PORTRAIT -> {
                    strike(
                        "$FOUND_THURGO Thurgo needed a picture of the sword before he could " +
                            "start work on a replacement. I took him a portrait of it."
                    )
                    if (player.inv.count(BLURITE_SWORD) > 0) {
                        strike("Thurgo has now smithed me a replica of Sir Vyvin's sword.")
                        line("")
                        line(
                            "I should return it to the <red>Squire</red> for my " +
                                "<red>reward.</red>"
                        )
                    } else {
                        // UNVERIFIED: journal line while Thurgo waits for the materials (guess)
                        line(
                            "<red>Thurgo</red> needs <red>two iron bars</red> and some " +
                                "<red>blurite ore</red> to make the sword."
                        )
                    }
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Thurgo needed a picture of the sword before he could start work on a " +
                    "replacement. I took him a portrait of it. After bringing Thurgo two iron " +
                    "bars and some blurite ore he made me a fine replica of Sir Vyvin's Sword, " +
                    "which I returned to the Squire for a reward."
            )
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    suspend fun Dialogue.squireStartOffer() {
        val lowMining = player.baseMiningLvl < MINING_LEVEL
        val lowCombat = player.combatLevel < RECOMMENDED_COMBAT
        when {
            lowMining && lowCombat ->
                mesbox(
                    "Before starting this quest, be aware that one or more of your skill levels " +
                        "are lower than what is required to fully complete it. Your combat level " +
                        "is also lower than the recommended level of $RECOMMENDED_COMBAT."
                )
            // UNVERIFIED: low-Mining-only start warning, first half of the combined one (wiki description)
            lowMining ->
                mesbox(
                    "Before starting this quest, be aware that one or more of your skill levels " +
                        "are lower than what is required to fully complete it."
                )
            // UNVERIFIED: low-combat-only start warning (guess)
            lowCombat ->
                mesbox(
                    "Before starting this quest, be aware that your combat level is lower than " +
                        "the recommended level of $RECOMMENDED_COMBAT."
                )
        }
        val accept = choice2("Yes.", true, "No.", false, title = "Start The Knight's Sword quest?")
        if (!accept) {
            chatPlayer(neutral, "No. I've got lots of mining work to do.")
            chatNpc(sad, "Oh man... I'm in such trouble...")
            return
        }
        // TODO(content): [quest:the_giant_dwarf] "I'll help, I know an Imcando dwarf!" when
        // Thurgo was met during The Giant Dwarf.
        quest.setQuestStage(access, STAGE_STARTED)
        chatPlayer(neutral, "Ok, I'll give it a go.")
        chatNpc(
            happy,
            "Thank you very much! As I say, the best place to start should be with Reldo...",
        )
    }

    /** Returns `false` when the quest hasn't started or is complete. */
    suspend fun Dialogue.squireDialogue(): Boolean {
        when (stage(player)) {
            STAGE_STARTED,
            STAGE_TOLD_BY_RELDO,
            STAGE_GAVE_PIE -> squireLookingForDwarves()
            STAGE_THURGO_NEEDS_PICTURE -> squirePortraitHint()
            STAGE_LOOKING_FOR_PORTRAIT -> squireAwaitingPortrait()
            STAGE_GAVE_PORTRAIT -> squireAwaitingSword()
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.squireLookingForDwarves() {
        chatNpc(quiz, "So how are you doing getting a sword?")
        chatPlayer(neutral, "I'm looking for Imcando dwarves to help me.")
        chatNpc(worried, "Please try and find them quickly... I am scared Sir Vyvin will find out!")
    }

    private suspend fun Dialogue.squirePortraitHint() {
        chatNpc(quiz, "So how are you doing getting a sword?")
        chatPlayer(
            happy,
            "I've found an Imcando dwarf but he needs a picture of the sword before he can make it.",
        )
        chatNpc(
            confused,
            "A picture eh? Hmmm.... The only one I can think of is in a small portrait of Sir " +
                "Vyvin's father... Sir Vyvin keeps it in a cupboard in his room I think.",
        )
        quest.setQuestStage(access, STAGE_LOOKING_FOR_PORTRAIT)
        chatPlayer(neutral, "Ok, I'll try and get that then.")
        chatNpc(confused, "Please don't let him catch you! He MUSTN'T know what happened!")
    }

    private suspend fun Dialogue.squireAwaitingPortrait() {
        chatNpc(quiz, "So how are you doing getting a sword?")
        if (player.inv.count(PORTRAIT) > 0) {
            chatPlayer(happy, "I have the picture. I'll just take it to the dwarf now!")
            chatNpc(worried, "Please hurry!")
            return
        }
        chatPlayer(neutral, "I didn't get the picture yet.")
        chatNpc(worried, "Please try and get it quickly... I am scared Sir Vyvin will find out!")
    }

    private suspend fun Dialogue.squireAwaitingSword() {
        if (player.worn.count(BLURITE_SWORD) > 0) {
            chatPlayer(happy, "I have retrieved your sword for you.")
            chatNpc(quiz, "So can you un-equip it and hand it over to me now please?")
            return
        }
        if (player.inv.count(BLURITE_SWORD) > 0) {
            handInSword()
            return
        }
        chatNpc(quiz, "So how are you doing getting a sword?")
        chatPlayer(
            neutral,
            "I've found a dwarf who will make the sword. I've just got to find the materials " +
                "for it now!",
        )
        chatNpc(worried, "Please hurry!")
    }

    private suspend fun Dialogue.handInSword() {
        chatPlayer(happy, "I have retrieved your sword for you.")
        chatNpc(
            happy,
            "Thank you, thank you, thank you! I was seriously worried I would have to own up to " +
                "Sir Vyvin!",
        )
        mesbox("You give the sword to the squire.")
        if (access.invDel(access.inv, BLURITE_SWORD, 1).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_COMPLETE)
    }

    fun ProtectedAccess.learnAboutImcando() {
        if (stage(player) == STAGE_STARTED) {
            quest.setQuestStage(this, STAGE_TOLD_BY_RELDO)
        }
    }

    /**
     * Thurgo's menu while the quest runs. Returns `false` when the quest hasn't started or is
     * complete; [pie] and [cape] are the Port Sarim module's non-quest topics.
     */
    suspend fun Dialogue.thurgoDialogue(
        pie: suspend Dialogue.() -> Unit,
        cape: suspend Dialogue.() -> Unit,
    ): Boolean {
        val stage = stage(player)
        if (stage == 0 || stage >= STAGE_COMPLETE) {
            return false
        }
        val hasPie = player.inv.count(REDBERRY_PIE) > 0
        val hasSword = player.inv.count(BLURITE_SWORD) > 0 || player.worn.count(BLURITE_SWORD) > 0
        val options = buildList {
            when (stage) {
                STAGE_STARTED,
                STAGE_TOLD_BY_RELDO ->
                    add("Are you an Imcando dwarf? I need a special sword." to ThurgoTopic.Imcando)
                // TODO(content): [verify:in_game] Thurgo's menu after the pie when the sword
                // question was skipped (the transcript only shows the post-pie menu).
                STAGE_GAVE_PIE -> add("Can you make a special sword for me?" to ThurgoTopic.Sword)
                STAGE_THURGO_NEEDS_PICTURE,
                STAGE_LOOKING_FOR_PORTRAIT -> add("About that sword..." to ThurgoTopic.Portrait)
                STAGE_GAVE_PORTRAIT ->
                    if (hasSword) {
                        add("Thanks for making that sword for me!" to ThurgoTopic.Thanks)
                    } else {
                        add("Can you make that replacement sword now?" to ThurgoTopic.Materials)
                    }
            }
            if (hasPie && stage >= STAGE_TOLD_BY_RELDO) {
                add("Would you like a redberry pie?" to ThurgoTopic.Pie)
            }
            add("What is that cape you're wearing?" to ThurgoTopic.Cape)
        }
        when (menu(options)) {
            ThurgoTopic.Imcando -> thurgoImcando(cape)
            ThurgoTopic.Pie ->
                if (stage == STAGE_TOLD_BY_RELDO) {
                    thurgoFirstPie(cape)
                } else {
                    pie()
                }
            ThurgoTopic.Sword -> thurgoSpecialSword()
            ThurgoTopic.Portrait -> thurgoPortrait()
            ThurgoTopic.Materials -> thurgoMaterials()
            ThurgoTopic.Thanks -> {
                chatPlayer(happy, "Thanks for making that sword for me!")
                chatNpc(happy, "You're welcome - thanks for the pie!")
            }
            ThurgoTopic.Cape -> cape()
        }
        return true
    }

    private suspend fun Dialogue.thurgoImcando(cape: suspend Dialogue.() -> Unit) {
        chatPlayer(quiz, "Are you an Imcando dwarf? I need a special sword.")
        chatNpc(neutral, "I don't talk about that sort of thing anymore. I'm getting old.")
        // TODO(content): [verify:in_game] whether the pie follow-up also shows before Reldo.
        if (stage(player) != STAGE_TOLD_BY_RELDO || player.inv.count(REDBERRY_PIE) == 0) {
            return
        }
        val offer =
            choice2("Would you like a redberry pie?", true, "I'll come back another time.", false)
        if (!offer) {
            chatPlayer(neutral, "I'll come back another time.")
            return
        }
        thurgoFirstPie(cape)
    }

    private suspend fun Dialogue.thurgoFirstPie(cape: suspend Dialogue.() -> Unit) {
        chatPlayer(quiz, "Would you like a redberry pie?")
        mesbox("You see Thurgo's eyes light up.")
        chatNpc(
            happy,
            "I'd never say no to a redberry pie! We Imcando dwarves love them - they're GREAT!",
        )
        if (access.invDel(access.inv, REDBERRY_PIE, 1).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_GAVE_PIE)
        objboxNp(REDBERRY_PIE, zoom = 400, "You hand over the pie.<br> <br> ")
        access.delay(3)
        access.soundSynth(EAT_SYNTH)
        objboxNp(REDBERRY_PIE, zoom = 400, "You hand over the pie.<br>Thurgo eats the pie.<br> ")
        access.delay(6)
        objbox(
            REDBERRY_PIE,
            zoom = 400,
            "You hand over the pie.<br>Thurgo eats the pie.<br>Thurgo pats his stomach.",
        )
        chatNpc(
            happy,
            "By Guthix! THAT was good pie! Anyone who makes pie like THAT has got to be alright!",
        )
        val sword =
            choice2(
                "Can you make a special sword for me?",
                true,
                "What is that cape you're wearing?",
                false,
            )
        if (sword) {
            thurgoSpecialSword()
        } else {
            cape()
        }
    }

    private suspend fun Dialogue.thurgoSpecialSword() {
        chatPlayer(happy, "Can you make a special sword for me?")
        chatNpc(
            neutral,
            "Well, after bringing me my favorite food I guess I should give it a go. What sort " +
                "of sword is it?",
        )
        chatPlayer(
            neutral,
            "I need you to make a sword for one of Falador's knights. He had one which was " +
                "passed down through five generations, but his squire has lost it.",
        )
        chatPlayer(quiz, "So we need an identical one to replace it.")
        chatNpc(
            neutral,
            "A knight's sword eh? Well, I'd need to know exactly how it looked before I could " +
                "make a new one.",
        )
        quest.setQuestStage(access, STAGE_THURGO_NEEDS_PICTURE)
        chatNpc(
            neutral,
            "All the Faladian knights used to have swords with unique designs according to " +
                "their position. Could you bring me a picture or something?",
        )
        chatPlayer(neutral, "I'll go and ask his squire and see if I can find one.")
    }

    private suspend fun Dialogue.thurgoPortrait() {
        chatPlayer(happy, "About that sword...")
        chatNpc(quiz, "Have you got a picture of the sword for me yet?")
        val portraits = player.inv.count(PORTRAIT)
        if (portraits == 0) {
            chatPlayer(sad, "Sorry, not yet.")
            chatNpc(neutral, "Well, come back when you do.")
            return
        }
        chatPlayer(neutral, "I have found a picture of the sword I would like you to make.")
        objbox(PORTRAIT, "You give the portrait to Thurgo. Thurgo studies the portrait.")
        // Every portrait in the inventory is taken.
        if (access.invDel(access.inv, PORTRAIT, portraits).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_GAVE_PORTRAIT)
        chatNpc(
            neutral,
            "You'll need to get me some stuff to make this. I'll need two iron bars to make the " +
                "sword, to start with. I'll also need an ore called blurite.",
        )
        chatNpc(
            neutral,
            "Blurite is useless for making actual weapons, except crossbows, but I'll need some " +
                "as decoration for the hilt.",
        )
        chatNpc(
            neutral,
            "It is a fairly rare ore. The only place I know to get it is under this cliff here, " +
                "but it is guarded by a very powerful ice giant.",
        )
        chatNpc(
            neutral,
            "Most of the rocks in that cliff are pretty useless, and don't contain much of " +
                "anything, but there's DEFINITELY some blurite in there.",
        )
        chatNpc(neutral, "You'll need a little bit of mining experience to be able to find it.")
        chatPlayer(neutral, "Okay. I'll go and find them then.")
    }

    private suspend fun Dialogue.thurgoMaterials() {
        chatPlayer(happy, "Can you make that replacement sword now?")
        chatNpc(quiz, "How are you doing finding those sword materials?")
        val hasOre = player.inv.count(BLURITE_ORE) >= 1
        val hasBars = player.inv.count(IRON_BAR) >= IRON_BARS_NEEDED
        when {
            !hasOre && !hasBars -> {
                chatPlayer(sad, "I don't have any of them yet.")
                chatNpc(
                    neutral,
                    "Well, I need a blurite ore and two iron bars. The only place I know to get " +
                        "blurite is under this cliff here, but it is guarded by a very powerful " +
                        "ice giant.",
                )
            }
            !hasOre -> {
                chatPlayer(sad, "I don't have any blurite ore yet.")
                chatNpc(
                    neutral,
                    "Better go get some then, huh? The only place I know to get it is under " +
                        "this cliff here, but it is guarded by a very powerful ice giant.",
                )
            }
            !hasBars -> {
                chatPlayer(sad, "I don't have two iron bars.")
                chatNpc(neutral, "Better go get some then, huh?")
            }
            else -> {
                chatPlayer(neutral, "I have them right here.")
                if (!makeSword(player)) {
                    return
                }
                objbox(
                    BLURITE_SWORD,
                    "You give the blurite ore and iron bars to Thurgo. Thurgo makes you a sword.",
                )
                chatPlayer(happy, "Thank you very much!")
                chatNpc(happy, "Just remember to call in with more pie some time!")
            }
        }
    }

    private enum class ThurgoTopic {
        Imcando,
        Pie,
        Sword,
        Portrait,
        Materials,
        Thanks,
        Cape,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_TOLD_BY_RELDO = 2
        const val STAGE_GAVE_PIE = 3
        const val STAGE_THURGO_NEEDS_PICTURE = 4
        const val STAGE_LOOKING_FOR_PORTRAIT = 5
        const val STAGE_GAVE_PORTRAIT = 6
        const val STAGE_COMPLETE = 7

        const val PORTRAIT = "obj.knights_portrait"
        const val BLURITE_SWORD = "obj.faladian_sword"
        const val BLURITE_ORE = "obj.blurite_ore"
        const val IRON_BAR = "obj.iron_bar"
        const val REDBERRY_PIE = "obj.redberry_pie"
        const val IRON_BARS_NEEDED = 2

        private const val SMITHING_XP = 12_725.0
        private const val MINING_LEVEL = 10
        private const val RECOMMENDED_COMBAT = 20

        private const val FOUND_THURGO =
            "I found an Imcando Dwarf named Thurgo thanks to information provided by Reldo. " +
                "He wasn't very talkative until I gave him a Redberry pie, which he gobbled up."

        // The cache names the eating sound (2393) after a Doom of Mokhaiotl attack.
        private const val EAT_SYNTH = "synth.dom_burrow_slam"

        /** Swaps one blurite ore and two iron bars for a blurite sword, or changes nothing. */
        fun makeSword(player: Player): Boolean {
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete {
                        from = inv
                        obj = BLURITE_ORE.asRSCM(RSCMType.OBJ)
                        strictCount = 1
                    }
                    delete {
                        from = inv
                        obj = IRON_BAR.asRSCM(RSCMType.OBJ)
                        strictCount = IRON_BARS_NEEDED
                    }
                    insert {
                        into = inv
                        obj = BLURITE_SWORD.asRSCM(RSCMType.OBJ)
                        strictCount = 1
                    }
                }
            return result.success
        }
    }
}
