package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.table.ImplingLootRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

data class ImplingDrop(
    val obj: String?,
    val min: Int,
    val max: Int,
    val noted: Boolean,
    val num: Int,
    val den: Int,
    val clueTier: String? = null,
) {
    val isNothing: Boolean get() = obj == null

    companion object {
        fun nothing(num: Int, den: Int): ImplingDrop = ImplingDrop(null, 0, 0, false, num, den)
    }
}

data class ImplingLootTable(
    val preRoll: List<ImplingDrop>,
    val main: List<ImplingDrop>,
    val tertiary: List<ImplingDrop>,
) {
    companion object {
        const val PRE_ROLL = 0
        const val MAIN = 1
        const val TERTIARY = 2
    }
}

class ImplingLootScript @Inject constructor() : PluginScript() {
    private val tables: Map<String, ImplingLootTable> by lazy {
        ImplingLootRow.all().associate { it.jar.internalName to table(it) }
    }

    override fun ScriptContext.startup() {
        for (jar in tables.keys) {
            val type = ServerCacheManager.getItem(jar.asRSCM(RSCMType.OBJ))
            when (type?.interfaceOptions?.indexOfFirst { it.equals(LOOT, ignoreCase = true) }) {
                0 -> onOpHeld1(jar) { loot(it.slot, jar) }
                1 -> onOpHeld2(jar) { loot(it.slot, jar) }
                3 -> onOpHeld4(jar) { loot(it.slot, jar) }
                else -> onOpHeld3(jar) { loot(it.slot, jar) }
            }
        }
    }

    suspend fun ProtectedAccess.loot(slot: Int, jar: String) {
        val table = tables.getValue(jar)
        if (inv.freeSpace() < 1) {
            mes("You need some free inventory space to loot the jar.")
            return
        }
        val mm2 = QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)
        val skipped = mutableListOf<String>()
        val drops =
            roll(table, mm2Complete = mm2, skipTertiary = { skip(it, skipped) }) { bound -> random.of(bound) }
        if (invDel(inv, jar, slot = slot).failure) return
        if (random.of(JAR_BREAK_DEN) >= JAR_BREAK_NUM) {
            invAdd(inv, ImplingScript.IMPLING_JAR, slot = slot)
        }
        for (drop in drops) {
            val count = if (drop.max > drop.min) random.of(drop.min, drop.max) else drop.min
            if (invAdd(inv, checkNotNull(drop.obj), count, cert = drop.noted).failure) {
                break
            }
        }
        for (tier in skipped) {
            mes("You have a sneaking suspicion that you would have received ${article(tier)} $tier clue scroll.")
        }
    }

    private fun ProtectedAccess.skip(drop: ImplingDrop, skipped: MutableList<String>): Boolean {
        val tier = drop.clueTier ?: return false
        return holdsClue(tier) && skipped.add(tier)
    }

    fun ProtectedAccess.holdsClue(tier: String): Boolean {
        val names = setOf("clue scroll ($tier)", "challenge scroll ($tier)")
        return listOf(inv, inv(BANK)).any { held ->
            held.objs.any { obj ->
                obj != null && ServerCacheManager.getItem(obj.id)?.name?.lowercase() in names
            }
        }
    }

    companion object {
        const val LOOT = "Loot"
        const val JAR_BREAK_NUM = 1
        const val JAR_BREAK_DEN = 10
        const val BANK = "inv.bank"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
        const val DRAGON_JAVELIN_TIPS = "obj.dragon_javelin_head"

        private val CLUE_NAME = Regex("clue scroll \\((\\w+)\\)")

        fun table(row: ImplingLootRow): ImplingLootTable {
            val drops =
                row.loot.groupBy({ it.t0 }) { (_, obj, min, max, noted, num, den) ->
                    val tier = CLUE_NAME.matchEntire(obj.name.lowercase())?.groupValues?.get(1)
                    ImplingDrop(obj.internalName, min, max, noted, num, den, tier)
                }
            val nothing = listOfNotNull(row.nothingRate?.let { ImplingDrop.nothing(1, it) })
            return ImplingLootTable(
                preRoll = drops[ImplingLootTable.PRE_ROLL].orEmpty(),
                main = drops[ImplingLootTable.MAIN].orEmpty() + nothing,
                tertiary = drops[ImplingLootTable.TERTIARY].orEmpty(),
            )
        }

        fun article(word: String): String = if (word.first() in "aeiou") "an" else "a"

        fun roll(
            table: ImplingLootTable,
            mm2Complete: Boolean = true,
            skipTertiary: (ImplingDrop) -> Boolean = { false },
            nextInt: (Int) -> Int,
        ): List<ImplingDrop> {
            val drops = mutableListOf<ImplingDrop>()
            val pre = table.preRoll.firstOrNull { hits(it, nextInt) }
            if (pre != null) {
                drops += pre
            } else {
                val main =
                    if (mm2Complete) table.main else table.main.filter { it.obj != DRAGON_JAVELIN_TIPS }
                pick(main, nextInt)?.let { drops += it }
            }
            drops += table.tertiary.filter { hits(it, nextInt) && !skipTertiary(it) }
            return drops.filterNot(ImplingDrop::isNothing)
        }

        private fun hits(drop: ImplingDrop, nextInt: (Int) -> Int): Boolean =
            nextInt(drop.den) < drop.num

        private fun pick(drops: List<ImplingDrop>, nextInt: (Int) -> Int): ImplingDrop? {
            if (drops.isEmpty()) return null
            val den = drops.fold(1L) { acc, d -> lcm(acc, d.den.toLong()) }
            val weights = drops.map { it.num * (den / it.den) }
            var r = nextInt(weights.sum().toInt()).toLong()
            for ((i, w) in weights.withIndex()) {
                if (r < w) return drops[i]
                r -= w
            }
            return null
        }

        private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

        private fun lcm(a: Long, b: Long): Long = a / gcd(a, b) * b
    }
}
