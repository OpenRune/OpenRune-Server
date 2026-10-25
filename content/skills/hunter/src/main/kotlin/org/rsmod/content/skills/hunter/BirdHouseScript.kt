package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import java.time.Clock
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.table.BirdhouseTypesRow
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BirdHouseScript
@Inject
constructor(private val objRepo: ObjRepository) : PluginScript() {
    var clock: Clock = Clock.systemUTC()

    override fun ScriptContext.startup() {
        for (space in SPACES) {
            onOpLoc1(space.loc) { op1(space) }
            onOpLoc2(space.loc) { addSeeds(space, null) }
            onOpLoc3(space.loc) { op3(space) }
            onOpLoc4(space.loc) { reset(space) }
            onOpLocU(space.loc) { useOn(space, it.objType.internalName) }
        }
        onCommand("birdhouses") {
            desc = "Fast-forward your seeded bird houses to full"
            cheat { fastForward() }
        }
    }

    private suspend fun ProtectedAccess.op1(space: Space) {
        if (state(player, space) == 0) {
            val carried = TIERS.filter { it.item.internalName in inv }
            val tier = carried.lastOrNull { stat(HUNTER) >= it.hunterLevel } ?: carried.firstOrNull()
            if (tier == null) {
                mes("You need a bird house to build here.")
                return
            }
            build(space, tier)
            return
        }
        if (state(player, space) % 3 == 0) {
            if (isFull(player, space)) {
                mes("Your bird house trap is full of birds.")
            } else {
                mes("Your bird house trap is still catching birds.")
            }
        }
    }

    private suspend fun ProtectedAccess.useOn(space: Space, obj: String) {
        val tier = TIERS.firstOrNull { it.item.internalName == obj }
        when {
            tier != null && state(player, space) == 0 -> build(space, tier)
            obj in SEEDS -> addSeeds(space, obj)
            else -> mes("Nothing interesting happens.")
        }
    }

    fun ProtectedAccess.build(space: Space, tier: BirdhouseTypesRow) {
        if (stat(HUNTER) < tier.hunterLevel) {
            mes("You need a Hunter level of ${tier.hunterLevel} to place this bird house.")
            return
        }
        if (invDel(inv, tier.item.internalName).failure) return
        setState(player, space, tier.tier * 3 + 1)
        VarPlayerIntMapSetter.set(player, space.seedsVarp, 0)
        VarPlayerIntMapSetter.set(player, space.seededVarp, 0)
        mes("You place the bird house.")
    }

    fun ProtectedAccess.addSeeds(space: Space, preferred: String?) {
        val state = state(player, space)
        if (state == 0) {
            mes("There is no bird house here.")
            return
        }
        if (state % 3 == 0) {
            mes("The bird house is already full of seeds.")
            return
        }
        var have = player.vars[space.seedsVarp]
        val order = listOfNotNull(preferred) + SEEDS
        for (seed in order) {
            if (have >= SEEDS_NEEDED) break
            val count = minOf(inv.count(seed), SEEDS_NEEDED - have)
            if (count <= 0) continue
            if (invDel(inv, seed, count).failure) continue
            have += count
        }
        if (have == player.vars[space.seedsVarp]) {
            mes("You need some seeds to put in the bird house.")
            return
        }
        VarPlayerIntMapSetter.set(player, space.seedsVarp, have)
        val tierBase = (state - 1) / 3 * 3
        if (have >= SEEDS_NEEDED) {
            VarPlayerIntMapSetter.set(player, space.seededVarp, nowMinutes())
            setState(player, space, tierBase + 3)
            mes("You fill the bird house with seeds.")
        } else {
            setState(player, space, tierBase + 2)
            mes("You add some seeds to the bird house.")
        }
    }

    private fun ProtectedAccess.op3(space: Space) {
        val state = state(player, space)
        when {
            state == 0 -> return
            state % 3 == 0 -> empty(space)
            else -> dismantle(space)
        }
    }

    private fun ProtectedAccess.dismantle(space: Space) {
        clear(player, space)
        invAddOrDrop(objRepo, CLOCKWORK)
        mes("You dismantle the bird house.")
    }

    fun ProtectedAccess.empty(space: Space): Boolean {
        val state = state(player, space)
        if (state == 0 || state % 3 != 0) return false
        if (!isFull(player, space)) {
            mes("The bird house isn't full of birds yet.")
            return false
        }
        val tier = TIERS[state / 3 - 1]
        val loot = rollLoot(tier, stat(HUNTER)) { bound -> random.of(bound) }
        clear(player, space)
        invAddOrDrop(objRepo, CLOCKWORK)
        invAddOrDrop(objRepo, RAW_BIRD_MEAT, BIRDS)
        invAddOrDrop(objRepo, FEATHER, loot.feathers)
        for (nest in loot.nests) invAddOrDrop(objRepo, nest)
        statAdvance(HUNTER, tier.hunterXp.toDouble())
        mes(collectMessage(loot.nests.size, loot.feathers, tier.hunterXp))
        return true
    }

    private fun ProtectedAccess.reset(space: Space) {
        if (!empty(space)) return
        val tier = TIERS.lastOrNull { it.item.internalName in inv && stat(HUNTER) >= it.hunterLevel }
        if (tier != null) build(space, tier)
    }

    private fun Cheat.fastForward() {
        for (space in SPACES) {
            if (state(player, space).let { it != 0 && it % 3 == 0 }) {
                VarPlayerIntMapSetter.set(player, space.seededVarp, nowMinutes() - FULL_MINUTES)
            }
        }
    }

    fun state(player: Player, space: Space): Int = player.vars[space.transmitVarp]

    private fun setState(player: Player, space: Space, value: Int) {
        VarPlayerIntMapSetter.set(player, space.transmitVarp, value)
    }

    private fun clear(player: Player, space: Space) {
        setState(player, space, 0)
        VarPlayerIntMapSetter.set(player, space.seedsVarp, 0)
        VarPlayerIntMapSetter.set(player, space.seededVarp, 0)
    }

    fun isFull(player: Player, space: Space): Boolean =
        nowMinutes() - player.vars[space.seededVarp] >= FULL_MINUTES

    private fun nowMinutes(): Int = (clock.millis() / 60_000L).toInt()

    data class Space(val loc: String, val suffix: Char) {
        val transmitVarp: String get() = "varp.birdhouse_transmit_$suffix"
        val seededVarp: String get() = "varp.birdhouse_seeded_$suffix"
        val seedsVarp: String get() = "varp.birdhouse_seeds_$suffix"
    }

    data class Loot(val feathers: Int, val nests: List<String>)

    companion object {
        const val HUNTER = "stat.hunter"
        const val CLOCKWORK = "obj.poh_clockwork_mechanism"
        const val RAW_BIRD_MEAT = "obj.spit_raw_bird_meat"
        const val FEATHER = "obj.feather"
        const val SEED_NEST = "obj.bird_nest_seeds_jan2019"
        const val RING_NEST = "obj.bird_nest_ring"
        const val EMPTY_NEST = "obj.bird_nest_empty"
        val EGG_NESTS =
            listOf("obj.bird_nest_egg_red", "obj.bird_nest_egg_green", "obj.bird_nest_egg_blue")
        const val SEEDS_NEEDED = 10
        const val FULL_MINUTES = 50
        const val BIRDS = 10
        const val NEST_ROLLS = 5

        val SPACES =
            listOf(
                Space("loc.birdhouse_1", 'a'),
                Space("loc.birdhouse_2", 'b'),
                Space("loc.birdhouse_3", 'c'),
                Space("loc.birdhouse_4", 'd'),
            )

        val TIERS: List<BirdhouseTypesRow> by lazy { BirdhouseTypesRow.all().sortedBy { it.tier } }

        val SEEDS =
            listOf(
                "barley_seed", "jute_seed", "hammerstone_hop_seed", "asgarnian_hop_seed",
                "yanillian_hop_seed", "krandorian_hop_seed", "wildblood_hop_seed", "potato_seed",
                "onion_seed", "cabbage_seed", "tomato_seed", "sweetcorn_seed", "strawberry_seed",
                "watermelon_seed", "snape_grass_seed", "marigold_seed", "rosemary_seed",
                "nasturtium_seed", "woad_seed", "limpwurt_seed", "white_lily_seed",
                "redberry_bush_seed", "cadavaberry_bush_seed", "dwellberry_bush_seed",
                "jangerberry_bush_seed", "whiteberry_bush_seed", "poisonivy_bush_seed",
                "guam_seed", "marrentill_seed", "tarromin_seed", "harralander_seed",
                "ranarr_seed", "toadflax_seed", "irit_seed", "avantoe_seed", "kwuarm_seed",
                "snapdragon_seed", "cadantine_seed", "lantadyme_seed", "dwarf_weed_seed",
                "torstol_seed",
            ).map { "obj.$it" }

        fun chance(level: Int, low: Int, high: Int): Int =
            low + (high - low) * (level.coerceIn(1, 99) - 1) / 98

        fun nestChance(level: Int, high: Int): Int = high * (level.coerceIn(50, 99) - 1) / 98

        fun rollLoot(tier: BirdhouseTypesRow, level: Int, nextInt: (Int) -> Int): Loot {
            val feathers = listOf(30, 40, 50, 60)[nextInt(4)]
            val nests = mutableListOf<String>()
            if (nextInt(256) < chance(level, 0, 200)) nests += SEED_NEST
            repeat(NEST_ROLLS) {
                if (nextInt(1000) < nestChance(level, tier.nestChance)) {
                    val r = nextInt(100)
                    nests +=
                        when {
                            r < 32 -> RING_NEST
                            r < 35 -> EGG_NESTS[r - 32]
                            else -> EMPTY_NEST
                        }
                }
            }
            return Loot(feathers, nests)
        }

        fun collectMessage(nests: Int, feathers: Int, xp: Int): String {
            val nestText =
                when (nests) {
                    0 -> ""
                    1 -> "a nest, "
                    else -> "$nests nests, "
                }
            val xpText = "%,d".format(xp)
            return "You dismantle and discard the trap, retrieving ${nestText}10 dead birds, " +
                "$feathers feathers and $xpText Hunter XP."
        }
    }
}
