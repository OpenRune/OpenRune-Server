package org.rsmod.content.quest.area.varrock.dragonslayer

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.Quest
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DragonSlayerQuest :
    QuestScript(
        "quest_dragonslayer1",
        "varp.dragonquest",
        rewards {
            xp("stat.strength", REWARD_XP)
            xp("stat.defence", REWARD_XP)
            extra("Ability to wear rune platebodies,")
            extra("green d'hide bodies and")
            extra("dragon platebodies")
        },
        ItemRewardDisplay(ELVARGS_HEAD, zoom = 400),
        questVarbit = "varbit.dragon_slayer_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Dragon Slayer I end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1(GUILDMASTER) { startDialogue(it.npc) { guildmaster() } }
    }

    override fun subTitle(): String =
        "speaking to the <col=800000>Guildmaster<col=000080> in the <col=800000>Champions' " +
            "Guild<col=000080>, south-west of Varrock."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val p = player.player
            val stage = stage(p)
            strike("The Guildmaster of the Champions' Guild said I could earn")
            strike("the right to wear rune armour if I went on a quest for")
            strike("Oziach, who makes the armour.")
            if (stage == STAGE_STARTED) {
                line("I should speak to <red>Oziach</red>, who lives by the cliffs to the")
                line("west of <red>Edgeville</red>.")
                return@questJournal
            }
            strike("I spoke to Oziach in Edgeville. He told me to slay the")
            strike("dragon of Crandor island.")
            if (stage == STAGE_TASK && p.explainedElvarg == 0) {
                line("I should return to the <red>Champions' Guild Guildmaster</red> for")
                line("more detailed instructions.")
                return@questJournal
            }
            if (stage < STAGE_NED_HIRED) {
                strike("The Champions' Guild Guildmaster gave me more detailed")
                strike("instructions.")
                line("To defeat the dragon I will need to find a <red>map</red> to Crandor, a")
                line("<red>ship</red>, a <red>captain</red> to take me there and some kind of")
                line("<red>protection</red> against the dragon's breath.")
                if (p.ownsAny(MELZAR_PIECE, CRANDOR_MAP)) {
                    strike("I found the piece of the map that was hidden in Melzar's")
                    strike("Maze.")
                } else {
                    line("One-third of the map is in <red>Melzar's Maze</red>, near")
                    line("<red>Rimmington</red>.")
                }
                if (p.ownsAny(THALZAR_PIECE, CRANDOR_MAP)) {
                    strike("I found the piece of the map that was hidden beneath Ice")
                    strike("Mountain.")
                } else {
                    line("One-third of the map is hidden and only the <red>Oracle</red> on <red>Ice")
                    line("Mountain</red> will know where it is.")
                }
                if (p.ownsAny(LOZAR_PIECE, CRANDOR_MAP)) {
                    strike("I found the piece of the map that the goblin, Wormbrain,")
                    strike("stole.")
                } else {
                    line("One-third of the map was stolen by a <red>goblin</red> from the")
                    line("<red>Goblin Village</red>.")
                }
                if (p.ownsAny(ANTI_DRAGON_SHIELD)) {
                    strike("The Duke of Lumbridge gave me an anti-dragonbreath")
                    strike("shield.")
                } else {
                    line("I should ask the <red>Duke of Lumbridge</red> for an <red>anti-")
                    line("dragonbreath shield</red>.")
                }
                when {
                    stage < STAGE_SHIP_BOUGHT ->
                        line("I should see if there is a <red>ship</red> for sale in <red>Port Sarim</red>.")
                    p.shipFullyFixed == 0 -> {
                        strike("I bought a ship in Port Sarim called the Lady Lumbridge.")
                        line("I need to repair the hole in bottom of the ship.")
                    }
                    else -> {
                        strike("I bought a ship in Port Sarim called the Lady Lumbridge.")
                        strike("I have repaired my ship using wooden planks and steel")
                        strike("nails.")
                        line("I need to find a <red>captain</red> to sail her to <red>Crandor</red>.")
                    }
                }
                return@questJournal
            }
            strike("The Champions' Guild Guildmaster told me I had to find")
            strike("three pieces of a map to Crandor, a ship, a captain to take")
            strike("me there and a shield to protect me from the dragon's")
            strike("breath.")
            strike("I found the piece of the map that was hidden in Melzar's")
            strike("Maze.")
            strike("I found the piece of the map that was hidden beneath Ice")
            strike("Mountain.")
            strike("I found the piece of the map that the goblin, Wormbrain,")
            strike("stole.")
            strike("The Duke of Lumbridge gave me an anti-dragonbreath")
            strike("shield.")
            if (stage == STAGE_NED_HIRED) {
                strike("I bought a ship in Port Sarim called the Lady Lumbridge")
                strike("I have repaired my ship using wooden planks and steel")
                strike("nails.")
                strike("Captain Ned from Draynor Village has agreed to sail the")
                strike("ship to Crandor for me.")
                line("I should go to my ship in <red>Port Sarim</red> and set sail for")
                line("<red>Crandor</red>!")
                return@questJournal
            }
            if (p.foundSecretDoor == 1) {
                strike("I have found a secret passage leading from Karamja to")
                strike("Crandor, so I no longer need to worry about finding a")
                strike("seaworthy ship and captain to take me there.")
            }
            if (stage == STAGE_CRANDOR) {
                line("Now all I need to do is kill the <red>dragon</red>!")
                return@questJournal
            }
            strike("I sailed to Crandor and killed the dragon.")
            line("I should return to <red>Oziach</red> in <red>Edgeville</red> and tell him I have")
            line("slain the dragon.")
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            strike("The Guildmaster of the Champions' Guild said I could earn")
            strike("the right to wear rune armour if I went on a quest for")
            strike("Oziach, who makes the armour.")
            strike("I spoke to Oziach in Edgeville. He told me to slay the")
            strike("dragon of Crandor island.")
            strike("The Champions' Guild Guildmaster told me I had to find")
            strike("three pieces of a map to Crandor, a ship, a captain to take")
            strike("me there and a shield to protect me from the dragon's")
            strike("breath.")
            strike("I found the three pieces of the map, a ship and a captain,")
            strike("and the Duke of Lumbridge gave me an anti-dragonbreath")
            strike("shield.")
            strike("I sailed to Crandor and killed the dragon. I am a true")
            strike("champion and have proved myself worthy to wear rune")
            strike("platemail!")
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun isComplete(player: Player): Boolean = stage(player) >= STAGE_COMPLETE

    fun setStage(access: ProtectedAccess, stage: Int) {
        quest.setQuestStage(access, stage)
    }

    fun complete(access: ProtectedAccess) {
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.guildmaster() {
        chatNpc(happy, "Greetings!")
        val stage = stage(player)
        val questOption =
            when {
                stage == 0 || stage == STAGE_STARTED -> "Can I have a quest?" to GuildTopic.Quest
                stage == STAGE_TASK && player.explainedElvarg == 0 ->
                    "I talked to Oziach..." to GuildTopic.TalkedToOziach
                stage in STAGE_TASK until STAGE_ELVARG_SLAIN ->
                    "About my quest to kill the dragon..." to GuildTopic.AboutQuest
                stage == STAGE_ELVARG_SLAIN -> "I killed the dragon!" to GuildTopic.Killed
                else -> null
            }
        val options = listOfNotNull("What is this place?" to GuildTopic.Place, questOption)
        when (menu(options)) {
            GuildTopic.Place -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "This is the Champions' Guild. Only adventurers who have proved themselves " +
                        "worthy by gaining influence from quests are allowed in here.",
                )
            }
            GuildTopic.Quest -> if (stage == 0) offerQuest() else alreadyOnQuest()
            GuildTopic.TalkedToOziach -> talkedToOziach()
            GuildTopic.AboutQuest -> aboutQuest()
            GuildTopic.Killed -> {
                chatPlayer(happy, "I killed the dragon!")
                chatNpc(shocked, "Really? That's amazing!")
                chatNpc(
                    happy,
                    "You are a true hero. There's no way that Oziach can refuse to sell you rune " +
                        "platemail now. You should tell him at once!",
                )
            }
        }
    }

    private suspend fun Dialogue.offerQuest() {
        chatPlayer(quiz, "Can I have a quest?")
        chatNpc(happy, "Aha!")
        chatNpc(
            happy,
            "Yes! A mighty and perilous quest fit only for the most powerful champions! And, at " +
                "the end of it, you will earn the right to wear the legendary rune platebody!",
        )
        recommendedLevelsWarning()
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Actually, I'll give it a miss.")
            return
        }
        setStage(access, STAGE_STARTED)
        chatPlayer(quiz, "So, what is this quest?")
        chatNpc(
            neutral,
            "You'll have to speak to Oziach, the maker of rune armour. He sets the quests that " +
                "champions must complete in order to wear it.",
        )
        chatNpc(
            neutral,
            "Oziach lives in a hut, by the cliffs to the west of Edgeville. He can be a " +
                "little...odd...sometimes, though.",
        )
    }

    private suspend fun Dialogue.recommendedLevelsWarning() {
        val lowCombat = player.combatLevel < RECOMMENDED_COMBAT
        val lowMagic = player.statBase("stat.magic") < RECOMMENDED_MAGIC
        val text =
            when {
                lowCombat && lowMagic ->
                    "Before starting this quest, be aware that your combat level is lower than " +
                        "the recommended level of $RECOMMENDED_COMBAT. One or more of your skill " +
                        "levels are also lower than recommended."
                lowCombat ->
                    "Before starting this quest, be aware that your combat level is lower than " +
                        "the recommended level of $RECOMMENDED_COMBAT."
                lowMagic ->
                    "Before starting this quest, be aware that one or more of your skill levels " +
                        "are lower than recommended."
                else -> return
            }
        player.statWarningShown = 1
        mesbox(text)
    }

    private suspend fun Dialogue.alreadyOnQuest() {
        chatPlayer(quiz, "Can I have a quest?")
        chatNpc(
            neutral,
            "You're already on a quest for me, if I recall correctly. Have you talked to Oziach yet?",
        )
        chatPlayer(neutral, "No, not yet.")
        chatNpc(
            neutral,
            "Well, he's the only one who can grant you the right to wear rune platemail. He lives " +
                "in a hut, by the cliffs west of Edgeville.",
        )
        chatPlayer(neutral, "Okay, I'll go and talk to him.")
    }

    private suspend fun Dialogue.talkedToOziach() {
        chatPlayer(neutral, "I talked to Oziach and he gave me a quest.")
        chatNpc(quiz, "Oh? What did he tell you to do?")
        chatPlayer(neutral, "Defeat the dragon of Crandor.")
        chatNpc(shocked, "The dragon of Crandor?")
        chatPlayer(worried, "Um, yes...")
        chatNpc(neutral, "Goodness, he hasn't given you an easy job, has he?")
        elvargStory()
        questIntro()
        questions()
    }

    private suspend fun Dialogue.aboutQuest() {
        chatPlayer(quiz, "About my quest to kill the dragon...")
        questIntro()
        questions()
    }

    private suspend fun Dialogue.elvargStory() {
        chatPlayer(quiz, "What's so special about this dragon?")
        chatNpc(
            neutral,
            "Thirty years ago, Crandor was a thriving community with a great tradition of mages " +
                "and adventurers. Many Crandorians even earned the right to be part of the " +
                "Champions' Guild!",
        )
        chatNpc(
            neutral,
            "One of their adventurers went too far, however. He descended into the volcano in " +
                "the centre of Crandor and woke the dragon Elvarg.",
        )
        chatNpc(
            neutral,
            "He must have fought valiantly against the dragon because they say that, to this " +
                "day, she has a scar down her side,",
        )
        chatNpc(
            sad,
            "but the dragon still won the fight. She emerged and laid waste to the whole of " +
                "Crandor with her fire breath!",
        )
        chatNpc(
            sad,
            "Some refugees managed to escape in fishing boats. They landed on the coast, north of " +
                "Rimmington, and set up camp but the dragon followed them and burned the camp to " +
                "the ground.",
        )
        chatNpc(
            neutral,
            "Out of all the people of Crandor there were only three survivors: a trio of wizards " +
                "who used magic to escape. Their names were Thalzar, Lozar and Melzar.",
        )
        player.explainedElvarg = 1
    }

    private suspend fun Dialogue.questIntro() {
        chatNpc(
            neutral,
            "If you're serious about taking on Elvarg, first you'll need to get to Crandor, and to " +
                "my knowledge, the only way to do that is by sea.",
        )
        chatPlayer(quiz, "So I just sail over there? Seems easy enough.")
        chatNpc(
            neutral,
            "If only that were true. The island is surrounded by dangerous underwater reefs. " +
                "You'll need a map to guide you through them and a ship capable of doing so.",
        )
        chatNpc(
            neutral,
            "And then of course there's the dragon herself. Once you reach Crandor, you'll need " +
                "some kind of protection against her breath.",
        )
    }

    private suspend fun Dialogue.questions() {
        while (true) {
            val topic =
                menu(
                    "Where can I find a map to Crandor?" to GuildQuestion.Map,
                    "What kind of ship do I need?" to GuildQuestion.Ship,
                    "How can I protect myself from the dragon's breath?" to GuildQuestion.Shield,
                    "What's so special about this dragon?" to GuildQuestion.Elvarg,
                    "Okay, I'll get going!" to GuildQuestion.Leave,
                )
            when (topic) {
                GuildQuestion.Map -> mapQuestions()
                GuildQuestion.Ship -> shipAnswer()
                GuildQuestion.Shield -> {
                    chatPlayer(quiz, "How can I protect myself from the dragon's breath?")
                    player.shieldInstructions = 0
                    chatNpc(
                        neutral,
                        "That part shouldn't be too difficult, actually. I believe the Duke of " +
                            "Lumbridge has a special shield in his armoury that is enchanted " +
                            "against dragon's breath.",
                    )
                }
                GuildQuestion.Elvarg -> elvargStory()
                GuildQuestion.Leave -> {
                    chatPlayer(neutral, "Okay, I'll get going!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mapQuestions() {
        chatPlayer(quiz, "Where can I find a map to Crandor?")
        chatNpc(
            neutral,
            "After what happened, people were afraid of enraging the dragon. Most maps to the " +
                "island were destroyed, but I have heard that one survived. It was held by " +
                "Melzar, Thalzar and Lozar.",
        )
        chatNpc(
            neutral,
            "However, the three apparently split the map into three pieces, which each of them " +
                "taking one piece.",
        )
        when (
            menu(
                "Where is Melzar's map piece?" to MapQuestion.Melzar,
                "Where is Thalzar's map piece?" to MapQuestion.Thalzar,
                "Where is Lozar's map piece?" to MapQuestion.Lozar,
            )
        ) {
            MapQuestion.Melzar -> melzarAnswer()
            MapQuestion.Thalzar -> {
                chatPlayer(quiz, "Where is Thalzar's map piece?")
                player.oracleInstructions = 0
                chatNpc(
                    neutral,
                    "Thalzar was the most paranoid of the three wizards. He hid his map piece and " +
                        "took the secret of its location to his grave.",
                )
                chatNpc(
                    neutral,
                    "I don't think you'd be able to find out where it is by ordinary means. " +
                        "You'll need to talk to the Oracle on Ice Mountain.",
                )
            }
            MapQuestion.Lozar -> {
                chatPlayer(quiz, "Where is Lozar's map piece?")
                player.wormbrainInstructions = 0
                chatNpc(
                    neutral,
                    "A few weeks ago, I'd have told you to speak to Lozar herself, in her house " +
                        "across the river from Lumbridge.",
                )
                chatNpc(
                    sad,
                    "Unfortunately, goblin raiders killed her and stole everything. One of the " +
                        "goblins from the Goblin Village probably has the map piece now.",
                )
            }
        }
    }

    private suspend fun Dialogue.melzarAnswer() {
        chatPlayer(quiz, "Where is Melzar's map piece?")
        player.melzarInstructions = 0
        chatNpc(
            neutral,
            "Melzar built a castle on the site of the Crandorian refugee camp, north of " +
                "Rimmington. He's locked himself in there and no one's seen him for years.",
        )
        chatNpc(
            neutral,
            "The inside of his castle is like a maze, and is populated by undead monsters. Maybe, " +
                "if you could get all the way through the maze, you could find his piece of the map.",
        )
        chatNpc(
            neutral,
            "Adventurers sometimes go in there to prove themselves, so I can give you this key to " +
                "Melzar's Maze.",
        )
        if (player.ownsAny(MAZE_KEY)) {
            return
        }
        if (access.invAdd(access.inv, MAZE_KEY, 1).failure) {
            mesbox("You don't have enough inventory space for the key.")
            return
        }
        objbox(MAZE_KEY, "The Guildmaster hands you a key.")
    }

    private suspend fun Dialogue.shipAnswer() {
        chatPlayer(quiz, "What kind of ship do I need?")
        player.shipInstructions = 0
        chatNpc(
            neutral,
            "Quite a specific one I'm afraid. Even with a map, very few ships will be able to " +
                "navigate the reefs around the island. You're going to need one of Crandorian design.",
        )
        chatNpc(
            neutral,
            "If there's still one in existence, it's probably in Port Sarim. However, a ship will " +
                "also need a captain, and I'm not sure where you'd find one willing to sail to Crandor!",
        )
        if (hasCompletedQuest(player, PANDEMONIUM)) {
            chatPlayer(
                quiz,
                "Could I not just captain it myself? I have some experience in that area.",
            )
            chatNpc(
                neutral,
                "I suppose you could, but given you have a dragon to be dealing with, I'd suggest " +
                    "getting someone else to worry about the captaining for you.",
            )
        }
    }

    /** Oziach until the quest is complete; his shop and post-quest lines stay in Edgeville. */
    suspend fun Dialogue.oziach(npc: Npc) {
        when (stage(player)) {
            0,
            STAGE_STARTED -> oziachFairDay()
            STAGE_ELVARG_SLAIN -> oziachFinish()
            else -> {
                chatNpc(shifty, "Have ye slayed that dragon yet?")
                chatPlayer(worried, "Um... no.")
                chatNpc(angry, "Be off with ye then.")
            }
        }
    }

    private suspend fun Dialogue.oziachFairDay() {
        chatNpc(shifty, "Aye, 'tis a fair day my friend.")
        val options = buildList {
            if (stage(player) == STAGE_STARTED) {
                add("Can you sell me a rune platebody?" to OziachTopic.Platebody)
            }
            add("I'm not your friend." to OziachTopic.NotFriend)
            add("Yes, it's a very nice day." to OziachTopic.NiceDay)
        }
        when (menu(options)) {
            OziachTopic.Platebody -> oziachPlatebody()
            OziachTopic.NotFriend -> {
                chatPlayer(angry, "I'm not your friend.")
                chatNpc(angry, "I'm surprised if you're anyone's friend with those kind of manners.")
            }
            OziachTopic.NiceDay -> {
                chatPlayer(neutral, "Yes, it's a very nice day.")
                chatNpc(neutral, "Aye, may the gods walk by yer side. Now leave me alone.")
            }
        }
    }

    private suspend fun Dialogue.oziachPlatebody() {
        chatPlayer(quiz, "Can you sell me a rune platebody?")
        chatNpc(shifty, "So, how does thee know I 'ave some?")
        val guildmaster =
            menu(
                "The Guildmaster of the Champions' Guild told me." to true,
                "I am a master detective." to false,
            )
        if (guildmaster) {
            chatPlayer(neutral, "The Guildmaster of the Champions' Guild told me.")
            chatNpc(
                angry,
                "Yes, I suppose he would, wouldn't he? He's always sending you fancy-pants " +
                    "'heroes' up to bother me. Telling me I'll give them a quest or sommat like that.",
            )
            chatNpc(
                angry,
                "Well, I'm not going to let just anyone wear my rune platemail. It's only for " +
                    "heroes. So, leave me alone.",
            )
        } else {
            chatPlayer(happy, "I am a master detective.")
            if (hasCompletedQuest(player, MURDER_MYSTERY)) {
                chatPlayer(happy, "I solved the Sinclair murder mystery by myself!")
                chatNpc(neutral, "Never 'eard of yer.")
            }
            chatNpc(
                angry,
                "Well, however you found out about it, I'm not going to let just anyone wear my " +
                    "rune platemail. It's only for heroes. So leave me alone.",
            )
        }
        val wantsQuest =
            menu(
                "I thought you were going to give me a quest." to true,
                "That's a pity, I'm not a hero." to false,
            )
        if (!wantsQuest) {
            chatPlayer(sad, "That's a pity, I'm not a hero.")
            chatNpc(laugh, "Aye, I ken tell!")
            return
        }
        chatPlayer(quiz, "I thought you were going to give me a quest.")
        chatNpc(sad, "*Sigh*")
        chatNpc(neutral, "All right, I'll give ye a quest. I'll let ye wear my rune platemail if ye...")
        chatNpc(angry, "Slay the dragon of Crandor!")
        val accept =
            menu(
                "A dragon, that sounds like fun." to true,
                "I may be a champion, but I don't think I'm up to dragon-killing yet." to false,
            )
        if (!accept) {
            chatPlayer(
                worried,
                "I may be a champion, but I don't think I'm up to dragon-killing yet.",
            )
            chatNpc(angry, "Yes, I can understand that. Yer a coward.")
            return
        }
        acceptTask()
        chatPlayer(happy, "A dragon, that sounds like fun!")
        chatNpc(
            laugh,
            "Hah, yes, you are a typical reckless adventurer, aren't you? Now go kill the dragon " +
                "and get out of my face.",
        )
        chatPlayer(quiz, "But how can I defeat the dragon?")
        chatNpc(
            angry,
            "Go talk to the Guildmaster in the Champions' Guild. He'll help ye out if yer so keen " +
                "on doing a quest. I'm not going to be handholding any adventurers.",
        )
    }

    /** The five Guildmaster questions open together; each answer clears its own bit. */
    private fun Dialogue.acceptTask() {
        player.melzarInstructions = 1
        player.oracleInstructions = 1
        player.wormbrainInstructions = 1
        player.shipInstructions = 1
        player.shieldInstructions = 1
        setStage(access, STAGE_TASK)
    }

    private suspend fun Dialogue.oziachFinish() {
        chatNpc(shifty, "Have ye slayed that dragon yet?")
        chatPlayer(happy, "Yes, I have!")
        if (player.inv.count(ELVARGS_HEAD) > 0) {
            chatPlayer(happy, "I have its head here.")
        }
        chatNpc(shocked, "You actually did it?")
        chatNpc(sad, "I underestimated ye, adventurer. I apologise.")
        chatNpc(happy, "Yer a true hero, and I'd be happy to sell ye rune platebodies.")
        complete(access)
    }

    fun isSeekingCrandor(player: Player): Boolean =
        stage(player) in STAGE_TASK until STAGE_ELVARG_SLAIN

    fun isCollectingMap(player: Player): Boolean =
        stage(player) in STAGE_TASK until STAGE_NED_HIRED

    fun canAskOracle(player: Player): Boolean =
        isCollectingMap(player) && player.oracleInstructions == 0

    suspend fun Dialogue.oracleRiddle() {
        chatPlayer(quiz, "I seek a piece of the map to the island of Crandor.")
        chatNpc(
            neutral,
            "The map's behind a door below, but entering is rather tough. This is what you need " +
                "to know: You must use the following stuff.",
        )
        player.secretTold = 1
        chatNpc(
            neutral,
            "First, a drink used by a mage. Next, some worm string, changed to sheet. Then, a " +
                "small crustacean cage. Last, a bowl that's not seen heat.",
        )
    }

    fun canAskGenerals(player: Player): Boolean =
        isCollectingMap(player) &&
            player.wormbrainInstructions == 0 &&
            !player.ownsAny(LOZAR_PIECE, CRANDOR_MAP)

    fun markAskedGenerals(player: Player) {
        player.askedGenerals = 1
    }

    fun canPassMagicDoor(player: Player): Boolean =
        isComplete(player) ||
            (player.usedSilk == 1 &&
                player.usedLobsterPot == 1 &&
                player.usedBowl == 1 &&
                player.usedMindBomb == 1)

    /**
     * Puts [obj] into Thalzar's door. Returns true once the fourth item is in, so the caller
     * opens the door straight away.
     */
    fun ProtectedAccess.putInMagicDoor(obj: String): Boolean {
        if (!isCollectingMap(player) || isComplete(player)) {
            mes("Nothing interesting happens.")
            return false
        }
        val (alreadyUsed, message) =
            when (obj) {
                SILK -> (player.usedSilk == 1) to "You put the silk into the opening in the door."
                LOBSTER_POT ->
                    (player.usedLobsterPot == 1) to "You put the Lobster pot into the opening in the door."
                UNFIRED_BOWL ->
                    (player.usedBowl == 1) to "You put the unfired bowl into the opening in the door."
                else ->
                    (player.usedMindBomb == 1) to "You pour the Wizard's Mind Bomb into the opening in the door."
            }
        if (alreadyUsed) {
            mes("Nothing interesting happens.")
            return false
        }
        if (invDel(inv, obj, 1).failure) {
            return false
        }
        when (obj) {
            SILK -> player.usedSilk = 1
            LOBSTER_POT -> player.usedLobsterPot = 1
            UNFIRED_BOWL -> player.usedBowl = 1
            else -> player.usedMindBomb = 1
        }
        mes(message)
        return canPassMagicDoor(player)
    }

    /** Searching Thalzar's open chest. */
    suspend fun ProtectedAccess.searchThalzarChest(): Boolean {
        if (!isCollectingMap(player) || player.ownsAny(THALZAR_PIECE, CRANDOR_MAP)) {
            return false
        }
        if (invAdd(inv, THALZAR_PIECE, 1).failure) {
            mes("You don't have enough inventory space.")
            return true
        }
        startDialogue { objbox(THALZAR_PIECE, "You find a map piece in the chest.") }
        return true
    }

    private fun hasCompletedQuest(player: Player, key: String): Boolean =
        Quest.get(key)?.isQuestCompleted(player) == true

    private enum class GuildTopic {
        Place,
        Quest,
        TalkedToOziach,
        AboutQuest,
        Killed,
    }

    private enum class GuildQuestion {
        Map,
        Ship,
        Shield,
        Elvarg,
        Leave,
    }

    private enum class MapQuestion {
        Melzar,
        Thalzar,
        Lozar,
    }

    private enum class OziachTopic {
        Platebody,
        NotFriend,
        NiceDay,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_TASK = 2
        const val STAGE_SHIP_BOUGHT = 3
        const val STAGE_SHIP_REPAIRED = 6
        const val STAGE_NED_HIRED = 7
        const val STAGE_CRANDOR = 8
        const val STAGE_ELVARG_SLAIN = 9
        const val STAGE_COMPLETE = 10

        const val GUILDMASTER = "npc.guildmaster"

        const val MAZE_KEY = "obj.melzarkey"
        const val MELZAR_PIECE = "obj.mappart1"
        const val LOZAR_PIECE = "obj.mappart2"
        const val THALZAR_PIECE = "obj.mappart3"
        const val CRANDOR_MAP = "obj.dragonmap"
        const val ANTI_DRAGON_SHIELD = "obj.antidragonbreathshield"
        const val ELVARGS_HEAD = "obj.dragon_slayer_qip_elvargs_head"
        const val SILK = "obj.silk"
        const val LOBSTER_POT = "obj.lobster_pot"
        const val UNFIRED_BOWL = "obj.bowl_unfired"
        const val MIND_BOMB = "obj.wizards_mind_bomb"
        val MAGIC_DOOR_ITEMS = listOf(SILK, LOBSTER_POT, UNFIRED_BOWL, MIND_BOMB)

        private const val PANDEMONIUM = "quest_pandemonium"
        private const val MURDER_MYSTERY = "quest_murdermystery"

        private const val REWARD_XP = 18_650.0
        private const val RECOMMENDED_COMBAT = 45
        private const val RECOMMENDED_MAGIC = 33

        var Player.statWarningShown by intVarBit("varbit.dragonslayer_preliminary_statdown")
        var Player.explainedElvarg by intVarBit("varbit.dragonslayer_champion_explained_elvarg")
        var Player.melzarInstructions by intVarBit("varbit.dragonslayer_instructions_melzar")
        var Player.oracleInstructions by intVarBit("varbit.dragonslayer_instructions_oracle")
        var Player.wormbrainInstructions by intVarBit("varbit.dragonslayer_instructions_wormbrain")
        var Player.shipInstructions by intVarBit("varbit.dragonslayer_instructions_ship")
        var Player.shieldInstructions by intVarBit("varbit.dragonslayer_instructions_shield")
        var Player.secretTold by intVarBit("varbit.dragonslayer_secret_told")
        var Player.usedSilk by intVarBit("varbit.dragonslayer_used_silk")
        var Player.usedBowl by intVarBit("varbit.dragonslayer_used_bowl_unfired")
        var Player.usedLobsterPot by intVarBit("varbit.dragonslayer_used_lobster_pot")
        var Player.usedMindBomb by intVarBit("varbit.dragonslayer_used_wizards_mind_bomb")
        var Player.askedGenerals by intVarBit("varbit.dragonslayer_asked_generals")
        var Player.shipOneThird by intVarBit("varbit.dragonslayer_ship_onethird_fixed")
        var Player.shipTwoThirds by intVarBit("varbit.dragonslayer_ship_twothird_fixed")
        var Player.shipFullyFixed by intVarBit("varbit.dragonslayer_ship_fullyfixed")
        var Player.askedNed by intVarBit("varbit.dragonslayer_asked_ned")
        var Player.nedKnowsShipFixed by intVarBit("varbit.dragonslayer_ned_knows_ship_is_fixed")
        var Player.foundSecretDoor by intVarBit("varbit.dragonslayer_crandor_found_secret_door")
        var Player.metSawmill by intVarBit("varbit.dragonslayer_met_sawmill")
        var Player.planksMade by intVarBit("varbit.dragonslayer_planks_made")

        fun Player.ownsAny(vararg objs: String): Boolean {
            val bank = invMap["inv.bank"]
            return objs.any { inv.contains(it) || worn.contains(it) || bank?.contains(it) == true }
        }
    }
}
