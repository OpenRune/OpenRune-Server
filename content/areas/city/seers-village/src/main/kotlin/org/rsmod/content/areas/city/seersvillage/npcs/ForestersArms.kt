package org.rsmod.content.areas.city.seersvillage.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.spam
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bartender_(Forester%27s_Arms)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:alfred_grimhands_barcrawl] barcrawl drink
class ForestersArms @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.foresters_bartender") { startDialogue(it.npc) { bartender() } }
    }

    private suspend fun Dialogue.bartender() {
        chatNpc(happy, "Good morning, what would you like?")
        val option =
            choice3(
                "What do you have?",
                1,
                "Beer please.",
                2,
                "I don't really want anything thanks.",
                4,
            )
        when (option) {
            1 -> menu()
            2 -> beer()
            4 -> chatPlayer(neutral, "I don't really want anything thanks.")
        }
    }

    private suspend fun Dialogue.menu() {
        chatPlayer(quiz, "What do you have?")
        chatNpc(
            happy,
            "Well we have beer, or if you want some food, we have our home made stew and meat " +
                "pies.",
        )
        val option =
            choice4(
                "Beer please.",
                1,
                "I'll try the meat pie.",
                2,
                "Could I have some stew please?",
                3,
                "I don't really want anything thanks.",
                4,
            )
        when (option) {
            1 -> beer()
            2 -> {
                chatPlayer(happy, "I'll try the meat pie.")
                chatNpc(happy, "Ok, that'll be 16 coins.")
                buy(MEAT_PIE_PRICE, "obj.meat_pie", "You buy a nice hot meat pie.")
            }
            3 -> {
                chatPlayer(quiz, "Could I have some stew please?")
                chatNpc(happy, "A bowl of stew, that'll be 20 coins please.")
                buy(STEW_PRICE, "obj.stew", "You buy a bowl of home made stew.")
            }
            4 -> chatPlayer(neutral, "I don't really want anything thanks.")
        }
    }

    private suspend fun Dialogue.beer() {
        chatPlayer(happy, "Beer please.")
        chatNpc(happy, "One beer coming up. Ok, that'll be two coins.")
        buy(BEER_PRICE, "obj.beer", "You buy a pint of beer.")
    }

    private suspend fun Dialogue.buy(price: Int, obj: String, message: String) {
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
            return
        }
        player.invAddOrDrop(objRepo, obj)
        player.spam(message)
    }

    private companion object {
        const val BEER_PRICE = 2
        const val MEAT_PIE_PRICE = 16
        const val STEW_PRICE = 20
    }
}
