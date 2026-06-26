package org.rsmod.api.bossbar

public enum class BossHpBarMode(public val id: Int) {
    NEVER(0),
    ON_ATTACK(1),
    ON_ENTER(2);

    public companion object {
        public fun fromId(id: Int): BossHpBarMode = entries.firstOrNull { it.id == id } ?: NEVER
    }
}
