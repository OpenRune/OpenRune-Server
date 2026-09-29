package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetMorphs
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GiantSquirrelDialogue @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
            onPetOpU(npc) { useItem(it.npc, it.objType) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "So how come you are so agile?")
                    chatNpc(
                        happy,
                        "If you were so nutty about nuts, maybe you would understand the great " +
                            "lengths we go to!",
                    )
                }
                1 -> {
                    chatPlayer(
                        laugh,
                        "What's up with all that squirrel fur? I guess fleas need a home too.",
                    )
                    chatNpc(angry, "You're pushing your luck! Stop it or you'll face my squirrely wrath.")
                }
                else -> {
                    chatPlayer(quiz, "Did you ever notice how big squirrels' teeth are?")
                    chatNpc(neutral, "No...")
                    chatPlayer(laugh, "You could land a gnome glider on those things!")
                    chatNpc(angry, "Watch it, I'll crush your nuts!")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        when {
            obj.name.equals("Acorn", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(happy, "Thank you! Ratatoskr be your guide and bring you many acorn trees!")
                    player.mes("Congratulations! You have paid your squirrel tax.... for now")
                }
            else -> startDialogue(npc) { chatNpc(angry, "That's not an acorn!") }
        }
    }

    private companion object {
        val FORMS =
            listOf(
                "npc.skillpet_agility",
                "npc.skillpet_agility_dark",
                "npc.skillpet_agility_bone",
            )
    }
}
