package org.rsmod.content.skills.runecrafting.essencepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.baseRunecraftingLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.skills.runecrafting.colossalPouchDegradeThreshold
import org.rsmod.content.skills.runecrafting.giantPouchDegradeThreshold
import org.rsmod.content.skills.runecrafting.largePouchDegradeThreshold
import org.rsmod.content.skills.runecrafting.mediumPouchDegradeThreshold
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

object EssencePouch {
    const val RUNE_ESSENCE = "obj.blankrune"
    const val PURE_ESSENCE = "obj.blankrune_high"
    const val DAEYALT_ESSENCE = "obj.blankrune_daeyalt"
    const val GUARDIAN_ESSENCE = "obj.gotr_guardian_essence"
    const val DARK_ESSENCE_FRAGMENT = "obj.bigblankrune"

    val fillableEssences = listOf(
        PURE_ESSENCE,
        DAEYALT_ESSENCE,
        GUARDIAN_ESSENCE,
        RUNE_ESSENCE,
        DARK_ESSENCE_FRAGMENT
    )

    enum class Tier(
        val levelReq: Int,
        val maxCapacity: Int,
        val degradable: Boolean,
        val intactItem: String,
        val degradedItem: String?,
    ) {
        Small(
            levelReq = 1,
            maxCapacity = 3,
            degradable = false,
            intactItem = "obj.rcu_pouch_small",
            degradedItem = null,
        ),
        Medium(
            levelReq = 25,
            maxCapacity = 6,
            degradable = true,
            intactItem = "obj.rcu_pouch_medium",
            degradedItem = "obj.rcu_pouch_medium_degrade",
        ),
        Large(
            levelReq = 50,
            maxCapacity = 9,
            degradable = true,
            intactItem = "obj.rcu_pouch_large",
            degradedItem = "obj.rcu_pouch_large_degrade",
        ),
        Giant(
            levelReq = 75,
            maxCapacity = 12,
            degradable = true,
            intactItem = "obj.rcu_pouch_giant",
            degradedItem = "obj.rcu_pouch_giant_degrade",
        ),
        Colossal(
            levelReq = 25,
            maxCapacity = 40,
            degradable = true,
            intactItem = "obj.rcu_pouch_colossal",
            degradedItem = "obj.rcu_pouch_colossal_degrade",
        );

        val items: Array<String> = if (degradedItem != null) {
            arrayOf(intactItem, degradedItem)
        } else {
            arrayOf(intactItem)
        }
    }


    fun isEssence(itemInternal: String): Boolean = itemInternal in fillableEssences

    fun tierForInvObj(obj: InvObj): Tier? =
        Tier.entries.firstOrNull { tier -> tier.items.any { obj.isType(it) } }

    fun hasPouchInInventory(player: Player): Boolean =
        player.inv.any { obj -> obj != null && tierForInvObj(obj) != null }


    fun shouldIntercept(player: Player, itemInternal: String): Boolean =
        isEssence(itemInternal) &&
            hasPouchInInventory(player) &&
            storedAmountForType(player, itemInternal) > 0

    fun storedAmountForType(player: Player, itemInternal: String): Int {
        if (!isEssence(itemInternal)) {
            return 0
        }
        return Tier.entries.sumOf { tier ->
            if (matchesStoredEssenceType(player, tier, itemInternal)) {
                storedAmount(player, tier)
            } else {
                0
            }
        }
    }

    fun matchesStoredEssenceType(player: Player, tier: Tier, itemInternal: String): Boolean {
        val stored = storedAmount(player, tier)
        if (stored <= 0) {
            return false
        }
        val storedType = tier.readStoredEssenceType(player)
        if (storedType == 0) {
            return false
        }
        return storedType == itemInternal.asRSCM(RSCMType.OBJ)
    }

    fun storedEssenceTypeName(player: Player, tier: Tier): String? {
        val storedType = tier.readStoredEssenceType(player)
        if (storedType == 0) {
            return null
        }
        return fillableEssences.firstOrNull { it.asRSCM(RSCMType.OBJ) == storedType }
    }

    fun setStoredEssenceType(player: Player, tier: Tier, itemInternal: String) {
        tier.setStoredEssenceType(player, itemInternal.asRSCM(RSCMType.OBJ))
    }

    fun clearStoredEssenceTypeIfEmpty(player: Player, tier: Tier) {
        if (storedAmount(player, tier) <= 0) {
            tier.setStoredEssenceType(player, 0)
        }
    }

    fun colossalCapacity(level: Int): Int =
        when {
            level >= 85 -> 40
            level >= 75 -> 27
            level >= 50 -> 16
            level >= 25 -> 8
            else -> 0
        }

    fun baseCapacity(player: Player, tier: Tier): Int =
        when (tier) {
            Tier.Colossal -> colossalCapacity(player.baseRunecraftingLvl)
            else -> tier.maxCapacity
        }

    fun capacity(player: Player, tier: Tier): Int {
        if (!tier.degradable || playerHasDegradeProtection(player)) {
            return baseCapacity(player, tier)
        }
        return degradedCapacity(player, tier, tier.degradeThreshold(player))
    }

    fun storedAmount(player: Player, tier: Tier): Int = tier.storedAmount(player)

    fun preferredEssenceType(player: Player, tier: Tier): String {
        storedEssenceTypeName(player, tier)?.let { return it }
        for (essence in fillableEssences) {
            if (player.inv.physicalCount(essence) > 0) {
                return essence
            }
        }
        return RUNE_ESSENCE
    }

    fun freeSpace(player: Player, tier: Tier): Int =
        (capacity(player, tier) - storedAmount(player, tier)).coerceAtLeast(0)

    fun depositUpTo(player: Player, tier: Tier, amount: Int): Int {
        val toAdd = minOf(amount, freeSpace(player, tier))
        if (toAdd <= 0) {
            return 0
        }
        tier.setStoredAmount(player, storedAmount(player, tier) + toAdd)
        return toAdd
    }

    fun removeStoredForType(player: Player, itemInternal: String, amount: Int): Int {
        var remaining = amount
        var removed = 0

        for (tier in Tier.entries.reversed()) {
            if (remaining <= 0) {
                break
            }
            if (!matchesStoredEssenceType(player, tier, itemInternal)) {
                continue
            }
            val current = storedAmount(player, tier)
            if (current <= 0) {
                continue
            }
            val take = minOf(remaining, current)
            tier.setStoredAmount(player, current - take)
            clearStoredEssenceTypeIfEmpty(player, tier)
            removed += take
            remaining -= take
        }

        return removed
    }

    fun removeStoredFromTier(player: Player, tier: Tier, amount: Int): Int {
        val current = storedAmount(player, tier)
        if (current <= 0) {
            return 0
        }
        val take = minOf(amount, current)
        tier.setStoredAmount(player, current - take)
        clearStoredEssenceTypeIfEmpty(player, tier)
        return take
    }

    fun applyDegradation(player: Player, tier: Tier, amountDeposited: Int, randomMultiplier: Int): Int {
        if (!tier.degradable) {
            return baseCapacity(player, tier)
        }
        val threshold = tier.degradeThreshold(player)
        val increment =
            when (tier) {
                Tier.Colossal -> amountDeposited
                else -> randomMultiplier * amountDeposited
            }
        val newThreshold = threshold + increment
        tier.setDegradeThreshold(player, newThreshold)
        return degradedCapacity(player, tier, newThreshold)
    }

    fun clearStorage(player: Player, tier: Tier) {
        tier.setStoredAmount(player, 0)
        tier.setStoredEssenceType(player, 0)
    }

    fun resetTier(player: Player, tier: Tier) {
        clearStorage(player, tier)
        if (tier.degradable) {
            tier.setDegradeThreshold(player, 0)
        }
    }

    val craftablePouchTiers =
        listOf(
            Tier.Small,
            Tier.Medium,
            Tier.Large,
            Tier.Giant,
        )

    fun hasColossalPouch(player: Player): Boolean =
        player.inv.contains(Tier.Colossal.intactItem) ||
            (Tier.Colossal.degradedItem != null && player.inv.contains(Tier.Colossal.degradedItem))

    fun hasRequiredCraftPouches(player: Player): Boolean =
        craftablePouchTiers.all { tier -> player.inv.contains(tier.intactItem) }

    fun craftPouchesAreEmpty(player: Player): Boolean =
        craftablePouchTiers.all { tier -> storedAmount(player, tier) == 0 }

    fun hasDegradedCraftPouch(player: Player): Boolean =
        craftablePouchTiers.any { tier ->
            tier.degradedItem != null && player.inv.contains(tier.degradedItem)
        }

    fun hasPouchesNeedingRepair(
        access: org.rsmod.api.player.protect.ProtectedAccess,
        includeColossal: Boolean = true,
    ): Boolean {
        val player = access.player
        val tiers =
            if (includeColossal) {
                Tier.entries.filter { it.degradable }
            } else {
                Tier.entries.filter { it.degradable && it != Tier.Colossal }
            }

        return tiers.any { tier ->
            val degraded =
                tier.degradedItem != null &&
                    (player.inv.contains(tier.degradedItem) || access.bank.contains(tier.degradedItem))
            degraded || tier.readDegradeThreshold(player) > 0
        }
    }

    fun repairPouches(access: org.rsmod.api.player.protect.ProtectedAccess, includeColossal: Boolean) {
        val player = access.player
        val tiers =
            if (includeColossal) {
                Tier.entries.filter { it.degradable }
            } else {
                Tier.entries.filter { it.degradable && it != Tier.Colossal }
            }

        for (tier in tiers) {
            resetTier(player, tier)
            replaceDegradedWithIntact(access.inv, tier)
            replaceDegradedWithIntact(access.bank, tier)
        }
    }

    private fun replaceDegradedWithIntact(inventory: org.rsmod.game.inv.Inventory, tier: Tier) {
        val degraded = tier.degradedItem ?: return
        val intactType = ServerCacheManager.getItem(tier.intactItem.asRSCM(RSCMType.OBJ)) ?: return
        for (slot in inventory.indices) {
            val obj = inventory[slot] ?: continue
            if (obj.isType(degraded)) {
                inventory[slot] = org.rsmod.game.inv.InvObj(intactType, obj.count, obj.vars)
            }
        }
    }

    fun playerHasDegradeProtection(player: Player): Boolean =
        "obj.skillcape_runecrafting" in player.worn ||
            "obj.skillcape_runecrafting_trimmed" in player.worn ||
            "obj.skillcape_max" in player.worn

    private fun degradedCapacity(player: Player, tier: Tier, threshold: Int): Int {
        val base = baseCapacity(player, tier)
        return when (tier) {
            Tier.Medium ->
                when {
                    threshold < 400 -> 6
                    threshold < 800 -> 3
                    else -> 0
                }
            Tier.Large ->
                when {
                    threshold < 400 -> 9
                    threshold < 600 -> 7
                    threshold < 800 -> 5
                    threshold < 1000 -> 3
                    else -> 0
                }
            Tier.Giant ->
                when {
                    threshold < 200 -> 12
                    threshold < 300 -> 9
                    threshold < 400 -> 8
                    threshold < 600 -> 7
                    threshold < 800 -> 6
                    threshold < 1000 -> 5
                    threshold < 1200 -> 3
                    else -> 0
                }
            Tier.Colossal -> {
                val firstDecay = colossalFirstDecayThreshold(base)
                if (threshold < firstDecay) {
                    return base
                }
                val step =
                    when {
                        threshold < firstDecay + 245 -> 35
                        threshold < firstDecay + 425 -> 30
                        threshold < firstDecay + 550 -> 25
                        threshold < firstDecay + 630 -> 20
                        threshold < firstDecay + 675 -> 15
                        threshold < firstDecay + 695 -> 10
                        threshold < firstDecay + 701 -> 5
                        else -> 0
                    }
                minOf(base, step)
            }
            else -> base
        }
    }

    private fun colossalFirstDecayThreshold(baseCapacity: Int): Int = 8 * baseCapacity

    private var Player.smallEssencePouch by intVarBit("varbit.small_essence_pouch")
    private var Player.mediumEssencePouch by intVarBit("varbit.medium_essence_pouch")
    private var Player.largeEssencePouch by intVarBit("varbit.large_essence_pouch")
    private var Player.giantEssencePouch by intVarBit("varbit.giant_essence_pouch")
    private var Player.colossalEssencePouch by intVarBit("varbit.colossal_essence_pouch")

    private var Player.smallPouchEssenceType by intVarp("varp.rcu_pouch_essence_type_small")
    private var Player.mediumPouchEssenceType by intVarp("varp.rcu_pouch_essence_type_med")
    private var Player.largePouchEssenceType by intVarp("varp.rcu_pouch_essence_type_large")
    private var Player.giantPouchEssenceType by intVarp("varp.rcu_pouch_essence_type_giant")
    private var Player.colossalPouchEssenceType by intVarp("varp.rcu_pouch_essence_type_colossal")

    private fun Tier.setStoredEssenceType(player: Player, typeId: Int) {
        when (this) {
            Tier.Small -> player.smallPouchEssenceType = typeId
            Tier.Medium -> player.mediumPouchEssenceType = typeId
            Tier.Large -> player.largePouchEssenceType = typeId
            Tier.Giant -> player.giantPouchEssenceType = typeId
            Tier.Colossal -> player.colossalPouchEssenceType = typeId
        }
    }

    private fun Tier.readStoredEssenceType(player: Player): Int =
        when (this) {
            Tier.Small -> player.smallPouchEssenceType
            Tier.Medium -> player.mediumPouchEssenceType
            Tier.Large -> player.largePouchEssenceType
            Tier.Giant -> player.giantPouchEssenceType
            Tier.Colossal -> player.colossalPouchEssenceType
        }

    private fun Tier.storedAmount(player: Player): Int =
        when (this) {
            Tier.Small -> player.smallEssencePouch
            Tier.Medium -> player.mediumEssencePouch
            Tier.Large -> player.largeEssencePouch
            Tier.Giant -> player.giantEssencePouch
            Tier.Colossal -> player.colossalEssencePouch
        }

    private fun Tier.setStoredAmount(player: Player, amount: Int) {
        when (this) {
            Tier.Small -> player.smallEssencePouch = amount
            Tier.Medium -> player.mediumEssencePouch = amount
            Tier.Large -> player.largeEssencePouch = amount
            Tier.Giant -> player.giantEssencePouch = amount
            Tier.Colossal -> player.colossalEssencePouch = amount
        }
    }

    private fun Tier.degradeThreshold(player: Player): Int = readDegradeThreshold(player)

    private fun Tier.readDegradeThreshold(player: Player): Int =
        when (this) {
            Tier.Medium -> player.mediumPouchDegradeThreshold
            Tier.Large -> player.largePouchDegradeThreshold
            Tier.Giant -> player.giantPouchDegradeThreshold
            Tier.Colossal -> player.colossalPouchDegradeThreshold
            else -> 0
        }

    private fun Tier.setDegradeThreshold(player: Player, threshold: Int) {
        when (this) {
            Tier.Medium -> player.mediumPouchDegradeThreshold = threshold
            Tier.Large -> player.largePouchDegradeThreshold = threshold
            Tier.Giant -> player.giantPouchDegradeThreshold = threshold
            Tier.Colossal -> player.colossalPouchDegradeThreshold = threshold
            else -> Unit
        }
    }
}
