package org.rsmod.content.slayer.items

import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.content.slayer.dialogue.SlayerContact.contactSlayerMaster
import org.rsmod.content.slayer.dialogue.SlayerContact.checkSlayerTask
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EnchantedGemScript @Inject constructor(private val npcRepo: NpcRepository) : PluginScript() {

    override fun ScriptContext.startup() {
        for (gem in listOf(
            "obj.slayer_gem",
            "obj.slayer_eternal_gem"
        )) {
            onOpHeld1(gem) { contactSlayerMaster(npcRepo) }
            onOpHeld2(gem) { checkSlayerTask() }
        }
    }
}
