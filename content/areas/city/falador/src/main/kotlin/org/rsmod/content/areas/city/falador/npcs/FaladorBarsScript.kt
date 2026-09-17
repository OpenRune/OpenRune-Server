package org.rsmod.content.areas.city.falador.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The Rising Sun Inn barmaids and the Party Room's bar staff. */
class FaladorBarsScript @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, name) in RISING_SUN_BARMAIDS) {
            val counter = if (npc == TINA) null else RISING_SUN_BAR
            onCounterTalk(npc, counter) { startDialogue(it) { risingSunBarmaid(name) } }
        }
        onCounterTalk("npc.partyroom_barmaid") { npc -> startDialogue(npc) { megan(npc) } }
        onCounterTalk("npc.partyroom_barmaid2") { startDialogue(it) { lucy() } }
    }

    private suspend fun Dialogue.risingSunBarmaid(name: String) {
        chatNpc(happy, "Heya! What can I get you?")
        val glasses = player.inv.count(BEER_GLASS)
        if (glasses == 0) {
            servingAles(name)
            return
        }
        val glassOption = if (glasses == 1) "I've got this beer glass..." else "I've got these beer glasses..."
        if (choice2("What ales are you serving?", true, glassOption, false)) {
            servingAles(name)
            return
        }
        chatPlayer(neutral, glassOption)
        if (glasses == 1) {
            chatNpc(happy, "We'll buy it for a couple of coins if you're interested.")
        } else {
            chatNpc(happy, "Ooh, we'll buy those off you if you're interested. 2 coins per glass.")
        }
        if (!choice2("Okay, sure.", true, "No thanks, I like empty beer glasses.", false)) {
            chatPlayer(neutral, "No thanks, I like empty beer glasses.")
            return
        }
        chatPlayer(happy, "Okay, sure.")
        if (player.invDel(player.inv, BEER_GLASS, count = glasses).success) {
            player.invAdd(player.inv, "obj.coins", count = glasses * GLASS_PRICE)
        }
        chatNpc(happy, "There you go.")
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.servingAles(name: String) {
        chatPlayer(quiz, "What ales are you serving?")
        chatNpc(
            happy,
            "Well, we've got Asgarnian Ale, Wizard's Mind Bomb and Dwarven Stout, all for only 3 coins.",
        )
        val (line, ale) =
            choice4(
                "One Asgarnian Ale, please.",
                "One Asgarnian Ale, please." to "obj.asgarnian_ale",
                "I'll try the Mind Bomb.",
                "I'll try the Mind Bomb." to "obj.wizards_mind_bomb",
                "Can I have a Dwarven Stout?",
                "Can I have a Dwarven Stout?" to "obj.dwarven_stout",
                "I don't feel like any of those.",
                "I don't feel like any of those." to null,
            )
        chatPlayer(if (ale == null) neutral else happy, line)
        if (ale == null) {
            return
        }
        if (!player.invTakeFee(ALE_PRICE)) {
            chatNpc(angry, "I said 3 coins! You haven't got 3 coins!")
            chatPlayer(sad, "Sorry, I'll come back another day.")
            return
        }
        player.invAddOrDrop(objRepo, ale)
        chatPlayer(happy, "Thanks, $name.")
    }

    private suspend fun Dialogue.megan(npc: Npc) {
        chatNpc(happy, "Hi! I'm Megan. Welcome to the Party Room!")
        when (
            choice3(
                "One beer please Megan!",
                1,
                "Can you dance, Megan?",
                2,
                "Do you have any news?",
                3,
            )
        ) {
            1 -> {
                chatPlayer(happy, "One beer please Megan!")
                partyRoomBeer("Megan")
            }
            2 -> {
                chatPlayer(quiz, "Can you dance, Megan?")
                chatNpc(happy, "Can I dance?!")
                chatNpc(laugh, "CAN I dance?!!")
                chatPlayer(happy, "Dance with me Megan!")
                danceTogether(npc)
            }
            else -> {
                chatPlayer(quiz, "Do you have any news?")
                chatNpc(
                    neutral,
                    "Not at the moment. I've heard that the known world is expanding as new places " +
                        "are discovered.",
                )
                chatNpc(happy, "These are exciting times indeed!")
            }
        }
    }

    private suspend fun Dialogue.lucy() {
        chatNpc(happy, "Hi! I'm Lucy. Welcome to the Party Room!")
        chatPlayer(happy, "One beer please Lucy!")
        partyRoomBeer("Lucy")
    }

    private suspend fun Dialogue.partyRoomBeer(barmaid: String) {
        chatNpc(happy, "Coming right up! That's two gold please.")
        if (!player.invTakeFee(BEER_PRICE)) {
            chatPlayer(sad, "I'm sorry but I don't have enough money!")
            return
        }
        player.invAddOrDrop(objRepo, "obj.beer")
        objbox("obj.beer", "$barmaid has given you a beer.")
    }

    private suspend fun Dialogue.danceTogether(npc: Npc) {
        access.anim(SEQ_DANCE)
        npc.anim(SEQ_DANCE)
        access.delay(DANCE_CYCLES)
        access.anim(SEQ_CHEER)
        npc.anim(SEQ_CHEER)
        access.delay(CHEER_CYCLES)
        access.anim(SEQ_BOW)
        npc.anim(SEQ_BOW)
    }

    private companion object {
        const val BEER_GLASS = "obj.beer_glass"
        const val GLASS_PRICE = 2
        const val ALE_PRICE = 3
        const val BEER_PRICE = 2

        const val SEQ_DANCE = "seq.emote_dance"
        const val SEQ_CHEER = "seq.emote_cheer"
        const val SEQ_BOW = "seq.emote_bow"
        const val DANCE_CYCLES = 5
        const val CHEER_CYCLES = 3

        const val TINA = "npc.risingsun_barmaid3"

        val RISING_SUN_BAR =
            ServiceCounter(
                room = CoordGrid(2955, 3365, 0) to CoordGrid(2962, 3377, 0),
                customerX = 2956,
                counterZ = 3370..3374,
            )

        val RISING_SUN_BARMAIDS =
            listOf(
                "npc.risingsun_barmaid" to "Emily",
                "npc.risingsun_barmaid2" to "Kaylee",
                TINA to "Tina",
            )
    }
}
