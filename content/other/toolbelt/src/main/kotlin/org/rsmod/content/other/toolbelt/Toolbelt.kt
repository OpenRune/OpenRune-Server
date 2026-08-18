package org.rsmod.content.other.toolbelt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

object Toolbelt {
    const val ADD_OPTION = "Add-to-toolbelt"
    const val OWNED_COUNT = 1

    private val settings: ToolbeltSettings
        get() = ToolbeltSettings.load()

    fun isEnabled(): Boolean = settings.isEnabled

    fun allowHigherTierUnlock(): Boolean = settings.allowHigherTierUnlock

    fun validateStorageOnLogin(player: Player) {
        if (!isEnabled()) {
            return
        }
        for (slot in rows()) {
            val index = storedItemIndex(player, slot) ?: continue
            val type = slot.allowedItems.getOrNull(index) ?: run {
                ToolbeltVars.clearSlot(player, slot.slotIndex)
                continue
            }
            if (!slot.accepts(type)) {
                ToolbeltVars.clearSlot(player, slot.slotIndex)
            }
        }
    }

    fun slotFor(type: ItemServerType): ToolbeltSlotsRow? {
        if (type.isCert || type.isPlaceholder || type.isDummyItem) {
            return null
        }
        return ToolbeltSlotsRow.all().firstOrNull { it.accepts(type) }
    }

    fun slotFor(internalName: String): ToolbeltSlotsRow? {
        val name =
            if (internalName.startsWith("obj.")) {
                internalName
            } else {
                "obj.$internalName"
            }
        return ToolbeltSlotsRow.all().firstOrNull { it.accepts(name) }
    }

    fun has(player: Player, slot: ToolbeltSlotsRow): Boolean = storedType(player, slot) != null

    fun storedType(player: Player, slot: ToolbeltSlotsRow): ItemServerType? {
        val index = storedItemIndex(player, slot) ?: return null
        return slot.allowedItems.getOrNull(index)
    }

    fun displayType(player: Player, slot: ToolbeltSlotsRow): ItemServerType {
        return storedType(player, slot) ?: slot.defaultItem
    }

    fun provides(player: Player, itemInternal: String): Boolean {
        if (!isEnabled()) {
            return false
        }
        val type = ServerCacheManager.getItem(itemInternal.asObjId()) ?: return false
        val slot = slotFor(type) ?: return false
        if (!ToolbeltUnlocks.isUnlocked(player, slot)) {
            return false
        }
        val stored = storedType(player, slot) ?: return false
        if (stored.id == type.id) {
            return true
        }
        if (!allowHigherTierUnlock() || !slot.tiered) {
            return false
        }
        return slot.accepts(type) &&
            toolTier(type) <= toolTier(stored) &&
            meetsLevelReq(player, slot, type)
    }

    fun providesContentGroup(player: Player, contentGroup: Int): Boolean {
        if (!isEnabled() || contentGroup < 0) {
            return false
        }
        return occupiedSlots(player).any { (_, type) -> type.contentGroup == contentGroup }
    }

    fun resolveAxe(player: Player): InvObj? =
        ToolbeltSlotsRow.getRow("dbrow.toolbelt_axe").let { resolveTieredTool(player, it) }

    fun resolvePickaxe(player: Player): InvObj? =
        ToolbeltSlotsRow.getRow("dbrow.toolbelt_pickaxe").let { resolveTieredTool(player, it) }

    fun resolveExact(player: Player, slot: ToolbeltSlotsRow): InvObj? {
        if (!isEnabled()) {
            return null
        }
        val stored = storedType(player, slot) ?: return null
        return InvObj(stored, OWNED_COUNT)
    }

    fun occupiedSlots(player: Player): List<Pair<ToolbeltSlotsRow, ItemServerType>> =
        rows().mapNotNull { slot ->
            storedType(player, slot)?.let { slot to it }
        }

    sealed class AddResult {
        data class Added(val item: ItemServerType) : AddResult()

        data class Replaced(val added: ItemServerType, val returned: ItemServerType) : AddResult()

        data class Failed(val message: String) : AddResult()
    }

    fun tryAdd(player: Player, type: ItemServerType): AddResult =
        addInternal(player, type, bypassRequirements = false)

    fun grantOwned(player: Player, type: ItemServerType, bypassRequirements: Boolean = false): AddResult =
        addInternal(player, type, bypassRequirements = bypassRequirements)

    fun tryRemove(player: Player, slot: ToolbeltSlotsRow): ItemServerType? {
        if (!isEnabled() || !slot.removable) {
            return null
        }
        val existing = storedType(player, slot) ?: return null
        ToolbeltVars.clearSlot(player, slot.slotIndex)
        return existing
    }

    fun allEligibleItems(): List<ItemServerType> = rows().flatMap { it.allowedItems }

    private fun addInternal(
        player: Player,
        type: ItemServerType,
        bypassRequirements: Boolean,
    ): AddResult {
        if (!isEnabled()) {
            return AddResult.Failed("The tool belt is currently disabled.")
        }
        val slot =
            slotFor(type) ?: return AddResult.Failed("You can't add that to your tool belt.")
        if (!bypassRequirements) {
            if (!ToolbeltUnlocks.isUnlocked(player, slot)) {
                return AddResult.Failed(ToolbeltUnlocks.unlockMessage(slot))
            }
            if (!meetsSlotAddRequirement(player, slot)) {
                val req = slot.requireLevel ?: 1
                val skill = slot.skillStat?.displayName?.ifBlank { null } ?: "skill"
                return AddResult.Failed("You need a $skill level of $req to add that to your tool belt.")
            }
        }

        val existing = storedType(player, slot)
        if (existing != null && existing.id == type.id) {
            return AddResult.Failed("Your tool belt already contains that.")
        }

        setStored(player, slot, type)
        return if (existing != null) {
            AddResult.Replaced(type, existing)
        } else {
            AddResult.Added(type)
        }
    }

    private fun setStored(player: Player, slot: ToolbeltSlotsRow, type: ItemServerType) {
        val index = slot.allowedItems.indexOfFirst { it.id == type.id }
        require(index >= 0) { "Item ${type.internalName} not in ${slot.displayName} slot list" }
        ToolbeltVars.setStoredAllowedIndex(player, slot.slotIndex, index + 1)
    }

    private fun storedItemIndex(player: Player, slot: ToolbeltSlotsRow): Int? {
        val stored = ToolbeltVars.readStoredAllowedIndex(player, slot.slotIndex)
        if (stored <= 0) {
            return null
        }
        val index = stored - 1
        if (index !in slot.allowedItems.indices) {
            return null
        }
        return index
    }

    private fun rows(): List<ToolbeltSlotsRow> =
        ToolbeltSlotsRow.all().sortedBy { it.slotIndex }

    private fun resolveTieredTool(player: Player, slot: ToolbeltSlotsRow): InvObj? {
        if (!isEnabled()) {
            return null
        }
        val stored = storedType(player, slot) ?: return null
        if (!allowHigherTierUnlock() || !slot.tiered) {
            return if (meetsLevelReq(player, slot, stored)) InvObj(stored, OWNED_COUNT) else null
        }

        val storedTier = toolTier(stored)
        val candidates =
            slot.allowedItems
                .filter { toolTier(it) <= storedTier }
                .filter { meetsLevelReq(player, slot, it) }

        val best =
            candidates.maxWithOrNull(compareBy<ItemServerType> { toolTier(it) }.thenBy { it.id })
                ?: return null
        return InvObj(best, OWNED_COUNT)
    }

    private fun toolTier(type: ItemServerType): Int = levelReq(type)

    private fun levelReq(type: ItemServerType): Int = type.paramOrNull(params.levelrequire) ?: 1

    private fun meetsSlotAddRequirement(player: Player, slot: ToolbeltSlotsRow): Boolean {
        val level = slot.requireLevel ?: return true
        val skill = slot.skillStat ?: return true
        return player.statBase(skill.internalName) >= level
    }

    private fun meetsLevelReq(
        player: Player,
        slot: ToolbeltSlotsRow,
        type: ItemServerType,
    ): Boolean {
        val skill = slot.skillStat ?: return true
        return player.statBase(skill.internalName) >= levelReq(type)
    }
}

private fun String.asObjId(): Int = asRSCM(RSCMType.OBJ)
