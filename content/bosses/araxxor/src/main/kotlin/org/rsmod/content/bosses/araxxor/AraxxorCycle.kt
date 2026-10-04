package org.rsmod.content.bosses.araxxor

internal enum class AraxyteKind(val egg: String, val spider: String) {
    ACIDIC("npc.araxxor_minion_egg_venom", "npc.araxxor_minion_venom"),
    MIRRORBACK("npc.araxxor_minion_egg_mirrorback", "npc.araxxor_minion_mirrorback"),
    RUPTURA("npc.araxxor_minion_egg_explode", "npc.araxxor_minion_explode"),
}

internal enum class AraxxorPhase { NORMAL, ENRAGED, CORPSE, FINISHED }
internal enum class AraxxorSpecial { ACID_BALL, ACID_SPRAY, ACID_TRAIL }

internal class AraxxorCycle(start: AraxyteKind) {
    val eggs = List(9) { AraxyteKind.entries[(start.ordinal + it) % 3] }
    val special = when (start) {
        AraxyteKind.ACIDIC -> AraxxorSpecial.ACID_BALL
        AraxyteKind.MIRRORBACK -> AraxxorSpecial.ACID_SPRAY
        AraxyteKind.RUPTURA -> AraxxorSpecial.ACID_TRAIL
    }
    var phase = AraxxorPhase.NORMAL
        private set
    var epoch = 0
        private set
    private var standardAttacks = 0
    private var nextEgg = 0
    private val eggHealth = IntArray(9) { EGG_HP }
    private var claimed = false
    val attackTicks get() = if (phase == AraxxorPhase.ENRAGED) 4 else 6

    data class Step(val hatch: Hatch? = null, val special: AraxxorSpecial? = null)
    data class Hatch(val index: Int, val kind: AraxyteKind, val hitpoints: Int)

    fun standardAttack(): Step {
        if (phase != AraxxorPhase.NORMAL) return Step()
        standardAttacks++
        val hatch = if (standardAttacks >= 3 && (standardAttacks - 3) % 6 == 0) hatchNext() else null
        return Step(hatch, if (standardAttacks % 6 == 0) special else null)
    }

    fun damageEgg(index: Int, damage: Int): Int {
        require(index in eggs.indices && damage >= 0)
        if (phase != AraxxorPhase.NORMAL || index < nextEgg) return 0
        val applied = minOf(damage, eggHealth[index])
        eggHealth[index] -= applied
        return applied
    }

    private fun hatchNext(): Hatch? {
        while (nextEgg < eggs.size && eggHealth[nextEgg] == 0) nextEgg++
        if (nextEgg == eggs.size) return null
        val index = nextEgg++
        val remaining = (SPIDER_HP - (EGG_HP - eggHealth[index])).coerceAtLeast(0)
        return Hatch(index, eggs[index], remaining)
    }

    fun updateHealth(hitpoints: Int): Boolean {
        require(hitpoints >= 0)
        if (phase != AraxxorPhase.NORMAL || hitpoints == 0 || hitpoints > ENRAGE_HP) return false
        phase = AraxxorPhase.ENRAGED
        epoch++
        return true
    }

    fun die(): Boolean {
        if (phase != AraxxorPhase.NORMAL && phase != AraxxorPhase.ENRAGED) return false
        phase = AraxxorPhase.CORPSE
        epoch++
        return true
    }

    fun claim(): Boolean {
        if (phase != AraxxorPhase.CORPSE || claimed) return false
        claimed = true
        phase = AraxxorPhase.FINISHED
        epoch++
        return true
    }

    fun dispose() {
        phase = AraxxorPhase.FINISHED
        epoch++
    }

    fun acceptsCallback(scheduledEpoch: Int): Boolean =
        epoch == scheduledEpoch && phase in setOf(AraxxorPhase.NORMAL, AraxxorPhase.ENRAGED)

    companion object {
        const val MAX_HP = 1020
        const val ENRAGE_HP = MAX_HP / 4
        const val EGG_HP = 65
        const val SPIDER_HP = 58
    }
}
