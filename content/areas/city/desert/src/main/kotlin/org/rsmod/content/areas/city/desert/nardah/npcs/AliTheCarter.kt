package org.rsmod.content.areas.city.desert.nardah.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ali_the_Carter (before Spirits of the Elid)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:spirits_of_the_elid] curse question during the quest and the glum
//  dialogue after it
// TODO(content): [system:loc_interactions] items used on Ali's water cart and barrel
class AliTheCarter @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ali_the_carter") { startDialogue(it.npc) { carter() } }
    }

    private suspend fun Dialogue.carter() {
        chatNpc(
            happy,
            "Water, water get your fresh water here, 1000 coins a waterskin, a bargain in these " +
                "difficult times.",
        )
        val option =
            choice3(
                "That's extortionate! Why so much?",
                1,
                "Ok, I'll buy some!",
                2,
                "I think I'll give it a miss.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(angry, "That's extortionate! Why so much?")
                chatNpc(shifty, "Well it's the cheapest water you'll find in this town.")
            }
            2 -> buyWater()
            3 -> chatPlayer(neutral, "I think I'll give it a miss.")
        }
    }

    private suspend fun Dialogue.buyWater() {
        if (!player.invTakeFee(WATER_PRICE)) {
            mesbox("You don't have 1000 coins to buy the water with.")
            return
        }
        player.invAddOrDrop(objRepo, WATERSKIN)
        mesbox("You buy a full waterskin for 1000 coins!")
    }

    private companion object {
        const val WATERSKIN = "obj.water_skin4"
        const val WATER_PRICE = 1000
    }
}
