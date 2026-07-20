package org.rsmod.content.skills.mining.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.content.skills.mining.configs.miningXp
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MiningProductPrepareScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingProductPrepareEvent> {
            val source = product.source as? SkillingProductSource.Mining ?: return@onEvent
            applyEssenceUpgrade()
            applyBraceletOfClay(source.rockData)
            applySandstoneOrGranite(source.rockData)
            applyInfernalSmelt()
        }
    }

    private fun SkillingProductPrepareEvent.applyEssenceUpgrade() {
        if (product.item == "obj.blankrune" && product.player.miningLvl >= 30) {
            product.item = "obj.blankrune_high"
        }
    }

    private fun SkillingProductPrepareEvent.applyBraceletOfClay(data: MiningRocksRow) {
        if ("obj.jewl_bracelet_of_clay" !in product.player.worn) {
            return
        }
        if (product.item != "obj.clay" && product.item != "obj.softclay") {
            return
        }
        product.item = "obj.softclay"
        if (isSoftClayRock(data)) {
            product.count = 2
        }
    }

    private fun SkillingProductPrepareEvent.applySandstoneOrGranite(data: MiningRocksRow) {
        val ore = data.oreItem ?: return
        val key = RSCM.getReverseMapping(RSCMType.OBJ, ore.id)
        val xpMod = if (data.miningXp > 0) product.experience / data.miningXp else 1.0
        when (key) {
            "obj.enakh_sandstone_tiny" -> {
                val (item, xp) = random.pick(SANDSTONE_SIZES)
                product.item = item
                product.experience = xp * xpMod
            }
            "obj.enakh_granite_tiny" -> {
                val (item, xp) = random.pick(GRANITE_SIZES)
                product.item = item
                product.experience = xp * xpMod
            }
            "obj.rubium_geode" -> {
                product.experience = random.of(10, 100).toDouble() * xpMod
            }
            "obj.infernal_shale" -> {
                product.experience = random.of(10, 103).toDouble() * xpMod
            }
        }
    }

    private fun SkillingProductPrepareEvent.applyInfernalSmelt() {
        val hasInfernal =
            product.player.worn.any {
                it != null && getInvObj(it).internalName == "obj.infernal_pickaxe"
            } ||
                product.player.inv.any {
                    it != null && getInvObj(it).internalName == "obj.infernal_pickaxe"
                }
        if (!hasInfernal) {
            return
        }
        if (random.of(3) != 0) {
            return
        }
        val bar = ORE_TO_BAR[product.item] ?: return
        val smithXp = BAR_SMITH_XP[bar] ?: return
        product.item = bar
        product.count = 1
        product.player.statAdvance("stat.smithing", smithXp)
    }

    private fun isSoftClayRock(data: MiningRocksRow): Boolean {
        val ore = data.oreItem ?: return false
        return RSCM.getReverseMapping(RSCMType.OBJ, ore.id) == "obj.softclay"
    }

    private companion object {
        private val SANDSTONE_SIZES =
            listOf(
                "obj.enakh_sandstone_tiny" to 30.0,
                "obj.enakh_sandstone_small" to 40.0,
                "obj.enakh_sandstone_medium" to 50.0,
                "obj.enakh_sandstone_large" to 60.0,
            )

        private val GRANITE_SIZES =
            listOf(
                "obj.enakh_granite_tiny" to 50.0,
                "obj.enakh_granite_small" to 60.0,
                "obj.enakh_granite_medium" to 75.0,
            )

        private val ORE_TO_BAR =
            mapOf(
                "obj.copper_ore" to "obj.bronze_bar",
                "obj.tin_ore" to "obj.bronze_bar",
                "obj.iron_ore" to "obj.iron_bar",
                "obj.silver_ore" to "obj.silver_bar",
                "obj.gold_ore" to "obj.gold_bar",
                "obj.mithril_ore" to "obj.mithril_bar",
                "obj.adamantite_ore" to "obj.adamantite_bar",
                "obj.runite_ore" to "obj.runite_bar",
            )

        private val BAR_SMITH_XP =
            mapOf(
                "obj.bronze_bar" to 6.25,
                "obj.iron_bar" to 12.5,
                "obj.silver_bar" to 13.67,
                "obj.gold_bar" to 22.5,
                "obj.mithril_bar" to 30.0,
                "obj.adamantite_bar" to 37.5,
                "obj.runite_bar" to 50.0,
            )
    }
}
