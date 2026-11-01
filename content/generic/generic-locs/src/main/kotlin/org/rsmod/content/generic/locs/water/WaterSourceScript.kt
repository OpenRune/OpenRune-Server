package org.rsmod.content.generic.locs.water

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.constants
import org.rsmod.api.enums.NamedEnums.water_container_fills
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WaterSourceScript : PluginScript() {
    private var wateringCans = emptySet<Int>()

    override fun ScriptContext.startup() {
        wateringCans = water_container_fills.filterValuesNotNull()
            .filter { (_, value) -> value.internalName == "obj.watering_can_8" }.map { it.key.id }.toSet() +
            "obj.zeah_wateringcan".asRSCM()
        onOpHeld4("obj.zeah_wateringcan") {
            mes("Your can has ${vars["varbit.zeah_wateringcan_charges"]} charges remaining.")
        }
        onOpContentLocU("content.water_source") { fill(it) { true } }
        // UNVERIFIED: other pumps follow Falador's source-free message (live capture).
        onOpContentLocU("content.water_source_pump") { fill(it, sourceSuffix = "") { true } }
        onOpContentLocU("content.water_source_tap") {
            fill(it) { key -> key.internalName != "obj.fishbowl_empty" }
        }
        onOpContentLocU("content.water_source_well") { fillWell(it) }
        onPlayerQueueWithArgs<CanFill>("queue.water_source_fill") { continueCans(it.args) }
        // UNVERIFIED: message type (wiki description)
        water_container_fills.filterValuesNotNull().keys.forEach { key ->
            onOpLocU("loc.civitas_bird_bath", key) {
                mes("There really isn't enough water in here to do that.")
            }
        }
    }

    private fun ProtectedAccess.fill(
        event: LocUDefaultEvents.OpContent,
        sourceSuffix: String = " from the ${event.type.name.lowercase()}",
        accepts: (ItemServerType) -> Boolean,
    ) {
        val filled = water_container_fills[event.objType]
        if (filled == null && event.objType.id !in wateringCans || !accepts(event.objType)) {
            // UNVERIFIED: unsupported containers get the engine default (guess)
            mes(constants.dm_default, ChatType.Engine)
            return
        }
        if (!fillContainer(event.invSlot, filled, event.objType, sourceSuffix)) return
        if (event.objType.id in wateringCans) {
            weakQueue("queue.water_source_fill", 4, CanFill(sourceSuffix))
        }
    }

    private fun ProtectedAccess.continueCans(task: CanFill) {
        val slot = inv.indexOfFirst {
            it != null && it.id in wateringCans &&
                WateringCans.needsRefill(player, checkNotNull(ServerCacheManager.getItem(it.id)).internalName)
        }
        if (slot < 0) return
        val container = checkNotNull(ServerCacheManager.getItem(checkNotNull(inv[slot]).id))
        if (fillContainer(slot, water_container_fills[container], container, task.sourceSuffix)) {
            weakQueue("queue.water_source_fill", 4, task)
        }
    }

    private fun ProtectedAccess.fillContainer(
        slot: Int,
        filled: ItemServerType?,
        container: ItemServerType,
        sourceSuffix: String,
    ): Boolean {
        val wateringCan = container.id in wateringCans
        if (wateringCan) {
            if (!WateringCans.refill(player, container.internalName, slot)) return false
        } else {
            // UNVERIFIED: other ordinary sources follow well/pump slot selection (guess).
            if (invReplace(inv, slot, 1, checkNotNull(filled)).failure) return false
        }
        anim(if (wateringCan) "seq.human_pickuptable" else "seq.human_pickuptable_walkmerge")
        // UNVERIFIED: tap/washbasin/geyser/barrel messages reuse this template
        // (wiki description).
        mes(
            "You fill the ${containerNoun(container)}$sourceSuffix.",
            ChatType.Spam,
        )
        soundSynth("synth.fill_container")
        return true
    }

    private fun ProtectedAccess.fillWell(event: LocUDefaultEvents.OpContent) {
        val key = event.objType.internalName
        when {
            key == "obj.bucket_empty" || key == "obj.cup_empty" -> {
                val filled = water_container_fills[event.objType] ?: return
                anim("seq.human_pickuptable_walkmerge")
                invReplace(inv, event.invSlot, 1, filled)
                mes("You fill the ${containerNoun(event.objType)} from the well.", ChatType.Spam)
                soundSynth("synth.fill_container_well")
            }
            event.objType.id in wateringCans -> {
                // UNVERIFIED: well can continuation follows other sources (live capture).
                fill(event) { true }
            }
            water_container_fills[event.objType] != null -> {
                val comma = if (key == "obj.jug_empty" || key == "obj.vial_empty") "," else ""
                mes(
                    "If I drop my ${containerNoun(event.objType)} down there$comma " +
                        "I don't think I'm likely to get it back."
                )
            }
            else -> mes(constants.dm_default, ChatType.Engine)
        }
    }

    private fun containerNoun(type: ItemServerType): String =
        type.name.removePrefix("Empty ").substringBefore('(').trim().lowercase()

    private data class CanFill(val sourceSuffix: String)
}
