package dtx.util

internal fun <T, R: Comparable<R>> List<T>.isSortedBy(bySelector: (T) -> R): Boolean {

    for (i in 1 until size) {

        val prev = bySelector(get(i - 1))
        val cur = bySelector(get(i))

        if (prev > cur) {
            return false
        }
    }

    return true
}
