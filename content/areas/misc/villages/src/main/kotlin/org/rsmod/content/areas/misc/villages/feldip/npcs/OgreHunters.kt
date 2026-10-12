package org.rsmod.content.areas.misc.villages.feldip.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Gruh
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class OgreHunters : PluginScript() {
    override fun ScriptContext.startup() {
        for (hunter in HUNTERS) {
            onOpNpc1(hunter) { startDialogue(it.npc) { ogreHunter() } }
        }
    }

    private suspend fun Dialogue.ogreHunter() {
        chatNpc(neutral, LINES[access.random.of(LINES.size)])
    }

    private companion object {
        val HUNTERS = listOf("npc.ogre_hunter1", "npc.ogre_hunter2", "npc.ogre_hunter3")
        val LINES =
            listOf(
                "Yous creature doesn't stick mee's chompy, else mee's squeeze creature till yous " +
                    "a deadun!",
                "Yous creatures too weedy for da ogre stabby chucker, yous best stick wiv da " +
                    "cluks-cluks.",
                "Chompy is bestest yumms...",
                "Me'se can't wait for da chompy yumms...I's dribbling all down me's for da tasty " +
                    "chompy.",
                "Da fatsy toadies geddin' all messy...",
                "Me's gonna ged da pridy chompy hatsies...an looks like a good-un wiv da laydies.",
                "Mee's scared of Jiggig and da zogries, deyz is not da right dead-uns.",
                "Mee's got no stabbers no more...gonna go home wiv no chompy...",
                "I's stickin' da chompy...",
                "De'se stabbers are worser dan a dead dog....",
            )
    }
}
