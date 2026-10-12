package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Petrified_Pete
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:volcanic_mine] paying the entry fee (30 numulites or 3,000 for
// permanent access), entering the mine, the Pay, Trade and Retrieve options, the ore shop, the
// item retrieval service and "Have you picked up anything I left in the mine?"
class PetrifiedPete : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fossil_mineguard") { startDialogue(it.npc) { pete() } }
    }

    private suspend fun Dialogue.pete() {
        chatPlayer(happy, "Hi there Pete.")
        chatNpc(shocked, "Argh! Who be there?")
        chatNpc(
            worried,
            "Oh, it's just the adventurer, phew! Things can be scary around here, I've seen some " +
                "weeeeeiiiird stuff in that cavern.",
        )
        val option =
            choice3("What is in the cavern?", 1, "Can I go inside?", 2, "I'll leave you be.", 3)
        when (option) {
            1 -> cavern()
            2 -> goInside()
            3 -> chatPlayer(neutral, "I'll leave you be.")
        }
    }

    private suspend fun Dialogue.cavern() {
        chatPlayer(quiz, "What is in the cavern?")
        chatNpc(shocked, "Oh no, now I'm thinking about it again! *screams*")
        chatPlayer(
            worried,
            "Okay okay, calm down! Why are you guarding it? What is the purpose of this cavern?",
        )
        chatNpc(
            worried,
            "It's a mine. It's special but dangerous, things move around and the mine could " +
                "collapse at any moment!.",
        )
        chatNpc(neutral, "I'd say you'll need at least level 50 Mining to be capable.")
        if (canEnter()) {
            chatPlayer(happy, "Well sounds like I am capable then!")
        } else {
            chatPlayer(sad, "I'm not quite up to scratch then...")
        }
        chatNpc(
            neutral,
            "The main idea is to chip down the big boulders, in order to find the fragments " +
                "inside. Deliver them to me and I'll show you some minin' tips to help you get " +
                "better at it.",
        )
        while (true) {
            val option =
                choice5(
                    "How do I traverse the mine?",
                    1,
                    "How do I keep the mine stable?",
                    2,
                    "Explain the dangers for me.",
                    3,
                    "Can I go inside?",
                    4,
                    "I'll leave you be.",
                    5,
                )
            when (option) {
                1 -> traverse()
                2 -> stability()
                3 -> dangers()
                4 -> {
                    goInside()
                    return
                }
                5 -> {
                    chatPlayer(neutral, "I'll leave you be.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.traverse() {
        chatPlayer(quiz, "How do I traverse the mine? Isn't it full of lava?")
        chatNpc(
            neutral,
            "I have a handy supply of heat-proof water containers. I'll give them out to anyone " +
                "going in with bit of water inside. They only hold so much though.",
        )
        chatNpc(
            neutral,
            "I reckon you could harden the lava with it! Just throw a bit of water on it. Make " +
                "yourself a path, but be careful, they won't last forever.",
        )
        chatPlayer(quiz, "Anything else?")
        chatNpc(
            sad,
            "Uh oh, there is one more thing, but it hurts. My backside has never felt the same " +
                "since, one of the reasons I don't dare go in there anymore. There are some gas " +
                "holes around, just feel 'em.",
        )
    }

    private suspend fun Dialogue.stability() {
        chatPlayer(quiz, "How do I keep the mine stable?")
        chatNpc(
            shocked,
            "Don't let it collapse!! It's petrifying, it will stick with you forever. The " +
                "rumbles create a flashback in my mind, arghhh.",
        )
        chatNpc(
            neutral,
            "There are some vents and chambers in there. Do better than I did with them, you " +
                "want around a 50% gas level in each chamber.",
        )
        chatNpc(
            neutral,
            "There are 3 in total, we spent awhile coming up with names, such as 'Vent west of " +
                "the pillar near the entrance on the left hand side', but decided A, B and C " +
                "would be easier.",
        )
        chatPlayer(quiz, "Presumably, a western chamber, central chamber and eastern chamber?")
        chatNpc(
            happy,
            "Yep! That's right. Block the chamber to let gas build up, unblock 'em to let gas " +
                "out. Any creatures in there will cause instability too.",
        )
        chatNpc(
            neutral,
            "Oh, also, if you have more than one rock to mine, keep them going at roughly the " +
                "same pace too. Everything gets a bit wobbly if you proceed too quickly down one " +
                "channel.",
        )
    }

    private suspend fun Dialogue.dangers() {
        chatPlayer(quiz, "Explain the dangers for me.")
        chatNpc(
            worried,
            "It is bad, very bad in there. If you're not prepared, you will be done for. " +
                "Wouldn't catch me in there.",
        )
        chatPlayer(quiz, "Okay, so how do I prepare?")
        chatNpc(
            worried,
            "Look out for the Lava beasts, they'll throw projectiles at anyone they see. You'll " +
                "have stalacmites falling on you, fire jumping out the lava towards you. " +
                "Seriously, you're insane.",
        )
        chatPlayer(neutral, "Thanks, I'll keep that in mind.")
    }

    private suspend fun Dialogue.goInside() {
        chatPlayer(quiz, "Can I go inside?")
        if (!canEnter()) {
            chatNpc(
                worried,
                "There are some crazy things down there, I don't think you're capable.",
            )
            mesbox("You need at least 50 Mining to enter the Volcanic Mine.")
            return
        }
        chatNpc(
            neutral,
            "Well luckily for you, or rather, unlucky for you, I'll let you go inside as you've " +
                "definitely got the mining skills.",
        )
        chatNpc(
            neutral,
            "I'll have to charge you though - upkeep of this place ain't cheap and cheerful. " +
                "I'll need 30 Numulites for one visit or 3,000 for unlimited trips.",
        )
        if (player.inv.count(NUMULITE) < ENTRY_FEE) {
            chatPlayer(sad, "I don't have 30 Numulites with me unfortunately.")
        }
    }

    private fun Dialogue.canEnter(): Boolean = player.stat("stat.mining") >= 50

    private companion object {
        const val NUMULITE = "obj.fossil_numulite"
        const val ENTRY_FEE = 30
    }
}
