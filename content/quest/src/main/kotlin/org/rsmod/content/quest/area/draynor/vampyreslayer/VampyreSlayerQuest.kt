package org.rsmod.content.quest.area.draynor.vampyreslayer

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class VampyreSlayerQuest :
    QuestScript(
        "quest_vampyreslayer",
        "varp.vampire",
        rewards { xp("stat.attack", ATTACK_XP) },
        ItemRewardDisplay(STAKE, zoom = 400),
        questVarbit = "varbit.vampyre_slayer_progress",
    ) {

    private var Player.metHarlow by intVarBit("varbit.vampyre_slayer_met_harlow")
    private var Player.harlowGivenBeer by intVarBit("varbit.harlow_given_beer")
    private var Player.harlowGivenStake by intVarBit("varbit.harlow_given_stake")

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Vampyre Slayer end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1("npc.dr_harlow") { startDialogue(it.npc) { harlow() } }
        onOpNpc1("npc.maris") { startDialogue(it.npc) { maris() } }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Morgan<col=000080> at his home in " +
            "<col=800000>Draynor Village<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val p = player.player
            val stage = stage(p)
            if (stage == STAGE_STARTED && p.metHarlow == 0) {
                line(STARTED_LOG)
                return@questJournal
            }
            strike(STARTED_LOG.withoutTags())
            when {
                stage == STAGE_STARTED && p.harlowGivenBeer == 0 ->
                    line(
                        "I travelled to the <red>Blue Moon Inn</red> in <red>Varrock</red> and " +
                            "found <red>Dr Harlow</red>. However, he doesn't seem willing to " +
                            "talk to me unless I buy him a <red>Beer</red> first."
                    )
                stage == STAGE_STARTED ->
                    line(
                        "I travelled to the <red>Blue Moon Inn</red> in <red>Varrock</red> and " +
                            "found <red>Dr Harlow</red>. I should see what he can tell me about " +
                            "killing vampyres."
                    )
                else -> line(stakeLog(hasStake = p.inv.count(STAKE) > 0))
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            strike(STARTED_LOG.withoutTags())
            strike(
                "I travelled to the Blue Moon Inn in Varrock and found Dr Harlow. He told me " +
                    "that vampyres are hard to kill due to their ability to regenerate. However, " +
                    "he gave me a Stake and told me I could use it to prevent the regeneration."
            )
            strike(
                " I entered Draynor Manor and found Count Draynor in the basement. However, I " +
                    "was able to use the Stake that Dr Harlow gave me to kill him for good."
            )
        }

    private fun stakeLog(hasStake: Boolean): String {
        val stakeText =
            if (hasStake) {
                "I should make sure to take the <red>Stake</red> with me, along with a " +
                    "<red>Hammer</red>. Most <red>General Stores</red> should stock a " +
                    "<red>Hammer</red>."
            } else {
                "I should make sure to take the <red>Stake</red> with me. If I lose it, I'll " +
                    "need to ask <red>Dr Harlow</red> for another. I'll also need a " +
                    "<red>Hammer</red>. Most <red>General Stores</red> should stock one."
            }
        return "I travelled to the <red>Blue Moon Inn</red> in <red>Varrock</red> and found " +
            "<red>Dr Harlow</red>. He told me that vampyres are hard to kill due to their " +
            "ability to regenerate. However, he gave me a <red>Stake</red> and told me I could " +
            "use it to prevent the regeneration. I'll now need to head to <red>Draynor " +
            "Manor</red>, find <red>Count Draynor</red> and kill him. $stakeText " +
            "<red>Dr Harlow</red> also suggested I carry some <red>Garlic</red> on me. " +
            "<red>Morgan</red> might have some."
    }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun isComplete(player: Player): Boolean = stage(player) >= STAGE_COMPLETE

    /** Morgan before the quest is finished; the Draynor module owns his post-quest lines. */
    suspend fun Dialogue.morgan() {
        if (stage(player) == 0) {
            morganOffer()
            return
        }
        chatNpc(worried, "Adventurer! Has Count Draynor been dealt with yet?")
        if (stage(player) >= STAGE_HAS_STAKE) {
            chatPlayer(
                neutral,
                "I'm still working on it, but I could use some garlic to help me out.",
            )
            chatNpc(
                happy,
                "Ah, yes! I heard vampyres didn't like garlic. I keep some in the cupboard " +
                    "upstairs, just in case Count Draynor comes and tries anything!",
            )
            when (menu(LORE_OPTION to true, "Perfect, thanks." to false)) {
                true -> morganLore()
                false -> {
                    chatPlayer(neutral, "Perfect, thanks.")
                    chatNpc(neutral, "Good luck in Draynor Manor, adventurer.")
                }
            }
            return
        }
        chatPlayer(neutral, "I'm still working on it.")
        chatNpc(worried, "Please hurry! I'm sure Dr Harlow in Varrock's Blue Moon Inn can help.")
        morganVisitMenu(LORE_OPTION)
    }

    private suspend fun Dialogue.morganOffer() {
        chatNpc(shocked, "Could it be? A bold adventurer! Please, you must help us!")
        chatPlayer(quiz, "What is it? What's the problem?")
        chatNpc(
            angry,
            "It's the evil vampyre, Count Draynor! For too long he's terrorised us from his " +
                "manor to the north. Someone finally needs to put a stop to him once and for all!",
        )
        if (player.combatLevel < RECOMMENDED_COMBAT) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            )
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(
                worried,
                "An evil vampyre? I don't think this is the job for me. It sounds far too scary!",
            )
            chatNpc(
                sad,
                "I understand... Hopefully someone else will come along to finally save us from " +
                    "this evil.",
            )
            return
        }
        quest.setQuestStage(access, STAGE_STARTED)
        chatPlayer(neutral, "Sounds like a job for me. Where should I start?")
        chatNpc(
            happy,
            "Oh, thank goodness! I've been hoping this day would come for a long time, so I've " +
                "made sure to do my research. I've heard of a retired vampyre hunter called Dr " +
                "Harlow who lives in Varrock.",
        )
        chatNpc(
            neutral,
            "If you speak to him, I'm sure he'll be able to help. I hear he's a bit of an old " +
                "soak these days, so he spends most of his time in the Blue Moon Inn.",
        )
        morganVisitMenu("What else can you tell me about this vampyre?")
    }

    private suspend fun Dialogue.morganVisitMenu(loreOption: String) {
        when (menu(loreOption to true, "Alright, I'll see about paying him a visit." to false)) {
            true -> morganLore(loreOption)
            false -> {
                chatPlayer(neutral, "Alright, I'll see about paying him a visit.")
                chatNpc(happy, "Thank you, brave adventurer.")
            }
        }
    }

    private suspend fun Dialogue.morganLore(question: String = LORE_OPTION) {
        chatPlayer(quiz, question)
        chatNpc(happy, "All sorts of things! I've studied the legends extensively!")
        chatPlayer(confused, "Wait... legends? What do you mean legends?")
        chatNpc(
            neutral,
            "The legends of Count Draynor! It's said that long ago, he travelled here from dark " +
                "lands in the east. He built Draynor Manor, and from there he began his campaign " +
                "of darkness against our peaceful lands!",
        )
        chatPlayer(
            quiz,
            "Long ago? If this has been going on for so long, why has no one tried to stop him?",
        )
        chatNpc(
            neutral,
            "Many have tried. All have failed. Vampyres are not killed easily. Only those with " +
                "the right skills and equipment have any hope of slaying one.",
        )
        chatPlayer(
            quiz,
            "Like this Dr Harlow? Home come he's never tried to kill this Count Draynor?",
        )
        chatNpc(confused, "I'm... not sure. Perhaps he was always too busy killing other vampyres?")
        chatPlayer(
            shifty,
            "Hmm... Something seems amiss here. Are you sure you're telling me everything?",
        )
        chatNpc(
            worried,
            "Well... it is true that some claim Count Draynor hasn't left his manor for " +
                "generations now.",
        )
        chatPlayer(quiz, "So he's not actually attacked the village?")
        chatNpc(worried, "Er... no. At least, not for a long time! He used to though!")
        chatPlayer(quiz, "When?")
        chatNpc(worried, "Well... not since I've moved here at least.")
        chatPlayer(
            quiz,
            "So if there's no actual danger, why are you so keen for someone to kill this vampyre?",
        )
        chatNpc(
            angry,
            "Because what if he does start attacking again? Are we just meant to live our lives " +
                "constantly fearing the future?",
        )
        chatPlayer(neutral, "It doesn't really sound like anyone is living in fear apart from you.")
        chatNpc(
            angry,
            "More fool them! Count Draynor has cast a shadow over Draynor Village for " +
                "generations. They even named the place after him for Saradomin's sake!",
        )
        chatNpc(
            angry,
            "Even if the people here don't realise it, we will never be at peace until we know " +
                "he's dead!",
        )
        chatPlayer(neutral, "If you're sure... I guess I'll go and see this Dr Harlow.")
        chatNpc(happy, "Thank you, brave adventurer.")
    }

    private suspend fun Dialogue.maris() {
        // TODO(content): [verify:in_game] Maris's lines before and after the quest.
        chatNpc(
            happy,
            "Hello there. You must be the adventurer that my husband mentioned. I wish you good " +
                "luck in your quest.",
        )
        chatPlayer(happy, "Thank you.")
    }

    private suspend fun Dialogue.harlow() {
        val stage = stage(player)
        when {
            stage >= STAGE_HAS_STAKE && stage < STAGE_COMPLETE -> harlowAfterStake()
            stage == STAGE_STARTED && player.harlowGivenBeer == 1 -> harlowAfterBeer()
            stage == STAGE_STARTED && player.metHarlow == 1 -> harlowAskForBeer()
            stage == STAGE_STARTED -> harlowFirstMeeting()
            else -> {
                chatNpc(drunk, "Buy me a drink pleassh...")
                chatPlayer(neutral, "I think you've had enough.")
            }
        }
    }

    private suspend fun Dialogue.harlowFirstMeeting() {
        chatNpc(drunk, "Buy me a drink pleassh...")
        val help =
            menu(
                "I need your help dealing with a vampyre." to true,
                "I think you've had enough." to false,
            )
        if (!help) {
            chatPlayer(neutral, "I think you've had enough.")
            return
        }
        chatPlayer(neutral, "I need your help dealing with a vampyre.")
        player.metHarlow = 1
        chatNpc(drunk, "A vampyre you shhay...?")
        chatPlayer(neutral, "Not just any vampyre. Count Draynor.")
        chatNpc(drunk, "Draynor? Well, buy me a beer firsht...")
        chatPlayer(quiz, "Are you sure you've not had enough?")
        chatNpc(drunk, "Huh? No, I don't think ssho. Now, buy ush a beer.")
    }

    private suspend fun Dialogue.harlowAskForBeer() {
        chatNpc(drunk, "Have yoush got me a beer?")
        val hasBeer = player.inv.count(BEER) > 0
        val options = buildList {
            if (hasBeer) add("Yes, here you go." to HarlowTopic.GiveBeer)
            else add("I'll go and get you one." to HarlowTopic.GetBeer)
            add("No. I think you've had enough." to HarlowTopic.Refuse)
        }
        when (menu(options)) {
            HarlowTopic.GetBeer -> {
                chatPlayer(neutral, "I'll go and get you one.")
                chatNpc(drunk, "Cheersh, matey...")
            }
            HarlowTopic.GiveBeer -> {
                chatPlayer(neutral, "Yes, here you go.")
                if (access.invDel(access.inv, BEER, 1).failure) {
                    return
                }
                player.harlowGivenBeer = 1
                objbox(BEER, "You give a beer to Dr Harlow.")
                chatNpc(drunk, "Cheersh, matey...")
                harlowAfterBeer()
            }
            else -> {
                chatPlayer(neutral, "No. I think you've had enough.")
                chatNpc(drunk, "No help for yoush then.")
            }
        }
    }

    private suspend fun Dialogue.harlowAfterBeer() {
        chatPlayer(neutral, "Now, about Count Draynor...")
        chatNpc(
            drunk,
            "Yesh, Count Draynor! The evil nashty vampyre that no one's ever sheen! Every nowsh " +
                "and then, some adventurer... theysh go and try to kill him, but none of 'em " +
                "ever comesh back.",
        )
        chatNpc(
            drunk,
            "You want to havesh a go? Then don't be likesh them! Be prepared! Vampyres... " +
                "Theysh hard to kill. Some... maybe imposhible.",
        )
        chatPlayer(quiz, "So how do I make sure I'm prepared?")
        chatNpc(
            drunk,
            "Most vampyres regenerate. Yoush need to stop them. I knowsh a few ways, but the " +
                "easiest ish a stake. Here, You can havesh thish one.",
        )
        if (!giveStake()) {
            return
        }
        chatNpc(
            drunk,
            "Takesh that to Draynor Manor. Find the vampyre inshide and show him what for! When " +
                "hesh weak, use a hammer to drive the stake in! Mosht general stores have them.",
        )
        harlowGarlicAdvice()
        harlowQuestions(firstOption = null)
    }

    private suspend fun Dialogue.harlowAfterStake() {
        chatNpc(drunk, "Yoush back! Killed that vampyre yet?")
        if (player.inv.count(STAKE) == 0) {
            chatPlayer(sad, "No. I lost that stake you gave me.")
            chatNpc(drunk, "Oh, hangsh on then...")
            if (!giveStake()) {
                return
            }
            chatNpc(
                drunk,
                "Good job I've gotsh some spares. Now, takesh that to Draynor Manor. Find the " +
                    "vampyre inshide and show him what for! When hesh weak, use a hammer to " +
                    "drive the stake in! Mosht general stores have them.",
            )
            harlowGarlicAdvice()
        }
        harlowQuestions(firstOption = "What do I need to do again?")
    }

    private suspend fun Dialogue.giveStake(): Boolean {
        if (access.invAdd(access.inv, STAKE, 1).failure) {
            mesbox("You don't have enough inventory space for the stake.")
            return false
        }
        player.harlowGivenStake = 1
        if (stage(player) < STAGE_HAS_STAKE) {
            quest.setQuestStage(access, STAGE_HAS_STAKE)
        }
        objbox(STAKE, "Dr Harlow hands you a stake.")
        return true
    }

    private suspend fun Dialogue.harlowGarlicAdvice() {
        chatNpc(
            drunk,
            "Oh, and yoush should take some garlic with you as well. Vampyres don't likesh garlic.",
        )
        chatPlayer(quiz, "Garlic? Hmm... I'll see if Morgan knows where I can get some.")
    }

    private suspend fun Dialogue.harlowQuestions(firstOption: String?) {
        var next = firstOption
        while (true) {
            val options = buildList {
                if (next != null) add(next to HarlowTopic.Again)
                add("You said no one's ever seen Count Draynor?" to HarlowTopic.Seen)
                add("So you were once a proper vampyre hunter?" to HarlowTopic.Hunter)
                val leave = if (firstOption == null) LEAVE else "Not yet. $LEAVE"
                add(leave to HarlowTopic.Leave)
            }
            when (menu(options)) {
                HarlowTopic.Again -> {
                    chatPlayer(quiz, "What do I need to do again?")
                    chatNpc(
                        drunk,
                        "Go to Draynor Manor. Find the vampyre inshide and show him what for! " +
                            "When hesh weak, use a hammer to drive that stake I gave you in! " +
                            "Mosht general stores have them.",
                    )
                    harlowGarlicAdvice()
                }
                HarlowTopic.Seen -> {
                    harlowSeen()
                    next = "What do I need to do again?"
                }
                HarlowTopic.Hunter -> {
                    harlowHunter()
                    next = "What do I need to do again?"
                }
                else -> {
                    chatPlayer(neutral, options.last().first)
                    chatNpc(drunk, "Good luck, matey...")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.harlowSeen() {
        chatPlayer(quiz, "You said no one's ever seen Count Draynor?")
        chatNpc(drunk, "No, butsh every now and then someonesh tried to kill him.")
        chatPlayer(
            quiz,
            "But they've never come back? Meaning someone or something killed them most likely. " +
                "Presumably Count Draynor?",
        )
        chatNpc(drunk, "Yesh, I'd say so.")
        chatPlayer(quiz, "So why does he never leave his manor?")
        chatNpc(drunk, "Who knowsh. Most likely he's grown too weak to do ssho.")
        chatPlayer(quiz, "Weak? So he shouldn't be too hard to kill then?")
        chatNpc(
            drunk,
            "Even a weak vampyre ish very dangerous. Just go ashk those other adventurers thatsh " +
                "never came back.",
        )
    }

    private suspend fun Dialogue.harlowHunter() {
        chatPlayer(quiz, "So you were once a proper vampyre hunter?")
        chatNpc(
            drunk,
            "Yesh! I travelled their homeland to the easht. Morytania! I killed lotsh of " +
                "vampyres over there!",
        )
        chatPlayer(quiz, "What was the most dangerous vampyre you killed?")
        chatNpc(drunk, "Dangerous? Oh, well... Ish never killed one of the really dangerous ones.")
        chatPlayer(quiz, "Why not?")
        chatNpc(drunk, "Can't. Theysh too strong. Nothing cansh hurt them.")
        chatPlayer(worried, "Oh... But what if Count Draynor's like that?")
        chatNpc(
            drunk,
            "Yoush don't get many vampyres around here. Pretty much none, actshually. There's " +
                "speshial magic that stops them coming over here. Any that are here... theysh " +
                "weak... and trapped.",
        )
        chatPlayer(quiz, "So how come you've never tried to kill him yourself?")
        chatNpc(
            drunk,
            "Why bother? Hesh not killed anyone in a long time, so no one really caresh about " +
                "him that much, apart from the odd adventurer thatsh looking to prove themshelves.",
        )
        chatPlayer(neutral, "I see. Fair enough.")
    }

    fun ProtectedAccess.completeBySlaying() {
        if (stage(player) in STAGE_STARTED until STAGE_COMPLETE) {
            quest.completeQuest(this)
        }
    }

    private enum class HarlowTopic {
        GetBeer,
        GiveBeer,
        Refuse,
        Again,
        Seen,
        Hunter,
        Leave,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_HAS_STAKE = 2
        const val STAGE_COMPLETE = 3

        const val STAKE = "obj.stake"
        const val HAMMER = "obj.hammer"
        const val GARLIC = "obj.garlic"
        const val BEER = "obj.beer"

        private const val ATTACK_XP = 4_825.0
        private const val RECOMMENDED_COMBAT = 20

        private fun String.withoutTags(): String = replace(TAG_REGEX, "")

        private val TAG_REGEX = Regex("<[^>]+>")

        private const val LEAVE = "I'd best get going."

        private const val LORE_OPTION = "What else can you tell me about Count Draynor?"

        private const val STARTED_LOG =
            "I spoke to <red>Morgan</red> at his home in <red>Draynor Village</red>. He told me " +
                "about <red>Count Draynor</red>, a vampyre living in <red>Draynor Manor</red> " +
                "north of the village. At his request, I agreed to try and kill <red>Count " +
                "Draynor</red>. <red>Morgan</red> suggested I begin by seeking out <red>Dr " +
                "Harlow</red>, a retired vampyre hunter, at the <red>Blue Moon Inn</red> in " +
                "<red>Varrock</red>."
    }
}
