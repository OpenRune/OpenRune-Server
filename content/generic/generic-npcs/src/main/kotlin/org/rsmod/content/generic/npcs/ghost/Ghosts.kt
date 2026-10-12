package org.rsmod.content.generic.npcs.ghost

import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Ghosts : PluginScript() {
    override fun ScriptContext.startup() {
        for ((index, ghost) in MELZARS_MAZE_GHOSTS.withIndex()) {
            onOpNpc1(ghost) { startDialogue(it.npc) { melzarsMazeGhost(index) } }
        }
        onOpNpc1("npc.zamorakghost") { startDialogue(it.npc) { scorpiusGhost(it.npc) } }
        onOpNpc1("npc.prif_ghost_visible") { startDialogue(it.npc) { mynyddGhost() } }
        onOpNpc1("npc.shadow_maj_ghost") { startDialogue(it.npc) { generalsShadowGhost() } }
        onOpNpc1("npc.sotn_khazard_scout_vis") { startDialogue(it.npc) { ghorrockScout() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_(Melzar%27s_Maze)
    private suspend fun Dialogue.melzarsMazeGhost(variant: Int) {
        if (!player.understandsGhosts()) {
            chatNpc(neutral, MELZARS_MAZE_WAILS[variant])
            return
        }
        val lines = MELZARS_MAZE_LINES[variant]
        chatNpc(worried, lines[access.random.of(lines.size)])
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_(Grave_of_Scorpius)
    private suspend fun Dialogue.scorpiusGhost(npc: Npc) {
        chatNpc(neutral, "We are waiting for you to join us.")
        npc.queueCombatRetaliate(player)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_(Mynydd)
    private suspend fun Dialogue.mynyddGhost() {
        if (!player.understandsGhosts()) {
            chatNpc(neutral, "Woooo wooo wooooo woooo")
            mesbox("You cannot understand the ghost.")
            return
        }
        chatNpc(worried, "Don't send me back to that library!")
        chatPlayer(confused, "What?")
        chatNpc(worried, "It took me too long to get out of there... Just don't send me back!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_(The_General%27s_Shadow)
    // TODO(content): [miniquest:the_generals_shadow] replacing a lost shadow sword
    private suspend fun Dialogue.generalsShadowGhost() {
        if (!player.understandsGhosts()) {
            chatNpc(neutral, "Whoooo wooo Whooooooooo.")
            return
        }
        chatPlayer(quiz, "I fought a really big ghost dog in here - is he gone now?")
        chatNpc(neutral, "Yes, he is gone from this place. You did a great thing removing him.")
        if (playerContainsObj("obj.shadow_maj_shadow_sword")) {
            chatNpc(
                neutral,
                "You were well rewarded for you effort. But you should leave this place, for it " +
                    "is not wise to tarry.",
            )
        }
        chatPlayer(quiz, "Who are you anyway...or were you...?")
        chatNpc(
            neutral,
            "Who I was does not matter. Now, I am a guardian for the Shadow Realm and I am " +
                "protecting this area from any more disturbances.",
        )
        chatNpc(neutral, "There are those who walk in the Shadow Realm who should not.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Scout_(Ghorrock_Dungeon)
    private suspend fun Dialogue.ghorrockScout() {
        if (!player.understandsGhosts()) {
            chatNpc(neutral, "Woooo wooo wooooo woooo.")
            return
        }
        chatNpc(neutral, "Adventurer.")
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            neutral,
            "Our orders are to guard this door and notify General Khazard of anyone or anything " +
                "emerging from it. He has requested that our observations stretch to the Shadow " +
                "Realm, which is where I come in.",
        )
        chatNpc(neutral, "Now, I ask you not to disturb me further. Mine is a critical task.")
    }

    private companion object {
        val MELZARS_MAZE_GHOSTS =
            listOf(
                "npc.dragonslayer_ghost_1_key",
                "npc.dragonslayer_ghost_2",
                "npc.dragonslayer_ghost_3",
                "npc.dragonslayer_ghost_4",
                "npc.dragonslayer_ghost_5",
            )

        val MELZARS_MAZE_WAILS =
            listOf(
                "WOOOOO!",
                "WOOO woooo wooo WOOOO!",
                "Wooo wooo wooOOooOOoo!",
                "Wooo wooo woooooooo!",
                "Wooo wooo woooooooo!",
            )

        val MELZARS_MAZE_LINES =
            listOf(
                listOf(
                    "Aaaah!",
                    "Children, run quickly!",
                    "Oh my gods, the dragon is coming back!",
                    "Quickly, run!",
                ),
                listOf(
                    "Aaaah!",
                    "Get the women and children to the boats!",
                    "On the horizon! Do you see it?",
                    "Put that fire out!",
                ),
                listOf("Eeeek!", "EIEEEEE!", "Mummy, are we safe now?", "Mummy! Where are you?"),
                listOf(
                    "Aaaah!",
                    "I don't want to die like this!",
                    "Oh no!",
                    "Someone help me! I'm trapped!",
                ),
                listOf(
                    "AAARGH!",
                    "Come on, you monster! Just finish me!",
                    "It's hopeless. We're all going to die!",
                    "We're all doomed.",
                ),
            )
    }
}
