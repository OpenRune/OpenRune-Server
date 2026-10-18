package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_hammers
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_nails
import org.rsmod.api.enums.DaddysHomeEnums.daddys_home_saws
import org.rsmod.api.enums.TrailEnums.trail_stash_units
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.table.TrailTierRow
import org.rsmod.api.table.cluehelper.CluehelperOutfitRow
import org.rsmod.game.entity.Player

/**
 * STASH units. `trail_stash_units` (Jagex enum 1368) maps each unit's loc to its tier code (x) and
 * constructed bit (z); the unit's contents live in a fixed block of [INV] slots derived from those,
 * so withdrawing returns exactly the objs that were stored (gilded and other variants included).
 */
object ClueStash {
    const val INV = "inv.trail_stash"

    private val units: Map<Int, Int> by lazy {
        trail_stash_units.backing.entries
            .filter { it.value != null }
            .associate { (loc, coord) ->
                loc.id to checkNotNull(coord).let { (it.x - 1) * UNITS_PER_TIER + it.z }
            }
    }

    private val unitSteps: Map<Int, ClueStep> by lazy {
        (TrailTiers.BEGINNER..MASTER)
            .flatMap(ClueSteps::all)
            .filter { it.hideyHole != null && it.outfit != null }
            .associateBy { checkNotNull(it.hideyHole).id }
    }

    fun isUnit(loc: ObjectServerType): Boolean = loc.id in units && loc.id in unitSteps

    fun allUnits(): List<ObjectServerType> =
        units.keys.filter { it in unitSteps }.mapNotNull(ServerCacheManager::getObject)

    fun tier(loc: ObjectServerType): TrailTierRow {
        val code = checkNotNull(units[loc.id]) / UNITS_PER_TIER + 1
        return TrailTiers.tier(if (code == BEGINNER_CODE) TrailTiers.BEGINNER else code)
    }

    fun built(player: Player, loc: ObjectServerType): Boolean =
        player.vars[builtVarbit(loc)] != 0

    fun filled(player: Player, loc: ObjectServerType?): Boolean {
        if (loc == null || !isUnit(loc)) return false
        val stash = player.invMap[INV] ?: return false
        return slots(loc).any { stash[it] != null }
    }

    suspend fun ProtectedAccess.build(loc: ObjectServerType) {
        if (built(player, loc)) return
        val tier = tier(loc)
        if (stat(CONSTRUCTION) < tier.stashLevel) {
            mes("You need a Construction level of ${tier.stashLevel} to build this.")
            return
        }
        if (daddys_home_hammers.none { (_, hammer) -> hammer != null && hammer in inv }) {
            mes("You need a hammer to build this.")
            return
        }
        if (daddys_home_saws.none { (_, saw) -> saw != null && saw in inv }) {
            mes("You need a saw to build this.")
            return
        }
        val plank = tier.stashPlank.symbol()
        val nails = nailsFor() ?: return mes("You need $NAILS nails of the same metal to build this.")
        if (inv.count(plank) < PLANKS) {
            mes("You need $PLANKS ${tier.stashPlank.name.lowercase()}s to build this.")
            return
        }
        anim(BUILD_SEQ)
        delay(BUILD_DELAY)
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    this.obj = tier.stashPlank.id
                    this.strictCount = PLANKS
                }
                delete {
                    this.from = from
                    this.obj = nails.asRSCM(RSCMType.OBJ)
                    this.strictCount = NAILS
                }
            }
        if (result.failure || built(player, loc)) return
        VarPlayerIntMapSetter.set(player, builtVarbit(loc), 1)
        statAdvance(CONSTRUCTION, tier.stashXp.toDouble())
        mes("You build a STASH unit.")
    }

    fun ProtectedAccess.search(loc: ObjectServerType) {
        if (filled(player, loc)) withdraw(loc) else deposit(loc)
    }

    private fun ProtectedAccess.deposit(loc: ObjectServerType) {
        val outfit = unitSteps[loc.id]?.outfit ?: return
        val stash = player.invMap.getOrPut(INV)
        val pieces = ClueOutfits.piecesIn(inv, outfit) ?: takeEquipped(outfit)
        if (pieces == null) {
            mes("You don't have the items needed to fill this STASH unit.")
            return
        }
        val slots = slots(loc)
        val result =
            player.invTransaction(inv, stash) {
                val from = select(inv)
                val into = select(stash)
                for ((index, slot) in pieces.withIndex()) {
                    val obj = checkNotNull(inv[slot])
                    delete {
                        this.from = from
                        this.obj = obj.id
                        this.strictCount = obj.count
                        this.strictSlot = slot
                    }
                    insert {
                        this.into = into
                        this.obj = obj.id
                        this.strictCount = obj.count
                        this.strictSlot = slots.first + index
                        this.vars = obj.vars
                    }
                }
            }
        if (result.failure) return
        mes("You store the items in the STASH unit.")
    }

    private fun ProtectedAccess.takeEquipped(outfit: CluehelperOutfitRow): List<Int>? {
        if (player.vars[TAKE_EQUIPPED] == 0) return null
        val wornSlots = ClueOutfits.wornPiecesNeeded(inv, worn, outfit) ?: return null
        if (inv.freeSpace() < wornSlots.size) {
            mes("You don't have enough inventory space to take your equipped items.")
            return null
        }
        for (slot in wornSlots) {
            if (wornUnequip(slot) != WornUnequipResult.Success) return null
        }
        return ClueOutfits.piecesIn(inv, outfit)
    }

    private fun ProtectedAccess.withdraw(loc: ObjectServerType) {
        val stash = player.invMap.getOrPut(INV)
        val stored = slots(loc).filter { stash[it] != null }
        val result =
            player.invTransaction(stash, inv) {
                val from = select(stash)
                val into = select(inv)
                for (slot in stored) {
                    val obj = checkNotNull(stash[slot])
                    delete {
                        this.from = from
                        this.obj = obj.id
                        this.strictCount = obj.count
                        this.strictSlot = slot
                    }
                    insert {
                        this.into = into
                        this.obj = obj.id
                        this.strictCount = obj.count
                        this.vars = obj.vars
                    }
                }
            }
        if (result.failure) {
            mes("You don't have enough inventory space to take the items out.")
            return
        }
        mes("You take the items out of the STASH unit.")
    }

    private fun ProtectedAccess.nailsFor(): String? =
        daddys_home_nails
            .mapNotNull { it.value?.symbol() }
            .filter { it != DRAGON_NAILS }
            .firstOrNull { inv.count(it) >= NAILS }

    private fun slots(loc: ObjectServerType): IntRange {
        val base = checkNotNull(units[loc.id]) * SLOTS_PER_UNIT
        return base until base + SLOTS_PER_UNIT
    }

    private fun builtVarbit(loc: ObjectServerType): String =
        TrailTiers.varbit(checkNotNull(ServerCacheManager.getObject(loc.id)).multiVarBit)

    private const val TAKE_EQUIPPED = "varbit.option_hidey_holes_equipped"
    private const val MASTER = 5
    private const val BEGINNER_CODE = 6
    private const val UNITS_PER_TIER = 32
    private const val SLOTS_PER_UNIT = 11
    private const val PLANKS = 2
    private const val NAILS = 10
    private const val BUILD_DELAY = 3
    private const val DRAGON_NAILS = "obj.nails_dragon"
    private const val CONSTRUCTION = "stat.construction"
    private const val BUILD_SEQ = "seq.human_poh_build"
}
