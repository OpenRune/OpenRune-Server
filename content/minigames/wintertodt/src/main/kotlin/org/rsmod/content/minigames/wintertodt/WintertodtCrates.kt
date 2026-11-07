package org.rsmod.content.minigames.wintertodt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

internal fun openExtraCrateTransaction(
    player: Player,
    slot: Int,
    rewards: List<WintertodtReward>,
): Boolean {
    val inv = player.inv
    val crate = inv[slot] ?: return false
    if (crate.id != EXTRA_CRATE.asRSCM(RSCMType.OBJ)) return false
    val opened =
        player.invTransaction(inv) {
            val from = select(inv)
            delete {
                this.from = from
                this.obj = crate.id
                this.strictSlot = slot
            }
            for (reward in rewards) {
                insert {
                    this.into = from
                    this.obj = reward.item.asRSCM(RSCMType.OBJ)
                    this.strictCount = reward.count
                    this.cert = reward.noted
                }
            }
        }
    return !opened.failure
}

internal fun grantCartReward(
    player: Player,
    reward: WintertodtReward,
    givePet: (Player, String) -> Boolean,
): Boolean {
    if (givePet(player, reward.item)) return true
    return WintertodtRewards.add(player, reward)
}
