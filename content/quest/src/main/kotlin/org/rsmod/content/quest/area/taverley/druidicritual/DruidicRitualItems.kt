package org.rsmod.content.quest.area.taverley.druidicritual

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.NamedEnums.druidic_ritual_meats
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player

internal object DruidicRitualItems {
    fun enchantedFor(raw: ItemServerType): ItemServerType? = druidic_ritual_meats[raw]

    /** Swaps one raw meat in [slot] for its enchanted form; false changes nothing. */
    fun dip(player: Player, raw: ItemServerType, slot: Int): Boolean {
        val enchanted = enchantedFor(raw) ?: return false
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete {
                from = inv
                obj = raw.id
                strictCount = 1
                strictSlot = slot
            }
            insert {
                into = inv
                obj = enchanted.id
                strictCount = 1
                strictSlot = slot
            }
        }.success
    }

    fun hasAllEnchanted(player: Player): Boolean =
        druidic_ritual_meats.values.all { it != null && player.inv.count(it.internalName) > 0 }

    /** Takes one of each enchanted meat, or nothing at all. */
    fun handIn(player: Player): Boolean {
        val meats = druidic_ritual_meats.values.filterNotNull()
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            for (meat in meats) {
                delete {
                    from = inv
                    obj = meat.id
                    strictCount = 1
                }
            }
        }.success
    }

    /** Sells the Herblore cape and hood for [CAPE_PRICE] coins in one transaction. */
    fun buyCape(player: Player): Boolean {
        val cape = if (player.countMaxedStats() > 1) CAPE_TRIMMED else CAPE
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete {
                from = inv
                obj = COINS.asRSCM(RSCMType.OBJ)
                strictCount = CAPE_PRICE
            }
            insert {
                into = inv
                obj = cape.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
            insert {
                into = inv
                obj = HOOD.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
        }.success
    }

    fun giveHood(player: Player): Boolean =
        player.invTransaction(player.inv) {
            val inv = select(player.inv)
            insert {
                into = inv
                obj = HOOD.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
        }.success

    fun ownsCape(player: Player): Boolean =
        player.ownsAny(CAPE) || player.ownsAny(CAPE_TRIMMED)

    fun ownsHood(player: Player): Boolean = player.ownsAny(HOOD)

    private fun Player.ownsAny(obj: String): Boolean =
        inv.contains(obj) || worn.contains(obj) || invMap.getOrPut("inv.bank").contains(obj)

    private fun Player.countMaxedStats(): Int =
        ServerCacheManager.getStats().keys.count { id ->
            statBase(RSCM.getReverseMapping(RSCMType.STAT, id)) >= MAX_LEVEL
        }

    const val CAPE_PRICE = 99_000
    const val COINS = "obj.coins"
    const val CAPE = "obj.skillcape_herblore"
    const val CAPE_TRIMMED = "obj.skillcape_herblore_trimmed"
    const val HOOD = "obj.skillcape_herblore_hood"
    const val MAX_LEVEL = 99
}
