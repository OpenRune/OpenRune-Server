package org.rsmod.content.areas.city.draynor.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DraynorTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.malak") { startDialogue(it.npc) { ruantun() } }
        onOpNpc1("npc.joe_vis") { startDialogue(it.npc) { joe() } }
        onOpNpc1("npc.lady_keli_vis") { startDialogue(it.npc) { ladyKeli() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ruantun
    // TODO(content): [quest:desert_treasure_i] Silver pot dialogue
    private suspend fun Dialogue.ruantun() {
        chatPlayer(neutral, "Hello.")
        chatNpc(shifty, "You ssshould not be down here...")
        val option =
            choice3(
                "Who are you?",
                1,
                "Why are you down here?",
                2,
                "Can I use your anvil?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(shifty, "My name isss unimportant... I live only to ssserve my massster.")
                chatPlayer(confused, "Um... Okay then.")
            }
            2 -> {
                chatPlayer(quiz, "Why are you down here?")
                chatNpc(
                    shifty,
                    "Thisss isss where I belong... Beingsss sssuch as myssself cannot abide in " +
                        "the light... It is in the darknesss where we find our homesss...",
                )
                chatPlayer(confused, "Uh... Okay then.")
            }
            3 -> {
                chatPlayer(quiz, "Can I use your anvil?")
                chatNpc(
                    shifty,
                    "Of courssse you may... I have very little ussse for it nowadaysss...",
                )
                chatPlayer(confused, "Uh... Thanks, I guess.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Joe_(jail)
    // TODO(content): [quest:prince_ali_rescue] Quest dialogue and using beer on Joe
    private suspend fun Dialogue.joe() {
        chatPlayer(quiz, "Hi. Who are you guarding here?")
        chatNpc(
            neutral,
            "Can't say. It's all very secret. You should get out of here. I am not supposed to " +
                "talk while I guard.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Keli
    // TODO(content): [quest:prince_ali_rescue] Quest dialogue
    private suspend fun Dialogue.ladyKeli() {
        chatNpc(angry, "What do you want?")
        chatPlayer(confused, "Nothing?")
        chatNpc(angry, "Clear off then.")
    }
}
