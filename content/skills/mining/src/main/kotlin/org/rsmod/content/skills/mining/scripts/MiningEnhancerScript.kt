package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.mining.MiningEquipment.wearingVarrockArmourAtLeast
import org.rsmod.content.skills.mining.configs.isGemRock
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningEnhancerScript
@Inject
constructor(private val random: GameRandom, private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.skill != "stat.mining") {
                return@onEvent
            }
            val source = product.source as? SkillingProductSource.Mining ?: return@onEvent
            val data = source.rockData
            val ore = product.item
            if (data.isGemRock || ore.startsWith("obj.uncut_")) {
                return@onEvent
            }

            if (data.miningCape && wearingMiningCape() && random.of(100) < 5) {
                awardBonus(ore, xp = product.experienceGranted, grantsXp = true, source)
            }
            if (player.wearingVarrockArmourAtLeast(data.varrockArmourLevel) && random.of(100) < 10) {
                awardBonus(ore, xp = product.experienceGranted, grantsXp = true, source)
            }
            if (data.celestialRing && wearingChargedCelestial() && random.of(100) < 10) {
                awardBonus(ore, xp = product.experienceGranted, grantsXp = true, source)
            }
        }
    }

    private fun SkillingActionCompleteEvent.awardBonus(
        ore: String,
        xp: Double,
        grantsXp: Boolean,
        source: SkillingProductSource.Mining,
    ) {
        player.awardSkillingProduct(
            eventBus,
            SkillingProduct(
                player = player,
                skill = "stat.mining",
                item = ore,
                count = 1,
                experience = xp,
                grantsExperience = grantsXp,
                source = source,
                isBonus = true,
            ),
        )
    }

    private fun SkillingActionCompleteEvent.wearingMiningCape(): Boolean =
        "obj.skillcape_mining" in player.worn || "obj.skillcape_mining_trimmed" in player.worn

    private fun SkillingActionCompleteEvent.wearingChargedCelestial(): Boolean =
        "obj.celestial_ring_charged" in player.worn ||
            "obj.celestial_signet_charged" in player.worn
}
