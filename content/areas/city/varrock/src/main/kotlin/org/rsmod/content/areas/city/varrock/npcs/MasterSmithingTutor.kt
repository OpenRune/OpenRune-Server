package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Master_smithing_tutor
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:barbarian_training] Otto's tasks line after Barbarian Smithing
class MasterSmithingTutor @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aide_tutor_smithing_master") { startDialogue(it.npc) { tutor() } }
    }

    private suspend fun Dialogue.tutor() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Well met! Are you interested in hearing about the art of smithing?")
        while (true) {
            val option =
                choice3(
                    "How can I train my smithing?",
                    1,
                    "What kind of things can I smith?",
                    2,
                    "Not right now, thank you.",
                    3,
                )
            when (option) {
                1 -> training()
                2 -> whatToSmith()
                3 -> {
                    chatPlayer(neutral, "Not right now, thank you.")
                    chatNpc(happy, "Well, just come back any time you want to know anything!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.training() {
        chatPlayer(quiz, "How can I train my smithing?")
        if (HAMMER in player.inv) {
            chatNpc(happy, "I see you already have a hammer. Nice one.")
        } else {
            chatNpc(
                happy,
                "To be able to smith anything, you're going to need one of these beauties.",
            )
            // TODO(content): [verify:in_game] full-inventory behaviour (the transcript doesn't say; drops the hammer)
            player.invAddOrDrop(objRepo, HAMMER)
            objbox(HAMMER, "The Master Smithing tutor gives you a hammer.")
        }
        val level = player.baseSmithingLvl
        when {
            level <= 20 -> beginnerAdvice()
            level <= 37 -> intermediateAdvice()
            else -> advancedAdvice(level)
        }
        chatNpc(happy, "Is there anything else you would like to know?")
    }

    private suspend fun Dialogue.beginnerAdvice() {
        chatNpc(
            happy,
            "You're going to need to get your hand on some metal bars. You could do this by " +
                "mining your own ores and smelting them at a furnace.",
        )
        chatNpc(
            happy,
            "There is a furnace located in Lumbridge, just north of the castle opposite the " +
                "general store.",
        )
        chatNpc(
            happy,
            "If you are looking for some ore, there is a mine east of Varrock. There you can " +
                "find some copper and tin ore. Don't forget to bring a pickaxe.",
        )
        chatNpc(
            happy,
            "When you have your bars, bring them to an anvil to open the smithing interface. If " +
                "the item name is in black, this means you do not have the level to smith the " +
                "item.",
        )
        chatNpc(
            happy,
            "If the item name is in white, this means you do have the level to smith the item. " +
                "You will see the bars required to smith the item underneath the name of the " +
                "item.",
        )
        chatNpc(
            happy,
            "If the number of bars is in orange, this means you do not have enough bars to " +
                "smith the item. If it is in green, this means you have enough bars for the item.",
        )
        chatPlayer(happy, "Thanks for the advice.")
    }

    private suspend fun Dialogue.intermediateAdvice() {
        chatNpc(
            happy,
            "You might find it beneficial to keep a nice stack of bars in your bank before you " +
                "start smithing. Unless you like to jump from task to task of course.",
        )
        chatNpc(
            happy,
            "If you want to smith something that you may not have the level for, you could try " +
                "gaining some temporary knowledge in smithing from a boost.",
        )
        chatNpc(happy, "You can buy a Dwarven stout at the Rising Sun Inn in Falador.")
        val started =
            !QuestRequirements.hasNotCompleted(player, KNIGHTS_SWORD) ||
                QuestRequirements.isOnQuest(player, KNIGHTS_SWORD)
        if (started) {
            return
        }
        chatNpc(
            happy,
            "If you are looking for a quest, you should talk to the squire in the courtyard of " +
                "the white knight's castle. He is looking for a bit of help with something. I " +
                "would recommend you be a little experienced with combat",
        )
        chatNpc(happy, "though.")
    }

    private suspend fun Dialogue.advancedAdvice(level: Int) {
        if (level >= 99) {
            chatNpc(
                laugh,
                "Cor blimey, I don't think there is anything I can teach you, but I'll do my best.",
            )
        }
        chatNpc(
            happy,
            "As you get better you'll find you will be able to smith Mithril and eventually " +
                "Adamantite and even Runite. This can be very lucrative but very expensive on " +
                "the coal front.",
        )
        chatNpc(
            happy,
            "It may be worth you stockpiling coal for a while before attempting these difficult " +
                "metals or even sticking to good old reliable iron by the bucket load.",
        )
        chatNpc(
            happy,
            "If you want to stop yourself from failing to smith iron, I suggest purchasing some " +
                "rings of forging. Be aware that they will break after a certain number of bars " +
                "are smelted.",
        )
        chatNpc(
            happy,
            "If you want to save on coal, you could try smithing your bars over at the Blast " +
                "Furnace in Keldagrim.",
        )
        chatNpc(
            happy,
            "If you are looking for something more interesting to smith, you could talk to Otto " +
                "Godblessed about smithing hastae.",
        )
    }

    private suspend fun Dialogue.whatToSmith() {
        chatPlayer(quiz, "What kind of things can I smith?")
        chatNpc(
            happy,
            "There are many things you can make, from weapons to your good old fashioned armour.",
        )
        chatNpc(
            happy,
            "Weapons are the cheapest to smith. They range from a measly one bar, all the way " +
                "to three bars.",
        )
        chatNpc(
            happy,
            "Armour can be the costliest item to smith, the cost of each item ranges from a " +
                "measly one bar all the way up to a whopping five bars.",
        )
        chatNpc(
            happy,
            "Some weapons and armour, such as darts, will require you to have gained knowledge " +
                "on how to smith them. This is due to the complex nature of the weapon.",
        )
        chatNpc(
            happy,
            "You might find that other items don't require conventional bars you would gather. " +
                "Some may require you to piece blades back together or even infuse a crystal " +
                "into a piece of armour.",
        )
        chatNpc(happy, "Is there anything else you want to know?")
    }

    private companion object {
        const val HAMMER = "obj.hammer"
        const val KNIGHTS_SWORD = "quest_knightssword"
    }
}
