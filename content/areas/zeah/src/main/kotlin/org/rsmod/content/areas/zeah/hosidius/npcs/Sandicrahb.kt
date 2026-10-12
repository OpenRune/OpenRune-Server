package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sandicrahb
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:kourend_and_kebos] 5,000 coin fare after the easy diary and free trips
//  after the medium diary
// TODO(content): [verify:in_game] landing tiles (taken from Reign's Sandicrahb.java), the trip
//  transition, what the Travel op does, and the menu after "Why so much?" at the 10,000 coin fare
class Sandicrahb : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tribes_boatman") { startDialogue(it.npc) { sandicrahb() } }
        onOpNpc3("npc.tribes_boatman") { startDialogue(it.npc) { ferry() } }
        onOpNpc1("npc.tribes_boatman_island") { startDialogue(it.npc) { island() } }
        onOpNpc3("npc.tribes_boatman_island") { returnToBeach() }
    }

    private suspend fun Dialogue.sandicrahb() {
        chatNpc(happy, "Good day to you bwana.")
        while (true) {
            val option =
                choice4(
                    "Can you ferry me to that island?",
                    1,
                    "Who are you?",
                    2,
                    "Good day to you too.",
                    3,
                    "That's a nice boat!",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can you ferry me to that island?")
                    ferry()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Why bwana, I'm Sandicrahb! I travel far over the ocean from the " +
                            "tropical land of Karamja.",
                    )
                    chatPlayer(quiz, "How come you're charging so much?")
                    chatNpc(happy, "The gold is for a good cause, I promise bwana.")
                    chatPlayer(shifty, "Uh huh, that's a mighty fine gold necklace you have.")
                    chatNpc(
                        happy,
                        "Well bwana, of course I take a cut, but the funds keep these beaches " +
                            "clean and tidy, I promise!",
                    )
                    chatPlayer(neutral, "If you say so...")
                }
                3 -> {
                    chatPlayer(happy, "Good day to you too.")
                    return
                }
                else -> {
                    chatPlayer(
                        shifty,
                        "That's a nice boat! Would be a shame if something were to happen to it.",
                    )
                    chatNpc(
                        neutral,
                        "Made from the wood of the Fyburglars tree, strong and light weight! " +
                            "Perhaps if it went missing, I'd find you.",
                    )
                    chatPlayer(shifty, "Perhaps.")
                }
            }
        }
    }

    private suspend fun Dialogue.ferry() {
        while (true) {
            chatNpc(happy, "Most certainly, but my services come at a price... of 10,000 coins")
            if (player.inv.count(COINS) < FARE) {
                chatPlayer(angry, "I don't have that sort of money!")
                tooMuch()
                return
            }
            val option =
                choice3(
                    "Ok, here's the ridiculous sum of money...",
                    1,
                    "Never mind.",
                    2,
                    "Why so much? You'd get more business if you charged less.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(neutral, "Ok, here's the ridiculous sum of money...")
                    if (!player.invTakeFee(FARE)) {
                        return
                    }
                    mesbox("You pay Sandicrahb 10,000 coins and depart for Crabclaw Isle.")
                    access.telejump(ISLAND_LANDING, TeleportType.Exempt)
                    return
                }
                2 -> {
                    chatPlayer(neutral, "Never mind.")
                    return
                }
                else -> {
                    chatPlayer(
                        quiz,
                        "Why so much? You'd get more business if you charged less.",
                    )
                    chatNpc(
                        happy,
                        "The price is to keep the island quiet, bwana. It's a great place to " +
                            "train, but competing with others can be a hassle and so, for a " +
                            "price, you train in peace!",
                    )
                    val again =
                        choice2(
                            "Can you ferry me to that island?",
                            true,
                            "That's too much for me.",
                            false,
                        )
                    if (!again) {
                        chatPlayer(neutral, "That's too much for me.")
                        tooMuch()
                        return
                    }
                    chatPlayer(quiz, "Can you ferry me to that island?")
                }
            }
        }
    }

    private suspend fun Dialogue.tooMuch() {
        chatNpc(neutral, "Then you do not belong on my island.")
        chatPlayer(angry, "Fine!")
        chatNpc(happy, "Bye bye bwana.")
    }

    private suspend fun Dialogue.island() {
        chatNpc(quiz, "Shall I return you to the beach?")
        val leave =
            choice2(
                "Yes please, I'm done here.",
                true,
                "Actually, I've not quite finished yet, I'll stay.",
                false,
            )
        if (leave) {
            chatPlayer(happy, "Yes please, I'm done here.")
            access.returnToBeach()
        } else {
            chatPlayer(neutral, "Actually, I've not quite finished yet, I'll stay.")
            chatNpc(happy, "Very well bwana.")
        }
    }

    private fun ProtectedAccess.returnToBeach() {
        telejump(BEACH_LANDING, TeleportType.Exempt)
    }

    private companion object {
        const val COINS = "obj.coins"
        const val FARE = 10_000
        val ISLAND_LANDING = CoordGrid(1781, 3412, 0)
        val BEACH_LANDING = CoordGrid(1783, 3459, 0)
    }
}
