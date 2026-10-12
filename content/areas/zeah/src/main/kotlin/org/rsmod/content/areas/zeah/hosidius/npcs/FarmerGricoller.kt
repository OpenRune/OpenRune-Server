package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Farmer_Gricoller
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:tithe_farm] Rewards op and "Have you any rewards?", the farm entry
//  dialogue, and items used on him (farmer's outfit points, farming manual)
// TODO(content): [quest:sins_of_the_father] "Life here seems rather peaceful." after a vyre
//  mentions the Tithe Farm, and the vyre noble clothing refusal
// TODO(content): [quest:a_kingdom_divided] Kharedst's memoirs page conversation
class FarmerGricoller : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_tithe_farmer") { startDialogue(it.npc) { gricoller() } }
    }

    private suspend fun Dialogue.gricoller() {
        val title = if (access.isBodyTypeA()) "sir" else "lady"
        chatNpc(happy, "Hello, young $title. Did you want to work on my farm?")
        var option = mainMenu()
        while (true) {
            when (option) {
                1 -> {
                    aboutFarm()
                    option =
                        choice4(
                            "Tell me about yourself.",
                            2,
                            "I'd like to work on your farm.",
                            3,
                            "Have you any rewards?",
                            4,
                            "I need to go.",
                            5,
                        )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "I'm a fruit farmer. But now I'm a bit too old for that. Maybe I should " +
                            "try writing books again, like I did when I was younger.",
                    )
                    option = mainMenu()
                }
                3 -> {
                    workOnFarm()
                    return
                }
                4 -> {
                    chatPlayer(quiz, "Have you any rewards?")
                    return
                }
                else -> {
                    chatPlayer(neutral, "I need to go.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mainMenu(): Int =
        choice5(
            "Tell me about your farm.",
            1,
            "Tell me about yourself.",
            2,
            "I'd like to work on your farm.",
            3,
            "Have you any rewards?",
            4,
            "I need to go.",
            5,
        )

    private suspend fun Dialogue.aboutFarm() {
        chatPlayer(quiz, "Tell me about your farm.")
        chatNpc(
            neutral,
            "I grow fruit here. Special fruit. It grows very fast, but it needs a lot of " +
                "tending, otherwise it gets the blight and dies.",
        )
        chatNpc(
            sad,
            "These days I'm getting old and slow. You can see all the dead plants from where " +
                "I've tried to farm them.",
        )
    }

    private suspend fun Dialogue.workOnFarm() {
        chatPlayer(happy, "I'd like to work on your farm.")
        chatNpc(
            happy,
            "Alright. Take a handful of seeds from the table, and head into the farm. When " +
                "you've harvested some fruit, deposit it straight into the sacks.",
        )
        chatNpc(
            neutral,
            "Remember, you have to water the plants at each stage, otherwise they die of the " +
                "blight. Fertiliser makes them grow faster... or die faster if you don't water " +
                "them.",
        )
        chatNpc(
            happy,
            "You'll gain a little extra experience if you grow enough of the fruit successfully.",
        )
    }
}
