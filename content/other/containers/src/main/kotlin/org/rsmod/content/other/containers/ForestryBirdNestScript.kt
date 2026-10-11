package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** A nest replaces an original log; bonus products cannot roll or spend harvest supplies. */
internal class ForestryBirdNestScript
@Inject constructor(
    private val store: ContainerStore,
    private val random: GameRandom,
    private val objs: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<SkillingProductPrepareEvent> {
            val source = product.source as? SkillingProductSource.Woodcutting ?: return@onEvent
            val player = product.player
            if (product.cancelled || product.isBonus || product.count <= 0 ||
                player.hitpoints <= 0 || player.pendingLogout || player.loggingOut ||
                source.productType.id !in eligibleLogs
            ) return@onEvent

            if (random.of(if (capePerk(player)) 230 else 256) != 0) return@onEvent
            val rabbit = player.worn[Wearpos.Front.slot]?.id == RABBIT.asRSCM()
            val roll = random.of(if (rabbit) 95 else 100)
            val nest = when (roll) {
                0 -> "obj.bird_nest_egg_red"
                1 -> "obj.bird_nest_egg_blue"
                2 -> "obj.bird_nest_egg_green"
                in 3..34 -> BirdNestSearchScript.RING
                else -> BirdNestSearchScript.SEEDS
            }
            val ground = Obj.fromOwner(player, player.coords, nest, 1)
            if (objs.add(ground, 200, Int.MAX_VALUE)) {
                product.cancelled = true
                player.mes("<col=ff0000>A bird's nest falls out of the tree.</col>")
            }
        }
    }

    private fun capePerk(player: Player): Boolean {
        val worn = player.worn[Wearpos.Back.slot]?.id ?: return false
        if (worn in ForestryContainers.capes || worn in maxCapes) return true
        if (worn != ForestryContainers.KIT.asRSCM() && worn !in ForestryContainers.basketIds) return false
        val kit = ForestryContainers.kit
        return store.count(player, kit, "obj.forestry_cape_pouch".asRSCM()) > 0 &&
            ForestryContainers.capes.any { store.count(player, kit, it) > 0 }
    }

    private companion object {
        const val RABBIT = "obj.hunting_strung_rabbit_foot"
        val maxCapes by lazy { setOf("obj.skillcape_max".asRSCM(), "obj.skillcape_max_worn".asRSCM()) }
        val eligibleLogs by lazy {
            listOf("logs", "oak_logs", "willow_logs", "teak_logs", "mahogany_logs", "maple_logs", "yew_logs", "magic_logs")
                .map { "obj.$it".asRSCM() }.toSet()
        }
    }
}
