package org.rsmod.content.bosses.bryophyta

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.rsGuaranteedTable
import kotlin.random.Random
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.tables.shared.GemTalisman
import org.rsmod.content.drops.tables.shared.buildGemDropTable
import org.rsmod.content.slayer.dialogue.SlayerMasters
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
val bryophytaDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Bryophyta Drops",
        npcs = npcs("npc.gb_mossgiant"),
        guaranteed =
            rsGuaranteedTable {
                add(
                    singleRollable<Player, DropRollItem> {
                        selectResult { player, args ->
                            RollResult.ListOf(rollBryophytaDrops(player, args))
                        }
                    }
                )
            },
    )

internal fun rollBryophytaDrops(
    player: Player,
    args: ArgMap = ArgMap(),
    random: Random = Random.Default,
): List<DropRollItem> {
    val drops = mutableListOf<DropRollItem>()
    drops += DropRollItem(if (player.members) GIANT_BONES else BIG_BONES, 1)

    val table = if (player.members) MEMBERS_MAIN else FREE_MAIN
    val entry = pick(table, random)
    when (entry.subtable) {
        Subtable.Herbs -> drops += pick(MEMBERS_HERBS, random)
        Subtable.Seeds -> drops += pick(MEMBERS_SEEDS, random)
        Subtable.Gems -> {
            val gemTable = if (player.members) MEMBERS_GEM else FREE_GEM
            val result = gemTable.roll(player, args).flatten()
            drops +=
                when (result) {
                    is RollResult.Single -> listOf(result.result)
                    is RollResult.ListOf -> result.results
                    else -> emptyList()
                }
        }
        null -> drops += entry.drops
    }

    if (random.nextInt(CLUE_DENOMINATOR) == 0) {
        drops +=
            DropRollItem(CLUE, 1, transformObj = { p -> p.clueScrollTransformObj(CLUE) })
    }
    val keyDenominator = if (isMossGiantTask(player)) 8 else 16
    if (random.nextInt(keyDenominator) == 0) {
        drops += DropRollItem(MOSSY_KEY, 1)
    }

    if (player.members) {
        if (player.isOnQuest(RAG_AND_BONE_MAN_II) && random.nextInt(4) == 0) {
            drops += DropRollItem("obj.rag_moss_giant_bone", 1)
        }
        if (random.nextInt(24) == 0) {
            drops += DropRollItem("obj.arceuus_corpse_giant", 1)
        }
        if (random.nextInt(400) == 0) {
            drops += DropRollItem("obj.dorgesh_construction_bone", 1)
        }
        if (random.nextInt(10025) < 2) {
            drops += DropRollItem("obj.dorgesh_construction_bone_curved", 1)
        }
        if (random.nextInt(5000) == 0) {
            drops += DropRollItem("obj.champions_challenge_giant", 1)
        }
    }
    return drops
}

private enum class Subtable {
    Herbs,
    Seeds,
    Gems,
}

private class MainEntry(
    val weight: Int,
    val drops: List<DropRollItem> = emptyList(),
    val subtable: Subtable? = null,
)

private class SubEntry(val weight: Int, val obj: String)

private fun pick(table: List<MainEntry>, random: Random): MainEntry {
    var roll = random.nextInt(table.sumOf { it.weight })
    for (entry in table) {
        if (roll < entry.weight) return entry
        roll -= entry.weight
    }
    error("Bryophyta drop weight out of range")
}

private fun pick(table: List<SubEntry>, random: Random): DropRollItem {
    var roll = random.nextInt(table.sumOf { it.weight })
    for (entry in table) {
        if (roll < entry.weight) return DropRollItem(entry.obj, 1)
        roll -= entry.weight
    }
    error("Bryophyta subtable weight out of range")
}

private fun isMossGiantTask(player: Player): Boolean {
    if (player.vars["varbit.slayer_master"] == SlayerMasters.TASK_WILDERNESS) {
        return false
    }
    val taskKey = SlayerTaskCredit.taskKey(player)
    if (taskKey <= 0) return false
    val bossType = ServerCacheManager.getNpc("npc.gb_mossgiant".asRSCM(RSCMType.NPC)) ?: return false
    return SlayerTaskCredit.counts(bossType, taskKey)
}

internal fun bryophytaMembersMainWeight(): Int = MEMBERS_MAIN.sumOf { it.weight }

internal fun bryophytaFreeMainWeight(): Int = FREE_MAIN.sumOf { it.weight }

private val MEMBERS_GEM = buildGemDropTable(talisman = GemTalisman.Nature)

private val FREE_GEM = buildGemDropTable(members = false)

private val MEMBERS_MAIN =
    listOf(
        MainEntry(5, listOf(DropRollItem("obj.black_sq_shield", 1))),
        MainEntry(2, listOf(DropRollItem("obj.magic_staff", 1))),
        MainEntry(2, listOf(DropRollItem("obj.steel_med_helm", 1))),
        MainEntry(2, listOf(DropRollItem("obj.mithril_sword", 1))),
        MainEntry(2, listOf(DropRollItem("obj.mithril_spear", 1))),
        MainEntry(1, listOf(DropRollItem("obj.steel_kiteshield", 1))),
        MainEntry(4, listOf(DropRollItem("obj.lawrune", 3))),
        MainEntry(3, listOf(DropRollItem("obj.airrune", 18))),
        MainEntry(3, listOf(DropRollItem("obj.earthrune", 27))),
        MainEntry(3, listOf(DropRollItem("obj.chaosrune", 7))),
        MainEntry(3, listOf(DropRollItem("obj.naturerune", 6))),
        MainEntry(2, listOf(DropRollItem("obj.cosmicrune", 3))),
        MainEntry(2, listOf(DropRollItem("obj.iron_arrow", 15))),
        MainEntry(1, listOf(DropRollItem("obj.steel_arrow", 30))),
        MainEntry(1, listOf(DropRollItem("obj.deathrune", 3))),
        MainEntry(1, listOf(DropRollItem("obj.bloodrune", 1))),
        MainEntry(19, listOf(DropRollItem("obj.coins", 37))),
        MainEntry(8, listOf(DropRollItem("obj.coins", 2))),
        MainEntry(10, listOf(DropRollItem("obj.coins", 119))),
        MainEntry(2, listOf(DropRollItem("obj.coins", 300))),
        MainEntry(6, listOf(DropRollItem("obj.steel_bar", 1))),
        MainEntry(1, listOf(DropRollItem("obj.coal", 1))),
        MainEntry(1, listOf(DropRollItem("obj.spinach_roll", 1))),
        MainEntry(5, subtable = Subtable.Herbs),
        MainEntry(4, subtable = Subtable.Gems),
        MainEntry(35, subtable = Subtable.Seeds),
    )

private val FREE_MAIN =
    listOf(
        MainEntry(5, listOf(DropRollItem("obj.black_sq_shield", 1))),
        MainEntry(2, listOf(DropRollItem("obj.magic_staff", 1))),
        MainEntry(2, listOf(DropRollItem("obj.steel_med_helm", 1))),
        MainEntry(2, listOf(DropRollItem("obj.mithril_sword", 1))),
        MainEntry(1, listOf(DropRollItem("obj.steel_kiteshield", 1))),
        MainEntry(4, listOf(DropRollItem("obj.lawrune", 3))),
        MainEntry(3, listOf(DropRollItem("obj.airrune", 18))),
        MainEntry(3, listOf(DropRollItem("obj.earthrune", 27))),
        MainEntry(3, listOf(DropRollItem("obj.chaosrune", 7))),
        MainEntry(3, listOf(DropRollItem("obj.naturerune", 6))),
        MainEntry(2, listOf(DropRollItem("obj.cosmicrune", 3))),
        MainEntry(2, listOf(DropRollItem("obj.iron_arrow", 15))),
        MainEntry(1, listOf(DropRollItem("obj.steel_arrow", 30))),
        MainEntry(1, listOf(DropRollItem("obj.deathrune", 3))),
        MainEntry(5, listOf(DropRollItem("obj.coins", 10))),
        MainEntry(35, listOf(DropRollItem("obj.coins", 5))),
        MainEntry(19, listOf(DropRollItem("obj.coins", 37))),
        MainEntry(11, listOf(DropRollItem("obj.coins", 2))),
        MainEntry(10, listOf(DropRollItem("obj.coins", 119))),
        MainEntry(2, listOf(DropRollItem("obj.coins", 300))),
        MainEntry(6, listOf(DropRollItem("obj.steel_bar", 1))),
        MainEntry(1, listOf(DropRollItem("obj.coal", 1))),
        MainEntry(1, listOf(DropRollItem("obj.spinach_roll", 1))),
        MainEntry(4, subtable = Subtable.Gems),
    )

private val MEMBERS_HERBS =
    listOf(
        SubEntry(32, "obj.unidentified_guam"),
        SubEntry(24, "obj.unidentified_marentill"),
        SubEntry(18, "obj.unidentified_tarromin"),
        SubEntry(14, "obj.unidentified_harralander"),
        SubEntry(11, "obj.unidentified_ranarr"),
        SubEntry(8, "obj.unidentified_irit"),
        SubEntry(6, "obj.unidentified_avantoe"),
        SubEntry(5, "obj.unidentified_kwuarm"),
        SubEntry(4, "obj.unidentified_cadantine"),
        SubEntry(3, "obj.unidentified_lantadyme"),
        SubEntry(3, "obj.unidentified_dwarf_weed"),
    )

private val MEMBERS_SEEDS =
    listOf(
        SubEntry(137, "obj.limpwurt_seed"),
        SubEntry(131, "obj.strawberry_seed"),
        SubEntry(125, "obj.marrentill_seed"),
        SubEntry(92, "obj.jangerberry_bush_seed"),
        SubEntry(85, "obj.tarromin_seed"),
        SubEntry(83, "obj.wildblood_hop_seed"),
        SubEntry(63, "obj.watermelon_seed"),
        SubEntry(56, "obj.harralander_seed"),
        SubEntry(40, "obj.snape_grass_seed"),
        SubEntry(39, "obj.ranarr_seed"),
        SubEntry(34, "obj.whiteberry_bush_seed"),
        SubEntry(29, "obj.mushroom_seed"),
        SubEntry(27, "obj.toadflax_seed"),
        SubEntry(18, "obj.belladonna_seed"),
        SubEntry(18, "obj.irit_seed"),
        SubEntry(13, "obj.poisonivy_bush_seed"),
        SubEntry(12, "obj.avantoe_seed"),
        SubEntry(12, "obj.cactus_seed"),
        SubEntry(9, "obj.kwuarm_seed"),
        SubEntry(8, "obj.potato_cactus_seed"),
        SubEntry(5, "obj.snapdragon_seed"),
        SubEntry(4, "obj.cadantine_seed"),
        SubEntry(3, "obj.lantadyme_seed"),
        SubEntry(2, "obj.dwarf_weed_seed"),
        SubEntry(1, "obj.torstol_seed"),
    )

private const val GIANT_BONES = "obj.giant_bones"
private const val BIG_BONES = "obj.big_bones"
private const val CLUE = "obj.trail_clue_beginner"
private const val MOSSY_KEY = "obj.mossy_key"
private const val CLUE_DENOMINATOR = 45
private const val RAG_AND_BONE_MAN_II = "quest_ragandboneman2"
