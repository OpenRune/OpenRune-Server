package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VorkiDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Hey Vorki, got any interesting dragon facts?")
            when (access.random.of(10)) {
                0 ->
                    chatNpc(
                        neutral,
                        "Although they have wings, dragons rarely fly. This is because the animals " +
                            "they prey on are all ground dwelling.",
                    )
                1 ->
                    chatNpc(
                        neutral,
                        "Unlike their creators, dragons have the ability to reproduce. Like most " +
                            "reptiles, they are oviparous. This means that they lay eggs rather than " +
                            "birthing live young.",
                    )
                2 ->
                    chatNpc(
                        neutral,
                        "Dragons have a very long lifespan and can live for thousands of years. With " +
                            "a lifespan that long, most dragons die to combat instead of age.",
                    )
                3 ->
                    chatNpc(
                        neutral,
                        "While very closely related, dragons and wyverns are actually different " +
                            "species. You can easily tell the difference between them by counting the " +
                            "number of legs, dragons have four while wyverns have two.",
                    )
                4 ->
                    chatNpc(
                        neutral,
                        "Metallic dragons were created by inserting molten metal into the eggs of " +
                            "other dragons. Very few eggs survived this process.",
                    )
                5 ->
                    chatNpc(
                        neutral,
                        "The dragonkin created dragons by fusing their own lifeblood with that of a " +
                            "lizard. The dragonkin created other species in similar ways by using " +
                            "different types of reptile.",
                    )
                6 ->
                    chatNpc(
                        neutral,
                        "Dragons have the ability to speak. However, most dragons don't have the " +
                            "brain capacity to do it very well.",
                    )
                7 ->
                    chatNpc(
                        neutral,
                        "Dragons share their name with dragon equipment, which was also created by " +
                            "the dragonkin. This equipment is fashioned out of Orikalkum.",
                    )
                8 ->
                    chatNpc(
                        neutral,
                        "Although very aggressive, dragons do not typically stray from their own " +
                            "territory. They instead make their homes in places where there is plenty " +
                            "of prey to be found.",
                    )
                else ->
                    chatNpc(
                        neutral,
                        "Dragons have a duct in their mouth from which they can expel various " +
                            "internally produced fluids. The most common of these is a fluid which " +
                            "ignites when it reacts with air. This is how dragons breathe fire.",
                    )
            }
        }

    private companion object {
        const val NPC = "npc.vorkath_pet"
    }
}
