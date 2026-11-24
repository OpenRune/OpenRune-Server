package org.rsmod.content.slayer.items

import jakarta.inject.Inject
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn4
import org.rsmod.content.slayer.dialogue.SlayerContact.checkSlayerTask
import org.rsmod.content.slayer.dialogue.SlayerContact.contactSlayerMaster
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
        for (ring in SLAYER_RINGS) {
            onOpHeld4(ring) { checkSlayerTask() }
            onOpWorn2(ring) { checkSlayerTask() }
            onOpWorn4(ring) { contactSlayerMaster(npcRepo) }
        }
    }

    private companion object {
        private val SLAYER_RINGS =
            listOf(
                "obj.slayer_ring_8",
                "obj.slayer_ring_7",
                "obj.slayer_ring_6",
                "obj.slayer_ring_5",
                "obj.slayer_ring_4",
                "obj.slayer_ring_3",
                "obj.slayer_ring_2",
                "obj.slayer_ring_1",
                "obj.slayer_ring_eternal",
            )
    }
}
