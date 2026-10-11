package org.rsmod.content.skills.runecrafting.essencepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onDropHeld
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EssencePouchScript : PluginScript() {

    override fun ScriptContext.startup() {
        EssencePouch.Tier.entries.flatMap { it.items.toList() }.forEach { pouch ->
            onOpHeld1(pouch) { if (canOperate()) fillPouch(it.slot) }
            onOpHeld2(pouch) { if (canOperate()) checkPouch(it.slot) }
            onOpHeld3(pouch) { if (canOperate()) emptyPouch(it.slot) }
        }
        onDropHeld { EssencePouch.tierForInvObj(obj)?.let { tier -> if (!EssencePouch.ownsTier(player, tier)) EssencePouch.clearStorage(player, tier) } }
        onDestroyHeld { EssencePouch.tierForInvObj(obj)?.let { tier -> if (!EssencePouch.ownsTier(player, tier) && tier.items.none { player.invMap["inv.bank"]?.contains(it) == true }) EssencePouch.resetTier(player, tier) } }
    }

    private fun ProtectedAccess.canOperate() = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut

    private fun ProtectedAccess.fillPouch(slot: Int) {
        val pouchObj = inv[slot] ?: return
        val tier = EssencePouch.tierForInvObj(pouchObj) ?: return

        if (player.baseRunecraftingLvl < tier.levelReq) {
            mes("This pouch requires level ${tier.levelReq} Runecrafting to use.")
            return
        }

        val storedType = EssencePouch.storedEssenceTypeName(player, tier)
        val essenceType = (storedType?.takeIf { inv.physicalCount(it) > 0 } ?: EssencePouch.fillableEssences.firstOrNull { inv.physicalCount(it) > 0 }) ?: run {
            mes("You do not have any essence to fill your pouch with.")
            return
        }

        val capacityBefore = EssencePouch.capacity(player, tier)
        val freeSpace = EssencePouch.freeSpace(player, tier)
        if (freeSpace <= 0) {
            mes("You cannot add any more essence to the pouch.")
            return
        }

        val requested = minOf(inv.physicalCount(essenceType), freeSpace)
        if (requested <= 0) {
            return
        }

        if (EssencePouch.storedAmount(player, tier) > 0 && !EssencePouch.matchesStoredEssenceType(player, tier, essenceType)) {
            mes("You can only fill this pouch with one type of essence at a time.")
            return
        }

        if (invDel(inv, essenceType, requested, ignoreVirtualStorage = true).failure) return
        EssencePouch.depositUpTo(player, tier, requested)
        EssencePouch.setStoredEssenceType(player, tier, essenceType)
        var newCapacity = capacityBefore
        if (tier.degradable && !EssencePouch.playerHasDegradeProtection(player)) {
            newCapacity = EssencePouch.applyDegradation(player, tier, requested, (0 until requested).sumOf { random.of(0, 3) })
            handleDegradation(slot, tier, pouchObj, capacityBefore, newCapacity)
            if (newCapacity <= 0) {
                return
            }
        }

        val excess = (EssencePouch.storedAmount(player, tier) - newCapacity).coerceAtLeast(0)
        if (excess > 0) {
            if (inv.freeSpace() >= excess) invAdd(inv, essenceType, excess, ignoreVirtualStorage = true)
            EssencePouch.removeStoredFromTier(player, tier, excess)
        }
        val essenceName =
            ServerCacheManager.getItem(essenceType.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: "essence"
        mes("You fill the pouch with $requested $essenceName.")
    }

    private fun ProtectedAccess.handleDegradation(
        slot: Int,
        tier: EssencePouch.Tier,
        pouchObj: org.rsmod.game.inv.InvObj,
        capacityBefore: Int,
        capacityAfter: Int,
    ) {
        if (capacityAfter <= 0) {
            val itemType = RSCM.getReverseMapping(RSCMType.OBJ, pouchObj.id)
            val name = ServerCacheManager.getItem(itemType.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: "pouch"
            mes("Your $name has disintegrated.")
            EssencePouch.resetTier(player, tier)
            invDel(inv, itemType, 1, slot = slot)
            return
        }

        if (capacityAfter < capacityBefore) {
            mes("Your pouch has decayed through use.")
            val degraded = tier.degradedItem
            if (degraded != null && pouchObj.isType(tier.intactItem)) {
                val degradedType = ServerCacheManager.getItem(degraded.asRSCM(RSCMType.OBJ)) ?: return
                invReplaceSlot(inv, slot, 1, degradedType)
            }
        }
    }

    private fun ProtectedAccess.emptyPouch(slot: Int) {
        val pouchObj = inv[slot] ?: return
        val tier = EssencePouch.tierForInvObj(pouchObj) ?: return

        val amount = EssencePouch.storedAmount(player, tier)
        if (amount <= 0) {
            mes("There are no essences in this pouch.")
            return
        }

        val essenceType =
            EssencePouch.storedEssenceTypeName(player, tier)
                ?: EssencePouch.preferredEssenceType(player, tier)

        val removeCount = minOf(inv.freeSpace(), amount)
        if (removeCount <= 0) {
            mes("You do not have any free space in your inventory.")
            return
        }

        val added =
            invAdd(inv, essenceType, removeCount, strict = false, ignoreVirtualStorage = true)
                .completed()
        if (added <= 0) {
            mes("You do not have any free space in your inventory.")
            return
        }

        EssencePouch.removeStoredFromTier(player, tier, added)
        mes("You empty $added essence${if (added != 1) "s" else ""} from the pouch.")
    }

    private fun ProtectedAccess.checkPouch(slot: Int) {
        val pouchObj = inv[slot] ?: return
        val tier = EssencePouch.tierForInvObj(pouchObj) ?: return

        val amount = EssencePouch.storedAmount(player, tier)
        if (amount <= 0) {
            mes("There are no essences in this pouch.")
            return
        }

        val capacity = EssencePouch.capacity(player, tier)
        val essenceType =
            EssencePouch.storedEssenceTypeName(player, tier)
                ?: EssencePouch.preferredEssenceType(player, tier)
        val essenceName =
            ServerCacheManager.getItem(essenceType.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: "essence"
        mes("There ${if (amount == 1) "is" else "are"} $amount $essenceName in this pouch.")
        if (tier == EssencePouch.Tier.Colossal) {
            mes("It can hold up to $capacity essence.")
        }
    }
}
