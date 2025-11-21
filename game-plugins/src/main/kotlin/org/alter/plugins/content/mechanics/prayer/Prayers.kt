package org.alter.plugins.content.mechanics.prayer

import org.alter.api.ClientScript
import org.alter.api.GameframeTab
import org.alter.api.InterfaceDestination
import org.alter.api.PrayerIcon
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.PROTECT_ITEM_ATTR
import org.alter.game.model.bits.INFINITE_VARS_STORAGE
import org.alter.game.model.bits.InfiniteVarsType
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.Plugin

object Prayers {
    private val PRAYER_DRAIN_COUNTER = AttributeKey<Int>()

    val PRAYER_DRAIN = TimerKey()
    private val DISABLE_OVERHEADS = TimerKey()

    private const val DEACTIVATE_PRAYER_SOUND = 2663

    private const val ACTIVE_PRAYERS_VARP = "varp.prayer0"
    private const val SELECTED_QUICK_PRAYERS_VARP = "varp.prayer1"

    // const val INF_PRAY_VARBIT = 5314
    private const val QUICK_PRAYERS_ACTIVE_VARBIT = "varbits.quickprayer_active"
    private const val KING_RANSOMS_QUEST_VARBIT = "varbits.kr_knightwaves_state"
    const val RIGOUR_UNLOCK_VARBIT = "varbits.prayer_rigour_unlocked"
    const val AUGURY_UNLOCK_VARBIT = "varbits.prayer_augury_unlocked"
    const val PRESERVE_UNLOCK_VARBIT = "varbits.prayer_preserve_unlocked"
    const val HUMBLE_CHIVALRY_VARBIT = "varbits.humble_chivalry"
    const val HUMBLE_PIETY_VARBIT = "varbits.humble_piety"

    fun disableOverheads(
        p: Player,
        cycles: Int,
    ) {
        p.timers[DISABLE_OVERHEADS] = cycles
    }

    fun deactivateAll(p: Player) {
        p.setVarp(ACTIVE_PRAYERS_VARP, 0)
        p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
        p.attr.remove(PROTECT_ITEM_ATTR)

        if (p.prayerIcon != -1) {
            p.prayerIcon = -1
            p.avatar.extendedInfo.setOverheadIcon(-1)
        }
    }

    suspend fun toggle(
        p: Player,
        it: QueueTask,
        prayer: Prayer,
    ) {

        if (p.isDead() || !p.lock.canUsePrayer()) {
            p.syncVarp(ACTIVE_PRAYERS_VARP)
            return
        } else if (!checkRequirements(p, prayer)) {
            return
        } else if (prayer.group == PrayerGroup.OVERHEAD && p.timers.has(DISABLE_OVERHEADS)) {
            p.syncVarp(ACTIVE_PRAYERS_VARP)
            p.message("You cannot use overhead prayers right now.")
            return
        } else if (p.getSkills().getCurrentLevel(Skills.PRAYER) == 0) {
            return
        }

        it.terminateAction = { p.syncVarp(ACTIVE_PRAYERS_VARP) }
        while (p.lock.delaysPrayer()) {
            it.wait(1)
        }
        val active = p.getVarbit(prayer.varbit) != 0
        if (active) {
            deactivate(p, prayer)
        } else {
            activate(p, prayer)
        }
        p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
    }

    fun activate(
        p: Player,
        prayer: Prayer,
        playSound: Boolean = true,
    ) {
        if (!isActive(p, prayer)) {
            deactivateConflictingPrayers(p, prayer)
            p.setVarbit(prayer.varbit, 1)

            if (playSound && prayer.sound != -1) {
                p.playSound(prayer.sound)
            }

            setOverhead(p)
            handlePrayerAttributes(p, prayer, activate = true)
        }
    }

    fun deactivate(
        p: Player,
        prayer: Prayer,
        playSound: Boolean = true,
    ) {
        if (isActive(p, prayer)) {
            p.setVarbit(prayer.varbit, 0)

            if (playSound) {
                p.playSound(DEACTIVATE_PRAYER_SOUND)
            }

            setOverhead(p)
            handlePrayerAttributes(p, prayer, activate = false)
        }
    }

    fun drainPrayer(p: Player) {
        if (p.isDead() || p.getVarp(ACTIVE_PRAYERS_VARP) == 0 || p.hasStorageBit(INFINITE_VARS_STORAGE, InfiniteVarsType.PRAY)) {
            p.attr.remove(PRAYER_DRAIN_COUNTER)
            return
        }

        val drain = calculateDrainRate(p)
        if (drain > 0) {
            val counter = p.attr.getOrDefault(PRAYER_DRAIN_COUNTER, 0) + drain
            val resistance = 60 + (p.getPrayerBonus() * 2)
            if (counter >= resistance) {
                val points = Math.floor((counter / resistance).toDouble()).toInt()
                p.getSkills().alterCurrentLevel(Skills.PRAYER, -points)
                p.attr.put(PRAYER_DRAIN_COUNTER, counter - (resistance * points))
            } else {
                p.attr.put(PRAYER_DRAIN_COUNTER, counter)
            }
        }

        if (p.getSkills().getCurrentLevel(Skills.PRAYER) == 0) {
            deactivateAll(p)
            p.message("You have run out of prayer points, you can recharge at an altar.")
        }
    }

    fun selectQuickPrayer(
        it: Plugin,
        prayer: Prayer,
    ) {
        val player = it.player

        if (player.isDead() || !player.lock.canUsePrayer()) {
            player.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
            return
        }

        val slot = prayer.quickPrayerSlot
        val enabled = (player.getVarp(SELECTED_QUICK_PRAYERS_VARP) and (1 shl slot)) != 0

        it.player.queue {
            if (!enabled) {
                if (checkRequirements(player, prayer)) {
                    val others =
                        Prayer.values.filter { other ->
                            prayer != other && other.group != null &&
                                (prayer.group == other.group || prayer.overlap.contains(other.group))
                        }
                    others.forEach { other ->
                        val otherEnabled = (player.getVarp(SELECTED_QUICK_PRAYERS_VARP) and (1 shl other.quickPrayerSlot)) != 0
                        if (otherEnabled) {
                            player.setVarp(
                                SELECTED_QUICK_PRAYERS_VARP,
                                player.getVarp(SELECTED_QUICK_PRAYERS_VARP) and (1 shl other.quickPrayerSlot).inv(),
                            )
                        }
                    }
                    player.setVarp(SELECTED_QUICK_PRAYERS_VARP, player.getVarp(SELECTED_QUICK_PRAYERS_VARP) or (1 shl slot))
                    // Sync interface state after selection
                    syncQuickPrayerInterface(player)
                }
            } else {
                player.setVarp(SELECTED_QUICK_PRAYERS_VARP, player.getVarp(SELECTED_QUICK_PRAYERS_VARP) and (1 shl slot).inv())
            }
        }
    }

    fun toggleQuickPrayers(
        p: Player,
        opt: Int,
    ) {
        if (p.isDead() || !p.lock.canUsePrayer()) {
            p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
            return
        }

        if (opt == 1) {
            val selectedQuickPrayers = p.getVarp(SELECTED_QUICK_PRAYERS_VARP)

            when {
                selectedQuickPrayers == 0 -> {
                    p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
                    p.message("You haven't selected any quick-prayers.")
                }
                p.getSkills().getCurrentLevel(Skills.PRAYER) <= 0 -> {
                    p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
                    p.message("You have run out of prayer points, you can recharge at an altar.")
                }
                p.getVarbit(QUICK_PRAYERS_ACTIVE_VARBIT) == 1 -> {
                    /*
                     * Quick prayers are currently active - turn them off.
                     */
                    deactivateQuickPrayers(p, selectedQuickPrayers)
                }
                else -> {
                    /*
                     * Quick prayers are not active - turn them on.
                     */
                    activateQuickPrayers(p, selectedQuickPrayers)
                }
            }
        } else if (opt == 2) {
            // Open quick prayer setup interface
            p.runClientScript(ClientScript(id = 915), 5)
            p.setInterfaceEvents(interfaceId = 77, component = 4, from = 0, to = 29, setting = 2)
            p.openInterface(interfaceId = 77, dest = InterfaceDestination.PRAYER)
            p.focusTab(GameframeTab.PRAYER)
            // Sync the quick prayer selections to the interface
            syncQuickPrayerInterface(p)
        }
    }

    fun isActive(
        p: Player,
        prayer: Prayer,
    ): Boolean = p.getVarbit(prayer.varbit) != 0

    /**
     * Builds the ACTIVE_PRAYERS_VARP value from currently active prayer varbits.
     * Uses quickPrayerSlot to determine which bit to set in the varp.
     */
    private fun buildActivePrayersVarp(p: Player): Int {
        var activePrayersVarp = 0
        Prayer.values.forEach { prayer ->
            if (prayer.quickPrayerSlot >= 0 && isActive(p, prayer)) {
                activePrayersVarp = activePrayersVarp or (1 shl prayer.quickPrayerSlot)
            }
        }
        return activePrayersVarp
    }

    /**
     * Syncs the quick prayer interface state based on the player's saved selections.
     * This updates the client-side interface to show which prayers are selected.
     */
    fun syncQuickPrayerInterface(p: Player) {
        // Sync the varp to the client so the interface reflects the selections
        p.syncVarp(SELECTED_QUICK_PRAYERS_VARP)
    }

    /**
     * Syncs prayer unlock varbits to the client and updates the prayer interface.
     * This ensures unlocked prayers (Rigour, Augury, Preserve, etc.) are visible in the interface.
     */
    fun syncPrayerUnlocks(p: Player) {
        // Sync all prayer unlock varbits
        p.setVarbit(RIGOUR_UNLOCK_VARBIT, p.getVarbit(RIGOUR_UNLOCK_VARBIT))
        p.setVarbit(AUGURY_UNLOCK_VARBIT, p.getVarbit(AUGURY_UNLOCK_VARBIT))
        p.setVarbit(PRESERVE_UNLOCK_VARBIT, p.getVarbit(PRESERVE_UNLOCK_VARBIT))
        p.setVarbit(KING_RANSOMS_QUEST_VARBIT, p.getVarbit(KING_RANSOMS_QUEST_VARBIT))
        p.setVarbit(HUMBLE_CHIVALRY_VARBIT, p.getVarbit(HUMBLE_CHIVALRY_VARBIT))
        p.setVarbit(HUMBLE_PIETY_VARBIT, p.getVarbit(HUMBLE_PIETY_VARBIT))

        // Run client script 2158 to update the prayer interface to show unlocked prayers
        p.runClientScript(ClientScript(id = 2158))
    }

    /**
     * Checks if a player can activate a prayer for quick prayers (without showing dialogue).
     */
    private fun canActivateQuickPrayer(p: Player, prayer: Prayer): Boolean {
        return checkPrayerLevel(p, prayer) && checkPrayerUnlocks(p, prayer)
    }

    private suspend fun checkRequirements(
        p: Player,
        prayer: Prayer,
    ): Boolean {
        if (!checkPrayerLevel(p, prayer)) {
            p.syncVarp(ACTIVE_PRAYERS_VARP)
            p.message("You need a Prayer level of ${prayer.level} to use ${prayer.named}.")
            return false
        }

        if (!checkPrayerUnlocks(p, prayer)) {
            p.syncVarp(ACTIVE_PRAYERS_VARP)
            p.message("You have not unlocked this prayer.")
            return false
        }

        return true
    }

    /**
     * Checks if the player has the required prayer level.
     */
    private fun checkPrayerLevel(p: Player, prayer: Prayer): Boolean {
        return p.getSkills().getBaseLevel(Skills.PRAYER) >= prayer.level
    }

    /**
     * Checks if the player has unlocked the prayer (for special prayers like Rigour, Augury, etc.).
     */
    private fun checkPrayerUnlocks(p: Player, prayer: Prayer): Boolean {
        return when (prayer) {
            Prayer.PRESERVE -> p.getVarbit(PRESERVE_UNLOCK_VARBIT) != 0
            Prayer.CHIVALRY -> p.getVarbit(KING_RANSOMS_QUEST_VARBIT) >= 8
            Prayer.PIETY -> p.getVarbit(KING_RANSOMS_QUEST_VARBIT) >= 8
            Prayer.RIGOUR -> p.getVarbit(RIGOUR_UNLOCK_VARBIT) != 0
            Prayer.AUGURY -> p.getVarbit(AUGURY_UNLOCK_VARBIT) != 0
            else -> true // Other prayers don't require unlocks
        }
    }

    private fun setOverhead(p: Player) {
        val icon =
            when {
                isActive(p, Prayer.PROTECT_FROM_MELEE) -> PrayerIcon.PROTECT_FROM_MELEE
                isActive(p, Prayer.PROTECT_FROM_MISSILES) -> PrayerIcon.PROTECT_FROM_MISSILES
                isActive(p, Prayer.PROTECT_FROM_MAGIC) -> PrayerIcon.PROTECT_FROM_MAGIC
                isActive(p, Prayer.RETRIBUTION) -> PrayerIcon.RETRIBUTION
                isActive(p, Prayer.SMITE) -> PrayerIcon.SMITE
                isActive(p, Prayer.REDEMPTION) -> PrayerIcon.REDEMPTION
                else -> PrayerIcon.NONE
            }

        if (p.prayerIcon != icon.id) {
            p.prayerIcon = icon.id
            p.avatar.extendedInfo.setOverheadIcon(icon.id)
        }
    }

    private fun calculateDrainRate(p: Player): Int = Prayer.values.filter { isActive(p, it) }.sumOf { it.drainEffect }

    /**
     * Activates all selected quick prayers. Only plays sound once regardless of how many prayers are activated.
     */
    private fun activateQuickPrayers(p: Player, selectedQuickPrayers: Int) {
        // First, deactivate any prayers that are currently active but not in the quick prayer selection
        deactivateNonSelectedPrayers(p, selectedQuickPrayers)

        // Then activate all selected quick prayers (without playing sounds individually)
        val prayersBySlot = Prayer.values.filter { it.quickPrayerSlot >= 0 }
            .groupBy { it.quickPrayerSlot }

        var anyActivated = false
        val firstPrayerSoundHolder = intArrayOf(-1) // Use array to avoid closure capture issues

        prayersBySlot.forEach { (slot, prayers) ->
            val isSelected = (selectedQuickPrayers and (1 shl slot)) != 0
            if (isSelected) {
                // If multiple prayers share the slot, prefer Smite over Piety (Piety isn't implemented)
                val prayer = prayers.firstOrNull { it != Prayer.PIETY } ?: prayers.firstOrNull()
                if (prayer != null && !isActive(p, prayer)) {
                    // Check requirements before activating
                    if (canActivateQuickPrayer(p, prayer)) {
                        activate(p, prayer, playSound = false)
                        anyActivated = true
                        // Store the first prayer's sound to play once at the end
                        if (firstPrayerSoundHolder[0] == -1 && prayer.sound != -1) {
                            firstPrayerSoundHolder[0] = prayer.sound
                        }
                    }
                }
            }
        }

        // Play sound once if any prayers were activated
        if (anyActivated && firstPrayerSoundHolder[0] != -1) {
            p.playSound(firstPrayerSoundHolder[0])
        }

        // Update varp to reflect active prayers
        p.setVarp(ACTIVE_PRAYERS_VARP, buildActivePrayersVarp(p))
        p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 1)
        setOverhead(p)
    }

    /**
     * Deactivates all selected quick prayers. Only plays sound once regardless of how many prayers are deactivated.
     */
    private fun deactivateQuickPrayers(p: Player, selectedQuickPrayers: Int) {
        val prayersBySlot = Prayer.values.filter { it.quickPrayerSlot >= 0 }
            .groupBy { it.quickPrayerSlot }

        var anyDeactivated = false

        prayersBySlot.forEach { (slot, prayers) ->
            val isSelected = (selectedQuickPrayers and (1 shl slot)) != 0
            if (isSelected) {
                // If multiple prayers share the slot, prefer Smite over Piety (Piety isn't implemented)
                val prayer = prayers.firstOrNull { it != Prayer.PIETY } ?: prayers.firstOrNull()
                if (prayer != null && isActive(p, prayer)) {
                    deactivate(p, prayer, playSound = false)
                    anyDeactivated = true
                }
            }
        }

        // Play sound once if any prayers were deactivated
        if (anyDeactivated) {
            p.playSound(DEACTIVATE_PRAYER_SOUND)
        }

        // Update varp to reflect remaining active prayers (if any)
        p.setVarp(ACTIVE_PRAYERS_VARP, buildActivePrayersVarp(p))
        p.setVarbit(QUICK_PRAYERS_ACTIVE_VARBIT, 0)
        setOverhead(p)
    }

    /**
     * Deactivates prayers that are currently active but not in the quick prayer selection.
     */
    private fun deactivateNonSelectedPrayers(p: Player, selectedQuickPrayers: Int) {
        Prayer.values.forEach { prayer ->
            if (prayer.quickPrayerSlot >= 0) {
                val isSelected = (selectedQuickPrayers and (1 shl prayer.quickPrayerSlot)) != 0
                if (!isSelected && isActive(p, prayer)) {
                    deactivate(p, prayer, playSound = false)
                }
            } else {
                // Also deactivate prayers that don't have a quick prayer slot (shouldn't happen, but be safe)
                if (isActive(p, prayer)) {
                    deactivate(p, prayer, playSound = false)
                }
            }
        }
    }

    /**
     * Deactivates prayers that conflict with the prayer being activated.
     */
    private fun deactivateConflictingPrayers(p: Player, prayer: Prayer) {
        val conflicting = Prayer.values.filter { other ->
            prayer != other && other.group != null &&
                (prayer.group == other.group || prayer.overlap.contains(other.group))
        }
        conflicting.forEach { other ->
            if (p.getVarbit(other.varbit) != 0) {
                p.setVarbit(other.varbit, 0)
            }
        }
    }

    /**
     * Handles prayer-specific attributes (e.g., PROTECT_ITEM_ATTR).
     */
    private fun handlePrayerAttributes(p: Player, prayer: Prayer, activate: Boolean) {
        if (prayer == Prayer.PROTECT_ITEM) {
            p.attr[PROTECT_ITEM_ATTR] = activate
        }
    }
}
