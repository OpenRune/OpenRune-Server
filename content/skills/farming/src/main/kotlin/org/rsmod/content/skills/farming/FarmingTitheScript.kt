package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.instances.events.InstancePlayerLeaveUnboundEvent
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.content.interfaces.omnishop.openOmnishop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingTitheScript @Inject constructor(private val tithe: FarmingTithe) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.tithe_plant_seed_table") {
            val tier = choice3("Golovanova (34 Farming)", 1, "Bologano (54 Farming)", 2, "Logavano (74 Farming)", 3)
            val crop = checkNotNull(FarmingTitheData.crop(tier))
            val count = countDialog("How many seeds would you like? (Up to 10,000)")
            if (!takeTitheSeeds(player, crop, count)) mes("You cannot take those seeds.")
        }
        onOpLoc1("loc.hosidius_tithe_farm_door") {
            if (player.vars[FarmingTithe.SESSION] > 0) tithe.leave(player) else tithe.enter(player)
        }
        onOpLocU(FarmingTithe.EMPTY) {
            val crop = FarmingTitheData.crops.firstOrNull { crop -> crop.seed.id == it.objType.id }
            if (crop != null && !tithe.plant(player, it.loc, crop, it.invSlot)) mes(NOTHING)
        }
        for (art in FarmingTitheData.row.stages) {
            val dry = art.t2.internalName
            val wet = art.t3.internalName
            val dead = art.t4.internalName
            if (art.t1 < 4) {
                onOpLoc1(dry) { if (!tithe.water(player, it.loc)) mes("You need water to tend this plant.") }
                for (loc in listOf(dry, wet)) {
                    onOpLocU(loc) {
                        when {
                            WateringCans.isCan(it.objType.internalName) ->
                                if (!tithe.water(player, it.loc, it.objType.internalName, it.invSlot)) mes(NOTHING)
                            FarmingComposts.byObj(it.objType.internalName) != null ->
                                mes("Only sulphurous fertiliser is effective on these plants.")
                            else -> mes(NOTHING)
                        }
                    }
                }
                onOpLoc1(dead) { if (!tithe.clear(player, it.loc)) mes("You need a spade to clear this plant.") }
            } else {
                onOpLoc1(dry) { if (!tithe.harvest(player, it.loc)) mes("You cannot harvest this plant right now.") }
            }
        }
        for (sack in listOf("loc.tithe_sack_of_fruit_empty", "loc.tithe_sack_of_fruit")) {
            onOpLoc1(sack) { tithe.deposit(player) }
        }
        for (crop in FarmingTitheData.crops) {
            onOpHeld1(crop.fruit.internalName) { mes("The scent doesn't appeal to you.") }
        }
        onOpNpc1(FARMER) {
            chatNpc(it.npc, mesanims.happy, "Hello, young one. Did you want to work on my farm?")
            when (choice3("Tell me about your farm.", 1, "I'd like to work on your farm.", 2, "Have you any rewards?", 3)) {
                1 -> chatNpc(it.npc, mesanims.happy, "I grow fruit here. Special fruit. It grows very fast, but it needs a lot of tending, otherwise it gets the blight and dies.")
                2 -> chatNpc(it.npc, mesanims.happy, "Take a handful of seeds from the table, and head into the farm. When you've harvested some fruit, deposit it straight into the sacks.")
                3 -> openRewards()
            }
        }
        onOpNpc3(FARMER) { openRewards() }
        onOpNpcU(FARMER) {
            val row = FarmingTitheData.row.refunds.firstOrNull { row -> row.t0.id == it.objType.id }
            if (row != null) refund(it.objType.id, it.invSlot, row.t1) else mes("No thanks.")
        }
        onPlayerLogin { tithe.login(player) }
        onPlayerLogout { tithe.logout(player) }
        onPlayerCoordsChanged { tithe.tick(player) }
        onPlayerSoftTimer(FarmingTithe.TIMER) { tithe.tick(player) }
        onEvent<InstancePlayerLeaveUnboundEvent> {
            if (key == FarmingTithe.KEY) tithe.departed(player, instanceId)
        }
    }
    private fun ProtectedAccess.openRewards() {
        FarmingTitheRewards.refreshCanOwnership(player)
        openOmnishop(FarmingTitheRewards.SHOP)
    }

    private suspend fun ProtectedAccess.refund(obj: Int, slot: Int, points: Int) {
        if (choice2("Exchange the item for $points reward points?", true, "Cancel", false) != true) return
        if (player.vars[FarmingTithe.POINTS] > FarmingTitheData.row.pointsCap - points) {
            mes("You cannot hold that many reward points.")
            return
        }
        refundTitheOutfit(player, obj, slot, points)
    }

    private companion object {
        const val FARMER = "npc.hosidius_tithe_farmer"
    }
}
