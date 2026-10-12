package org.rsmod.content.areas.zeah.forthos.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ForthosDungeon : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosdun_aimeri") { startDialogue(it.npc) { brotherAimeri() } }
        onOpNpc1("npc.hosdun_olbertus") { startDialogue(it.npc) { olbertus() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brother_Aimeri (injured)
    // TODO(content): [miniquest:in_search_of_knowledge] healing him with food
    //  (varbit.hosdun_aimeri_status), the healed dialogue, artifacts and the spider silk trade
    // TODO(content): [verify:in_game] which foods count as "cooked fish, meat or vegetables"
    private suspend fun Dialogue.brotherAimeri() {
        chatNpc(
            sad,
            "Nilsal, traveller. I was injured while exploring down here. Could you spare me some " +
                "cooked fish, meat or vegetables?",
        )
        if ("content.food" in player.inv) {
            chatPlayer(happy, "It just so happens that I have some with me!")
            chatNpc(happy, "Oh, please hand it over if you can spare it.")
        } else {
            chatPlayer(sad, "I'm afraid that I don't have any of that type of food with me.")
            chatNpc(neutral, "Oh, well if you return with any I would greatly appreciate it.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Olbertus (while possessed)
    // TODO(content): [system:forthos_temple_coin] trading five dragonstones for the temple coin,
    //  fighting the Shaeded Beast, and the freed dialogue (varbit.hosdun_olbertus_status)
    private suspend fun Dialogue.olbertus() {
        chatNpc(angry, "It's mine. You can't have it, we found it!")
        while (true) {
            val option =
                choice4(
                    "What have you found?",
                    1,
                    "I don't want it, I have something better.",
                    2,
                    "Since it's valuable, I think I'll just just kill you for it.",
                    3,
                    "You're crazy!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What have you found?")
                    chatNpc(
                        neutral,
                        "Not dragonstones. I like dragonstones, that's why I came here. However, " +
                            "I found a valuable coin instead, it speaks to me... but you can't " +
                            "have it! It's precious to me!",
                    )
                }
                2 -> return somethingBetter()
                3 -> return killForIt()
                else -> return chatPlayer(neutral, "You're crazy!")
            }
        }
    }

    private suspend fun Dialogue.somethingBetter() {
        chatPlayer(neutral, "I don't want it, I have something better.")
        chatNpc(quiz, "What is it? Is it dragonstones? I like dragonstones!")
        chatNpcSpecific("Olbertus (possessed)", OLBERTUS, angry, "Ask it for five! It looks rich!")
        chatNpc(quiz, "Five? Will it give us five?")
        if (player.inv.count(DRAGONSTONE) < DRAGONSTONES) {
            chatPlayer(neutral, "No. I don't have five dragonstones.")
            chatNpc(angry, "Well then we are not interested!")
            return
        }
        chatPlayer(
            neutral,
            "Yes. I have five dragonstones, tell you what, I'll trade you them for that coin you " +
                "found.",
        )
    }

    private suspend fun Dialogue.killForIt() {
        chatPlayer(angry, "Since it's valuable, I think I'll just just kill you for it.")
        chatNpc(shocked, "Please! No!")
        val attack =
            choice2(
                "Attack Olbertus.",
                true,
                "Just kidding.",
                false,
                title = "This is a dangerous fight, you will lose your items on death.",
            )
        if (!attack) {
            chatPlayer(laugh, "Just kidding!")
        }
    }

    private companion object {
        const val OLBERTUS = "npc.hosdun_olbertus_corrupted"
        const val DRAGONSTONE = "obj.dragonstone"
        const val DRAGONSTONES = 5
    }
}
