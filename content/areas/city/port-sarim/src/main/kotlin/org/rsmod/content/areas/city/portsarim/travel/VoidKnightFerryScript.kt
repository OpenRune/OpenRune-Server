package org.rsmod.content.areas.city.portsarim.travel

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VoidKnightFerryScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SARIM_SQUIRE) { startDialogue(it.npc) { sarimSquire() } }
        onOpNpc3(SARIM_SQUIRE) { sailFerry(OUTPOST, OUTPOST_NAME, fare = 0) }
        onOpNpc1(OUTPOST_SQUIRE) { startDialogue(it.npc) { outpostSquire() } }
        onOpNpc3(OUTPOST_SQUIRE) { sailFerry(PORT_SARIM, PORT_SARIM_NAME, fare = 0) }
    }

    private suspend fun Dialogue.sarimSquire() {
        chatNpc(neutral, "Hi, how can I help you?")
        val topic =
            choice4(
                "Who are you?",
                SquireTopic.Who,
                "Where does this ship go?",
                SquireTopic.Where,
                "I'd like to go to your outpost.",
                SquireTopic.Go,
                "I'm fine thanks.",
                SquireTopic.Leave,
            )
        when (topic) {
            SquireTopic.Who -> whoAreYou()
            SquireTopic.Where -> {
                chatPlayer(quiz, "Where does this ship go?")
                chatNpc(neutral, "To the Void Knight outpost. It's a small island just off Karamja.")
                if (choice2("I'd like to go to your outpost.", true, "That's nice.", false)) {
                    goToOutpost()
                } else {
                    chatPlayer(neutral, "That's nice.")
                }
            }
            SquireTopic.Go -> goToOutpost()
            else -> chatPlayer(neutral, "I'm fine thanks.")
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "I'm a Squire for the Void Knights.")
        chatPlayer(quiz, "The who?")
        chatNpc(
            happy,
            "The Void Knights, they are great warriors of balance who do Guthix's work here in " +
                "Gielinor.",
        )
        val topic =
            choice4(
                "Wow, can I join?",
                SquireTopic.Join,
                "What kind of work?",
                SquireTopic.Work,
                "What's 'Gielinor'?",
                SquireTopic.Gielinor,
                "Uh huh, sure.",
                SquireTopic.Leave,
            )
        when (topic) {
            SquireTopic.Join -> {
                chatPlayer(happy, "Wow, can I join?")
                chatNpc(
                    neutral,
                    "Entry is strictly invite only, however we do need help continuing Guthix's " +
                        "work.",
                )
                if (choice2("What kind of work?", true, "Good luck with that.", false)) {
                    whatKindOfWork()
                } else {
                    chatPlayer(neutral, "Good luck with that.")
                }
            }
            SquireTopic.Work -> whatKindOfWork()
            SquireTopic.Gielinor -> whatsGielinor()
            else -> chatPlayer(shifty, "Uh huh, sure.")
        }
    }

    private suspend fun Dialogue.whatKindOfWork() {
        chatPlayer(quiz, "What kind of work?")
        chatNpc(
            neutral,
            "Ah well you see we try to keep Gielinor as Guthix intended, it's very challenging. " +
                "Actually we've been having some problems recently, maybe you could help us?",
        )
        val topic =
            choice3(
                "Yeah ok, what's the problem?",
                SquireTopic.Problem,
                "What's 'Gielinor'?",
                SquireTopic.Gielinor,
                "I'd rather not, sorry.",
                SquireTopic.Leave,
            )
        when (topic) {
            SquireTopic.Problem -> {
                chatPlayer(quiz, "Yeah ok, what's the problem?")
                chatNpc(
                    sad,
                    "Well the order has become quite diminished over the years, it's a very long " +
                        "process to learn the skills of a Void Knight. Recently there have been " +
                        "breaches into our realm from somewhere else, and strange creatures",
                )
                chatNpc(
                    sad,
                    "have been pouring through. We can't let that happen, and we'd be very " +
                        "grateful if you'd help us.",
                )
                if (choice2("How can I help?", true, "Sorry, but I can't.", false)) {
                    chatPlayer(quiz, "How can I help?")
                    chatNpc(
                        neutral,
                        "We send launchers from our outpost to the nearby islands. If you go and " +
                            "wait in the lander there that'd really help.",
                    )
                } else {
                    chatPlayer(neutral, "Sorry, but I can't.")
                }
            }
            SquireTopic.Gielinor -> whatsGielinor()
            else -> chatPlayer(neutral, "I'd rather not, sorry.")
        }
    }

    private suspend fun Dialogue.whatsGielinor() {
        chatPlayer(quiz, "What's 'Gielinor'?")
        chatNpc(
            neutral,
            "It is the name that Guthix gave to this world, so we honour him with its use.",
        )
    }

    private suspend fun Dialogue.goToOutpost() {
        chatPlayer(neutral, "I'd like to go to your outpost.")
        chatNpc(neutral, "Certainly, right this way.")
        access.sailFerry(OUTPOST, OUTPOST_NAME, fare = 0)
    }

    private suspend fun Dialogue.outpostSquire() {
        chatNpc(neutral, "Hi, how can I help you?")
        val leave =
            choice2(
                "I'd like to go back to Port Sarim please.",
                true,
                "I'm fine thanks.",
                false,
            )
        if (!leave) {
            chatPlayer(neutral, "I'm fine thanks.")
            return
        }
        chatPlayer(neutral, "I'd like to go back to Port Sarim please.")
        chatNpc(neutral, "Ok, but please come back soon and help us.")
        access.sailFerry(PORT_SARIM, PORT_SARIM_NAME, fare = 0)
    }

    private enum class SquireTopic {
        Who,
        Where,
        Go,
        Join,
        Work,
        Gielinor,
        Problem,
        Leave,
    }

    private companion object {
        const val SARIM_SQUIRE = "npc.pest_squire_ship_portsarim"
        const val OUTPOST_SQUIRE = "npc.pest_squire_ship_island"
        const val OUTPOST_NAME = "the Void Knight outpost"
        const val PORT_SARIM_NAME = "Port Sarim"

        val OUTPOST = CoordGrid(2659, 2675, 0)
        val PORT_SARIM = CoordGrid(3041, 3202, 0)
    }
}
