package org.rsmod.content.generic.npcs.crabs

import jakarta.inject.Inject
import org.rsmod.content.generic.npcs.disguise.Disguise
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SandCrabs @Inject constructor(private val disguises: DisguisedNpcs) : PluginScript() {
    override fun ScriptContext.startup() {
        with(disguises) {
            for (disguise in SAND_CRABS) {
                bind(disguise)
            }
        }
    }

    companion object {
        val SAND_CRABS =
            listOf(
                Disguise(dormant = "npc.zeah_sandcrab_inactive", awake = "npc.zeah_sandcrab"),
                Disguise(
                    dormant = "npc.zeah_sandcrab_small_inactive",
                    awake = "npc.zeah_sandcrab_small",
                ),
            )
    }
}
