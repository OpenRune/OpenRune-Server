package org.rsmod.content.areas.misc.morytania.darkmeyer

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] the disguised dialogue before The Blood Moon Rises;
//  without it every shopkeeper sends the player back to the slave pens
// TODO(content): [system:shops] the Darkmeyer General Store, Lantern Shop, Meat Shop and
//  Seamstress have no inv in the cache dump, so neither Talk-to nor Trade opens them
class DarkmeyerShopkeepers @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.darkm_trader_general") { startDialogue(it.npc) { drasdan() } }
        onOpNpc1("npc.darkm_trader_candles") { startDialogue(it.npc) { grinka() } }
        onOpNpc1("npc.darkm_trader_food") { startDialogue(it.npc) { lenyig() } }
        onOpNpc1("npc.darkm_trader_clothes") { startDialogue(it.npc) { varrian() } }
        onOpNpc1(DESPOINA) { startDialogue(it.npc) { despoina() } }
    }

    private fun Dialogue.freedFromTithes(): Boolean =
        QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)

    private suspend fun Dialogue.slavePens() {
        chatNpc(angry, "Human! Get back to the slave pens before I call the guards!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drasdan_Ranor
    private suspend fun Dialogue.drasdan() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(neutral, "Looking to trade?")
        if (choice2("What have you got?", true, "No thanks.", false)) {
            chatPlayer(quiz, "What have you got?")
            chatNpc(neutral, "Take a look.")
            return
        }
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Then we have nothing more to discuss.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grinka_Krast
    private suspend fun Dialogue.grinka() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(neutral, "Yes?")
        chatPlayer(quiz, "What are you selling?")
        chatNpc(neutral, "Lanterns. Want one?")
        while (true) {
            val option =
                choice3("Yes please.", 1, "Why are you selling lanterns?", 2, "No thanks.", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Why are you selling lanterns?")
                    chatNpc(neutral, "It's dark around here.")
                    chatPlayer(neutral, "You make a very valid point.")
                    chatNpc(neutral, "Yes. Want one?")
                }
                3 -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Suit yourself.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lenyig_Karna (after The Blood Moon Rises)
    private suspend fun Dialogue.lenyig() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(happy, "Looking for a bit of meat?")
        chatPlayer(confused, "I beg your pardon?")
        chatNpc(happy, "Meat! We've got all the good stuff here.")
        while (true) {
            val option =
                choice3(
                    "Show me what you've got.",
                    1,
                    "Why do you have all this meat?",
                    2,
                    "I'm good, thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Show me what you've got.")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Why do you have all this meat?")
                    chatNpc(
                        neutral,
                        "Some prefer solids to go with their blood. That, and the werewolf and " +
                            "human visitors need something to eat too.",
                    )
                    chatPlayer(quiz, "Where does it all come from?")
                    chatNpc(
                        shifty,
                        "Well, it's not like anything nefarious if that's what you're worried " +
                            "about... Not any more at least...",
                    )
                    chatPlayer(worried, "You know what, I think I've heard enough.")
                    chatNpc(neutral, "Whatever you say. So, want some meat?")
                }
                3 -> {
                    chatPlayer(neutral, "I'm good, thanks.")
                    chatNpc(happy, "Come back if you ever want some meat!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Varrian_Sobak (after The Blood Moon Rises)
    private suspend fun Dialogue.varrian() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(happy, "Now you look like someone in need of some new threads.")
        while (true) {
            val option = choice3("Let's trade.", 1, "Do I?", 2, "I'm fine, thanks.", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's trade.")
                    return
                }
                2 -> {
                    chatPlayer(confused, "Do I?")
                    chatNpc(
                        happy,
                        "Oh yes. And luckily for you, I have everything you could ever need. " +
                            "Clothes, or the materials to make them if that's more your thing. " +
                            "All pre-scented as well.",
                    )
                    chatPlayer(quiz, "Pre-scented?")
                    chatNpc(
                        shifty,
                        "Oh don't go acting like you don't know what I mean. Humans may be " +
                            "allowed here now, but it's still much easier for you if everyone " +
                            "thinks you're a noble vyre.",
                    )
                    chatPlayer(quiz, "Does it not bother you having humans here?")
                    chatNpc(
                        neutral,
                        "Me? No. As long as you've got money, I don't care who you are.",
                    )
                    chatPlayer(neutral, "Fair enough.")
                    chatNpc(quiz, "So, want to take a look?")
                }
                3 -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    chatNpc(neutral, "I'll be here if you change your mind.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Despoina_Callidra
    private suspend fun Dialogue.despoina() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(neutral, "Yes?")
        chatPlayer(quiz, "What are you serving?")
        chatNpc(neutral, "Blood.")
        chatPlayer(quiz, "Is that it?")
        chatNpc(neutral, "Yes, by the pint. Now, do you want one or not?")
        if (!choice2("Sure.", true, "I'll pass.", false)) {
            chatPlayer(neutral, "I'll pass.")
            chatNpc(angry, "Clear off then.")
            return
        }
        chatPlayer(happy, "Sure.")
        chatNpc(neutral, "Two coins.")
        if (!player.invTakeFee(BLOOD_PINT_PRICE)) {
            chatPlayer(sad, "Oh dear, I don't seem to have enough money.")
            chatNpc(angry, "Clear off then.")
            return
        }
        player.invAddOrDrop(objRepo, BLOOD_PINT)
        mesbox("You buy a pint of blood.")
    }

    private companion object {
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val DESPOINA = "npc.darkm_trader_pub"
        const val BLOOD_PINT = "obj.blood_pint"
        const val BLOOD_PINT_PRICE = 2
    }
}
