package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.util.Wearpos
import dtx.core.*
import dtx.rs.*
import dtx.table.TableHooks
import kotlin.math.sqrt
import org.rsmod.api.death.hasSkullDeathPenalty
import org.rsmod.api.death.isHighRiskSkulled
import org.rsmod.api.droptable.*
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

/** Dynamic Wiki table with a definition-only unskulled/off-task preview. */
internal class RevenantLootTable(private val combat: Int, private val random: GameRandom = DefaultGameRandom()) : RSTable<Player, DropRollItem>, TableHooks<Player, DropRollItem> by TableHooks.Default() {
    override val tableIdentifier = "Revenant loot ($combat)"
    private val root = sqrt(combat.toDouble()).toInt()
    internal val a = 2200 / root
    internal val b = minOf(a, 15 + (combat + 60) * (combat + 60) / 200)
    override val tableEntries: Collection<Rollable<Player, DropRollItem>> by lazy { preview().tableEntries }
    override fun selectResult(target: Player, otherArgs: ArgMap): RollResult<DropRollItem> {
        val skulled = target.hasSkullDeathPenalty() && !target.isHighRiskSkulled()
        val npc = otherArgs[KillRollContext.npc]
        val onTask = npc != null && target.vars["varp.slayer_count"] > 0 &&
            target.vars["varp.slayer_target"] == npc.type.param(org.rsmod.api.config.refs.params.slayer_task_id) && SlayerTaskManager.isOnWildernessSlayerTask(target)
        val result = mutableListOf(DropRollItem(RevenantCharges.ETHER, random.of(1, root + 1) * 2))
        // The unique roll is independent of the primary common/artefact/coins roll.
        if (random.of(a * if (skulled) 44 else 80) < if (onTask) 15 else 3) result += item(pick(uniques), target, note = false)
        val primary = random.of(a)
        result += when {
            primary == 0 -> item(pick(if (skulled) rareSkulled else rare), target)
            primary < b -> item(pick(common), target)
            else -> DropRollItem("obj.coins", random.of(1, 1 + 25 * root))
        }
        if (random.of(250 - combat) < 100) result += item(pick(supplies), target, note = false)
        if (listOf("obj.looting_bag", "obj.looting_bag_open").none { ownsBag(target, it) } &&
            random.of(if (combat < 10) 15 else if (combat > 50) 3 else 150 / combat) == 0) result += DropRollItem(lookup("Looting bag"), 1)
        val champion = when (combat) { 7 -> "Imp champion scroll"; 15 -> "Goblin champion scroll"; 60 -> "Hobgoblin champion scroll"; else -> null }
        if (champion != null && random.of(5000) == 0) result += DropRollItem(lookup(champion), 1)
        return RollResult.ListOf(result)
    }
    private fun pick(entries: List<Entry>): Entry {
        var roll = random.of(entries.sumOf { it.weight })
        return entries.first { roll -= it.weight; roll < 0 }
    }
    private fun ownsBag(player: Player, name: String): Boolean =
        name in player.inv || name in player.worn || player.invMap["inv.bank"]?.let { name in it } == true
    private fun item(entry: Entry, player: Player, note: Boolean = true): DropRollItem {
        if (entry.name.isEmpty()) return nothingDrop { true }
        val base = lookup(entry.name)
        val quantity = random.of(entry.count.first, entry.count.last)
        val avarice = player.worn[Wearpos.Front.slot]?.isType("obj.wild_cave_amulet") == true
        val noted = if (entry.noted || note && avarice) certificate(base) ?: base else base
        return DropRollItem(noted, quantity)
    }
    private fun preview(): RSDropTable<Player, DropRollItem> {
        fun weighted(entries: List<Entry>) = rsPlayerWeightedTable(total = entries.sumOf { it.weight }) {
            for (entry in entries) entry.weight weight if (entry.name.isEmpty()) nothingDrop { true } else DropRollItem(if (entry.noted) certificate(lookup(entry.name)) ?: lookup(entry.name) else lookup(entry.name), entry.count)
        }
        return RSDropTable(tableIdentifier = tableIdentifier,
            guaranteed = rsPlayerGuaranteedTable { RevenantCharges.ETHER count 2..(2 * (root + 1)) },
            preRoll = rsPlayerPrerollTable { 3 outOf (80 * a) rolls weighted(uniques) },
            mainTable = rsPlayerWeightedTable(total = a) {
                1 weight weighted(rare)
                (b - 1) weight weighted(common)
                if (a > b) (a - b) weight DropRollItem("obj.coins", 1..(1 + 25 * root))
            },
            tertiaries = rsPlayerTertiaryTable { 100 outOf (250 - combat) rolls weighted(supplies) })
    }
    internal companion object {
        data class Entry(val name: String, val weight: Int, val count: IntRange = 1..1, val noted: Boolean = false)
        private val names = mutableMapOf<String, String>()
        private val certificates = mutableMapOf<String, String?>()
        fun lookup(name: String): String = names.getOrPut(name) { checkNotNull(ServerCacheManager.getItemTypes().filter {
            it.name.equals(name, ignoreCase = true) && !it.internalName.contains("placeholder") && !it.internalName.startsWith("obj.cert_") && !it.internalName.contains("_dummy")
        }.minByOrNull { it.id }) { "Native item missing: $name" }.internalName }
        fun certificate(base: String): String? = certificates.getOrPut(base) { ServerCacheManager.getItemTypes().firstOrNull { it.internalName == "obj.cert_${base.removePrefix("obj.")}" }?.internalName }
        val uniques = listOf(Entry("Amulet of avarice", 2), Entry("Craw's bow (u)", 1), Entry("Thammaron's sceptre (u)", 1), Entry("Viggora's chainmace (u)", 1))

        // Published item chances occupy 39/40 (21/22 skulled). Reserve the unexplained slot
        // instead of silently increasing every published chance by renormalising the weights.
        val rare = listOf(Entry("Ancient relic", 1), Entry("Ancient effigy", 1), Entry("Ancient medallion", 1), Entry("Ancient statuette", 2), Entry("Magic seed", 4, 2..6), Entry("Yew seed", 4, 2..6), Entry("Ancient crystal", 3), Entry("Ancient totem", 4), Entry("Ancient emblem", 6), Entry("Dragon med helm", 13), Entry("", 1))
        val rareSkulled = rare.filter { it.name != "Dragon med helm" }.map { if (it.name == "Ancient emblem") it.copy(weight = 1) else it }
        val common = listOf(
            Entry("Bracelet of ethereum (uncharged)", 30), Entry("Battlestaff", 10, 4..4),
            Entry("Rune full helm", 4, 2..2), Entry("Rune platebody", 4, 2..2), Entry("Rune platelegs", 4, 2..2), Entry("Rune kiteshield", 4, 2..2), Entry("Rune warhammer", 4, 2..2),
            Entry("Dragon platelegs", 1, 1..2), Entry("Dragon plateskirt", 1, 1..2), Entry("Dragon dagger", 2, 2..2), Entry("Dragon longsword", 2, 2..2),
            Entry("Dragonstone bolt tips", 6, 20..40), Entry("Onyx bolt tips", 6, 3..6), Entry("Death rune", 10, 30..60), Entry("Blood rune", 10, 50..100), Entry("Law rune", 10, 20..45),
            Entry("Coal", 12, 30..60, true), Entry("Adamantite bar", 12, 4..6), Entry("Runite ore", 8, 2..4), Entry("Runite bar", 6, 2..3), Entry("Black dragonhide", 8, 4..4),
            Entry("Mahogany plank", 6, 8..16, true), Entry("Blighted manta ray", 8, 10..15), Entry("Yew logs", 8, 20..40, true), Entry("Magic logs", 4, 8..16, true), Entry("Uncut dragonstone", 2, 2..5),
            Entry("Revenant cave teleport", 10, 1..5), Entry("Blighted super restore(4)", 6, 1..3),
        )
        val supplies = listOf(Entry("Blighted ancient ice sack", 10, 1..10), Entry("Blighted entangle sack", 20, 1..10), Entry("Blighted teleport spell sack", 10, 1..10), Entry("Blighted vengeance sack", 10, 1..10), Entry("Blighted manta ray", 15, 1..2), Entry("Blighted anglerfish", 15, 1..2), Entry("Blighted karambwan", 10, 1..2), Entry("Blighted super restore(4)", 5), Entry("Revenant cave teleport", 5))
    }
}

object RevenantDropTables {
    private fun table(species: RevenantSpecies, level: Int) = RSDropTable<Player, DropRollItem>("${species.name} revenant", npcs = listOf(species.npcType), mainTable = RevenantLootTable(level), rollBeforeProgression = true)

    @field:RegisterDropTable @JvmField val imp = table(RevenantSpecies.Imp, 7)

    @field:RegisterDropTable @JvmField val goblin = table(RevenantSpecies.Goblin, 15)

    @field:RegisterDropTable @JvmField val pyrefiend = table(RevenantSpecies.Pyrefiend, 52)

    @field:RegisterDropTable @JvmField val hobgoblin = table(RevenantSpecies.Hobgoblin, 60)

    @field:RegisterDropTable @JvmField val cyclops = table(RevenantSpecies.Cyclops, 82)

    @field:RegisterDropTable @JvmField val hellhound = table(RevenantSpecies.Hellhound, 90)

    @field:RegisterDropTable @JvmField val demon = table(RevenantSpecies.Demon, 98)

    @field:RegisterDropTable @JvmField val ork = table(RevenantSpecies.Ork, 105)

    @field:RegisterDropTable @JvmField val darkBeast = table(RevenantSpecies.DarkBeast, 120)

    @field:RegisterDropTable @JvmField val knight = table(RevenantSpecies.Knight, 126)

    @field:RegisterDropTable @JvmField val dragon = table(RevenantSpecies.Dragon, 135)

    @field:RegisterDropTable @JvmField val maledictus = RSDropTable<Player, DropRollItem>("Revenant maledictus", npcs = listOf(RevenantSpecies.MALEDICTUS),
        guaranteed = rsPlayerGuaranteedTable {
            add(rsPlayerWeightedTable(total = 2) { 1 weight "obj.wild_cave_artifact_500" count 1; 1 weight "obj.wild_cave_artifact_1000" count 1 })
        }, mainTable = RevenantLootTable(135), mainRolls = 2, rollBeforeProgression = true)
}
