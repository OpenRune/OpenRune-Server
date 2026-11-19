package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.shieldofarrav.ShieldOfArravQuest
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): [quest:priest_in_peril] [quest:the_blood_moon_rises] topics after those quests
class KingRoald @Inject constructor(private val shieldOfArrav: ShieldOfArravQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.king_roald") { startDialogue(it.npc) { kingRoald() } }
    }

    private suspend fun Dialogue.kingRoald() {
        chatPlayer(neutral, "Greetings, your majesty.")
        if (shieldOfArrav.canReclaimLamp(player)) {
            with(shieldOfArrav) { roaldLamp() }
            return
        }
        if (!shieldOfArrav.hasRoaldTopic(player)) {
            chatNpc(neutral, "Do you have anything of importance to say?")
            chatPlayer(neutral, "...Not really.")
            chatNpc(
                neutral,
                "You will have to excuse me, then. I am very busy as I have a kingdom to run!",
            )
            return
        }
        chatNpc(neutral, "Yes, citizen. Do you need something?")
        if (choice2("I'm here about the Shield of Arrav.", true, "Not really.", false)) {
            with(shieldOfArrav) { roaldShieldTopic() }
            return
        }
        chatPlayer(neutral, "Not really.")
        chatNpc(
            neutral,
            "You will have to excuse me then. I am very busy as I have a kingdom to run!",
        )
    }
}
