package dev.openrune.tables.skills

enum class QuestReq(val id: Int) {
    Completed(0),
    InProgress(1),
    NotCompleted(2);

    companion object {
        fun of(id: Int?): QuestReq? = entries.firstOrNull { it.id == id }
    }
}

enum class VarbitCompare(val id: Int) {
    EQ(0),
    NE(1),
    LT(2),
    LTE(3),
    GT(4),
    GTE(5);

    fun passes(actual: Int, expected: Int): Boolean =
        when (this) {
            EQ -> actual == expected
            NE -> actual != expected
            LT -> actual < expected
            LTE -> actual <= expected
            GT -> actual > expected
            GTE -> actual >= expected
        }

    companion object {
        fun of(id: Int?): VarbitCompare? = entries.firstOrNull { it.id == id }
    }
}
