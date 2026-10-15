package org.rsmod.content.areas.city.edgeville.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Marley loitering by the graveyard, and Doris, Evil Dave's mum, in the house north of the bank. */
class EdgevilleLocals : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bim_marley_edge") { marley(it.npc) }
        onOpNpc1("npc.bim_marley") { marley(it.npc) }
        onOpNpc1("npc.hundred_dave_mum") { doris(it.npc) }
    }

    private suspend fun ProtectedAccess.marley(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(happy, "Hello there!")
            mesbox("He doesn't seem too interested in talking to you.")
        }
    }

    private suspend fun ProtectedAccess.doris(npc: Npc) {
        startDialogue(npc) {
            chatNpc(happy, "Hello again dearie. How are you doing?")
            when (
                choice4(
                    "Pretty good!",
                    1,
                    "Not too good actually",
                    2,
                    "What's it like living so close to the wilderness?",
                    3,
                    "How did Dave come to be evil?",
                    4,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "Pretty good!")
                    chatNpc(happy, "That's good to hear.")
                }
                2 -> {
                    chatPlayer(sad, "Not too good actually!")
                    chatNpc(sad, "Oh well.")
                }
                3 -> livingByTheWilderness()
                else -> howDaveTurnedEvil()
            }
        }
    }

    private suspend fun Dialogue.livingByTheWilderness() {
        chatPlayer(quiz, "What's it like living so close to the wilderness?")
        chatNpc(
            sad,
            "Oh, it's not all that bad. It was a bit scary at first but as long as I don't go " +
                "past the warning signs I'm all right.",
        )
        chatNpc(
            neutral,
            "It's actually pretty quiet here. I used to live in Lumbridge and, let me tell you, " +
                "that's the dangerous place for people like me.",
        )
        chatNpc(
            scared,
            "Thieves were picking people's pockets with impunity, and killing them on the " +
                "streets in broad daylight!",
        )
        chatNpc(
            sad,
            "And there weren't even any guards to protect us! Not that the guards in the other " +
                "cities do much good to protect people from what I've heard.",
        )
        chatNpc(
            neutral,
            "I just thank goodness I haven't got an Attack option any more! I wouldn't last " +
                "five minutes!",
        )
    }

    private suspend fun Dialogue.howDaveTurnedEvil() {
        chatPlayer(quiz, "How did Dave come to be evil?")
        chatNpc(
            neutral,
            "Oh, I don't know really. He's always been evil, I think. Maybe growing up so close " +
                "to the wilderness has affected him in some way.",
        )
        chatNpc(
            happy,
            "When he was little he used to want to go into Edgeville dungeon. You know how kids " +
                "are, always wanting to be dungeon adventurers! But he didn't want to be a " +
                "hero, he wanted to be a monster!",
        )
        chatNpc(
            neutral,
            "And then he spent all summer building a tree-house. Except it wasn't a tree-house, " +
                "of course, it was his Tower of Fear!",
        )
        chatNpc(
            sad,
            "Of course I was hoping he'd have grown out of it by now and maybe even left home. " +
                "But at least it keeps him happy.",
        )
    }

    private val Dialogue.scared
        get() = mesanim("mesanim.scared")
}
