package org.rsmod.content.events.demonicpacts

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.dbcol.DbHelper
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.SkillModifiers
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * Transmute's Alchemic Divergence and Convergence, which take the place of High and Low Level
 * Alchemy: up to 10 of a resource move one tier up or down its transmutation chain.
 */
object TransmuteSpells {
    private val logger = InlineLogger()

    private const val TABLE = "dbtable.transmutation"
    private const val TIER_COLUMN = "dbcol.transmutation:tier_item"
    private const val TABLE_ID = 225
    private const val TOGGLES = "varp.league_relic_effect_toggle_1"
    private const val ALCHEMY_TOGGLE_BIT = 5
    private const val MAX_ITEMS = 10
    private const val XP_PER_ITEM = 10.0
    private const val CAST_TICKS = 5
    private const val HIGH_ALCH_ANIM = 713
    private const val HIGH_ALCH_SPOTANIM = 113
    private const val LOW_ALCH_ANIM = 712
    private const val LOW_ALCH_SPOTANIM = 112
    private const val SPOTANIM_HEIGHT = 92

    /** Each chain lists a resource's obj ids from lowest to highest tier. */
    val chains: List<List<Int>> by lazy { loadChains() }

    private val chainByObj: Map<Int, List<Int>> by lazy {
        buildMap { chains.forEach { chain -> chain.forEach { obj -> putIfAbsent(obj, chain) } } }
    }

    fun install() {
        val previous = SkillModifiers.alchemyReplacement
        SkillModifiers.alchemyReplacement = { access, slot, obj, high ->
            if (transmutes(access.player)) {
                access.transmute(slot, obj, upgrade = high)
                true
            } else {
                previous(access, slot, obj, high)
            }
        }
    }

    fun transmutes(player: Player): Boolean =
        player.hasRelic(RelicNames.TRANSMUTE) &&
            (player.vars[TOGGLES] ushr ALCHEMY_TOGGLE_BIT) and 1 == 0

    /** Switches alchemy between transmuting (the default with Transmute) and making coins. */
    fun setTransmuting(player: Player, enabled: Boolean) {
        val current = player.vars[TOGGLES]
        val bit = 1 shl ALCHEMY_TOGGLE_BIT
        VarPlayerIntMapSetter.set(player, TOGGLES, if (enabled) current and bit.inv() else current or bit)
    }

    /** The obj [obj] would become when moved one tier [upgrade] or down, or null at the end. */
    fun next(obj: Int, upgrade: Boolean): Int? {
        val chain = chainByObj[obj] ?: return null
        val index = chain.indexOf(obj) + if (upgrade) 1 else -1
        return chain.getOrNull(index)
    }

    private fun ProtectedAccess.transmute(slot: Int, target: ItemServerType, upgrade: Boolean) {
        val spellName = if (upgrade) "Alchemic Divergence" else "Alchemic Convergence"
        val noted = target.isCert
        val base = if (noted) target.certlink else target.id
        val result = next(base, upgrade)
        if (result == null) {
            val reason =
                when {
                    chainByObj[base] == null -> "$spellName can't transmute that."
                    upgrade -> "That is already the highest tier."
                    else -> "That is already the lowest tier."
                }
            mes(reason)
            return
        }
        val available =
            if (noted) {
                inv[slot]?.count ?: 0
            } else {
                inv.count { it?.id == base }
            }
        val count = minOf(MAX_ITEMS, available)
        if (count <= 0) {
            return
        }
        val transaction =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    obj = target.id
                    strictCount = count
                    if (noted) {
                        strictSlot = slot
                    }
                }
                insert {
                    into = from
                    obj = result
                    strictCount = count
                    cert = noted
                }
            }
        if (!transaction.success) {
            mes("You don't have enough inventory space to do that.")
            return
        }
        actionDelay = mapClock + CAST_TICKS
        val anim = if (upgrade) HIGH_ALCH_ANIM else LOW_ALCH_ANIM
        val spotanim = if (upgrade) HIGH_ALCH_SPOTANIM else LOW_ALCH_SPOTANIM
        anim(RSCM.getReverseMapping(RSCMType.SEQ, anim))
        spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, spotanim), height = SPOTANIM_HEIGHT)
        statAdvance("stat.magic", XP_PER_ITEM * count)
        val name = ServerCacheManager.getItem(result)?.name?.lowercase() ?: "items"
        mes("You transmute $count ${target.name.lowercase()} into $name.")
    }

    private fun loadChains(): List<List<Int>> {
        val rows =
            runCatching { DbHelper.table(TABLE).map { it.getColumn(TIER_COLUMN).column.values } }
                .getOrElse {
                    ServerCacheManager.getRowsForTable(TABLE_ID).map { row ->
                        runCatching { DbHelper(row).getColumn(0).column.values }.getOrNull()
                    }
                }
        val chains =
            rows.mapNotNull { values ->
                val ids = values?.map { (it as Number).toInt() } ?: return@mapNotNull null
                ids.filterIndexed { i, _ -> i % 2 == 0 }.takeIf { it.size >= 2 }
            }
        if (chains.isEmpty()) {
            logger.warn { "Demonic Pacts League: no transmutation chains found in the cache." }
        }
        return chains
    }
}
