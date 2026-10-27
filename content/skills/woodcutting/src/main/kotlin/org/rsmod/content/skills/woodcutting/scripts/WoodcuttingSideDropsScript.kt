package org.rsmod.content.skills.woodcutting.scripts

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

class WoodcuttingSideDropsScript
@Inject
constructor(
    private val random: GameRandom,
    private val objRepo: ObjRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            if (product.isBonus || product.skill != "stat.woodcutting") return@onEvent
            val source = product.source as? SkillingProductSource.Woodcutting ?: return@onEvent
            rollNest(source)
        }
    }

    private fun SkillingActionCompleteEvent.rollNest(source: SkillingProductSource.Woodcutting) {
        if (source.productType.internalName == "obj.redwood_logs" ||
            source.productType.internalName == "obj.rosewood_logs") return
        val cape = "obj.skillcape_woodcutting" in player.worn ||
            "obj.skillcape_woodcutting_trimmed" in player.worn
        if (random.of(2560) >= if (cape) 11 else 10) return
        val nest = nestFor(random.of(if ("obj.hunting_strung_rabbit_foot" in player.worn) 95 else 100))
        // shortcut: five captures show the nest on a random adjacent tile; the blocked-tile rule is unobserved.
        val tiles = ArrayList<CoordGrid>(8)
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val tile = player.coords.translate(dx, dz)
                if (!collision.isWalkBlocked(tile)) tiles.add(tile)
            }
        }
        val tile = if (tiles.isEmpty()) player.coords else random.pick(tiles)
        objRepo.add(nest, tile, duration = NEST_DESPAWN, receiver = player)
        player.mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
        player.soundSynth("synth.bird_nest_fall")
        player.runClientScript(
            "clientscript.[clientscript,local_notification]".asRSCM(),
            "A bird's nest has spawned!", "A bird nest has fallen out of the tree.",
        )
    }

    companion object {
        private const val NEST_DESPAWN = 200

        internal fun nestFor(roll: Int): String = when (roll) {
            0 -> "obj.bird_nest_egg_red"
            1 -> "obj.bird_nest_egg_blue"
            2 -> "obj.bird_nest_egg_green"
            in 3..34 -> "obj.bird_nest_ring"
            else -> "obj.bird_nest_seeds_jan2019"
        }
    }
}
