package dtx.rs

import dtx.table.TableHooks

public fun npcs(vararg ids: String): List<String> = ids.toList()

public fun locs(vararg ids: String): List<String> = ids.toList()

public fun areas(vararg ids: String): List<String> = ids.toList()

public class RSDropTableBuilder<T, R> {
    public var tableIdentifier: String = "Unnamed Drop Table"
    public var npcs: List<String> = emptyList()
    public var locs: List<String> = emptyList()
    public var areas: List<String> = emptyList()
    public var guaranteed: RSTable<T, R> = RSGuaranteedTable.Empty()
    public var preRoll: RSTable<T, R> = RSPreRollTable.Empty()
    public var separateRolls: RSTable<T, R> = RSPreRollTable.Empty()
    public var mainTable: RSTable<T, R> = RSWeightedTable.Empty()
    public var tertiaries: RSTable<T, R> = RSPreRollTable.Empty()
    public var hooks: TableHooks<T, R> = TableHooks.Default()

    public fun npcs(vararg ids: String) {
        npcs = ids.toList()
    }

    public fun locs(vararg ids: String) {
        locs = ids.toList()
    }

    public fun areas(vararg ids: String) {
        areas = ids.toList()
    }

    public fun build(): RSDropTable<T, R> =
        RSDropTable(
            tableIdentifier = tableIdentifier,
            npcs = npcs,
            locs = locs,
            areas = areas,
            guaranteed = guaranteed,
            preRoll = preRoll,
            separateRolls = separateRolls,
            mainTable = mainTable,
            tertiaries = tertiaries,
            hooks = hooks,
        )
}

public fun <T, R> rsDropTable(block: RSDropTableBuilder<T, R>.() -> Unit): RSDropTable<T, R> {
    return RSDropTableBuilder<T, R>().apply(block).build()
}
