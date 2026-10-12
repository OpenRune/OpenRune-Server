package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Cook_(Blue_Moon_Inn)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] easy clue reward
class BlueMoonCook @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fai_varrock_bluemoon_chef") { startDialogue(it.npc) { cook() } }
    }

    private suspend fun Dialogue.cook() {
        chatNpc(angry, "What do you want? I'm a little busy here, so make it quick!")
        val option =
            choice3(
                "Can you sell me any food?",
                1,
                "Can you give me any free food?",
                2,
                "I don't want anything from this horrible kitchen.",
                3,
            )
        when (option) {
            1 -> sellCabbage()
            2 -> {
                chatPlayer(quiz, "Can you give me any free food?")
                chatNpc(angry, "Can you give me any free money?")
                chatPlayer(confused, "Why should I give you free money?")
                chatNpc(angry, "Why should I give you free food?")
                chatPlayer(angry, "Oh, forget it.")
            }
            3 -> {
                chatPlayer(angry, "I don't want anything from this horrible kitchen.")
                chatNpc(
                    angry,
                    "How dare you? I put a lot of effort into cleaning this kitchen. My daily " +
                        "sweat and elbow-grease keep this kitchen clean!",
                )
                chatPlayer(shocked, "Ewww!")
                chatNpc(angry, "Oh, just leave me alone.")
            }
        }
    }

    private suspend fun Dialogue.sellCabbage() {
        chatPlayer(quiz, "Can you sell me any food?")
        chatNpc(
            neutral,
            "I suppose I could sell you some cabbage, if you're willing to pay for it. Cabbage " +
                "is good for you.",
        )
        if (access.invTotal(player.inv, COINS) < PRICE) {
            chatPlayer(sad, "Oh, I haven't got any money.")
            chatNpc(
                angry,
                "Why are you asking me to sell you food if you haven't got any money? Go away!",
            )
            return
        }
        if (player.inv.isFull()) {
            chatPlayer(sad, "Oh, I haven't got enough space to carry it.")
            chatNpc(
                angry,
                "Why are you asking me to sell you food if you can't carry it? Go away!",
            )
            return
        }
        when (choice2("Alright, I'll buy a cabbage.", 1, "No thanks, I don't like cabbage.", 2)) {
            1 -> {
                chatPlayer(happy, "Alright, I'll buy a cabbage.")
                if (!player.invTakeFee(PRICE)) {
                    return
                }
                player.invAddOrDrop(objRepo, CABBAGE)
                chatNpc(
                    happy,
                    "It's a deal. Now, make sure you eat it all up. Cabbage is good for you.",
                )
            }
            2 -> {
                chatPlayer(neutral, "No thanks, I don't like cabbage.")
                chatNpc(angry, "Bah! People these days only appreciate junk food.")
            }
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val CABBAGE = "obj.cabbage"
        const val PRICE = 1
    }
}
