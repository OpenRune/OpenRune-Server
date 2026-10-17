package org.rsmod.content.generic.npcs.crabs

import jakarta.inject.Inject
import org.rsmod.content.generic.npcs.disguise.Disguise
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Crabs @Inject constructor(private val disguises: DisguisedNpcs) : PluginScript() {
    override fun ScriptContext.startup() {
        with(disguises) {
            for (disguise in CRABS) {
                bind(disguise)
            }
        }
    }

    companion object {
        val CRABS =
            listOf(
                Disguise(dormant = "npc.zeah_sandcrab_inactive", awake = "npc.zeah_sandcrab"),
                Disguise(
                    dormant = "npc.zeah_sandcrab_small_inactive",
                    awake = "npc.zeah_sandcrab_small",
                ),
                Disguise(dormant = "npc.horror_rockcrab_inactive", awake = "npc.horror_rockcrab"),
                Disguise(
                    dormant = "npc.horror_rockcrab_small_inactive",
                    awake = "npc.horror_rockcrab_small",
                ),
                Disguise(dormant = "npc.giant_rockcrab_hidden", awake = "npc.giant_rockcrab"),
                Disguise(
                    dormant = "npc.giant_rockcrab_hidden_deeper",
                    awake = "npc.giant_rockcrab_deeper",
                ),
                Disguise(
                    dormant = "npc.giant_rockcrab_crypt_of_tonali_hidden",
                    awake = "npc.giant_rockcrab_crypt_of_tonali",
                ),
                Disguise(
                    dormant = "npc.fossil_ammonitecrab_inactive",
                    awake = "npc.fossil_ammonitecrab",
                ),
                Disguise(dormant = "npc.swampcrab_inactive", awake = "npc.swampcrab"),
                Disguise(
                    dormant = "npc.kourend_rockcrab_inactive",
                    awake = "npc.kourend_rockcrab",
                    tolerant = false,
                ),
                Disguise(
                    dormant = "npc.frostcrab_inactive",
                    awake = "npc.frostcrab",
                    tolerant = false,
                ),
                Disguise(
                    dormant = "npc.frostcrab_small_inactive",
                    awake = "npc.frostcrab_small",
                    tolerant = false,
                ),
            )
    }
}
