package org.rsmod.content.skills.mining

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.table.mining.MiningRocksRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object MiningEquipment {
    private val CHARGED_GLORY =
        listOf(
            "obj.amulet_of_glory_1",
            "obj.amulet_of_glory_2",
            "obj.amulet_of_glory_3",
            "obj.amulet_of_glory_4",
            "obj.amulet_of_glory_5",
            "obj.amulet_of_glory_6",
            "obj.amulet_of_glory_inf",
            "obj.trail_amulet_of_glory_1",
            "obj.trail_amulet_of_glory_2",
            "obj.trail_amulet_of_glory_3",
            "obj.trail_amulet_of_glory_4",
            "obj.trail_amulet_of_glory_5",
            "obj.trail_amulet_of_glory_6",
        )

    private val BASIC_ORES =
        setOf(
            "obj.copper_ore",
            "obj.tin_ore",
            "obj.iron_ore",
            "obj.silver_ore",
            "obj.lead_ore",
            "obj.coal",
            "obj.gold_ore",
        )

    private val SUPERIOR_ORES = setOf("obj.mithril_ore", "obj.adamantite_ore")

    private val EXPERT_ONLY_ORES =
        setOf("obj.runite_ore", "obj.enakh_sandstone_tiny", "obj.amethyst")

    private val PROSPECTOR_HATS =
        listOf("obj.motherlode_reward_hat", "obj.motherlode_reward_hat_gold")
    private val PROSPECTOR_TOPS =
        listOf("obj.motherlode_reward_top", "obj.motherlode_reward_top_gold")
    private val PROSPECTOR_LEGS =
        listOf("obj.motherlode_reward_legs", "obj.motherlode_reward_legs_gold")
    private val PROSPECTOR_BOOTS =
        listOf("obj.motherlode_reward_boots", "obj.motherlode_reward_boots_gold")

    fun Player.wearingChargedGlory(): Boolean = CHARGED_GLORY.any { it in worn }

    fun Player.wearingVarrockArmourAtLeast(required: Int): Boolean {
        if (required <= 0) {
            return false
        }
        val wornTier =
            when {
                "obj.varrock_armour_elite" in worn -> 4
                "obj.varrock_armour_hard" in worn -> 3
                "obj.varrock_armour_medium" in worn -> 2
                "obj.varrock_armour_easy" in worn -> 1
                else -> 0
            }
        return wornTier >= required
    }

    fun Player.miningGloveExtras(data: MiningRocksRow): Int {
        val ore = data.oreItem ?: return 0
        val key = RSCM.getReverseMapping(RSCMType.OBJ, ore.id)
        val tier =
            when {
                "obj.mguild_gloves_expert" in worn -> GloveTier.Expert
                "obj.mguild_gloves_superior" in worn -> GloveTier.Superior
                "obj.mguild_gloves" in worn -> GloveTier.Basic
                else -> return 0
            }
        return when {
            key in BASIC_ORES ->
                when (tier) {
                    GloveTier.Expert -> 3
                    GloveTier.Superior -> 2
                    GloveTier.Basic -> 1
                }
            key in SUPERIOR_ORES ->
                when (tier) {
                    GloveTier.Expert -> 2
                    GloveTier.Superior -> 1
                    GloveTier.Basic -> 0
                }
            key in EXPERT_ONLY_ORES ->
                when (tier) {
                    GloveTier.Expert -> 1
                    else -> 0
                }
            else -> 0
        }
    }

    fun Player.hasProspectorHat(): Boolean = PROSPECTOR_HATS.any { hat.isType(it) }

    fun Player.hasProspectorTop(): Boolean = PROSPECTOR_TOPS.any { torso.isType(it) }

    fun Player.hasProspectorLegs(): Boolean = PROSPECTOR_LEGS.any { legs.isType(it) }

    fun Player.hasProspectorBoots(): Boolean = PROSPECTOR_BOOTS.any { feet.isType(it) }

    fun Player.hasProspectorTopForSet(): Boolean =
        hasProspectorTop() || torso.isType("obj.varrock_armour_elite")

    fun Player.hasFullProspector(): Boolean =
        hasProspectorHat() &&
            hasProspectorTopForSet() &&
            hasProspectorLegs() &&
            hasProspectorBoots()

    private enum class GloveTier {
        Basic,
        Superior,
        Expert,
    }
}
