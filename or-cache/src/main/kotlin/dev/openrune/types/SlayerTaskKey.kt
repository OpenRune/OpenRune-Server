package dev.openrune.types

object SlayerTaskKey {
    const val BOSS_TASK_ID = 98
    const val BOSS_KEY_BASE = 1000

    fun of(taskId: Int, bossId: Int = 0): Int =
        if (taskId == BOSS_TASK_ID && bossId > 0) BOSS_KEY_BASE + bossId else taskId

    fun isBoss(key: Int): Boolean = key > BOSS_KEY_BASE

    fun encode(keys: List<Int>): String = keys.joinToString(",")

    fun contains(primary: Int?, extras: String?, key: Int): Boolean {
        if (primary == key) return true
        if (extras.isNullOrEmpty()) return false
        return extras.splitToSequence(',').any { it.toIntOrNull() == key }
    }
}
