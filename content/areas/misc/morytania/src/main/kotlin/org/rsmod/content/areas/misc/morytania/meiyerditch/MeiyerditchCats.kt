package org.rsmod.content.areas.misc.morytania.meiyerditch

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Cat_(Meiyerditch)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MeiyerditchCats : PluginScript() {
    override fun ScriptContext.startup() {
        for (cat in CATS) {
            onOpNpc1(cat) {
                if (!player.speaksCat()) {
                    it.npc.say("Miaoww!")
                    return@onOpNpc1
                }
                startDialogue(it.npc) { cat() }
            }
        }
    }

    private fun Player.speaksCat(): Boolean = CATSPEAK.any { it in worn }

    private suspend fun Dialogue.cat() {
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            chatPlayer(happy, "Hello there.")
            chatNpc(
                angry,
                "All this work they're doing to fix this place up and they still barely feed me " +
                    "anything! It's an outrage!",
            )
            chatPlayer(
                neutral,
                "There's plenty of rats around. Maybe you could help out with the work by " +
                    "getting rid of a few.",
            )
            chatNpc(neutral, "Hmm... Yes, very well.")
            return
        }
        while (true) {
            val option =
                choice5(
                    "What is this place?",
                    1,
                    "How are you doing?",
                    2,
                    "What's that awful smell?",
                    3,
                    "How long have you been here?",
                    4,
                    "Okay, thanks.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        sad,
                        "It's a desolate, sad place... The peoples here seldom feed me - take me " +
                            "with you!",
                    )
                    chatPlayer(
                        sad,
                        "I'm sorry, I can't take you with me, but I will help if I can.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "How are you doing?")
                    chatNpc(
                        sad,
                        "Not well, I have to ssssssay, but you look healthy. I wish I were " +
                            "healthy like you.",
                    )
                }
                3 -> {
                    chatPlayer(confused, "What's that awful smell?")
                    chatNpc(
                        neutral,
                        "*Sniff, sniff* I don't think it's me. All I can smell is dirty " +
                            "vyrewatches tithing the poor humans.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "How long have you been here?")
                    chatNpc(
                        sad,
                        "As long as I can remember. So far we've not found any way out of this " +
                            "place, and after all, we couldn't leave these poor humans to fend " +
                            "for themselves.",
                    )
                }
                else -> {
                    chatPlayer(happy, "Okay, thanks.")
                    return
                }
            }
        }
    }

    private companion object {
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"

        val CATS = listOf("npc.myq3_cat_01", "npc.myq3_cat_02")

        val CATSPEAK = listOf("obj.ics_little_amulet_of_catspeak", "obj.twocats_amuletofcatspeak")
    }
}
