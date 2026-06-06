package dtx.rs

public typealias RSSeparateRollsTable<T, R> = RSPreRollTable<T, R>

public fun <T, R> rsSeparateRollsTable(block: RSPrerollTableBuilder<T, R>.() -> Unit): RSSeparateRollsTable<T, R> {
    return rsPrerollTable(block)
}
