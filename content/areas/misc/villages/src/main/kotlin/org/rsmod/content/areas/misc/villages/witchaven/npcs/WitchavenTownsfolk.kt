package org.rsmod.content.areas.misc.villages.witchaven.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WitchavenTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.caroline") { startDialogue(it.npc) { caroline() } }
        onOpNpc1("npc.holgartland") { startDialogue(it.npc) { holgart() } }
        onOpNpc1("npc.slug2_jeb") { startDialogue(it.npc) { jeb() } }
        onOpNpc1("npc.slug2_maledict") { startDialogue(it.npc) { brotherMaledict() } }
        onOpNpc1("npc.kennith_land") { startDialogue(it.npc) { kennith() } }
        onOpNpc1("npc.kent_land") { startDialogue(it.npc) { kent() } }
        for (villager in VILLAGERS) {
            onOpNpc1(villager) { startDialogue(it.npc) { villager() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Caroline
    // TODO(content): [system:treasure_trails] medium clue dialogue
    private suspend fun Dialogue.caroline() {
        if (QuestRequirements.hasCompleted(player, SEA_SLUG)) {
            chatPlayer(happy, "Hello again.")
            chatNpc(happy, "Hello traveller, how are you?")
            chatPlayer(happy, "Not bad thanks, yourself?")
            chatNpc(
                happy,
                "I'm good. Busy as always looking after Kent and Kennith but no complaints.",
            )
            return
        }
        // TODO(content): [quest:sea_slug] quest start and quest stages
        chatPlayer(happy, "Hello there.")
        chatNpc(worried, "Is there any chance you could help me?")
        chatPlayer(quiz, "What's wrong?")
        chatNpc(
            worried,
            "It's my husband, he works on a fishing platform. Once a month he takes our son, " +
                "Kennith, out with him.",
        )
        chatNpc(
            worried,
            "They usually write to me regularly, but I've heard nothing all week. It's very " +
                "strange.",
        )
        chatPlayer(neutral, "Maybe the post was lost!")
        chatNpc(
            worried,
            "Maybe, but no-one's heard from the other fishermen on the platform. Their families " +
                "are becoming quite concerned.",
        )
        chatNpc(
            worried,
            "Is there any chance you could visit the platform and find out what's going on?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Holgart
    private suspend fun Dialogue.holgart() {
        if (
            QuestRequirements.isOnQuest(player, SLUG_MENACE) ||
                QuestRequirements.hasCompleted(player, SLUG_MENACE)
        ) {
            chatNpc(sad, "I don't know what to do without my boat.")
            return
        }
        // TODO(content): [quest:sea_slug] quest stages and travel to the Fishing Platform
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Well hello. Beautiful day isn't it.")
        chatPlayer(neutral, "Not bad I suppose.")
        chatNpc(happy, "Just smell that sea air... beautiful.")
        chatPlayer(neutral, "Hmm... lovely...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jeb (Witchaven)
    // TODO(content): [quest:the_slug_menace] travel to the Fishing Platform after the quest
    private suspend fun Dialogue.jeb() {
        chatNpc(neutral, "Hello there.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Maledict
    private suspend fun Dialogue.brotherMaledict() {
        chatNpc(happy, "The blessings of Saradomin be with you child.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kennith (after Sea Slug)
    // TODO(content): [quest:sea_slug] Kennith is only home in Witchaven after the quest
    private suspend fun Dialogue.kennith() {
        chatPlayer(happy, "Hello there, Kennith. How are you today?")
        chatNpc(happy, "Great mister! Thanks again for rescuing me.")
        chatPlayer(happy, "It was my pleasure Kennith. You take care now.")
        chatNpc(happy, "Sure mister. Goodbye now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kent (after Sea Slug)
    // TODO(content): [quest:sea_slug] Kent is only home in Witchaven after the quest
    private suspend fun Dialogue.kent() {
        chatNpc(happy, "Hello there, ${player.displayName}!")
        chatPlayer(happy, "Hello again Kent.")
        chatNpc(
            neutral,
            "I never did get the chance to thank you properly for saving Kennith and myself.",
        )
        chatPlayer(happy, "Oh, don't be silly, it was nothing really.")
        chatNpc(happy, "Play it down if you will. It was a truly brave thing you did.")
        chatPlayer(
            neutral,
            "I only did what anyone would have done in my position. I have to go now, take care.",
        )
        chatNpc(happy, "You too, ${player.displayName}. Goodbye.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Witchaven_villager
    // TODO(content): [verify:in_game] fifth greeting (a fishing song) is not included
    private suspend fun Dialogue.villager() {
        if (QuestRequirements.hasCompleted(player, SLUG_MENACE)) {
            possessedVillager()
            return
        }
        when (access.random.of(4)) {
            0 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(angry, "What have you got to be so cheerful about?")
                chatPlayer(happy, "Well, it's another nice day.")
                chatNpc(
                    angry,
                    "Ha! Try worrying about how you feed a family with no job. Then tell me how " +
                        "nice the day is!",
                )
                chatPlayer(worried, "Okay, I guess I've caught you at a bad time. Goodbye.")
            }
            1 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(confused, "Hmm? Oh, hello there.")
                chatPlayer(quiz, "Are you okay? You seem a bit preoccupied.")
                chatNpc(neutral, "It's nothing stranger. No need to concern yourself.")
            }
            2 -> {
                chatPlayer(happy, "Hello there.")
                chatNpc(sad, "Spare a coin mister?")
                chatPlayer(quiz, "What do you need it for?")
                chatNpc(sad, "For a poor unemployed fisherman what needs to eat.")
                chatPlayer(confused, "Why don't you just fish for some food?")
                chatNpc(shifty, "Err... Goodbye mister.")
            }
            else -> {
                chatNpc(angry, "Can you believe they did this to us?")
                chatPlayer(confused, "Wha...")
                chatNpc(angry, "I mean, what did they think would happen?")
                chatPlayer(confused, "Who...")
                chatNpc(angry, "Building that whacking great Fishing Platform just off the coast.")
                chatPlayer(confused, "Fish...")
                chatNpc(angry, "Dratted thing stole all of our trade.")
                chatPlayer(confused, "Excuse...")
                chatNpc(angry, "I'm sorry, I'm too angry to speak right now. Goodbye")
            }
        }
    }

    private suspend fun Dialogue.possessedVillager() {
        chatPlayer(happy, "Hello there.")
        mesbox("Their eyes are staring vacantly into space.")
        when (access.random.of(5)) {
            0 -> {
                chatNpc(neutral, "Ye mariners all, as ye pass by,")
                chatNpc(neutral, "Come in and drink if you are dry,")
                chatNpc(neutral, "Come spend, me lads, your money brisk,")
                chatNpc(neutral, "And pop your nose in a jug of this.")
                chatPlayer(neutral, "You're not fooling anyone you know.")
                chatNpc(neutral, "We fooled you easily enough.")
            }
            1 -> {
                chatNpc(neutral, "Free. She is free...")
                chatPlayer(confused, "What?")
                chatNpc(neutral, "The mother is free.")
            }
            2 -> {
                chatNpc(angry, "You! You did it!")
                chatPlayer(worried, "I didn't mean to!")
                chatNpc(angry, "You killed him!")
                chatPlayer(confused, "It was an accide... Killed who?")
                chatNpc(angry, "Our Prince, you killed our Prince.")
                chatPlayer(neutral, "Oh that, yes I did.")
                chatNpc(angry, "Leave us alone.")
            }
            3 -> {
                chatNpc(neutral, "Soon now... So soon...")
                chatNpc(neutral, "The stars are almost right.")
                chatPlayer(quiz, "For what?")
                chatNpc(neutral, ". . .")
            }
            else -> {
                chatNpc(neutral, "Ahh, our saviour.")
                chatPlayer(sad, "Please don't remind me.")
                chatNpc(neutral, "Do not worry, soon your regret will be gone.")
                chatPlayer(angry, "If you think you will get to me...")
                chatNpc(neutral, "All in good time.")
            }
        }
    }

    private companion object {
        const val SEA_SLUG = "quest_seaslug"
        const val SLUG_MENACE = "quest_slugmenace"

        val VILLAGERS =
            listOf(
                "npc.slug2_villager1_stage1_multi",
                "npc.slug2_villager2_stage1_multi",
                "npc.slug2_villager3_stage1_multi",
            )
    }
}
