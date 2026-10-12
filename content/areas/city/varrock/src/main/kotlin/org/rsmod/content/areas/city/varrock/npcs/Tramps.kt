package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Tramps : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tramppg") { startDialogue(it.npc) { charlie() } }
        onOpNpc1("npc.tramp") { startDialogue(it.npc) { tramp() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charlie_the_Tramp
    // TODO(content): [quest:shield_of_arrav] gang membership answers and quest options
    // TODO(content): [system:treasure_trails] Charlie's item requests
    private suspend fun Dialogue.charlie() {
        chatNpc(sad, "Spare some change guv?")
        var options =
            listOf(
                "Who are you?" to 1,
                "Sorry, I haven't got any." to 2,
                "Go get a job!" to 3,
                "Ok. Here you go." to 4,
                "Is there anything down this alleyway?" to 5,
            )
        while (true) {
            when (menu(options)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Charles. Charles E. Trampin' at your service. Now, about that change " +
                            "you were going to give me...",
                    )
                    options = options.filter { it.second != 1 }
                }
                2 -> {
                    chatPlayer(neutral, "Sorry, I haven't got any.")
                    chatNpc(sad, "Thanks anyway.")
                    return
                }
                3 -> {
                    chatPlayer(angry, "Go get a job!")
                    chatNpc(angry, "You startin? I hope your nose falls off!")
                    return
                }
                4 -> {
                    charlieGiveCoin()
                    return
                }
                5 -> {
                    charlieAlleyway()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.charlieGiveCoin() {
        chatPlayer(happy, "Ok. Here you go.")
        if (!player.invTakeFee(1)) {
            chatPlayer(sad, "Oh wait. I haven't got any, sorry.")
            chatNpc(sad, "Thanks anyway.")
            return
        }
        chatNpc(happy, "Hey, thanks a lot!")
        val option =
            choice2(
                "No problem.",
                1,
                "Don't I get some sort of quest hint or something now?",
                2,
            )
        when (option) {
            1 -> chatPlayer(happy, "No problem.")
            2 -> {
                chatPlayer(quiz, "So...don't I get some sort of quest hint or something now?")
                chatNpc(
                    confused,
                    "Huh? What do you mean? That wasn't why I asked you for money.",
                )
                chatNpc(sad, "I just need to eat...")
            }
        }
    }

    private suspend fun Dialogue.charlieAlleyway() {
        chatPlayer(quiz, "Is there anything down this alleyway?")
        chatNpc(shifty, "Funny you should mention that...there is actually.")
        chatNpc(
            shifty,
            "The ruthless and notorious criminal gang known as the Black Arm Gang have their " +
                "headquarters down there.",
        )
        val option =
            choice2("Thanks for the warning!", 1, "Do you think they would let me join?", 2)
        if (option == 1) {
            chatPlayer(happy, "Thanks for the warning!")
            chatNpc(happy, "Don't worry about it.")
        }
        chatPlayer(quiz, "Do you think they would let me join?")
        chatNpc(
            neutral,
            "You never know. You'll find a lady down there called Katrine. Speak to her.",
        )
        chatNpc(worried, "But don't upset her, she's pretty dangerous.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tramp
    // TODO(content): [system:met_flags] "I know I've asked you before..." greeting after the player refused
    // TODO(content): [system:npc_overhead] shouts after the player refuses to give coins
    private suspend fun Dialogue.tramp() {
        chatNpc(sad, "Got any spare change, mate?")
        if ("obj.coins" !in player.inv) {
            chatPlayer(neutral, "I haven't got any.")
            chatNpc(angry, "Yeah, right, they all say that.")
            return
        }
        val option =
            choice2(
                "Yes, I can spare a little money.",
                1,
                "Sorry, you'll have to earn it yourself, just like I did.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes, I can spare a little money.")
                if (player.invTakeFee(1)) {
                    chatNpc(happy, "Thanks, mate!")
                }
            }
            2 -> {
                chatPlayer(neutral, "Sorry, you'll have to earn it yourself, just like I did.")
                chatNpc(angry, "Please yourself.")
            }
        }
    }
}
