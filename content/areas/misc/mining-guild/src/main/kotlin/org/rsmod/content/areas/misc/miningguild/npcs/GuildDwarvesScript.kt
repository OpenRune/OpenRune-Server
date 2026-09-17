package org.rsmod.content.areas.misc.miningguild.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GuildDwarvesScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (entranceDwarf in ENTRANCE_DWARVES) {
            onOpNpc1(entranceDwarf) { talk(it.npc) { entranceGreeting() } }
        }
        for (doorGuard in DOOR_GUARDS) {
            onOpNpc1(doorGuard) { talk(it.npc) { doorGuardTalk() } }
        }
        onOpNpc1("npc.motherlode_mguild_guard") { talk(it.npc) { tunnelGuardTalk() } }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, conversation: suspend Dialogue.() -> Unit) =
        startDialogue(npc) { conversation() }

    private suspend fun Dialogue.entranceGreeting() {
        chatNpc(happy, "Welcome to the Mining Guild. Can I help you with anything?")
        guildQuestions()
    }

    private suspend fun Dialogue.tunnelGuardTalk() {
        chatNpc(neutral, "Hello there. Do you need anything?")
        chatPlayer(quiz, "What's through this cave?")
        chatNpc(
            neutral,
            "This cave leads to the Mining Guild, home to the finest mining site around. Is there " +
                "anything else I can help you with?",
        )
        guildQuestions()
    }

    private suspend fun Dialogue.guildQuestions() {
        while (true) {
            when (
                choice3(
                    "What have you got in the Guild?",
                    1,
                    "What do you dwarves do with the ore you mine?",
                    2,
                    "No thanks, I'm fine.",
                    3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "What have you got in the guild?")
                    describeGuild()
                }
                2 -> {
                    chatPlayer(quiz, "What do you dwarves do with the ore you mind?")
                    describeOreUse()
                }
                else -> {
                    chatPlayer(neutral, "No thanks, I'm fine.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.doorGuardTalk() {
        chatPlayer(quiz, "What's through this door?")
        chatNpc(
            neutral,
            "This leads deeper into the guild. Through here you'll find even more rocks including " +
                "runite and amethyst!",
        )
        chatPlayer(quiz, "Can I go in?")
        chatNpc(happy, "Of course. Go right ahead.")
    }

    private companion object {
        val ENTRANCE_DWARVES = listOf("npc.mguild_dwarf1", "npc.mguild_dwarf2")
        val DOOR_GUARDS = listOf("npc.mguild_guard1", "npc.mguild_guard2")
    }
}
