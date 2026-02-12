package dev.openrune.types

import dev.openrune.definition.Definition
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

data class InventoryServerType(
    override var id: Int = -1,
    var stack: InvStackType = InvStackType.Normal,
    var size: Int = 1,
    var scope : InvScope = InvScope.Temp,
    var flags: Int = pack(
        protect = true,
        allStock = false,
        restock = false,
        runWeight = false,
        dummyInv = false,
        placeholders = false,
    ),
    val stock: Array<InvStock> = emptyArray(),
) : Definition {

    public val restock: Boolean
        get() = flags and RESTOCK_FLAG != 0

    public val allStock: Boolean
        get() = flags and ALL_STOCK_FLAG != 0

    public val protect: Boolean
        get() = flags and PROTECT_FLAG != 0

    public val runWeight: Boolean
        get() = flags and RUN_WEIGHT_FLAG != 0

    public val dummyInv: Boolean
        get() = flags and DUMMY_INV_FLAG != 0

    public val placeholders: Boolean
        get() = flags and PLACEHOLDERS_FLAG != 0


    public companion object {
        public const val PROTECT_FLAG: Int = 0x1
        public const val ALL_STOCK_FLAG: Int = 0x2
        public const val RESTOCK_FLAG: Int = 0x4
        public const val RUN_WEIGHT_FLAG: Int = 0x8
        public const val DUMMY_INV_FLAG: Int = 0x10
        public const val PLACEHOLDERS_FLAG: Int = 0x20

        fun pack(
            protect: Boolean,
            allStock: Boolean,
            restock: Boolean,
            runWeight: Boolean,
            dummyInv: Boolean,
            placeholders: Boolean,
        ): Int {
            var flags = 0
            if (protect) {
                flags = flags or PROTECT_FLAG
            }
            if (allStock) {
                flags = flags or ALL_STOCK_FLAG
            }
            if (restock) {
                flags = flags or RESTOCK_FLAG
            }
            if (runWeight) {
                flags = flags or RUN_WEIGHT_FLAG
            }
            if (dummyInv) {
                flags = flags or DUMMY_INV_FLAG
            }
            if (placeholders) {
                flags = flags or PLACEHOLDERS_FLAG
            }
            return flags
        }
    }

}

data class InvStock constructor(public val obj: Int, public val count: Int, public val restockCycles: Int)

public enum class InvStackType(public val id: Int) {
    Normal(0),
    Always(1),
    Never(2),
}

public enum class InvScope(public val id: Int) {
    /** Temporary inventories that do not save. (e.g., clue scrolls) */
    Temp(0),
    /** Persistent inventories that will save. (e.g., bank) */
    Perm(1),
    /** Global Inventories that are shared. (e.g., shops) */
    Shared(2),
}
