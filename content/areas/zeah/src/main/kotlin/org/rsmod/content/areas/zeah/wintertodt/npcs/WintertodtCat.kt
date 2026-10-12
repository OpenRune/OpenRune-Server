package org.rsmod.content.areas.zeah.wintertodt.npcs

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Cat_(Wintertodt_Camp)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WintertodtCat : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wint_troublecat") { startDialogue(it.npc) { cat() } }
    }

    private suspend fun Dialogue.cat() {
        if (CATSPEAK_AMULET !in player.worn) {
            chatNpc(neutral, "Meow.")
            return
        }
        chatPlayer(happy, "Hello puss.")
        chatNpc(happy, "Want a mouse?")
        chatPlayer(neutral, "No thanks.")
        chatNpc(happy, "Want this half dead bird?")
        chatPlayer(confused, "Errrr... no.")
        chatNpc(happy, "Want this crunched up spider?")
        chatPlayer(confused, "Um... not really.")
        chatNpc(quiz, "Oh well. Got any tuna?")
        if (TUNA !in player.inv) {
            chatPlayer(neutral, "No.")
            chatNpc(angry, "Hmmmph.")
            return
        }
        chatPlayer(happy, "Why yes I do have some tuna.")
        chatNpc(happy, "Give! *purr*")
        if (!choice2("Give a tuna to the cat.", true, "Keep the tuna for yourself.", false)) {
            chatPlayer(neutral, "Nah... my tuna.")
            chatNpc(angry, "Hissss...")
            return
        }
        if (player.invDel(player.inv, TUNA, count = 1).failure) {
            return
        }
        mesbox("You give some tuna to the cat. It devours it and purrs loudly.")
        chatNpc(happy, "Catch toad for me.")
        chatPlayer(confused, "What? I think not...")
    }

    private companion object {
        const val CATSPEAK_AMULET = "obj.twocats_amuletofcatspeak"
        const val TUNA = "obj.tuna"
    }
}
