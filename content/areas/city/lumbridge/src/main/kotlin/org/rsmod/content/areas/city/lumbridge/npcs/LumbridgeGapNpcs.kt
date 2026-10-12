package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LumbridgeGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.favour_seth_groats") { startDialogue(it.npc) { sethGroats() } }
        onOpNpc1("npc.canoeing_tarquin") { startDialogue(it.npc) { tarquin() } }
        onOpNpc1("npc.surok_rat") { startDialogue(it.npc) { ratBurgiss() } }
        onOpNpc1("npc.gillie_the_milkmaid") { startDialogue(it.npc) { gillieGroats() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Seth_Groats
    private suspend fun Dialogue.sethGroats() {
        chatNpc(happy, "M'arnin'....going to milk me cowsies!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tarquin
    // TODO(content): [system:canoe_axe_storage] axe storing dialogue
    private suspend fun Dialogue.tarquin() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello old bean. Is there something I can help you with?")
        val choice = choice2("Who are you?", 1, "Can you teach me about Canoeing?", 2)
        if (choice == 2) {
            chatPlayer(quiz, "Can you teach me about canoes?")
            canoeLesson()
            return
        }
        chatPlayer(quiz, "Who are you?")
        chatNpc(happy, "My name is Tarquin Marjoribanks.")
        chatNpc(happy, "I'd be surprised if you haven't already heard of me?")
        chatPlayer(quiz, "Why would I have heard of you Mr. Marjoribanks?")
        chatNpc(angry, "It's pronounced 'Marchbanks'!")
        chatNpc(
            happy,
            "You should know of me because I am a member of the royal family of Misthalin!",
        )
        chatPlayer(quiz, "Are you related to King Roald?")
        chatNpc(happy, "Oh yes! Quite closely actually.")
        chatNpc(happy, "I'm his 4th cousin, once removed on his mothers side.")
        chatPlayer(confused, "Er... Okay. What are you doing here then?")
        chatNpc(
            happy,
            "I'm canoeing on the river! It's enormous fun! Would you like to know how?",
        )
        if (choice2("Yes", 1, "No", 2) == 2) {
            chatPlayer(neutral, "No thanks, not right now.")
            return
        }
        chatPlayer(quiz, "Could you teach me about canoes?")
        canoeLesson()
    }

    // TODO(content): [verify:in_game] whether the Woodcutting tiers use the boosted level
    private suspend fun Dialogue.canoeLesson() {
        val level = player.woodcuttingLvl
        if (level < 12) {
            chatNpc(neutral, "Well, you don't look like you have the skill to make a canoe.")
            chatNpc(neutral, "You need to have at least level 12 woodcutting.")
            chatNpc(
                happy,
                "Once you are able to make a canoe it makes travel along the river much quicker!",
            )
            return
        }
        chatNpc(
            happy,
            "It's really quite simple. Just walk down to that tree on the bank and chop it down.",
        )
        chatNpc(
            happy,
            "When you have done that you can shape the log further with your axe to make a " +
                "canoe.",
        )
        chatNpc(
            happy,
            "My personal favourite is the Stable Dugout canoe. A finer craft you'll never see " +
                "old bean!",
        )
        chatNpc(
            happy,
            "A Stable Dugout canoe will take you pretty much the length of the Lum river.",
        )
        chatNpc(neutral, "Of course there are other canoes.")
        when {
            level < 27 -> {
                chatNpc(
                    neutral,
                    "Further up river, near the Barbarian Village, I saw some darned fool " +
                        "'canoeing' on a log!",
                )
                chatNpc(
                    neutral,
                    "Unfortunately, you don't have the skill to create anything more than one " +
                        "of those logs. I dare say it will only get 1 stop down the river!",
                )
                chatNpc(neutral, "Still, I'm sure it will satisfy one such as yourself.")
                chatPlayer(angry, "What's that supposed to mean?")
                chatNpc(
                    angry,
                    "Do not profane the royal house of Varrock by engaging me in further " +
                        "discourse you knave!",
                )
                chatPlayer(angry, "Pfft! I doubt he even knows the King!")
            }
            level < 42 -> {
                chatNpc(happy, "You seem to be quite handy with an axe though!")
                chatNpc(
                    neutral,
                    "I'm sure you can build a Dugout canoe. Not as fine as a Stable Dugout but " +
                        "it will carry you 2 stops on the river.",
                )
                chatNpc(neutral, "I should imagine it would suit your limited means.")
                chatPlayer(angry, "What do you mean when you say 'limited means'?")
                chatNpc(neutral, "Well, you're just an itinerant adventurer!")
                chatNpc(
                    angry,
                    "What possible reason would you have for cluttering up my river with your " +
                        "inferior water craft!",
                )
            }
            level < 57 -> {
                chatNpc(
                    happy,
                    "Ah! Perfect! You can make a Stable Dugout canoe! One of those will carry " +
                        "you to any civilised place on the river.",
                )
                chatNpc(
                    neutral,
                    "If you were of good pedigree I'd let you join my boat club. You seem to be " +
                        "one of those vagabond adventurers though.",
                )
                chatPlayer(angry, "Charming!")
                chatNpc(angry, "Be off with you rogue!")
            }
            else -> {
                chatNpc(neutral, "Well ... erm. You seem to be able to make a Waka!")
                chatPlayer(quiz, "Sounds fun, what's a Waka.")
                chatNpc(
                    neutral,
                    "I've only ever seen one man on the river who uses a Waka. A big, fearsome " +
                        "looking fellow up near Edgeville.",
                )
                chatNpc(
                    neutral,
                    "People say he was born in the Wilderness and that he is looking for a route " +
                        "back.",
                )
                chatPlayer(shocked, "Is that true!")
                chatNpc(angry, "How should I know? I would not consort with such a base fellow!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rat_Burgiss (members world)
    private suspend fun Dialogue.ratBurgiss() {
        chatPlayer(happy, "Hello there!")
        chatNpc(
            neutral,
            "Oh, hello. I'd love to chat right now, but I'm a bit busy. Perhaps you could come " +
                "back and chat another time?",
        )
        chatPlayer(neutral, "Of course. Sorry to bother you!")
        chatNpc(happy, "Not at all! Farewell!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gillie_Groats
    // TODO(content): [verify:in_game] the lost cowbell amulet option and what grants it
    private suspend fun Dialogue.gillieGroats() {
        chatNpc(happy, "Hello, I'm Gillie the Milkmaid. What can I do for you?")
        val choice =
            choice3(
                "Who are you?",
                1,
                "Can you tell me how to milk a cow?",
                2,
                "I'm fine, thanks.",
                3,
            )
        when (choice) {
            1 -> {
                chatNpc(
                    happy,
                    "My name is Gillie Groats. My father is a farmer and I milk the cows for him.",
                )
                chatPlayer(quiz, "Do you have any buckets of milk spare?")
                chatNpc(
                    neutral,
                    "I'm afraid not. We need all of our milk to sell to market, but you can milk " +
                        "the cow yourself if you need the milk.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> {
                chatPlayer(quiz, "So how do you get milk from a cow then?")
                chatNpc(
                    happy,
                    "It's very easy. First you need an empty bucket to hold the milk.",
                )
                chatNpc(
                    happy,
                    "Then find a dairy cow to milk - you can't milk just any cow.",
                )
                chatPlayer(quiz, "How do I find a dairy cow?")
                chatNpc(
                    happy,
                    "They are easy to spot - they are dark brown and white, unlike beef cows, " +
                        "which are light brown and white. We also tether them to a post to stop " +
                        "them wandering around all over the place.",
                )
                chatNpc(happy, "There are a couple very near, in this field.")
                chatNpc(
                    happy,
                    "Then just milk the cow and your bucket will fill with tasty, nutritious milk.",
                )
            }
            else -> chatPlayer(neutral, "I'm fine, thanks.")
        }
    }
}
