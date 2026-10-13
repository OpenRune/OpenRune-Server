package org.rsmod.content.other.nerocurator

internal object CuratorStyle {
    const val VERSION = 2

    fun initialise(original: IntArray, get: (Int) -> Int, set: (Int, Int) -> Unit) {
        when (get(6)) {
            0 -> {
                for (c in 0..5) set(c, original[c])
                set(6, VERSION)
            }
            1 -> {
                // Preserve the old physical size, including saved presets, when changing units.
                set(5, (get(5) / 3.0).toInt().coerceIn(25, 300))
                for (n in 0..2) if (get(13 + n * 7) == 1) {
                    val size = 12 + n * 7
                    set(size, (get(size) / 3.0).toInt().coerceIn(25, 300))
                }
                set(6, VERSION)
            }
        }
    }
}
