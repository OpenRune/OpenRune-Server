package org.rsmod.content.bosses.royaltitans

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import java.text.NumberFormat
import java.util.Locale
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player

enum class RoyalTitansLootMode {
    Loot,
    Sacrifice,
    Pages,
}

/**
 * Rolls the per-titan loot a player's contribution entitles them to. Uniques are rolled first and
 * replace the two standard rolls when one is received; sacrifice only rolls the doubled pet rate
 * and clues; take-pages rolls 10-19 pages plus the tertiaries.
 */
class RoyalTitansLoot
@Inject
constructor(
    private val deps: BossDeps,
    private val objRepo: ObjRepository,
) {
    private val Player.lootNotificationOn by boolVarBit("varbit.option_lootnotification_on")
    private val Player.lootNotificationUntradeables by
        boolVarBit("varbit.option_lootnotification_untradeables")
    private val Player.lootNotificationValue by intVarBit("varbit.option_lootnotification_value")

    fun roll(
        element: TitanElement,
        mode: RoyalTitansLootMode,
        player: Player,
        percent: Int,
    ): List<RolledLoot> {
        val tables = tablesFor(element)
        val rolled = mutableListOf<RolledLoot>()
        when (mode) {
            RoyalTitansLootMode.Loot -> {
                val uniques = rollTable(tables.uniques, player).filter { rollPercent(percent) }
                if (uniques.isNotEmpty()) {
                    rolled += uniques.map { RolledLoot(it, SCALE_FULL) }
                } else {
                    repeat(MAIN_ROLLS) {
                        rolled +=
                            rollTable(tables.main, player).map { RolledLoot(it, percent) }
                    }
                }
            }
            RoyalTitansLootMode.Sacrifice -> Unit
            RoyalTitansLootMode.Pages ->
                rolled +=
                    rollTable(tables.pages, player).map { RolledLoot(it, percent) }
        }
        rolled += rollTable(tables.clues, player).map { RolledLoot(it, SCALE_FULL) }
        rolled += rollTable(tables.pet(mode), player).map { RolledLoot(it, SCALE_FULL) }
        return rolled
    }

    fun give(player: Player, npcId: Int, eventId: Int, rolled: RolledLoot) {
        val item = rolled.item
        if (item.isNothing || !item.condition(player)) {
            return
        }
        val obj = item.transformObj(player) ?: item.obj
        if (obj.isEmpty()) {
            return
        }
        val type = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)) ?: return
        val base = item.rollCount(deps.random)
        val count = scaleCount(base, rolled.scalePercent)
        CollectionLog.grant(player, obj, count)
        notifyValuableDrop(player, type, count)
        player.invAddOrDrop(objRepo, obj, count)
        ClientScripts.lootTrackerAddLoot(player, npcId, eventId, type.id, count)
    }

    private fun notifyValuableDrop(
        player: Player,
        type: dev.openrune.types.ItemServerType,
        count: Int,
    ) {
        if (!player.lootNotificationOn) {
            return
        }
        if (!type.tradeable && !player.lootNotificationUntradeables) {
            return
        }
        val value = type.cost.toLong() * count
        val threshold = player.lootNotificationValue
        if (threshold > 0 && value < threshold) {
            return
        }
        val coins = NUMBER.format(value)
        player.mes(
            "<col=ef1020>Valuable drop: $count x ${type.name} ($coins coins)</col>",
            ChatType.GameMessage,
        )
    }

    private fun rollTable(table: dtx.table.Table<Player, DropRollItem>, player: Player): List<DropRollItem> {
        val result = table.roll(player, ArgMap()).flatten()
        return when (result) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(result.result)
            is RollResult.ListOf -> result.results
        }
    }

    private fun rollPercent(percent: Int): Boolean =
        percent >= SCALE_FULL || deps.random.of(SCALE_FULL) < percent

    private fun scaleCount(base: Int, percent: Int): Int =
        RoyalTitansMath.scaleQuantity(base, percent)

    private fun tablesFor(element: TitanElement): TitanTables =
        if (element == TitanElement.Fire) BRANDA else ELDRIC

    private class TitanTables(
        val main: dtx.table.Table<Player, DropRollItem>,
        val uniques: dtx.table.Table<Player, DropRollItem>,
        val clues: dtx.table.Table<Player, DropRollItem>,
        val normalPet: dtx.table.Table<Player, DropRollItem>,
        val sacrificePet: dtx.table.Table<Player, DropRollItem>,
        val pages: dtx.table.Table<Player, DropRollItem>,
    ) {
        fun pet(mode: RoyalTitansLootMode): dtx.table.Table<Player, DropRollItem> =
            if (mode == RoyalTitansLootMode.Sacrifice) sacrificePet else normalPet
    }

    data class RolledLoot(val item: DropRollItem, val scalePercent: Int)

    private companion object {
        private const val MAIN_ROLLS = 2
        private const val SCALE_FULL = 100
        private val NUMBER = NumberFormat.getIntegerInstance(Locale.UK)

        private val BRANDA =
            TitanTables(
                main = rsPlayerWeightedTable(total = 55) {
                    name("Branda the Fire Queen")
                    2 weight "obj.cert_mystic_fire_staff" count 4
                    2 weight "obj.cert_fire_battlestaff" count 6
                    2 weight "obj.rune_plateskirt" count 2
                    2 weight "obj.rune_scimitar" count 2
                    2 weight "obj.rune_pickaxe" count 2
                    2 weight "obj.firerune" count 1500..3000
                    2 weight "obj.chaosrune" count 200..400
                    2 weight "obj.deathrune" count 150..300
                    2 weight "obj.naturerune" count 100..150
                    2 weight "obj.lawrune" count 100..150
                    2 weight "obj.soulrune" count 100..150
                    2 weight "obj.bloodrune" count 100..150
                    2 weight "obj.rune_arrow" count 100..300
                    2 weight "obj.cert_coal" count 280..320
                    2 weight "obj.cert_gold_ore" count 60..100
                    2 weight "obj.cert_fire_orb" count 20..40
                    2 weight "obj.cert_unidentified_avantoe" count 6..8
                    2 weight "obj.cert_unidentified_cadantine" count 6..8
                    2 weight "obj.cert_unidentified_dwarf_weed" count 6..8
                    2 weight "obj.cert_unidentified_irit" count 6..8
                    2 weight "obj.cert_unidentified_kwuarm" count 6..8
                    2 weight "obj.cert_unidentified_lantadyme" count 6..8
                    2 weight "obj.cert_unidentified_ranarr" count 6..8
                    1 weight "obj.maple_seed" count 4..6
                    1 weight "obj.palm_tree_seed" count 2
                    1 weight "obj.yew_seed" count 2
                    2 weight "obj.coins" count 10000..30000
                    2 weight "obj.4doseprayerrestore" count 2..4
                    2 weight "obj.desiccated_page" count 6..24
                },
                uniques =
                    rsPlayerPrerollTable {
                        1 outOf 75 weight "obj.twinflame_piece_2" count 1
                        1 outOf 75 weight "obj.mystic_vigour_prayer_scroll" count 1
                        1 outOf 16 weight "obj.giantsoul_amulet_charged" count 1
                    },
                clues = cluesTable(),
                normalPet =
                    rsPlayerTertiaryTable {
                        1 outOf 3000 weight "obj.rtbrandapet" count 1
                    },
                sacrificePet =
                    rsPlayerTertiaryTable {
                        1 outOf 1500 weight "obj.rtbrandapet" count 1
                    },
                pages = pageTable(),
            )

        private val ELDRIC =
            TitanTables(
                main = rsPlayerWeightedTable(total = 56) {
                    name("Eldric the Ice King")
                    2 weight "obj.cert_mystic_water_staff" count 4
                    2 weight "obj.cert_water_battlestaff" count 6
                    2 weight "obj.rune_platelegs" count 2
                    2 weight "obj.rune_sq_shield" count 2
                    2 weight "obj.rune_axe" count 2
                    1 weight "obj.rune_pickaxe" count 2
                    2 weight "obj.waterrune" count 1500..3000
                    2 weight "obj.chaosrune" count 200..400
                    2 weight "obj.deathrune" count 150..300
                    2 weight "obj.naturerune" count 100..150
                    2 weight "obj.lawrune" count 100..150
                    2 weight "obj.soulrune" count 100..150
                    2 weight "obj.bloodrune" count 100..150
                    2 weight "obj.rune_arrow" count 100..300
                    2 weight "obj.cert_coal" count 280..320
                    2 weight "obj.cert_gold_ore" count 60..100
                    2 weight "obj.cert_water_orb" count 20..40
                    2 weight "obj.avantoe_seed" count 4
                    2 weight "obj.cadantine_seed" count 4
                    2 weight "obj.dwarf_weed_seed" count 3
                    2 weight "obj.irit_seed" count 4
                    2 weight "obj.kwuarm_seed" count 4
                    2 weight "obj.lantadyme_seed" count 4
                    2 weight "obj.ranarr_seed" count 4
                    1 weight "obj.maple_seed" count 4..6
                    1 weight "obj.palm_tree_seed" count 2
                    1 weight "obj.yew_seed" count 2
                    2 weight "obj.coins" count 10000..30000
                    2 weight "obj.4doseprayerrestore" count 2..4
                    2 weight "obj.desiccated_page" count 6..24
                },
                uniques =
                    rsPlayerPrerollTable {
                        1 outOf 75 weight "obj.twinflame_piece_1" count 1
                        1 outOf 75 weight "obj.deadeye_prayer_scroll" count 1
                        1 outOf 16 weight "obj.giantsoul_amulet_charged" count 1
                    },
                clues = cluesTable(),
                normalPet =
                    rsPlayerTertiaryTable {
                        1 outOf 3000 weight "obj.rteldricpet" count 1
                    },
                sacrificePet =
                    rsPlayerTertiaryTable {
                        1 outOf 1500 weight "obj.rteldricpet" count 1
                    },
                pages = pageTable(),
            )

        private fun cluesTable() =
            rsPlayerTertiaryTable {
                1 outOf 25 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_clue_hard_map001")
                }
                1 outOf 100 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
                    player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
                }
            }

        private fun pageTable() =
            rsPlayerTertiaryTable {
                1 outOf 1 weight "obj.desiccated_page" count 10..19
            }
    }
}
