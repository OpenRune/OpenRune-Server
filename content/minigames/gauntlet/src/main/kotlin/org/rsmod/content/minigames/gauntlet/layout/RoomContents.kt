package org.rsmod.content.minigames.gauntlet.layout

import kotlin.random.Random
import org.rsmod.content.minigames.gauntlet.GauntletMode

enum class MonsterKind(val locName: String) {
    RAT("rat"),
    SPIDER("spider"),
    BAT("bat"),
    UNICORN("unicorn"),
    SCORPION("scorpion"),
    WOLF("wolf"),
    BEAR("bear"),
    DRAGON("dragon"),
    DARK_BEAST("dark_beast");

    companion object {
        val WEAK = listOf(RAT, SPIDER, BAT)
        val STRONG = listOf(UNICORN, SCORPION, WOLF)
        val DEMI = listOf(BEAR, DRAGON, DARK_BEAST)
    }
}

data class ResourceSpawn(val kind: ResourceKind, val tile: Tile)

data class MonsterSpawn(val kind: MonsterKind, val tile: Tile)

class RoomContents(val resources: List<ResourceSpawn>, val monsters: List<MonsterSpawn>)

object RoomContentsGenerator {
    private const val DEMI_CHANCE = 0.10
    private const val OBJECT_CHANCE = 0.75
    private const val NPC_ALONGSIDE_OBJECTS_CHANCE = 0.25
    private const val GRYM_CHANCE = 0.20
    private const val WEAK_CHANCE_INNER = 0.75
    private const val GUARANTEED_NODES = 3
    private const val DEMI_TILE = 7
    private val DEPLETING =
        listOf(
            ResourceKind.DEPOSIT,
            ResourceKind.PHREN,
            ResourceKind.LINUM,
            ResourceKind.FISHING,
        )
    private val DEMI_CELLS =
        listOf(
            GauntletLayout.index(3, 0),
            GauntletLayout.index(6, 3),
            GauntletLayout.index(3, 6),
            GauntletLayout.index(0, 3),
        )

    fun generate(
        layout: GauntletLayout,
        mode: GauntletMode,
        slots: RoomSlots,
        random: Random,
    ): Map<Int, RoomContents> {
        val builders =
            layout
                .all()
                .filter { it.kind != RoomKind.START && it.kind != RoomKind.BOSS }
                .associate { it.index to Builder(slots[it.kind, it.variant], random) }
        val demi = demiRooms(layout, builders.keys, random)
        val guaranteed = innerGuarantees(layout, mode, builders, random)
        for ((index, builder) in builders) {
            val room = layout.all()[index]
            val demiKind = demi[index]
            if (demiKind != null) {
                builder.monsters += MonsterSpawn(demiKind, Tile(DEMI_TILE, DEMI_TILE))
                continue
            }
            fill(room, builder, guaranteed[index], random)
        }
        return builders.mapValues { it.value.build() }
    }

    private fun demiRooms(
        layout: GauntletLayout,
        playable: Set<Int>,
        random: Random,
    ): Map<Int, MonsterKind> {
        val result = mutableMapOf<Int, MonsterKind>()
        for (room in layout.all()) {
            if (room.index !in playable) continue
            val guaranteed = room.index in DEMI_CELLS
            val eligible = room.kind == RoomKind.EDGE || room.kind == RoomKind.CORNER
            if (guaranteed || (eligible && random.nextDouble() < DEMI_CHANCE)) {
                result[room.index] = MonsterKind.DEMI.random(random)
            }
        }
        return result
    }

    private sealed interface Guarantee {
        data class Nodes(val kind: ResourceKind) : Guarantee

        data object WeakMonsters : Guarantee
    }

    private fun innerGuarantees(
        layout: GauntletLayout,
        mode: GauntletMode,
        builders: Map<Int, Builder>,
        random: Random,
    ): Map<Int, Guarantee> {
        val wanted: List<Guarantee> =
            if (mode.corrupted) {
                listOf(Guarantee.Nodes(ResourceKind.FISHING))
            } else {
                listOf(
                    Guarantee.Nodes(ResourceKind.DEPOSIT),
                    Guarantee.Nodes(ResourceKind.LINUM),
                    Guarantee.Nodes(ResourceKind.PHREN),
                    Guarantee.WeakMonsters,
                )
            }
        val candidates =
            layout
                .all()
                .filter { it.x in 2..4 && it.z in 2..4 && it.index in builders }
                .map { it.index }
                .shuffled(random)
                .toMutableList()
        val result = mutableMapOf<Int, Guarantee>()
        for (guarantee in wanted) {
            val pick =
                candidates.firstOrNull { index ->
                    guarantee !is Guarantee.Nodes ||
                        builders.getValue(index).slots.of(guarantee.kind).size >= GUARANTEED_NODES
                } ?: candidates.firstOrNull() ?: continue
            candidates -= pick
            result[pick] = guarantee
        }
        return result
    }

    private fun fill(room: GauntletRoom, builder: Builder, guarantee: Guarantee?, random: Random) {
        if (guarantee is Guarantee.Nodes) repeat(GUARANTEED_NODES) { builder.place(guarantee.kind) }
        val spawnObjects = guarantee is Guarantee.Nodes || random.nextDouble() < OBJECT_CHANCE
        val spawnNpcs =
            guarantee == Guarantee.WeakMonsters ||
                !spawnObjects ||
                random.nextDouble() < NPC_ALONGSIDE_OBJECTS_CHANCE
        if (spawnObjects && guarantee !is Guarantee.Nodes) {
            val count = random.nextInt(if (spawnNpcs) 1 else 2, 4)
            repeat(count) { builder.place(DEPLETING.random(random)) }
        }
        if (random.nextDouble() < GRYM_CHANCE) {
            repeat(if (random.nextInt(6) <= 1) random.nextInt(1, 4) else random.nextInt(1, 3)) {
                builder.place(ResourceKind.GRYM)
            }
        }
        if (spawnNpcs) {
            val inner = room.x in 1..5 && room.z in 1..5
            val weak = guarantee == Guarantee.WeakMonsters ||
                (inner && random.nextDouble() < WEAK_CHANCE_INNER)
            val pool = if (weak) MonsterKind.WEAK else MonsterKind.STRONG
            val count = if (weak) random.nextInt(1, 5) else 2
            repeat(count) { builder.placeMonster(pool.random(random)) }
        }
    }

    private class Builder(val slots: TemplateSlots, private val random: Random) {
        val resources = mutableListOf<ResourceSpawn>()
        val monsters = mutableListOf<MonsterSpawn>()

        private fun used(): List<Tile> = resources.map { it.tile } + monsters.map { it.tile }

        private fun free(candidates: List<Tile>): Tile? {
            val taken = used()
            return candidates.shuffled(random).firstOrNull { tile ->
                taken.none { maxOf(kotlin.math.abs(it.x - tile.x), kotlin.math.abs(it.z - tile.z)) <= 1 }
            }
        }

        fun place(kind: ResourceKind) {
            val tile = free(slots.of(kind)) ?: return
            resources += ResourceSpawn(kind, tile)
        }

        fun placeMonster(kind: MonsterKind) {
            val tile = free(slots.all) ?: return
            monsters += MonsterSpawn(kind, tile)
        }

        fun build() = RoomContents(resources.toList(), monsters.toList())
    }
}
