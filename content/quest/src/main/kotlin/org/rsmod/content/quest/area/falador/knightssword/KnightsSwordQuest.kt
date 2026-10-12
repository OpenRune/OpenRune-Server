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

// https://oldschool.runescape.wiki/w/The_Knight%27s_Sword
// Stages, text and mesanims: live capture, rev 241, 20261012T145648-df49f48@5538-6819.
// Branches the capture didn't take are from
// https://oldschool.runescape.wiki/w/Transcript:The_Knight%27s_Sword
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KnightsSwordQuest :
    QuestScript(
        "quest_knightssword",
        "varp.squire",
        rewards { xp("stat.smithing", SMITHING_XP) },
        // live capture, rev 241, 20261012T145648-df49f48@6819: faladian_sword, zoom 400
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
        "talking to the <col=800000>Squire</col> in the courtyard of the " +
            "<col=800000>White Knights' Castle</col> in <col=800000>Falador</col>."

    // UNVERIFIED: journal wording (written from the walkthrough; no capture of the journal)
    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(player.player)
            description(
                "The <red>Squire</red> in the White Knights' Castle has lost <red>Sir Vyvin's</red> " +
                    "sword, an heirloom made by the <red>Imcando dwarves</red>. I have offered " +
                    "to find someone who can make a new one."
            )
            objective(
                "<red>Reldo</red>, the librarian in <red>Varrock Palace</red>, may know where " +
                    "to find the Imcando dwarves."
            ) {
                visibleWhen { stage == STAGE_STARTED }
            }
            objective(
                "Reldo told me the Imcando dwarves love <red>redberry pie</red>, and that some " +
                    "live near the cliffs on the <red>Asgarnian southern peninsula</red>, south " +
                    "of Port Sarim."
            ) {
                visibleWhen { stage == STAGE_TOLD_BY_RELDO }
            }
            objective(
                "I gave <red>Thurgo</red>, an Imcando dwarf south of Port Sarim, a redberry pie. " +
                    "I should ask him to make the sword."
            ) {
                visibleWhen { stage == STAGE_GAVE_PIE }
            }
            objective(
                "Thurgo needs a picture of the sword before he can make it. The <red>Squire</red> " +
                    "may know where to find one."
            ) {
                visibleWhen { stage == STAGE_THURGO_NEEDS_PICTURE }
            }
            objective(
                "The Squire told me there is a <red>portrait</red> of Sir Vyvin's father in a " +
                    "<red>cupboard</red> in Sir Vyvin's room. I must not let him catch me."
            ) {
                visibleWhen { stage == STAGE_LOOKING_FOR_PORTRAIT }
                hasItem(
                    "knights_portrait",
                    "I have the <red>portrait</red>. I should take it to <red>Thurgo</red>.",
                )
            }
            objective(
                "Thurgo needs <red>two iron bars</red> and a <red>blurite ore</red> to make the " +
                    "sword. Blurite can be mined in the caves under the cliff near his hut."
            ) {
                visibleWhen { stage == STAGE_GAVE_PORTRAIT }
                hasItem(
                    "faladian_sword",
                    "Thurgo has made the sword. I should take it to the <red>Squire</red>.",
                )
            }
        }

    // UNVERIFIED: completed journal wording (written from the walkthrough)
    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Sir Vyvin's squire had lost his master's sword, an heirloom made by the Imcando " +
                    "dwarves. Reldo told me where the last of them might live."
            )
            line(
                "A redberry pie won over Thurgo, an Imcando dwarf south of Port Sarim. With a " +
                    "portrait of the sword from Sir Vyvin's cupboard, two iron bars and some " +
                    "blurite ore, he made a new sword, and Sir Vyvin never noticed the difference."
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
            // UNVERIFIED: the transcript only gives the combined warning; this is its first half.
            lowMining ->
                mesbox(
                    "Before starting this quest, be aware that one or more of your skill levels " +
                        "are lower than what is required to fully complete it."
                )
            // UNVERIFIED: wording taken from The Restless Ghost's combat warning.
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
        if (access.invDel(access.inv, BLURITE_SWORD, 1).failure) {
            return
        }
        mesbox("You give the sword to the squire.")
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
        objbox(
            REDBERRY_PIE,
            zoom = 400,
            "You hand over the pie. Thurgo eats the pie. Thurgo pats his stomach.",
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
        // https://oldschool.runescape.wiki/w/Portrait: every portrait in the inventory is taken.
        if (access.invDel(access.inv, PORTRAIT, portraits).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_GAVE_PORTRAIT)
        objbox(PORTRAIT, "You give the portrait to Thurgo. Thurgo studies the portrait.")
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
