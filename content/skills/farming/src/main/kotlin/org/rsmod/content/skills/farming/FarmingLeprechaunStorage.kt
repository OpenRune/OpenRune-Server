package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum
import dev.openrune.types.varp.bits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.farming.FarmingServicesRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

internal object FarmingServicesData {
    val row by lazy { FarmingServicesRow.getRow("dbrow.farming_services") }
    val CAPACITIES by lazy { enum<ItemServerType, Int>("farming_leprechaun_capacities") }
}

internal class LeprechaunTool(
    val obj: ItemServerType,
    val component: String,
    val variables: List<String>,
) {
    val capacity: Int get() = FarmingServicesData.CAPACITIES.getValue(obj)
}

object FarmingLeprechaunStorage {
    private const val CAN = "obj.watering_can_dummy"
    private const val SECATEURS = "obj.secateurs"
    private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
    private const val MAGIC_STORED = "varbit.farming_tools_fairysecateurs"
    private const val BUCKET_TYPE = "varbit.farming_tools_bottomless_bucket_type"
    private const val BUCKET_CHARGES = "varbit.farming_tools_bottomless_bucket_quantity"

    internal val tools by lazy {
        FarmingServicesData.row.tools.map { LeprechaunTool(it.t0, it.t1, it.t2.split(',')) }
    }
    private val cans by lazy { enum<Int, ItemServerType>("farming_leprechaun_watering_cans").backing }
    private val byObj by lazy { tools.associateBy { it.obj.id } }
    private val notable by lazy {
        val crops = FarmingCrops.all
        val grimy = crops.filter { it.kind == PatchKind.HERB }.mapNotNull { it.produce }.toSet()
        buildSet {
            crops.filter { it.harvest != HarvestModel.CHOP }.mapNotNullTo(this) { it.produce }
            crops.mapNotNullTo(this) { it.roots }
            FarmingSaplings.chains.mapTo(this) { it.sapling.internalName }
            HerbloreCleaningRow.all().filter { it.input.internalName in grimy }
                .mapTo(this) { it.output.internalName }
            FarmingServicesData.row.notingAdd.mapTo(this) { it.internalName }
            removeAll(FarmingServicesData.row.notingExclude.map { it.internalName }.toSet())
        }
    }

    internal fun tool(type: ItemServerType): LeprechaunTool? {
        val plain = if (type.isCert) ServerCacheManager.getItem(type.certlink) ?: return null else type
        byObj[plain.id]?.let { return it }
        return when {
            plain.internalName == MAGIC_SECATEURS -> byObj[SECATEURS.asRSCM()]
            cans.values.any { it?.id == plain.id } -> byObj[CAN.asRSCM()]
            FarmingBottomlessCompost.isBucket(plain.internalName) ->
                byObj[FarmingBottomlessCompost.EMPTY.asRSCM()]
            else -> null
        }
    }

    internal fun stored(player: Player, tool: LeprechaunTool): InvObj? {
        if (tool.obj.internalName == CAN) {
            val type = cans[player.vars[tool.variables.single()]] ?: return null
            return InvObj(type)
        }
        if (tool.obj.internalName == FarmingBottomlessCompost.EMPTY) {
            val type = player.vars[BUCKET_TYPE]
            if (type !in 1..Compost.entries.size) return null
            val charges = player.vars[BUCKET_CHARGES]
            if (charges !in 0..FarmingBottomlessCompost.capacity) return null
            val kind = Compost.entries[type - 1]
            if ((kind == Compost.NONE) != (charges == 0)) return null
            return FarmingBottomlessCompost.bucket(kind, charges)
        }
        val count = count(player, tool)
        if (count <= 0) return null
        val type = if (tool.obj.internalName == SECATEURS && player.vars[MAGIC_STORED] == 1) {
            checkNotNull(ServerCacheManager.getItem(MAGIC_SECATEURS.asRSCM()))
        } else tool.obj
        return InvObj(type, count)
    }

    private fun count(player: Player, tool: LeprechaunTool): Int {
        var shift = 0
        var count = 0
        for (variable in tool.variables) {
            count = count or (player.vars[variable] shl shift)
            val bits = checkNotNull(ServerCacheManager.getVarbit(variable.asRSCM(RSCMType.VARBIT))).bits
            shift += bits.last - bits.first + 1
        }
        return count
    }

    private fun setCount(player: Player, tool: LeprechaunTool, count: Int) {
        var rest = count
        for (variable in tool.variables) {
            val bits = checkNotNull(ServerCacheManager.getVarbit(variable.asRSCM(RSCMType.VARBIT))).bits
            val width = bits.last - bits.first + 1
            VarPlayerIntMapSetter.set(player, variable, rest and ((1 shl width) - 1))
            rest = rest ushr width
        }
        check(rest == 0)
    }

    /** Transfers the selected physical item; capacity truncation never consumes the remainder. */
    fun deposit(player: Player, slot: Int, requested: Int): Int {
        if (requested <= 0) return 0
        val held = player.inv[slot] ?: return 0
        val type = getInvObj(held)
        val plain = if (type.isCert) ServerCacheManager.getItem(type.certlink) ?: return 0 else type
        val tool = tool(type) ?: return 0
        val current = stored(player, tool)
        if (tool.capacity == 1) {
            if (player.vars[tool.variables.single()] != 0) return 0
            val bucket = if (tool.obj.internalName == FarmingBottomlessCompost.EMPTY) {
                FarmingBottomlessCompost.contents(if (type.isCert) InvObj(plain) else held) ?: return 0
            } else null
            val can = if (tool.obj.internalName == CAN) {
                cans.entries.firstOrNull { it.value?.id == plain.id }?.key ?: return 0
            } else null
            val result = player.invTransaction(player.inv) {
                delete(select(player.inv), held.id, 1, slot)
            }
            if (!result.success) return 0
            if (bucket != null) {
                // The client distinguishes only none/empty/filled; tier numbers are server-private.
                VarPlayerIntMapSetter.set(player, BUCKET_TYPE, bucket.kind.ordinal + 1)
                VarPlayerIntMapSetter.set(player, BUCKET_CHARGES, bucket.charges)
            } else VarPlayerIntMapSetter.set(player, tool.variables.single(), checkNotNull(can))
            return 1
        }
        // shortcut: cross-variant secateur replacement stays held until its swap rule is sourced.
        if (current != null && current.id != plain.id) return 0
        val oldCount = current?.count ?: 0
        val amount = minOf(requested, tool.capacity - oldCount, player.inv.physicalCount(type.internalName))
        if (amount <= 0) return 0
        if (!player.invTransaction(player.inv) {
            delete(select(player.inv), held.id, amount)
        }.success) return 0
        setCount(player, tool, oldCount + amount)
        if (tool.obj.internalName == SECATEURS) {
            VarPlayerIntMapSetter.set(player, MAGIC_STORED, if (plain.internalName == MAGIC_SECATEURS) 1 else 0)
        }
        return amount
    }

    fun withdraw(player: Player, component: String, requested: Int, notes: Boolean = false): Int {
        if (requested <= 0) return 0
        val tool = tools.firstOrNull { it.component == component } ?: return 0
        if (notes && tool.capacity == 1) return 0
        val stored = stored(player, tool) ?: return 0
        val type = getInvObj(stored)
        val output = if (notes) {
            if (!type.canCert) return 0
            ServerCacheManager.getItem(type.certlink) ?: return 0
        } else type
        val amount = player.inv.fits(output, minOf(requested, stored.count))
        if (amount <= 0) return 0
        if (!player.invTransaction(player.inv) {
            add(select(player.inv), output.id, amount, vars = stored.vars)
        }.success) return 0
        when (tool.obj.internalName) {
            CAN -> VarPlayerIntMapSetter.set(player, tool.variables.single(), 0)
            FarmingBottomlessCompost.EMPTY -> {
                VarPlayerIntMapSetter.set(player, BUCKET_TYPE, 0)
                VarPlayerIntMapSetter.set(player, BUCKET_CHARGES, 0)
            }
            else -> {
                setCount(player, tool, stored.count - amount)
                if (tool.obj.internalName == SECATEURS && stored.count == amount) {
                    VarPlayerIntMapSetter.set(player, MAGIC_STORED, 0)
                }
            }
        }
        return amount
    }

    internal fun depositTool(player: Player, tool: LeprechaunTool, requested: Int): Int {
        var remaining = requested
        for (type in carriedTypes(tool)) {
            val ids = if (type.canCert) listOf(type.id, type.certlink) else listOf(type.id)
            for (id in ids) {
                val slot = player.inv.indices.firstOrNull { player.inv[it]?.id == id } ?: continue
                val moved = deposit(player, slot, remaining)
                remaining -= moved
                if (remaining <= 0) return requested
            }
        }
        return requested - remaining
    }

    private fun carriedTypes(tool: LeprechaunTool): List<ItemServerType> = when (tool.obj.internalName) {
        CAN -> cans.entries.sortedByDescending { it.key }.mapNotNull { it.value }
        SECATEURS -> listOf(checkNotNull(ServerCacheManager.getItem(MAGIC_SECATEURS.asRSCM())), tool.obj)
        FarmingBottomlessCompost.EMPTY -> listOf(
            checkNotNull(ServerCacheManager.getItem(FarmingBottomlessCompost.FILLED.asRSCM())), tool.obj,
        )
        else -> listOf(tool.obj)
    }

    fun depositAll(player: Player): Int = tools.sumOf { depositTool(player, it, Int.MAX_VALUE) }

    fun canNote(type: ItemServerType): Boolean = type.internalName in notable

    fun note(player: Player, obj: Int): Boolean {
        val type = ServerCacheManager.getItem(obj) ?: return false
        if (type.isCert || !type.canCert || !canNote(type)) return false
        val count = player.inv.physicalCount(type.internalName)
        if (count <= 0) return false
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, obj, count)
            add(inv, type.certlink, count)
        }.success
    }
}
