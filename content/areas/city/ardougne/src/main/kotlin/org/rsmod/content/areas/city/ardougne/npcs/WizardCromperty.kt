package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.area.lumbridge.RuneMysteriesQuest
import org.rsmod.content.skills.runecrafting.essence.teleportToRuneEssenceMine
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Wizard_Cromperty
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:ardougne] "Claim free pure essence." option and the Claim op
// TODO(content): [system:essence_mine_portal] the mine's exit portal should return here
// TODO(content): [system:treasure_trails] master clue torn scroll
// TODO(content): [verify:in_game] the essence mine option and op before Rune Mysteries
class WizardCromperty @Inject constructor(private val runeMysteries: RuneMysteriesQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(CROMPERTY) { startDialogue(it.npc) { cromperty() } }
        onOpNpc3(CROMPERTY) { teleportToRuneEssenceMine(it.npc) }
    }

    private suspend fun Dialogue.cromperty() {
        chatNpc(happy, "Hello there. My name is Cromperty. I am a Wizard, and an inventor.")
        if (!runeMysteries.quest.isQuestCompleted(player)) {
            chat()
            return
        }
        when (choice2("Chat.", 1, "Can you teleport me to the Rune Essence?", 2)) {
            1 -> chat()
            2 -> {
                chatPlayer(quiz, "Can you teleport me to the Rune Essence?")
                teleportToRuneEssenceMine()
            }
        }
    }

    private suspend fun Dialogue.chat() {
        chatNpc(
            happy,
            "You must be ${player.displayName}. My good friend Sedridor has told me about you. " +
                "As both wizard and inventor, he has aided me in my great invention!",
        )
        val option =
            choice3(
                "Two jobs? That's got to be tough.",
                1,
                "So what have you invented?",
                2,
                "Well, I shall leave you to your inventing.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Two jobs? That's got to be tough.")
                chatNpc(laugh, "Not when you combine them it isn't! I invent MAGIC things!")
                if (choice2("So what have you invented?", true, LEAVE, false)) {
                    invention()
                } else {
                    leave()
                }
            }
            2 -> invention()
            3 -> leave()
        }
    }

    private suspend fun Dialogue.invention() {
        chatPlayer(quiz, "So what have you invented?")
        chatNpc(
            happy,
            "Ah! My latest invention is my patent pending teleportation block! It emits a low " +
                "level magical signal, that will allow me to locate it anywhere in the world, " +
                "and teleport anything",
        )
        chatNpc(
            happy,
            "directly to it! I hope to revolutionise the entire teleportation system! Don't you " +
                "think I'm great? Uh, I mean it's great?",
        )
        val option =
            choice3(
                "So where is the other block?",
                1,
                "Can I be teleported please?",
                2,
                "Well done, that's very clever.",
                3,
            )
        when (option) {
            1 -> otherBlock()
            2 -> teleportBlock()
            3 -> {
                chatPlayer(happy, "Well done, that's very clever.")
                chatNpc(
                    happy,
                    "Yes it is isn't it? Forgive me for feeling a little smug, this is a major " +
                        "breakthrough in the field of teleportation!",
                )
            }
        }
    }

    private suspend fun Dialogue.otherBlock() {
        chatPlayer(quiz, "So where is the other block?")
        chatNpc(
            confused,
            "Well... Hmm. I would guess somewhere between here and the Wizards' Tower in " +
                "Misthalin. All I know is that it hasn't got there yet as the wizards there " +
                "would have contacted me.",
        )
        chatNpc(
            neutral,
            "I'm using the GPDT for delivery. They assured me it would be delivered promptly.",
        )
        if (choice2("Can I be teleported please?", true, "Who are the GPDT?", false)) {
            teleportBlock()
            return
        }
        chatPlayer(quiz, "Who are the GPDT?")
        chatNpc(
            happy,
            "The Gielinor Parcel Delivery Team. They come very highly recommended. Their motto " +
                "is: 'We aim to deliver your stuff at some point after you have paid us!'",
        )
    }

    private suspend fun Dialogue.teleportBlock() {
        chatPlayer(quiz, "Can I be teleported please?")
        chatNpc(
            happy,
            "By all means! I'm afraid I can't give you any specifics as to where you will come " +
                "out however. Presumably wherever the other block is located.",
        )
        val teleport =
            choice2(
                "Yes, that sounds good. Teleport me!",
                true,
                "That sounds dangerous. Leave me here.",
                false,
            )
        if (!teleport) {
            chatPlayer(worried, "That sounds dangerous. Leave me here.")
            chatNpc(neutral, "As you wish.")
            return
        }
        chatPlayer(happy, "Yes, that sounds good. Teleport me!")
        chatNpc(happy, "Okey dokey! Ready?")
        chatNpc(confused, "Hmmm.... that's odd... I can't seem to get a signal...")
        chatPlayer(sad, "Oh well, never mind.")
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, "Well, I shall leave you to your inventing.")
        chatNpc(happy, "Thanks for dropping by! Stop again anytime!")
    }

    private companion object {
        const val CROMPERTY = "npc.ardounge_wizard"
        const val LEAVE = "Well, I shall leave you to your inventing."
    }
}
