package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.table.ImplingLootRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.hunter.HunterClues.clueReward
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

data class ImplingDrop(
    internal val obj: String?,
    internal val min: Int,
    internal val max: Int,
    internal val noted: Boolean,
    internal val num: Int,
    internal val den: Int,
    internal val clueTier: String? = null,
) {
    internal val isNothing: Boolean get() = obj == null

    internal companion object {
        fun nothing(num: Int, den: Int): ImplingDrop = ImplingDrop(null, 0, 0, false, num, den)
    }
}

data class ImplingLootTable(
    internal val preRoll: List<ImplingDrop>,
    internal val main: List<ImplingDrop>,
    internal val tertiary: List<ImplingDrop>,
) {
    internal companion object {
        const val PRE_ROLL = 0
        const val MAIN = 1
        const val TERTIARY = 2
    }
}

@Singleton
class ImplingLootScript @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
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

    internal suspend fun ProtectedAccess.loot(slot: Int, jar: String) {
        if (inv.freeSpace() < 1) {
            mes("You'll need to clear some space in your pack before looting the jar.")
            return
        }
        val drops = rollJar(jar)
        if (invDel(inv, jar, slot = slot).failure) return
        if (random.of(JAR_BREAK_DEN) >= JAR_BREAK_NUM) {
            invAdd(inv, ImplingScript.IMPLING_JAR, slot = slot)
        }
        give(drops)
    }

    internal fun ProtectedAccess.rollJar(jar: String): List<ImplingDrop> {
        val table = tables[jar] ?: return emptyList()
        val mm2 = QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)
        return roll(table, mm2Complete = mm2) { bound -> random.of(bound) }
    }

    private var lootTrackerEventId: Int = 0

    internal fun ProtectedAccess.give(drops: List<ImplingDrop>, from: Npc? = null) {
        val eventId = from?.let { nextLootTrackerEventId() }
        for (drop in drops) {
            val obj = checkNotNull(drop.obj)
            val given = drop.clueTier?.let { clueReward(it, obj) ?: continue } ?: obj
            val count = if (drop.max > drop.min) random.of(drop.min, drop.max) else drop.min
            val type = if (drop.noted) ocCert(given) else checkNotNull(ServerCacheManager.getItem(given.asRSCM(RSCMType.OBJ)))
            invAddOrDropType(objRepo, type, count)
            if (from != null && eventId != null) {
                ClientScripts.lootTrackerAddLoot(player, from.visType.id, eventId, type.id, count)
            }
        }
    }

    private fun nextLootTrackerEventId(): Int {
        lootTrackerEventId = if (lootTrackerEventId == Int.MAX_VALUE) 1 else lootTrackerEventId + 1
        return lootTrackerEventId
    }

    internal companion object {
        const val LOOT = "Loot"
        const val JAR_BREAK_NUM = 1
        const val JAR_BREAK_DEN = 10
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

        fun roll(
            table: ImplingLootTable,
            mm2Complete: Boolean = true,
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
            drops += table.tertiary.filter { hits(it, nextInt) }
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
