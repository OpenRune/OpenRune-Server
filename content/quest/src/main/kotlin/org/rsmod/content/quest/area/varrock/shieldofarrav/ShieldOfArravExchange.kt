package org.rsmod.content.quest.area.varrock.shieldofarrav

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

internal object ShieldOfArravExchange {
    /** Takes every [take] stack and adds every [give] stack in one transaction, or changes nothing. */
    fun exchange(
        player: Player,
        take: List<Pair<String, Int>> = emptyList(),
        give: List<Pair<String, Int>> = emptyList(),
    ): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                for ((obj, count) in take) {
                    delete {
                        from = inv
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        strictCount = count
                    }
                }
                for ((obj, count) in give) {
                    insert {
                        into = inv
                        this.obj = obj.asRSCM(RSCMType.OBJ)
                        strictCount = count
                    }
                }
            }
        return result.success
    }

    /** Puts [obj] in the inventory, using up a copy in the bank first so only one exists. */
    fun takeFromStorage(player: Player, obj: String): Boolean {
        if (!exchange(player, give = listOf(obj to 1))) {
            return false
        }
        val bank = player.invMap.getOrPut("inv.bank")
        if (obj in bank) {
            player.invTransaction(bank) {
                val stored = select(bank)
                delete {
                    from = stored
                    this.obj = obj.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        }
        return true
    }
}
