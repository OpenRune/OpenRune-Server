package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.other.pets.cats.CatCare
import org.rsmod.content.other.pets.cats.CatColour
import org.rsmod.content.other.pets.cats.CatStage
import org.rsmod.content.other.pets.cats.Cats
import org.rsmod.content.other.pets.cats.catMedalGiven
import org.rsmod.content.other.pets.cats.catRatsCaught
import org.rsmod.content.other.pets.onPetOpIfPresent
import org.rsmod.content.other.pets.storesAnyObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Gertrude @Inject constructor(private val care: CatCare) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(GERTRUDE) { talk(it.npc) }
        onPetOpIfPresent(GERTRUDE, KITTEN_OP) { quickBuy(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            if (medalDue(player)) {
                medal()
                return@startDialogue
            }
            chatPlayer(happy, "Hello again.")
            chatNpc(quiz, "Did you ever get to talk to those ratcatchers I told you about?")
            chatPlayer(neutral, "Yes, they were... interesting...")
            chatNpc(happy, "Well, my dear. Is there anything I can do for you?")
            when (choice2("Do you have any more kittens?", 1, "I'll be off. See you another time.", 2)) {
                1 -> askForKitten()
                2 -> chatPlayer(neutral, "I'll be off. See you another time.")
            }
        }

    private suspend fun Dialogue.medal() {
        chatPlayer(happy, "Hello again Gertrude!")
        chatNpc(happy, "Well, hello adventurer! How are you?")
        chatPlayer(happy, "My cat has caught 100 rats!")
        if (access.inv.isFull()) {
            chatNpc(happy, "Well well! You are good with cats! I'd give you a little present if you had space to take it.")
            chatPlayer(happy, "That's very kind of you - I'll come back again when I've got more space.")
            return
        }
        chatNpc(happy, "Well well! You are good with cats! Here, I have a little present for you...")
        objbox(MEDAL, "Gertrude shows you a small medal.")
        chatPlayer(happy, "Hey, thanks Gertrude.")
        access.invAdd(access.inv, MEDAL, 1)
        player.catMedalGiven = true
    }

    private suspend fun Dialogue.askForKitten() {
        chatPlayer(quiz, "Do you have any more kittens?")
        if (hasYoungCat(player)) {
            chatNpc(
                neutral,
                "Aren't you still raising that other kitten? Only once it's fully grown and it no longer " +
                    "needs your attention will I let you have another kitten.",
            )
            return
        }
        chatNpc(happy, "Indeed I have. They are 100 coins each, do you want one?")
        if (access.invTotal(access.inv, COINS) < PRICE) {
            chatPlayer(sad, "Oops, looks like I'm a bit short. I'll have to come back later.")
            return
        }
        val option =
            if (wearingCharos(player)) {
                choice3("Yes please.", 1, "No thanks.", 2, "[Charm] I'm quite fussy over cats - can I pick my own?", 3)
            } else {
                choice2("Yes please.", 1, "No thanks.", 2)
            }
        when (option) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                chatNpc(happy, "Okay then, here you go.")
                chatPlayer(happy, "Thanks.")
                if (access.giveKitten(access.randomColour())) {
                    mesbox("Gertrude gives you another kitten.")
                }
            }
            2 -> chatPlayer(neutral, "No thanks, I've paid that boy enough already.")
            3 -> {
                chatPlayer(happy, "I'm quite fussy over cats - can I pick my own?")
                chatNpc(happy, "Well okay, but they're all just as cuddly as each other.")
                val colour = pickColour() ?: return
                if (access.giveKitten(colour)) {
                    mesbox("Gertrude gives you another kitten.")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.quickBuy(npc: Npc) {
        if (hasYoungCat(player)) {
            startDialogue(npc) {
                chatNpc(
                    neutral,
                    "Aren't you still raising that other kitten? Only once it's fully grown and it no " +
                        "longer needs your attention will I let you have another kitten.",
                )
            }
            return
        }
        if (invTotal(inv, COINS) < PRICE) {
            startDialogue(npc) { chatNpc(neutral, "I'll want 100 coins for a kitten.") }
            return
        }
        startDialogue(npc) {
            val colour = if (wearingCharos(player)) pickColour() ?: return@startDialogue else access.randomColour()
            if (access.giveKitten(colour)) {
                mesbox("Gertrude gives you another kitten.")
            }
        }
    }

    private suspend fun Dialogue.pickColour(): CatColour? {
        val colours = CatColour.natural
        val selection = access.menu(PICK_TITLE, *colours.map { it.name }.toTypedArray())
        return colours.getOrNull(selection)
    }

    private fun ProtectedAccess.randomColour(): CatColour = CatColour.natural[random.of(CatColour.natural.size)]

    private fun ProtectedAccess.giveKitten(colour: CatColour): Boolean {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return false
        }
        if (!player.invTakeFee(PRICE)) {
            return false
        }
        invAdd(inv, Cats.of(CatStage.Kitten, colour).obj, 1)
        care.resetKitten(player)
        return true
    }

    private fun wearingCharos(player: Player): Boolean = CHAROS in player.worn

    private fun medalDue(player: Player): Boolean =
        !player.catMedalGiven && player.catRatsCaught >= MEDAL_RATS && hasYoungCat(player)

    private fun hasYoungCat(player: Player): Boolean =
        ownsStage(player, CatStage.Kitten) || ownsStage(player, CatStage.Cat)

    private fun ownsStage(player: Player, stage: CatStage): Boolean =
        care.following(player)?.stage === stage || player.storesAnyObj(Cats.objsOf(stage))

    private companion object {
        const val GERTRUDE = "npc.gertrude"
        const val KITTEN_OP = "Kitten"
        const val COINS = "obj.coins"
        const val CHAROS = "obj.ring_of_charos_unlocked"
        const val MEDAL = "obj.felinemedal"
        const val PRICE = 100
        const val MEDAL_RATS = 100
        const val PICK_TITLE = "Pick a kitten"
    }
}
