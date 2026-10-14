package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.enums.GiantsFoundryEnums
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.content.skills.smithing.util.SmithingData
import org.rsmod.game.entity.Player

internal fun Player.foundryVar(name: String): Int = vars[name]

internal fun Player.setFoundryVar(name: String, value: Int) =
    VarPlayerIntMapSetter.set(this, name, value)

/** Sword part a mould shapes; its position in [entries] matches `giants_foundry_mould_current_tab`. */
internal enum class MouldPart(val enum: String, val unlockPrefix: String) {
    Forte("enum.giants_foundry_moulds_forte", "ricasso"),
    Blade("enum.giants_foundry_moulds_blade", "blade"),
    Tip("enum.giants_foundry_moulds_tip", "tip"),
}

/**
 * A mould from the cache: `giants_foundry_moulds_*` map a list position (1-based, as stored in
 * `giants_foundry_mould_selected_*`) to a struct holding its name, level and style scores.
 */
internal data class Mould(
    val part: MouldPart,
    val position: Int,
    val struct: Int,
    val name: String,
    val level: Int,
    val broadNarrow: Int,
    val heavyLight: Int,
    val spikedFlat: Int,
) {
    /** Score for one commission word; mirrors clientscript `script6105`. */
    fun score(word: CommissionWord): Int {
        val value =
            when (word) {
                CommissionWord.Narrow,
                CommissionWord.Broad -> broadNarrow
                CommissionWord.Light,
                CommissionWord.Heavy -> heavyLight
                CommissionWord.Flat,
                CommissionWord.Spiked -> spikedFlat
            }
        return if (word.positive) value.coerceAtLeast(0) else (-value).coerceAtLeast(0)
    }
}

/** Values of `giants_foundry_commission_word_*`, keys of enum `giants_foundry_commission_words`. */
internal enum class CommissionWord(val id: Int, val positive: Boolean) {
    Narrow(1, false),
    Light(2, false),
    Flat(3, false),
    Broad(4, true),
    Heavy(5, true),
    Spiked(6, true);

    val label: String
        get() = GiantsFoundryEnums.giants_foundry_commission_words.getValue(id)

    /** The style axis (broad/narrow, heavy/light, spiked/flat) this word sits on. */
    val axis: Int
        get() = (id - 1) % 3

    companion object {
        fun forId(id: Int): CommissionWord? = entries.firstOrNull { it.id == id }
    }
}

internal object FoundryData {
    /**
     * The 12 commissions: the first word is broad/narrow or heavy/light, the second heavy/light
     * or spiked/flat, never two words on one axis.
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Commissions
     */
    val commissions: List<Pair<CommissionWord, CommissionWord>> by lazy {
        val first =
            listOf(CommissionWord.Broad, CommissionWord.Narrow, CommissionWord.Heavy, CommissionWord.Light)
        val second =
            listOf(CommissionWord.Heavy, CommissionWord.Light, CommissionWord.Spiked, CommissionWord.Flat)
        first.flatMap { a -> second.filter { it.axis != a.axis }.map { a to it } }
    }

    val moulds: Map<MouldPart, List<Mould>> by lazy {
        MouldPart.entries.associateWith { part -> loadMoulds(part) }
    }

    fun mould(part: MouldPart, position: Int): Mould? =
        moulds.getValue(part).firstOrNull { it.position == position }

    private fun loadMoulds(part: MouldPart): List<Mould> {
        val enum =
            ServerCacheManager.getEnum(part.enum.asRSCM(RSCMType.ENUM))
                ?: error("Missing mould enum ${part.enum}")
        return enum.values.map { (key, value) ->
            val structId = (value as Number).toInt()
            val struct = ServerCacheManager.getStruct(structId) ?: error("Missing struct $structId")
            Mould(
                part = part,
                position = (key as Number).toInt(),
                struct = structId,
                name = struct.getString(param("param.giants_foundry_mould_name")),
                level = struct.getInt(param("param.giants_foundry_mould_level")),
                broadNarrow = struct.getInt(param("param.giants_foundry_mould_broad_narrow")),
                heavyLight = struct.getInt(param("param.giants_foundry_mould_heavy_light")),
                spikedFlat = struct.getInt(param("param.giants_foundry_mould_spiked_flat")),
            )
        }
    }

    /**
     * Unlock varbit for a bought mould, or null for the default ones. Clientscript
     * `giants_foundry_mould_is_unlocked` gates each part's five highest struct ids (the bought
     * moulds) on `giants_foundry_unlocked_mould_<part>_7..11`, in struct order.
     */
    fun unlockVarbit(mould: Mould): String? {
        val bought = moulds.getValue(mould.part).map { it.struct }.sorted().takeLast(BOUGHT_PER_PART)
        val index = bought.indexOf(mould.struct)
        return if (index < 0) null else "varbit.giants_foundry_unlocked_mould_${mould.part.unlockPrefix}_${index + 7}"
    }

    private const val BOUGHT_PER_PART = 5

    private fun param(name: String): Int = name.asRSCM(RSCMType.PARAM)

    /** The bar for each crucible metal, in [METALS] order. */
    val bars: List<SmithingBarsRow> by lazy { METAL_BARS.map(SmithingBarsRow::getRow) }

    private val METAL_BARS =
        listOf(
            "dbrow.bronze",
            "dbrow.iron",
            "dbrow.steel",
            "dbrow.mithril",
            "dbrow.adamantite",
            "dbrow.runite",
        )

    /**
     * Smithed items the crucible melts down: item id to (metal index, bars worth). An item is
     * worth one bar less than it takes to smith, so one-bar items are refused.
     */
    val smithedItems: Map<Int, Pair<Int, Int>> by lazy {
        val barRows = bars.map { it.rowId }
        SmithingData.productMeta
            .mapNotNull { meta ->
                val metal = barRows.indexOf(meta.bar.rowId)
                if (metal < 0 || meta.barCount <= 1) null
                else meta.product.id to (metal to meta.barCount - 1)
            }
            .toMap()
    }

    /** Smithing level to add [bar]: its smelting level, at least what Sleeping Giants needs. */
    fun barLevel(bar: SmithingBarsRow): Int {
        val smelt = bar.statReq.firstOrNull { it.t0.id == SMITHING.asRSCM(RSCMType.STAT) }?.t1 ?: 1
        return maxOf(SleepingGiants.requiredSmithing, smelt)
    }

    private const val SMITHING = "stat.smithing"
}
