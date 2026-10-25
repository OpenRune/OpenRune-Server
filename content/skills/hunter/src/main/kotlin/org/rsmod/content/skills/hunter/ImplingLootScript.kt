package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.table.ImplingTypesRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

data class ImplingDrop(
    val obj: Int,
    val min: Int,
    val max: Int,
    val noted: Boolean,
    val num: Int,
    val den: Int,
) {
    val isNothing: Boolean get() = obj == -1

    companion object {
        fun nothing(num: Int, den: Int): ImplingDrop = ImplingDrop(-1, 0, 0, false, num, den)
    }
}

data class ImplingLootTable(
    val preRoll: List<ImplingDrop>,
    val main: List<ImplingDrop>,
    val tertiary: List<ImplingDrop>,
)

class ImplingLootScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        val jars = ImplingTypesRow.all().map { it.jar.internalName } + CRYSTAL_JAR
        for (jar in jars.distinct()) {
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
        val table = ImplingLootTables.tables[jar.asRSCM(RSCMType.OBJ)]
        if (table == null) {
            mes("Nothing interesting happens.")
            return
        }
        if (inv.freeSpace() < 1) {
            mes("You need some free inventory space to loot the jar.")
            return
        }
        val mm2 = QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)
        val held = CLUE_TIERS.filterValues { holdsClue(it) }.keys
        val skipped = mutableListOf<Int>()
        val drops =
            roll(table, mm2Complete = mm2, skipTertiary = { it in held && skipped.add(it) }) { bound ->
                random.of(bound)
            }
        if (invDel(inv, jar, slot = slot).failure) return
        if (random.of(JAR_BREAK_DEN) >= JAR_BREAK_NUM) {
            invAdd(inv, ImplingScript.IMPLING_JAR, slot = slot)
        }
        for (drop in drops) {
            val count = if (drop.max > drop.min) random.of(drop.min, drop.max) else drop.min
            if (invAdd(inv, RSCM.getReverseMapping(RSCMType.OBJ, drop.obj), count, cert = drop.noted).failure) {
                break
            }
        }
        for (clue in skipped) {
            val tier = CLUE_TIERS.getValue(clue)
            mes("You have a sneaking suspicion that you would have received ${article(tier)} $tier clue scroll.")
        }
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
        const val CRYSTAL_JAR = "obj.ii_captured_impling_12"
        const val LOOT = "Loot"
        const val JAR_BREAK_NUM = 1
        const val JAR_BREAK_DEN = 10
        const val BANK = "inv.bank"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
        const val DRAGON_JAVELIN_TIPS = 19582

        val CLUE_TIERS =
            mapOf(23182 to "beginner", 2677 to "easy", 2801 to "medium", 2722 to "hard", 12073 to "elite")

        fun article(word: String): String = if (word.first() in "aeiou") "an" else "a"

        fun roll(
            table: ImplingLootTable,
            mm2Complete: Boolean = true,
            skipTertiary: (Int) -> Boolean = { false },
            nextInt: (Int) -> Int,
        ): List<ImplingDrop> {
            val drops = mutableListOf<ImplingDrop>()
            val pre = table.preRoll.firstOrNull { hits(it, nextInt) }
            if (pre != null) {
                drops += pre
            } else {
                val main = if (mm2Complete) table.main else table.main.filter { it.obj != DRAGON_JAVELIN_TIPS }
                pick(main, nextInt)?.let { drops += it }
            }
            drops += table.tertiary.filter { hits(it, nextInt) && !skipTertiary(it.obj) }
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
