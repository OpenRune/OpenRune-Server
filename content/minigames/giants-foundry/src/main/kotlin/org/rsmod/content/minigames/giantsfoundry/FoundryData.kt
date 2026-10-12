package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.game.entity.Player

internal fun Player.foundryVar(name: String): Int = vars[name]

internal fun Player.setFoundryVar(name: String, value: Int) =
    VarPlayerIntMapSetter.set(this, name, value)

/** Sword part a mould shapes; [index] matches `giants_foundry_mould_current_tab`. */
internal enum class MouldPart(val index: Int, val enum: String, val unlockPrefix: String) {
    Forte(0, "enum.giants_foundry_moulds_forte", "ricasso"),
    Blade(1, "enum.giants_foundry_moulds_blade", "blade"),
    Tip(2, "enum.giants_foundry_moulds_tip", "tip"),
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

/** Values of `giants_foundry_commission_word_*` (enum `giants_foundry_commission_words`). */
internal enum class CommissionWord(val id: Int, val label: String, val positive: Boolean) {
    Narrow(1, "Narrow", false),
    Light(2, "Light", false),
    Flat(3, "Flat", false),
    Broad(4, "Broad", true),
    Heavy(5, "Heavy", true),
    Spiked(6, "Spiked", true);

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

    /** The foundry needs 15 Smithing (Sleeping Giants). */
    private const val FOUNDRY_LEVEL = 15

    private fun param(name: String): Int = name.asRSCM(RSCMType.PARAM)

    /**
     * The bar for each crucible metal (same order as [METALS]), from the cache's `smithing_bars`
     * table. https://oldschool.runescape.wiki/w/Crucible
     */
    val bars: List<SmithingBarsRow> by lazy {
        METALS.map { metal ->
            SmithingBarsRow.all().firstOrNull { it.prefix.equals(metal, ignoreCase = true) }
                ?: error("No smithing_bars row for $metal")
        }
    }

    /** Smithing level to add [bar]: its smelting level, at least the foundry's 15. */
    fun barLevel(bar: SmithingBarsRow): Int {
        val smelt = bar.statReq.firstOrNull { it.t0.id == SMITHING.asRSCM(RSCMType.STAT) }?.t1 ?: 1
        return maxOf(FOUNDRY_LEVEL, smelt)
    }

    private const val SMITHING = "stat.smithing"
}
