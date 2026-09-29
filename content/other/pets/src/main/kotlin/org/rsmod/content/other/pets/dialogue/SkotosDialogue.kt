package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkotosDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOpU(NPC) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (choice3("You look cute.", 1, "Where did you come from?", 2, "What can you do for me?", 3)) {
                1 -> {
                    chatPlayer(happy, "You look cute.")
                    chatNpc(
                        angry,
                        "I do not thinke thou understand the depths of the darkness you have " +
                            "unleashed upon the world. To dub it in such a scintillant manner is " +
                            "offensive to mine being.",
                    )
                    chatPlayer(quiz, "So why are you following me around?")
                    chatNpc(neutral, "Dark forces of which ye know nought have deemed that this is my geas.")
                    chatPlayer(confused, "Your goose?")
                    chatNpc(bored, "*Sighs* Nae. But thine is well and truly cooked.")
                }
                2 -> {
                    chatPlayer(quiz, "Where did you come from?")
                    chatNpc(
                        neutral,
                        "I am spawned of darkness. I am filled with darkness. I am darkness " +
                            "incarnate and to darkness I will return.",
                    )
                    chatPlayer(neutral, "Sounds pretty... dark.")
                    chatNpc(
                        angry,
                        "Knowest thou not of the cursed place? Knowest thou not about the future " +
                            "yet to befall your puny race?",
                    )
                    chatPlayer(bored, "Oh yes, I've heard that before.")
                    chatNpc(neutral, "Then it is good that ye can laugh in the face of the end.")
                    chatPlayer(confused, "The end has a face? Which end?")
                    chatNpc(bored, "*Sighs* The darkness giveth, and the darkness taketh.")
                }
                else -> {
                    chatPlayer(quiz, "What can you do for me?")
                    chatNpc(
                        neutral,
                        "Nothing. Ye are already tainted in my sight by the acts of light. However " +
                            "there may be some hope for you if you continue to aid the darkness.",
                    )
                    chatPlayer(happy, "I do have a lantern around here somewhere.")
                    chatNpc(angry, "Do not bring that foul and repellant thing near mine self.")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        val name = obj.name
        when {
            name.equals("Book of darkness", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(happy, "A book of darkness? I don't usually read, but that seems like a good book!")
                }
            LIGHT_SOURCE_WORDS.any { name.contains(it, ignoreCase = true) } ->
                startDialogue(npc) {
                    chatNpc(angry, "I told thee to keep thy filthy light away from me!")
                }
        }
    }

    private companion object {
        const val NPC = "npc.skotizo_pet"
        val LIGHT_SOURCE_WORDS = listOf("candle", "lantern", "torch", "firemaking cape", "bruma torch")
    }
}
