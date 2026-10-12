package org.rsmod.content.areas.zeah.shayzien.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server invs for Blair's Armour, Briget's Weapons and
//  Jennifer's General Field Supplies (Trade ops)
class ShayzienUnstockedShops : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shayzien_armourshop") { startDialogue(it.npc) { blair() } }
        onOpNpc1("npc.shayzien_weaponshop") { startDialogue(it.npc) { briget() } }
        onOpNpc1("npc.shayzien_generalstore") { startDialogue(it.npc) { jennifer() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blair
    private suspend fun Dialogue.blair() {
        chatNpc(neutral, "Looking to buy some armour?")
        if (choice2("Show me what you've got.", true, "I'm good thanks.", false)) {
            chatPlayer(happy, "Show me what you've got.")
        } else {
            chatPlayer(neutral, "I'm good thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Briget
    private suspend fun Dialogue.briget() {
        chatNpc(neutral, "What do you want? Weapons is what I got.")
        if (choice2("I'll take a look.", true, "I don't need anything.", false)) {
            chatPlayer(happy, "I'll take a look.")
        } else {
            chatPlayer(neutral, "I don't need anything.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jennifer_(Shayzien)
    private suspend fun Dialogue.jennifer() {
        chatNpc(happy, "What're yer looking for, me luv'?")
        if (!choice2("Let's trade.", true, "I don't need anything right now.", false)) {
            chatPlayer(neutral, "I don't need anything right now.")
            return
        }
        chatPlayer(happy, "Let's trade.")
        chatNpc(
            happy,
            "Well me luv', I've got some special rations for the soldiers in the fields " +
                "ou'there; courtesy of the mainland Void Knights. Take a looksie.",
        )
    }
}
