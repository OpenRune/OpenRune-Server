package dtx.example

import dtx.table.uniformTable

/**
 * 1d12, has an equal chance to pick any of the entries in this table.
 */
val uniformTableExample = uniformTable<Player, Int> {
    (1..12).forEach { side ->
        add(side)
    }
}
