package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.ironman.hasSafeDeathMark
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Singleton
class ZulrahDeathRecovery @Inject constructor() {
    fun hasItems(player: Player): Boolean = player.invMap[RECOVERY_INVENTORY]?.isNotEmpty() == true

    fun processDeath(
        player: Player,
        handling: PlayerDeathHandling,
        result: DeathDropResult,
        inZulrah: Boolean,
    ): DeathDropResult {
        if (player.hasSafeDeathMark()) return result
        discard(player)
        return if (inZulrah) store(player, handling, result) else result
    }

    fun discard(player: Player) {
        if (!hasItems(player)) return
        player.invMap.getValue(RECOVERY_INVENTORY).fillNulls()
        player.mes("Your unclaimed items at Zul-Andra have been lost because you died again.")
    }

    fun store(
        player: Player,
        handling: PlayerDeathHandling,
        result: DeathDropResult,
    ): DeathDropResult {
        val lost = recoverableItems(handling, result)
        if (lost.isEmpty()) return result
        val inventory = player.invMap.getOrPut(RECOVERY_INVENTORY)
        check(inventory.isEmpty()) { "Zulrah recovery items must be claimed before another encounter." }
        check(lost.size <= inventory.size) { "Zulrah recovery inventory is too small." }
        lost.forEachIndexed { slot, item -> inventory[slot] = item }
        player.mes("The priest at Zul-Andra is holding your lost items. Reclaim them for free.")
        player.mes("If you die another unsafe death before reclaiming them, they will be lost.")
        return result.copy(
            supplyPile = emptyList(),
            lostTradeable = emptyList(),
            lostUntradeable = emptyList(),
            coinsForKiller = 0,
        )
    }

    fun reclaim(player: Player) {
        val inventory = player.invMap[RECOVERY_INVENTORY]
        if (inventory == null || inventory.isEmpty()) {
            player.mes("You have no lost items to reclaim.")
            return
        }
        for (slot in inventory.indices) {
            val item = inventory[slot] ?: continue
            player.invTransfer(
                from = inventory,
                fromSlot = slot,
                count = item.count,
                into = player.inv,
                strict = false,
            )
        }
        if (inventory.isEmpty()) {
            player.mes("You have reclaimed all your lost items.")
        } else {
            player.mes("Make more inventory space and speak to the priest again to reclaim the rest.")
        }
    }

    internal fun recoverableItems(
        handling: PlayerDeathHandling,
        result: DeathDropResult,
    ): List<InvObj> {
        val untradeables =
            if (handling.untradeableHandling == UntradeableHandling.DROP) result.lostUntradeable
            else emptyList()
        return result.supplyPile + result.lostTradeable + untradeables
    }

    internal companion object {
        const val RECOVERY_INVENTORY = "inv.zulrah_recovery"
    }
}
