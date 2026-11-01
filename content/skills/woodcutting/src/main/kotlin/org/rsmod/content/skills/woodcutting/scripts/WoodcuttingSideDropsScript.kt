package org.rsmod.content.skills.woodcutting.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.enums.NamedEnums.bird_nest_woodcutting_types
import org.rsmod.api.enums.NamedEnums.bird_nest_woodcutting_types_rabbit_foot
import org.rsmod.api.player.back
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
        if (
            source.productType.internalName == "obj.redwood_logs" ||
                source.productType.internalName == "obj.rosewood_logs"
        ) {
            return
        }
        // The cape boost scales the 1/256 itself rather than adding a second roll.
        val boosted = wearingNestBoostCape()
        if (random.of(2560) >= if (boosted) 11 else 10) return
        // TODO: Twitcher's gloves raise the chance of the selected nest type by 20%; the wiki
        //  gives no exact roll adjustment.
        val rabbitFoot = "obj.hunting_strung_rabbit_foot" in player.worn
        val weights =
            if (rabbitFoot) bird_nest_woodcutting_types_rabbit_foot else bird_nest_woodcutting_types
        val nest = nestFor(weights.filterValuesNotNull().backing, random)
        // UNVERIFIED: blocked tiles are excluded from the landing choice (guess)
        // TODO: the landing tile is biased towards the west by an unstated amount.
        val tiles = ArrayList<CoordGrid>(8)
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val tile = player.coords.translate(dx, dz)
                if (!collision.isWalkBlocked(tile)) tiles.add(tile)
            }
        }
        val tile = if (tiles.isEmpty()) player.coords else random.pick(tiles)
        objRepo.add(nest.internalName, tile, duration = NEST_DESPAWN, receiver = player)
        player.mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
        // TODO: 1/250 of drops play the Armadyl crossbow special attack sound instead; it has no
        //  synth gameval.
        player.soundSynth("synth.bird_nest_fall")
        player.runClientScript(
            "clientscript.[clientscript,local_notification]".asRSCM(),
            "A bird's nest has spawned!",
            "A bird nest has fallen out of the tree.",
        )
    }

    private fun SkillingActionCompleteEvent.wearingNestBoostCape(): Boolean {
        if ("obj.skillcape_woodcutting" in player.worn) return true
        if ("obj.skillcape_woodcutting_trimmed" in player.worn) return true
        return player.back?.let { ServerCacheManager.getItem(it.id) }
            ?.isContentType("content.max_cape") == true
    }

    companion object {
        private const val NEST_DESPAWN = 200

        internal fun nestFor(weights: Map<ItemServerType, Int>, random: GameRandom): ItemServerType {
            var roll = random.of(weights.values.sum())
            return weights.entries
                .first {
                    roll -= it.value
                    roll < 0
                }
                .key
        }
    }
}
