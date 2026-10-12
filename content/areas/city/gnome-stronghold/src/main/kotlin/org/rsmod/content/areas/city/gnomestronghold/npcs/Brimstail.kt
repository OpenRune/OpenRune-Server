package org.rsmod.content.areas.city.gnomestronghold.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.runecrafting.essence.teleportToRuneEssenceMine
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Brimstail
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_eyes_of_glouphrie] quest start and quest stages
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
// TODO(content): [system:treasure_trails] medium clue step
// TODO(content): [verify:in_game] the essence mine option before Rune Mysteries (the Teleport op
//  only appears after it)
class Brimstail
@Inject
constructor(private val runeMysteries: RuneMysteriesQuest, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BRIMSTAIL) { startDialogue(it.npc) { brimstail() } }
        onOpNpc3("npc.gnome_brimstail_2op") { teleportToRuneEssenceMine(it.npc) }
    }

    private suspend fun Dialogue.brimstail() {
        chatNpc(happy, "Hello adventurer, what can I do for you?")
        val questDone = QuestRequirements.hasCompleted(player, EYES_OF_GLOUPHRIE)
        val chatOption =
            if (questDone) {
                "Hello, just wanted a chat."
            } else {
                "What's that cute creature wandering around?"
            }
        val teleport =
            runeMysteries.quest.isQuestCompleted(player) &&
                choice2(chatOption, false, "Can you teleport me to the Rune Essence Mine?", true)
        if (teleport) {
            chatPlayer(quiz, "Can you teleport me to the Rune Essence?")
            chatNpc(happy, "Okay. Hold onto your hat!")
            teleportToRuneEssenceMine()
            return
        }
        if (questDone) {
            justAChat()
        } else {
            cuteCreature()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brimstail
    // TODO(content): [verify:in_game] the "What's this seed do?" and "What's this crystal saw
    //  do?" branches; the transcript doesn't say how they rank against "How are your researches
    //  going?"
    private suspend fun Dialogue.justAChat() {
        chatPlayer(happy, "Hello, just wanted a chat..")
        chatNpc(
            happy,
            "It's always a pleasure to catch up with an another mage! Especially one who has " +
                "helped me so much!",
        )
        if (access.playerContainsAnyObj(CRYSTAL_SAW, CRYSTAL_SAW_SEED)) {
            chatNpc(quiz, "How are your researches going?")
            chatPlayer(
                neutral,
                "Oh, not so bad. I'll let you know if I find anything I think you'll find " +
                    "interesting...",
            )
            chatNpc(happy, "Thanks! I'd appreciate that.")
            return
        }
        chatNpc(
            neutral,
            "Oh, by the way, I found this odd thing lying around, does it belong to you?",
        )
        objbox(CRYSTAL_SAW_SEED, "Brimstail shows you a small crystal seed.")
        chatPlayer(happy, "Oh, yes - I was wondering where that had gone...")
        chatNpc(neutral, "You should look after your things more carefully you know...")
        player.invAddOrDrop(objRepo, CRYSTAL_SAW_SEED)
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Eyes_of_Glouphrie (getting started)
    private suspend fun Dialogue.cuteCreature() {
        chatPlayer(quiz, "What's that cute creature wandering around?")
        chatNpc(happy, "Oh Izzie? He's my pet.")
        chatPlayer(quiz, "What kind of creature is Izzie?")
        chatNpc(
            happy,
            "I'm not sure. He's very cute though don't you think? It's very restful having a " +
                "pet, especially when you are stuck in a cave for as long as I am!",
        )
        chatPlayer(quiz, "So, what do you do down here anyway?")
        chatNpc(
            neutral,
            "Isn't that obvious? I investigate Thaumaturgy, I research the Arcane, I attempt to " +
                "explain the inexplicable.",
        )
        chatPlayer(
            happy,
            "You study magic then, what kind of magic? I do a bit of magic myself you know.",
        )
        chatNpc(happy, "Are you a budding mage? I can tell you more about my research if you'd like.")
        if (!choice2("Yes, that sounds fascinating...", true, "Err, maybe some other time.", false)) {
            chatPlayer(neutral, "Err, maybe some other time.")
            chatNpc(neutral, "Well, I'd better carry on with my research then..")
            return
        }
        chatPlayer(happy, "Yes, that sounds fascinating...")
        chatNpc(
            happy,
            "Excellent, it's so rare I can find someone to discuss my researches, most gnomes " +
                "seem to find it all very tedious.",
        )
        chatPlayer(
            neutral,
            "Never. How could they think that? Though, I suppose gnomes aren't known for their " +
                "magic.",
        )
        chatNpc(
            neutral,
            "Well that's true. These days, apart from a few combat spells most gnomes seem to be " +
                "a bit suspicious of mages.",
        )
        chatPlayer(quiz, "Really? Why is that?")
        chatNpc(
            neutral,
            "Well there is a bit of history there, do you want me to tell you about it?",
        )
        while (true) {
            val option =
                choice3(
                    "Oh, yes I love a bit of History.",
                    1,
                    "I'm more interested in your researches right now.",
                    2,
                    "Actually I'd better get going...",
                    3,
                )
            when (option) {
                1 -> {
                    history()
                    return
                }
                2 -> {
                    chatPlayer(neutral, "I'm more interested in your researches right now.")
                    chatNpc(
                        angry,
                        "Gah! Young mages these days. No patience. No perseverance. Magic is not " +
                            "a toy or a shiny weapon you know! You cannot alter reality unless " +
                            "you understand it. And you cannot understand unless you search for " +
                            "the true causes of things. And history is a record of the causes " +
                            "of things.",
                    )
                    chatPlayer(sad, "I stand corrected - tell me more.")
                }
                else -> {
                    chatPlayer(neutral, "Actually I'd better get going...")
                    chatNpc(neutral, "Well, I'd better carry on with my research then..")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.history() {
        chatPlayer(happy, "Oh, yes I love a bit of History.")
        chatNpc(
            happy,
            "Very wise, a good knowledge of the History of Magic is crucial to understanding the " +
                "Arcane.",
        )
        chatNpc(neutral, "Well, the most famous Gnomish Mage of all got into a bit of .... trouble.")
        chatNpc(
            neutral,
            "Most gnomes don't really talk about him. Other than telling stories about him as if " +
                "he was some sort of boogie-man.",
        )
        chatNpc(
            neutral,
            "Ridiculous! He was a gnome like any other - he sounds like a fascinating character.",
        )
        chatNpc(
            neutral,
            "He was one of King Healthorg the Great's advisors during the Grand Migration. But " +
                "he was exiled after the Goblin Wars.",
        )
        chatPlayer(quiz, "Why was that?")
        chatNpc(
            neutral,
            "Well, I'm not really sure. This was all before I was born - and no-one talks about " +
                "what happened to Glouphrie these days.",
        )
        chatPlayer(neutral, "He must have done something very bad then.")
        chatNpc(
            neutral,
            "Well, maybe? I'm not so sure. As it happens I've been looking into that part of our " +
                "History. All I've managed to establish was that he was particularly interested " +
                "in optics and light magic.",
        )
        chatPlayer(
            confused,
            "It's hard to see what he could misuse light in a way that would get him exiled.",
        )
        chatNpc(
            neutral,
            "Well, indeed. I have also found though he was generally disliked by other gnomes - " +
                "his moniker was 'Glouphrie the Untrusted'.",
        )
        chatNpc(neutral, "Oaknock the Engineer and Glouphrie didn't like each other either...")
        chatPlayer(quiz, "Oaknock? I haven't heard of him before.")
        chatNpc(
            neutral,
            "Ah, Oaknock was an engineer and architect. Very famous. There are statues to him " +
                "around the place - his reputation amongst gnomes is second only to King " +
                "Healthorg himself. He built most of the Stronghold you know.",
        )
        chatNpc(
            quiz,
            "Are you really interested in all of this? Do you want to see something interesting?",
        )
    }

    private companion object {
        const val BRIMSTAIL = "npc.gnome_brimstail"
        const val EYES_OF_GLOUPHRIE = "quest_eyesofglouphrie"
        const val CRYSTAL_SAW = "obj.eyeglo_crystal_saw"
        const val CRYSTAL_SAW_SEED = "obj.crystal_seed_old_small"
    }
}
