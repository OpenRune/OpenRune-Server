package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.other.pets.cats.CatDialogue
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EastVarrock @Inject constructor(private val cats: CatDialogue) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dimintheis") { startDialogue(it.npc) { dimintheis() } }
        onOpNpc1("npc.bioguard1") { startDialogue(it.npc) { gateGuard() } }
        onOpNpc1("npc.guidor") { startDialogue(it.npc) { guidor() } }
        onOpNpc1("npc.guidors_wife") { startDialogue(it.npc) { julie() } }
        onOpNpc1("npc.vc_pox") { startDialogue(it.npc) { pox() } }
        onOpNpc1("npc.vc_trotters") { startDialogue(it.npc) { trotters() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dimintheis (before Family Crest)
    // TODO(content): [miniquest:family_pest] after-miniquest dialogue and steel gauntlets
    private suspend fun Dialogue.dimintheis() {
        if (QuestRequirements.hasCompleted(player, FAMILY_CREST)) {
            dimintheisAfterQuest()
            return
        }
        chatNpc(neutral, "Hello. My name is Dimintheis, of the noble family Fitzharmon.")
        val option =
            choice3(
                "Why would a nobleman live in a dump like this?",
                1,
                "You're rich then? Can I have some money?",
                2,
                "Hi, I am a bold adventurer.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(confused, "Why would a nobleman live in a dump like this?")
                chatNpc(
                    sad,
                    "The King has taken my estate from me until such time as I can show my " +
                        "family crest to him.",
                )
                when (choice2("Why would he do that?", 1, "So where is this crest?", 2)) {
                    1 -> {
                        chatPlayer(quiz, "Why would he do that?")
                        crestHistory()
                        chatPlayer(quiz, "So where is this crest?")
                        whereIsCrest()
                    }
                    2 -> {
                        chatPlayer(quiz, "So where is this crest?")
                        whereIsCrest()
                    }
                }
            }
            2 -> {
                chatPlayer(happy, "You're rich then? Can I have some money?")
                chatNpc(
                    angry,
                    "Gah! Lousy beggar! Your sort is what's ruining this great land! Why don't " +
                        "you just go and get a job if you need money so badly?",
                )
            }
            3 -> {
                chatPlayer(happy, "Hi, I am a bold adventurer.")
                chatNpc(
                    neutral,
                    "An adventurer hmmm? How lucky. I may have an adventure for you. I " +
                        "desperately need my family crest returning to me. It is of utmost " +
                        "importance.",
                )
                val next =
                    choice3(
                        "Why are you so desperate for it?",
                        1,
                        "So where is this crest?",
                        2,
                        "I'm not interested in that adventure right now.",
                        3,
                    )
                when (next) {
                    1 -> {
                        chatPlayer(quiz, "Why are you so desperate for it?")
                        crestHistory()
                        chatPlayer(quiz, "So where is this crest?")
                        whereIsCrest()
                    }
                    2 -> {
                        chatPlayer(quiz, "So where is this crest?")
                        whereIsCrest()
                    }
                    3 -> {
                        chatPlayer(neutral, "I'm not interested in that adventure right now.")
                        chatNpc(sad, "I realise it was a lot to ask of a stranger.")
                    }
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dimintheis (after Family Crest)
    private suspend fun Dialogue.dimintheisAfterQuest() {
        if (choice2("Have you got any quests for me?", 1, "Other", 2) == 1) {
            chatPlayer(
                happy,
                "Do you have any quests for me? I really want one. I really want to help out!",
            )
            mesbox(
                "You murmur under your breath something about how great the rewards were last " +
                    "time you did a quest for Dimintheis."
            )
            chatNpc(
                neutral,
                "My family consider what you did for us a great service. We couldn't possibly " +
                    "trouble you further.",
            )
            chatPlayer(
                quiz,
                "There must be SOMETHING you need help with. I am a great adventurer after all.",
            )
            chatNpc(happy, "There really isn't anything, I am very content with life right now.")
            val option =
                choice2(
                    "I guess money does buy happiness.",
                    1,
                    "Oh come on, however menial, I want to help!",
                    2,
                )
            if (option == 1) {
                chatPlayer(shifty, "I guess money really does buy happiness.")
                chatNpc(confused, "What was that? I didn't quite catch that.")
                chatPlayer(neutral, "Nothing...")
                return
            }
            chatPlayer(
                quiz,
                "Oh come on, surely there's something I can help out with! I want to help, " +
                    "however menial it is.",
            )
            chatNpc(
                neutral,
                "Okay okay. Fine then. Whilst I don't have anything for you, how about you try " +
                    "my sons, they are always wanting something done.",
            )
            chatPlayer(happy, "Fantastic! I'll get straight to it!")
            mesbox("You smell the sweet smell of the great rewards that may soon come.")
            return
        }
        if (QuestRequirements.hasCompleted(player, DEFENDER_OF_VARROCK)) {
            chatNpc(
                happy,
                "Hello again, adventurer. I hope that Shield of Arrav business marks the start " +
                    "of something big for my family. Maybe it will help me finally get out of " +
                    "this hole!",
            )
        } else {
            chatNpc(
                happy,
                "You have my eternal gratitude for all you have done in saving my family's " +
                    "honour, adventurer. Perhaps some day my sons will overcome their differences.",
            )
        }
        chatPlayer(quiz, "Do you have any gauntlets for me?")
        if (FAMILY_GAUNTLETS.any { it in player.inv || it in player.worn || it in access.bank }) {
            chatNpc(neutral, "No, I don't think so. You must have them yourself somewhere.")
            return
        }
        // TODO(content): [quest:family_crest] returning lost gauntlets needs the player's
        //  gauntlet type from the quest state
    }

    private suspend fun Dialogue.crestHistory() {
        chatNpc(
            neutral,
            "Well, there is a long standing rule of chivalry amongst the Varrockian " +
                "aristocracy, where each noble family is in possession of a unique crest, " +
                "which signifies the honour and lineage of the family. More than this",
        )
        chatNpc(
            neutral,
            "however, it also represents the lawful rights of each family to prove their " +
                "ownership of their wealth and lands. If the family crest is lost, then the " +
                "family's estate is handed over to the current monarch until the crest is " +
                "restored.",
        )
        chatNpc(
            neutral,
            "This dates back to the times when there was much in-fighting amongst the noble " +
                "families and their clans, and was introduced as a way of reducing the " +
                "bloodshed that was devestating the ranks of the ruling classes at that time.",
        )
        chatNpc(
            neutral,
            "When you captured a rival family's clan, you also captured their lands and wealth.",
        )
    }

    // TODO(content): [quest:family_crest] "Start the Family Crest quest?" offer
    private suspend fun Dialogue.whereIsCrest() {
        chatNpc(
            sad,
            "Well, my three sons took it with them many years ago when they rode out to fight " +
                "in the war against the undead necromancer and his army in the battle to save " +
                "Varrock.",
        )
        chatNpc(
            sad,
            "For many years I had assumed them all dead, as I had heard no word from them, but " +
                "recently I heard word that my son Caleb is alive and well, and trying to earn " +
                "his fortune as a great chef in the lands beyond White Wolf Mountain, although " +
                "I know not where.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(East_Varrock)
    // TODO(content): [quest:biohazard] confiscating the vials at the gate
    private suspend fun Dialogue.gateGuard() {
        chatNpc(
            neutral,
            "Please don't disturb me, I've got to keep any eye out for suspicious individuals.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guidor
    // TODO(content): [verify:in_game] members line before Biohazard (the transcript only has the F2P line)
    // TODO(content): [quest:biohazard] quest dialogue
    private suspend fun Dialogue.guidor() {
        if (QuestRequirements.hasCompleted(player, BIOHAZARD)) {
            chatPlayer(happy, "Hello again Guidor. How are you doing?")
            chatNpc(neutral, "I'm hanging in there.")
            chatPlayer(happy, "Good for you.")
            return
        }
        chatNpc(neutral, "I don't really want any visitors just now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Julie
    // TODO(content): [quest:biohazard] bedroom door, priest gown and quest dialogue
    private suspend fun Dialogue.julie() {
        chatNpc(worried, "Oh dear! Oh dear! I don't have time to chat!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pox
    // TODO(content): [quest:ratcatchers] "The cat is too sick to respond." while Pox is poisoned
    private suspend fun Dialogue.pox() {
        chatPlayer(happy, "Hi cat!")
        if (!cats.catspeak(player)) {
            chatNpc(happy, "Miaooow!")
            return
        }
        if (QuestRequirements.hasCompleted(player, RATCATCHERS)) {
            chatNpc(happy, "Hello ${player.displayName}.")
            chatPlayer(quiz, "How are you feeling now?")
            chatNpc(
                sad,
                "Still a bit below purrrr. Thank you for fetching the medicine for me.",
            )
            chatPlayer(happy, "No problem, I hope you get better soon.")
            return
        }
        chatNpc(happy, "Hello.")
        chatPlayer(quiz, "What are you doing?")
        chatNpc(
            happy,
            "I think I'll go hunting soon, there's a warehouse with some rats in it nearby.",
        )
        chatPlayer(happy, "Happy hunting.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trotters
    private suspend fun Dialogue.trotters() {
        chatPlayer(happy, "Hello there.")
        if (!cats.catspeak(player)) {
            chatNpc(neutral, "Meow.")
            return
        }
        chatNpc(bored, "Oh, what do you want? Can't you see I'm trying to nap?")
        chatPlayer(quiz, "Shouldn't you be catching rats?")
        chatNpc(happy, "Oh my, no. I have humans for that.")
    }

    private companion object {
        const val FAMILY_CREST = "quest_familycrest"
        const val DEFENDER_OF_VARROCK = "quest_defenderofvarrock"
        const val BIOHAZARD = "quest_biohazard"
        const val RATCATCHERS = "quest_ratcatchers"
        val FAMILY_GAUNTLETS =
            listOf(
                "obj.steel_gauntlets",
                "obj.gauntlets_of_goldsmithing",
                "obj.gauntlets_of_cooking",
                "obj.gauntlets_of_chaos",
            )
    }
}
