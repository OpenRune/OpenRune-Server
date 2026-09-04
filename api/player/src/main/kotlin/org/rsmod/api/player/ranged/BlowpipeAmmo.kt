package org.rsmod.api.player.ranged

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.righthand
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/**
 * Accesses the cache-packed ammunition stored inside toxic and rosewood blowpipes.
 *
 * Both blowpipe families store their dart type and count in the same varobj bit fields rather than
 * in the quiver. Toxic blowpipes additionally store their scales in that packed value.
 */
public object BlowpipeAmmo {
    /** A resolved dart type together with the cache ordinal stored in the equipped blowpipe. */
    public data class LoadedDart(val type: ItemServerType, val index: Int)

    /** The remaining ammunition after a successful stored-ammo reduction. */
    public data class Consumption(
        val dartsLeft: Int,
        val scalesLeft: Int,
        val becameEmpty: Boolean,
        val canFire: Boolean,
    )

    /** The exact supplies recovered from one specific blowpipe by the unload interaction. */
    public data class UnloadedContents(
        val dart: LoadedDart?,
        val dartCount: Int,
        val scaleCount: Int,
    )

    /** The exact darts recovered by the normal Unload interaction. */
    public data class UnloadedDarts(val dart: LoadedDart?, val count: Int)

    /** Returns the dart currently loaded in [obj], or null when it has no usable stored dart. */
    public fun loadedDart(obj: InvObj?): LoadedDart? {
        val blowpipe = obj ?: return null
        val variant = variant(blowpipe) ?: return null
        if (!variant.loaded) {
            return null
        }

        return storedDart(blowpipe)
    }

    /** Returns the stored dart even when a depleted pipe has changed to its empty item variant. */
    public fun storedDart(obj: InvObj?): LoadedDart? {
        val blowpipe = obj ?: return null
        if (variant(blowpipe) == null) {
            return null
        }
        val dartCount = darts(blowpipe)
        if (dartCount <= 0) {
            return null
        }

        val index = blowpipe.vars.getBits(dartType.bits)
        val type = dartTypes.getOrNull(index) ?: return null
        return LoadedDart(type = type, index = index)
    }

    /**
     * Returns whether the stored dart is valid for this blowpipe family.
     *
     * Toxic blowpipes accept the full dart range through dragon; rosewood blowpipes stop at
     * rune (wiki: "It is able to shoot up to rune darts").
     */
    public fun canUseLoadedDart(obj: InvObj?): Boolean {
        val blowpipe = obj ?: return false
        val variant = variant(blowpipe) ?: return false
        val dart = loadedDart(blowpipe) ?: return false
        return dart.index <= variant.family.maxDartIndex
    }

    /** Every revision-240 toxic-blowpipe variant that stores its own scales and darts. */
    public fun toxicBlowpipeAliases(): List<String> =
        variants.filter { it.family.usesScales }.map { it.alias }

    /** Charged-cache variants which expose Wield, Check, Unload, and Uncharge options. */
    public fun toxicLoadedBlowpipeAliases(): List<String> =
        variants.filter { it.family.usesScales && it.loaded }.map { it.alias }

    /** All cache-recognised dart aliases that can be inserted into a toxic blowpipe. */
    public fun toxicDartAliases(): List<String> = dartAliases

    /** Returns whether [obj] is one of the toxic (scale-consuming) blowpipe variants. */
    public fun isToxic(obj: InvObj?): Boolean = variant(obj)?.family?.usesScales == true

    /**
     * Stores up to [requested] copies of [dart] in the exact blowpipe at [slot].
     *
     * A pipe may only contain one dart type. Callers delete exactly the returned amount from the
     * inventory, so hitting capacity never deletes excess darts.
     */
    public fun storeDarts(
        inventory: Inventory,
        slot: Int,
        dart: ItemServerType,
        requested: Int,
    ): Int {
        require(requested >= 0) { "requested must not be negative." }
        val obj = inventory[slot] ?: return 0
        val variant = variant(obj) ?: return 0
        val dartIndex = dartTypes.indexOfFirst { it.id == dart.id }
        if (dartIndex !in 0..variant.family.maxDartIndex) {
            return 0
        }

        val loaded = storedDart(obj)
        if (loaded != null && loaded.type.id != dart.id) {
            return 0
        }

        val current = darts(obj)
        val total = (current + requested).coerceAtMost(MAX_STORED_AMOUNT)
        val added = total - current
        if (added == 0) {
            return 0
        }

        val packed = obj.vars.withBits(dartType.bits, dartIndex).withBits(dartCount.bits, total)
        inventory[slot] = InvObj(item(variant.loadedAlias), count = obj.count, vars = packed)
        return added
    }

    /** Stores up to [requested] Zulrah's scales in the exact toxic blowpipe at [slot]. */
    public fun storeScales(inventory: Inventory, slot: Int, requested: Int): Int {
        require(requested >= 0) { "requested must not be negative." }
        val obj = inventory[slot] ?: return 0
        val variant = variant(obj) ?: return 0
        if (!variant.family.usesScales) {
            return 0
        }

        val current = scales(obj)
        val total = (current + requested).coerceAtMost(MAX_STORED_AMOUNT)
        val added = total - current
        if (added == 0) {
            return 0
        }

        val packed = obj.vars.withBits(scaleCount.bits, total)
        inventory[slot] = InvObj(item(variant.loadedAlias), count = obj.count, vars = packed)
        return added
    }

    /** Removes only the stored darts, preserving every scale in this specific pipe. */
    public fun unloadDarts(inventory: Inventory, slot: Int): UnloadedDarts? {
        val obj = inventory[slot] ?: return null
        val variant = variant(obj) ?: return null
        val count = darts(obj)
        val dart = storedDart(obj)
        val packed = obj.vars.withBits(dartType.bits, 0).withBits(dartCount.bits, 0)
        val keepLoaded = variant.family.usesScales && scales(obj) > 0
        val alias = if (keepLoaded) variant.loadedAlias else variant.emptyAlias
        inventory[slot] = InvObj(item(alias), count = obj.count, vars = packed)
        return UnloadedDarts(dart = dart, count = count)
    }

    /**
     * Clears the packed charges on the pipe at [slot] and returns the exact contents for the caller
     * to restore to the inventory after checking capacity.
     */
    public fun uncharge(inventory: Inventory, slot: Int): UnloadedContents? {
        val obj = inventory[slot] ?: return null
        val variant = variant(obj) ?: return null
        val dartCount = darts(obj)
        val dart = storedDart(obj)
        val scaleCount = scales(obj)
        val packed =
            obj.vars
                .withBits(dartType.bits, 0)
                .withBits(this.dartCount.bits, 0)
                .withBits(this.scaleCount.bits, 0)
        inventory[slot] = InvObj(item(variant.emptyAlias), count = obj.count, vars = packed)
        return UnloadedContents(dart = dart, dartCount = dartCount, scaleCount = scaleCount)
    }

    /** Returns the stored dart count, or zero when [obj] is not a tracked blowpipe. */
    public fun darts(obj: InvObj?): Int {
        val blowpipe = obj ?: return 0
        if (variant(blowpipe) == null) {
            return 0
        }
        return blowpipe.vars.getBits(dartCount.bits)
    }

    /** Returns the stored toxic-scale count, or zero for non-toxic blowpipes. */
    public fun scales(obj: InvObj?): Int {
        val blowpipe = obj ?: return 0
        val variant = variant(blowpipe) ?: return 0
        if (!variant.family.usesScales) {
            return 0
        }
        return blowpipe.vars.getBits(scaleCount.bits)
    }

    /** Returns whether [obj] has at least [amount] stored darts. */
    public fun hasDarts(obj: InvObj?, amount: Int): Boolean {
        require(amount >= 0) { "amount must not be negative." }
        return variant(obj) != null && darts(obj) >= amount
    }

    /** Returns whether [obj] has at least [amount] stored toxic scales. */
    public fun hasScales(obj: InvObj?, amount: Int): Boolean {
        require(amount >= 0) { "amount must not be negative." }
        return scales(obj) >= amount
    }

    /**
     * Removes [darts] and [scales] from the equipped blowpipe in one packed-value update.
     *
     * A toxic blowpipe remains its loaded cache item while it still contains either darts or
     * scales. This keeps Check/Unload/Uncharge available when one supply reaches zero. It changes
     * to the empty cache item only when both supplies are gone. Null means the equipped item did
     * not have enough matching stored ammunition.
     */
    public fun consume(player: Player, darts: Int, scales: Int = 0): Consumption? {
        require(darts >= 0) { "darts must not be negative." }
        require(scales >= 0) { "scales must not be negative." }

        val obj = player.righthand ?: return null
        val variant = variant(obj) ?: return null
        if (!variant.loaded || (!variant.family.usesScales && scales != 0)) {
            return null
        }

        val currentDarts = this.darts(obj)
        val currentScales = this.scales(obj)
        if (currentDarts < darts || currentScales < scales) {
            return null
        }

        val consumption =
            resolveConsumption(
                currentDarts,
                currentScales,
                darts,
                scales,
                variant.family.usesScales,
            ) ?: return null
        if (darts == 0 && scales == 0) {
            return consumption
        }

        var packed = obj.vars.withBits(dartCount.bits, consumption.dartsLeft)
        if (variant.family.usesScales) {
            packed = packed.withBits(scaleCount.bits, consumption.scalesLeft)
        }

        player.righthand =
            if (consumption.becameEmpty) {
                InvObj(item(variant.emptyAlias), count = obj.count, vars = packed)
            } else {
                obj.copy(vars = packed)
            }
        return consumption
    }

    internal fun resolveConsumption(
        currentDarts: Int,
        currentScales: Int,
        darts: Int,
        scales: Int,
        usesScales: Boolean,
    ): Consumption? {
        if (darts < 0 || scales < 0 || currentDarts < darts || currentScales < scales) {
            return null
        }
        val dartsLeft = currentDarts - darts
        val scalesLeft = currentScales - scales
        val becameEmpty = dartsLeft == 0 && (!usesScales || scalesLeft == 0)
        val canFire = dartsLeft > 0 && (!usesScales || scalesLeft > 0)
        return Consumption(dartsLeft, scalesLeft, becameEmpty, canFire)
    }

    /** Returns the stored dart strength contribution for the equipment-bonus calculation. */
    public fun rangedStrengthBonus(obj: InvObj?): Int {
        val dart = loadedDart(obj) ?: return 0
        return if (canUseLoadedDart(obj)) {
            dart.type.param(params.ranged_strength)
        } else {
            0
        }
    }

    private fun variant(obj: InvObj?): BlowpipeVariant? = obj?.let { variantsById[it.id] }

    private fun item(alias: String): ItemServerType =
        ServerCacheManager.getItem(alias.asRSCM(RSCMType.OBJ))
            ?: error("Missing cache item: $alias")

    private fun varobj(alias: String) =
        ServerCacheManager.getVarObj(alias.asRSCM(RSCMType.VAROBJ))
            ?: error("Missing cache varobj: $alias")

    private enum class BlowpipeFamily(val maxDartIndex: Int, val usesScales: Boolean) {
        Toxic(maxDartIndex = DRAGON_DART_INDEX, usesScales = true),

        // Wiki: "It is able to shoot up to rune darts" - torka's original had this capped at
        // adamant, which was wrong (or predates a later expansion of the cap on the live wiki).
        Rosewood(maxDartIndex = RUNE_DART_INDEX, usesScales = false),
    }

    private data class BlowpipeVariant(
        val alias: String,
        val loadedAlias: String,
        val emptyAlias: String,
        val family: BlowpipeFamily,
        val loaded: Boolean,
    )

    private val variantsById: Map<Int, BlowpipeVariant> by lazy {
        variants.associateBy { it.alias.asRSCM(RSCMType.OBJ) }
    }

    private val dartTypes: List<ItemServerType> by lazy { dartAliases.map(::item) }

    private val dartType by lazy { varobj("varobj.snakeboss_blowpipe_darttype") }
    private val dartCount by lazy { varobj("varobj.snakeboss_blowpipe_dartcount") }
    private val scaleCount by lazy { varobj("varobj.snakeboss_blowpipe_flakes") }

    private val variants =
        listOf(
            BlowpipeVariant(
                alias = "obj.toxic_blowpipe_loaded",
                loadedAlias = "obj.toxic_blowpipe_loaded",
                emptyAlias = "obj.toxic_blowpipe",
                family = BlowpipeFamily.Toxic,
                loaded = true,
            ),
            BlowpipeVariant(
                alias = "obj.toxic_blowpipe_loaded_ornament",
                loadedAlias = "obj.toxic_blowpipe_loaded_ornament",
                emptyAlias = "obj.toxic_blowpipe_ornament",
                family = BlowpipeFamily.Toxic,
                loaded = true,
            ),
            BlowpipeVariant(
                alias = "obj.rosewood_blowpipe",
                loadedAlias = "obj.rosewood_blowpipe",
                emptyAlias = "obj.rosewood_blowpipe_empty",
                family = BlowpipeFamily.Rosewood,
                loaded = true,
            ),
            BlowpipeVariant(
                alias = "obj.toxic_blowpipe",
                loadedAlias = "obj.toxic_blowpipe_loaded",
                emptyAlias = "obj.toxic_blowpipe",
                family = BlowpipeFamily.Toxic,
                loaded = false,
            ),
            BlowpipeVariant(
                alias = "obj.toxic_blowpipe_ornament",
                loadedAlias = "obj.toxic_blowpipe_loaded_ornament",
                emptyAlias = "obj.toxic_blowpipe_ornament",
                family = BlowpipeFamily.Toxic,
                loaded = false,
            ),
            BlowpipeVariant(
                alias = "obj.rosewood_blowpipe_empty",
                loadedAlias = "obj.rosewood_blowpipe",
                emptyAlias = "obj.rosewood_blowpipe_empty",
                family = BlowpipeFamily.Rosewood,
                loaded = false,
            ),
        )

    private val dartAliases =
        listOf(
            "obj.bronze_dart",
            "obj.iron_dart",
            "obj.steel_dart",
            "obj.black_dart",
            "obj.mithril_dart",
            "obj.adamant_dart",
            "obj.rune_dart",
            "obj.amethyst_dart",
            "obj.dragon_dart",
        )

    private const val MAX_STORED_AMOUNT: Int = 16_383

    private const val RUNE_DART_INDEX: Int = 6
    private const val DRAGON_DART_INDEX: Int = 8
}
