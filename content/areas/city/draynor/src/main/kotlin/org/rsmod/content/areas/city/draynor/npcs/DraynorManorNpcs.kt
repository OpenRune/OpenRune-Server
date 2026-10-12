package org.rsmod.content.areas.city.draynor.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DraynorManorNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.anma_assistant") { startDialogue(it.npc) { ava() } }
        onOpNpc3("npc.anma_assistant") { startDialogue(it.npc) { avaTrade() } }
        onOpNpc1("npc.anma_witch") { startDialogue(it.npc) { witch() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Animal_Magnetism
    // TODO(content): [quest:animal_magnetism] "I would be happy to make your home a better place."
    //  starts the quest; quest, Devices and post-quest dialogue (Transcript:Ava) are missing.
    private suspend fun Dialogue.ava() {
        chatNpc(
            happy,
            "Hello there and welcome to my humble abode. It's sadly rather more humble than I'd " +
                "like, to be honest, although perhaps you can help with that?",
        )
        val option =
            choice2(
                "I'm not much into interior design, to tell the truth.",
                1,
                "What's a nice girl like you doing in a place like this?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I'm not much into interior design, to tell the truth.")
                chatNpc(
                    sad,
                    "Very well, I suppose I'll just have to resort to the Professor's patented " +
                        "sleeping aid in order to get some rest. It does so bruise my head, " +
                        "though.",
                )
            }
            2 -> {
                chatPlayer(quiz, "What's a nice girl like you doing in a place like this?")
                chatNpc(
                    neutral,
                    "I'm not a nice girl. I'm a respectable scientist researching the " +
                        "interactions between ghosts and the force of pure science.",
                )
                chatNpc(
                    neutral,
                    "I took over the position of assistant to the Professor upstairs, " +
                        "although, to be honest, his methods are somewhat eccentric. Did you " +
                        "head about the Ernest scandal?",
                )
                chatPlayer(neutral, "Ah, yes. I helped sort that out; he did seem a bit odd.")
                chatNpc(
                    neutral,
                    "You don't know the half of it - can you believe that he thinks he has " +
                        "proven Goldbach's First Conjecture using an optimised segmented sieve?",
                )
                chatPlayer(confused, "Er, quite, Yes.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Animal_Magnetism
    // TODO(content): [quest:animal_magnetism] Ava's Odds and Ends shop after the quest
    private suspend fun Dialogue.avaTrade() {
        chatNpc(neutral, "Hello there, I'm busy with my research. Come back in a bit, could you?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Witch
    // TODO(content): [quest:animal_magnetism] the in-quest dialogue
    private suspend fun Dialogue.witch() {
        if (QuestRequirements.hasCompleted(player, ANIMAL_MAGNETISM)) {
            chatNpc(angry, "Hello my ickle...oh, it's you. What do you want?")
            chatPlayer(neutral, "I wondered whether you had anything interesting to say.")
            chatNpc(angry, "Clearly not.")
            return
        }
        chatNpc(happy, "Hello deary. Don't worry, I'm friendly.")
        chatPlayer(
            confused,
            "You look very familiar but I can't remember chatting to you before.",
        )
        chatNpc(
            neutral,
            "Well, my sisters used to live here but folk kept on killing them. A terrible " +
                "affair, it was. I knew that no one would want to kill a chatty old dear like " +
                "me, though.",
        )
        chatPlayer(confused, "Oh, well...")
        chatNpc(
            angry,
            "We still have a problem with local louts, though. Poisoning my fish, spilling my " +
                "compost all over the place and causing a nuisance for the dear count.",
        )
        chatPlayer(confused, "I think...")
        chatNpc(
            happy,
            "I'm sure a nice young sort like you won't be any trouble, though. In any case, the " +
                "Professor will soon share the secret of chickenisation with me... Not that I'd " +
                "use it these days of course.",
        )
        chatPlayer(neutral, "I have to go now.")
    }

    private companion object {
        const val ANIMAL_MAGNETISM = "quest_animalmagnetism"
    }
}
