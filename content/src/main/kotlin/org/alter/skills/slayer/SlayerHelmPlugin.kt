package org.alter.skills.slayer

import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM.getRSCM

/**
 * Handles slayer helm crafting and recoloring.
 *
 * Slayer helm recipe:
 * - Black mask (any charge)
 * - Earmuffs
 * - Nosepeg
 * - Facemask
 * - Spiny helmet (optional - protects from wall beasts)
 * - Enchanted gem
 *
 * Requires: 55 Crafting, Slayer helm unlock
 */
class SlayerHelmPlugin : PluginEvent() {

    companion object {
        // Slayer helm unlock varbit
        private const val SLAYER_HELM_UNLOCK = "varbits.slayer_helm_unlocked"

        // Crafting requirement
        private const val CRAFTING_REQUIREMENT = 55

        // Components for slayer helm
        private const val BLACK_MASK = "items.harmless_black_mask"
        private const val EARMUFFS = "items.slayer_earmuffs"
        private const val NOSEPEG = "items.slayer_nosepeg"
        private const val FACEMASK = "items.slayer_facemask"
        private const val ENCHANTED_GEM = "items.slayer_gem"
        private const val SLAYER_HELM = "items.slayer_helm"

        // Black mask variants (charged)
        val BLACK_MASK_VARIANTS = listOf(
            "items.harmless_black_mask_10",
            "items.harmless_black_mask_9",
            "items.harmless_black_mask_8",
            "items.harmless_black_mask_7",
            "items.harmless_black_mask_6",
            "items.harmless_black_mask_5",
            "items.harmless_black_mask_4",
            "items.harmless_black_mask_3",
            "items.harmless_black_mask_2",
            "items.harmless_black_mask_1",
            "items.harmless_black_mask",
        )

        // Slayer helm color variants
        enum class HelmVariant(val itemKey: String, val unlockVarbit: String, val colorItem: String?) {
            REGULAR("items.slayer_helm", "", null),
            BLACK("items.slayer_helm_black", "varbits.slayer_unlock_helm_black", "items.kbd_heads"),
            GREEN("items.slayer_helm_green", "varbits.slayer_unlock_helm_green", "items.kq_head"),
            RED("items.slayer_helm_red", "varbits.slayer_unlock_helm_red", "items.abyssal_head"),
            PURPLE("items.slayer_helm_purple", "varbits.slayer_unlock_helm_purple", "items.dark_claw"),
            TURQUOISE("items.slayer_helm_turquoise", "varbits.slayer_unlock_helm_turquoise", "items.vorkath_head"),
            HYDRA("items.slayer_helm_hydra", "varbits.slayer_unlock_helm_hydra", "items.hydra_leather"),
            TWISTED("items.slayer_helm_twisted", "varbits.slayer_unlock_helm_twisted", "items.twisted_horns"),
        }
    }

    override fun init() {
        // Register item-on-item combinations for helm crafting
        // Black mask on earmuffs starts the combination
        BLACK_MASK_VARIANTS.forEach { maskVariant ->
            try {
                onItemOnItem(maskVariant, EARMUFFS) {
                    player.queue { attemptCraftHelm(player, maskVariant) }
                }
                onItemOnItem(EARMUFFS, maskVariant) {
                    player.queue { attemptCraftHelm(player, maskVariant) }
                }
            } catch (_: Exception) {
                // Item doesn't exist
            }
        }

        // Register helm recoloring
        HelmVariant.entries.filter { it.colorItem != null }.forEach { variant ->
            try {
                onItemOnItem(SLAYER_HELM, variant.colorItem!!) {
                    player.queue { attemptRecolorHelm(player, variant) }
                }
                onItemOnItem(variant.colorItem!!, SLAYER_HELM) {
                    player.queue { attemptRecolorHelm(player, variant) }
                }
            } catch (_: Exception) {
                // Item doesn't exist
            }
        }

        // Register disassembly option on helm
        try {
            // This would need a right-click option handler
            // For now, we can use item-on-item with a chisel or similar
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * Attempt to craft a slayer helm.
     */
    private suspend fun QueueTask.attemptCraftHelm(player: Player, blackMaskVariant: String) {
        // Check unlock
        if (!hasHelmUnlock(player)) {
            messageBox(player, "You need to unlock the ability to craft a Slayer helmet first.<br><br>Visit a Slayer master's rewards shop.")
            return
        }

        // Check crafting level
        if (player.getSkills().getCurrentLevel(Skills.CRAFTING) < CRAFTING_REQUIREMENT) {
            messageBox(player, "You need level $CRAFTING_REQUIREMENT Crafting to make a Slayer helmet.")
            return
        }

        // Check for all components
        val hasBlackMask = hasAnyBlackMask(player)
        val hasEarmuffs = player.inventory.contains(EARMUFFS)
        val hasNosepeg = player.inventory.contains(NOSEPEG)
        val hasFacemask = player.inventory.contains(FACEMASK)
        val hasGem = player.inventory.contains(ENCHANTED_GEM)

        if (!hasBlackMask) {
            player.message("You need a black mask to make a Slayer helmet.")
            return
        }
        if (!hasEarmuffs) {
            player.message("You need earmuffs to make a Slayer helmet.")
            return
        }
        if (!hasNosepeg) {
            player.message("You need a nosepeg to make a Slayer helmet.")
            return
        }
        if (!hasFacemask) {
            player.message("You need a facemask to make a Slayer helmet.")
            return
        }
        if (!hasGem) {
            player.message("You need an enchanted gem to make a Slayer helmet.")
            return
        }

        // Confirm
        messageBox(player, "Create a Slayer helmet?<br><br>This will combine:<br>- Black mask<br>- Earmuffs<br>- Nosepeg<br>- Facemask<br>- Enchanted gem")

        val confirm = options(player, "Yes, make Slayer helmet.", "No, cancel.")
        if (confirm != 1) return

        // Remove components
        removeBlackMask(player)
        player.inventory.remove(getRSCM(EARMUFFS))
        player.inventory.remove(getRSCM(NOSEPEG))
        player.inventory.remove(getRSCM(FACEMASK))
        player.inventory.remove(getRSCM(ENCHANTED_GEM))

        // Give slayer helm
        player.inventory.add(getRSCM(SLAYER_HELM))

        // Animation and message
        player.animate("sequences.human_crafting")
        player.message("You combine the items into a Slayer helmet.")

        // Award crafting XP
        player.addXp(Skills.CRAFTING, 15.0)
    }

    /**
     * Check if player has any black mask variant.
     */
    private fun hasAnyBlackMask(player: Player): Boolean {
        return BLACK_MASK_VARIANTS.any { variant ->
            try {
                player.inventory.contains(variant)
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Remove black mask from inventory.
     */
    private fun removeBlackMask(player: Player) {
        for (variant in BLACK_MASK_VARIANTS) {
            try {
                if (player.inventory.contains(variant)) {
                    player.inventory.remove(getRSCM(variant))
                    return
                }
            } catch (_: Exception) {
                continue
            }
        }
    }

    /**
     * Attempt to recolor a slayer helm.
     */
    private suspend fun QueueTask.attemptRecolorHelm(player: Player, variant: HelmVariant) {
        // Check unlock for this color
        if (variant.unlockVarbit.isNotEmpty()) {
            try {
                if (player.getVarbit(variant.unlockVarbit) != 1) {
                    messageBox(player, "You need to unlock this color variant first.<br><br>Visit a Slayer master's rewards shop.")
                    return
                }
            } catch (_: Exception) {
                // Varbit doesn't exist, assume locked
                messageBox(player, "You need to unlock this color variant first.")
                return
            }
        }

        // Check for helm and color item
        if (!player.inventory.contains(SLAYER_HELM)) {
            player.message("You need a Slayer helmet to recolor.")
            return
        }

        if (variant.colorItem != null && !player.inventory.contains(variant.colorItem)) {
            player.message("You need the required trophy head to recolor your helmet.")
            return
        }

        // Confirm
        messageBox(player, "Recolor your Slayer helmet?<br><br>This will consume the trophy head.")

        val confirm = options(player, "Yes, recolor it.", "No, keep it as is.")
        if (confirm != 1) return

        // Remove components
        player.inventory.remove(getRSCM(SLAYER_HELM))
        variant.colorItem?.let { player.inventory.remove(getRSCM(it)) }

        // Give colored helm
        player.inventory.add(getRSCM(variant.itemKey))

        player.message("You recolor your Slayer helmet.")
    }

    /**
     * Disassemble a slayer helm back into components.
     */
    fun disassembleHelm(player: Player, helmItemKey: String): Boolean {
        if (!player.inventory.contains(helmItemKey)) return false

        // Check inventory space (need 5 slots: mask, earmuffs, nosepeg, facemask, gem)
        if (player.inventory.freeSlotCount < 4) { // 4 because helm becomes 1 slot
            player.message("You need at least 4 free inventory slots to disassemble the helmet.")
            return false
        }

        // Remove helm
        player.inventory.remove(getRSCM(helmItemKey))

        // Return components (black mask is uncharged)
        player.inventory.add(getRSCM(BLACK_MASK))
        player.inventory.add(getRSCM(EARMUFFS))
        player.inventory.add(getRSCM(NOSEPEG))
        player.inventory.add(getRSCM(FACEMASK))
        player.inventory.add(getRSCM(ENCHANTED_GEM))

        player.message("You disassemble your Slayer helmet.")
        return true
    }

    /**
     * Check if player has unlocked slayer helm crafting.
     */
    fun hasHelmUnlock(player: Player): Boolean {
        return try {
            player.getVarbit(SLAYER_HELM_UNLOCK) == 1
        } catch (_: Exception) {
            false
        }
    }
}

