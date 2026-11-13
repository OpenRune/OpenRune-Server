package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.ironman.IronmanActivity
import org.rsmod.api.player.ironman.IronmanRestrictions
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj

object FarmingSeedVault {
    internal const val MAIN = "interface.seed_vault"
    internal const val SIDE = "interface.seed_vault_deposit"
    internal const val SELECTED = "varp.seed_vault_selected_qty"
    internal const val REQUESTED = "varp.seed_vault_requested_qty"
    internal const val LOCKS = "varbit.bank_side_slot_overview"
    internal const val IGNORE_LOCKS = "varbit.bank_side_slot_ignoreinvlocks"
    internal const val SHOW_LOCKS = "varbit.bank_side_slot_showop"
    private const val EMPTY_FAVOURITE = 255

    internal fun store(player: Player): Inventory = player.invMap.getOrPut("inv.seed_vault")

    internal fun blocked(player: Player): Boolean =
        IronmanRestrictions.block(player, IronmanActivity.UIM_SEED_VAULT)

    internal fun accessible(player: Player): Boolean =
        MAIN in player.ui && SIDE in player.ui && !blocked(player)

    internal fun initialize(player: Player) {
        if (player.vars["varp.seed_vault_favourites1"] == 0 && player.vars["varp.seed_vault_favourites2"] == 0) {
            for (i in 1..8) VarPlayerIntMapSetter.set(player, favouriteKey(i), EMPTY_FAVOURITE)
        }
        if (player.vars[SELECTED] <= 0) VarPlayerIntMapSetter.set(player, SELECTED, 1)
    }

    internal fun storable(type: ItemServerType): Boolean =
        !type.isPlaceholder && !type.isCert && type.seedVaultCategory() > 0

    internal fun locked(player: Player, slot: Int): Boolean =
        slot in 0..27 && player.vars[IGNORE_LOCKS] == 0 && player.vars[LOCKS] and (1 shl slot) != 0

    internal fun deposit(player: Player, slot: Int, expected: InvObj, requested: Int): Int {
        if (!accessible(player) || requested <= 0 || slot !in player.inv.indices || player.inv[slot] != expected || locked(player, slot)) return 0
        val input = getInvObj(expected)
        val type = if (input.isCert) ServerCacheManager.getItem(input.certlink) ?: return 0 else input
        if (!storable(type)) return 0
        val vault = store(player)
        val stored = vault.countOf(type.id)
        val placeholder = type.placeholderLink != 0 && vault.any { it?.id == type.placeholderLink }
        if (stored == 0 && !placeholder && !vault.hasFreeSpace()) return 0
        val amount = minOf(requested.toLong(), expected.count.toLong(), Int.MAX_VALUE.toLong() - stored).toInt()
        if (amount <= 0) return 0
        val result = player.invTransaction(player.inv, vault) {
            delete(select(player.inv), expected.id, amount, slot)
            add(select(vault), type.id, amount)
        }
        return if (result.success) amount else 0
    }

    internal fun withdraw(player: Player, slot: Int, expected: InvObj, requested: Int, notes: Boolean): Int {
        if (!accessible(player) || requested <= 0) return 0
        val vault = store(player)
        if (slot !in vault.indices || vault[slot] != expected) return 0
        val type = getInvObj(expected)
        if (!storable(type)) return 0
        val output = if (notes && type.canCert) ServerCacheManager.getItem(type.certlink) ?: return 0 else type
        val amount = player.inv.fits(output, minOf(requested, expected.count))
        if (amount <= 0) return 0
        val result = player.invTransaction(vault, player.inv) {
            delete(select(vault), expected.id, amount, slot, placehold = true)
            add(select(player.inv), output.id, amount)
        }
        return if (result.success) amount else 0
    }

    internal fun favourite(player: Player, slot: Int): Boolean {
        if (!accessible(player)) return false
        val vault = store(player)
        if (slot !in vault.indices || vault[slot] == null) return false
        val existing = (1..8).firstOrNull { player.vars[favouriteKey(it)] == slot }
        if (existing != null) {
            VarPlayerIntMapSetter.set(player, favouriteKey(existing), EMPTY_FAVOURITE)
            return true
        }
        if (getInvObj(checkNotNull(vault[slot])).isPlaceholder) return false
        val free = (1..8).firstOrNull { player.vars[favouriteKey(it)] == EMPTY_FAVOURITE } ?: return false
        VarPlayerIntMapSetter.set(player, favouriteKey(free), slot)
        return true
    }

    internal fun removePlaceholders(player: Player, selected: Int? = null) {
        if (!accessible(player)) return
        val vault = store(player)
        for (slot in vault.indices) {
            if (selected != null && selected != slot) continue
            val obj = vault[slot] ?: continue
            if (getInvObj(obj).isPlaceholder && !isFavourite(player, slot)) vault[slot] = null
        }
    }

    internal fun reorder(player: Player, from: Int, into: Int): Boolean {
        if (!accessible(player)) return false
        val vault = store(player)
        if (from !in vault.indices || from == into) return false
        if (into == vault.size) return favourite(player, from)
        if (into !in vault.indices) return false
        val first = vault[from] ?: return false
        val second = vault[into] ?: return false
        val firstType = baseType(getInvObj(first))
        val secondType = baseType(getInvObj(second))
        if (firstType.seedVaultCategory() != secondType.seedVaultCategory() && !(isFavourite(player, from) && isFavourite(player, into))) return false
        vault[from] = second
        vault[into] = first
        for (i in 1..8) {
            val key = favouriteKey(i)
            when (player.vars[key]) {
                from -> VarPlayerIntMapSetter.set(player, key, into)
                into -> VarPlayerIntMapSetter.set(player, key, from)
            }
        }
        return true
    }

    private fun baseType(type: ItemServerType): ItemServerType =
        if (type.isPlaceholder) checkNotNull(ServerCacheManager.getItem(type.placeholderLink)) else type

    private fun isFavourite(player: Player, slot: Int): Boolean =
        (1..8).any { player.vars[favouriteKey(it)] == slot }

    private fun favouriteKey(index: Int): String = "varbit.seed_vault_fave$index"
}
