package org.rsmod.content.quest.area.varrock.dragonslayer

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

/**
 * Takes every `obj to count` in [take] and adds every `obj to count` in [give] in one inventory
 * transaction: either all of it happens or nothing does.
 */
internal fun Player.exchange(
    take: List<Pair<String, Int>>,
    give: List<Pair<String, Int>> = emptyList(),
): Boolean =
    invTransaction(inv) {
        val inventory = select(inv)
        for ((obj, count) in take) {
            delete {
                from = inventory
                this.obj = obj.asRSCM(RSCMType.OBJ)
                strictCount = count
            }
        }
        for ((obj, count) in give) {
            insert {
                into = inventory
                this.obj = obj.asRSCM(RSCMType.OBJ)
                strictCount = count
            }
        }
    }.success
