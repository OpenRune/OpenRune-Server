package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Kebab_seller
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] medium cipher clue and challenge scroll
class KebabSeller @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(KEBAB_SELLER) { startDialogue(it.npc) { kebabSeller() } }
    }

    private suspend fun Dialogue.kebabSeller() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello. What can I do for you?")
        chatPlayer(quiz, "I don't know, what can you do for me?")
        chatNpc(neutral, "Well, that depends.")
        chatPlayer(confused, "Depends on what?")
        chatNpc(neutral, "It depends on whether you like kebabs or not.")
        chatPlayer(quiz, "Why is that?")
        chatNpc(
            neutral,
            "Seeing as I'm in the kebab construction industry, I mainly help people in need of a " +
                "kebab.",
        )
        chatPlayer(quiz, "Well then, what kind of kebabs do you, er, construct?")
        chatNpc(
            happy,
            "I offer two different types of kebabs: the standard run- of-the-mill kebab seen " +
                "throughout Gielinor and enjoyed by many an intoxicated dwarf, and my speciality, " +
                "the extra-hot kebab. So which shall it be?",
        )
        var topic = initialMenu()
        while (true) {
            when (topic) {
                Topic.Kebab -> buyKebab()
                Topic.SuperKebab -> buySuperKebab()
                Topic.Sauce -> askForSauce()
                Topic.Difference -> {
                    topic = difference()
                    continue
                }
                Topic.Information -> information()
                Topic.Leave -> return
            }
            chatNpc(happy, "Is there anything else I can do for you?")
            if (!choice2("Yes, actually.", true, "No thanks, I'm good.", false)) {
                chatPlayer(happy, "No thanks, I'm good.")
                chatNpc(happy, "Come back soon, I could do with a few more customers.")
                return
            }
            topic = initialMenu()
        }
    }

    private suspend fun Dialogue.initialMenu(): Topic =
        choice5(
            "I want a standard kebab, please.",
            Topic.Kebab,
            "Could I have an extra-hot kebab please?",
            Topic.SuperKebab,
            "Would you sell me that bottle of special kebab sauce?",
            Topic.Sauce,
            "What is the difference between the standard and the extra-hot kebab?",
            Topic.Difference,
            "I need some information.",
            Topic.Information,
        )

    private suspend fun Dialogue.buyKebab() {
        chatPlayer(happy, "I want a standard kebab, please.")
        chatNpc(happy, "That will be three gold coins.")
        if (buy(KEBAB, KEBAB_PRICE)) {
            chatNpc(happy, "Here you go.")
            chatPlayer(happy, "Thanks.")
        } else {
            chatPlayer(sad, "I seem to be short on coins. Sorry.")
        }
    }

    private suspend fun Dialogue.buySuperKebab() {
        chatPlayer(happy, "Could I have one of those crazy hot kebabs of yours?")
        chatNpc(
            happy,
            "One super kebab coming up! Be careful, they really are as hot as they're made out to " +
                "be.",
        )
        chatPlayer(happy, "Sure, sure.")
        if (buy(SUPER_KEBAB, SUPER_KEBAB_PRICE)) {
            chatNpc(happy, "Here you go.")
            chatPlayer(happy, "Thanks.")
        } else {
            chatPlayer(sad, "I seem to be short on coins. Sorry.")
        }
    }

    // https://oldschool.runescape.wiki/w/Red_hot_sauce
    // TODO(content): [verify:in_game] what he says when the player already owns a bottle
    private suspend fun Dialogue.askForSauce() {
        chatPlayer(quiz, "Would you sell me that bottle of special kebab sauce?")
        chatNpc(neutral, "Well, that depends. What do you intend to do with it?")
        chatPlayer(happy, "Don't worry, I've no intention of setting up a rival kebab shop.")
        chatNpc(confused, "But then what do you want it for?")
        chatPlayer(happy, "I don't know, but I think I could have a little bit of fun with it.")
        chatNpc(
            shifty,
            "You're not going to put it in Drunken Ali's drink, are you? That's what happened the " +
                "last time I gave someone the sauce.",
        )
        chatPlayer(
            happy,
            "No, that would be a bit cliché. I think I'll come up with something more original.",
        )
        if (playerContainsObj(SAUCE)) {
            return
        }
        if (access.inv.isFull()) {
            chatPlayer(
                neutral,
                "Actually, I might get back to you on the sauce. I'm a bit overburdened as it is.",
            )
            return
        }
        player.invAddOrDrop(objRepo, SAUCE)
        chatNpc(neutral, "Just be careful with it, it's potent enough to give a camel the runs.")
        chatPlayer(happy, "Thank you very much.")
    }

    private suspend fun Dialogue.difference(): Topic {
        chatPlayer(quiz, "What is the difference between the standard and the extra-hot kebab?")
        chatNpc(
            shifty,
            "If I told you that I would have to kill you. Kebab construction is a cut-throat " +
                "industry. The extra-hot kebab gives me my competitive edge and if I were to " +
                "divulge my secrets to every passing adventurer, I'd",
        )
        chatNpc(shifty, "soon be out of business.")
        chatPlayer(quiz, "So there's no difference except the sauce you use?")
        chatNpc(
            worried,
            "Shh! Keep your voice down. You should have told me you were from the union.",
        )
        val topic =
            choice4(
                "I want a kebab, please.",
                Topic.Kebab,
                "Could I have an extra-hot kebab please?",
                Topic.SuperKebab,
                "Would you sell me that bottle of special kebab sauce?",
                Topic.Sauce,
                "Listen I've got to run. I'll catch you later.",
                Topic.Leave,
            )
        if (topic == Topic.Leave) {
            chatPlayer(neutral, "Listen, I've got to run. I'll catch you later.")
        }
        return topic
    }

    private suspend fun Dialogue.information() {
        chatPlayer(quiz, "I need some information.")
        chatNpc(
            neutral,
            "If information is what you need, there are many better people to ask than myself " +
                "who could help you. One of the town's street urchins or perhaps the local drunk. " +
                "I may be many things to many different people, but a",
        )
        chatNpc(neutral, "gossip I am not.")
    }

    private fun Dialogue.buy(obj: String, price: Int): Boolean {
        if (!player.invTakeFee(price)) {
            return false
        }
        player.invAddOrDrop(objRepo, obj)
        return true
    }

    private enum class Topic {
        Kebab,
        SuperKebab,
        Sauce,
        Difference,
        Information,
        Leave,
    }

    private companion object {
        const val KEBAB_SELLER = "npc.feud_kebabman_multi"
        const val KEBAB = "obj.kebab"
        const val SUPER_KEBAB = "obj.super_kebab"
        const val SAUCE = "obj.superhot_kebab_sauce"
        const val KEBAB_PRICE = 3
        const val SUPER_KEBAB_PRICE = 5
    }
}
