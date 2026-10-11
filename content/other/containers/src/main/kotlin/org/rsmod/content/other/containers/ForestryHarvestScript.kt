package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.skilling.*
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionLocOp
import org.rsmod.game.interact.InteractionLocT
import org.rsmod.game.interact.InteractionOp
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ForestryHarvestScript @Inject constructor(private val store: ContainerStore, private val random: GameRandom, private val players: PlayerRepository, private val bus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogout { player.attr.remove(PREPARED) }
        onEvent<SkillingProductPrepareEvent> {
            val source = product.source as? SkillingProductSource.Woodcutting ?: return@onEvent
            if (product.isBonus || source.productType.id !in ForestryContainers.logs.accepted) return@onEvent
            product.player.attr[PREPARED] = Prepared(product.player.currentMapClock, source.tree.id, source.tree.coords, product.player.inv.size - product.player.inv.occupiedSpace() >= 2)
        }
        onEvent<SkillingActionCompleteEvent> {
            val product = context as? SkillingActionContext.Product ?: return@onEvent
            val source = product.source as? SkillingProductSource.Woodcutting ?: return@onEvent
            if (product.isBonus || product.count <= 0 || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return@onEvent
            val prepared = player.attr[PREPARED]
            player.attr.remove(PREPARED)
            if (source.productType.id !in ForestryContainers.logs.accepted) return@onEvent
            if (consume(player, OFFERING)) {
                val space = prepared != null && prepared.clock == player.currentMapClock && prepared.id == source.tree.id && prepared.coords == source.tree.coords && prepared.space
                val others = players.findAll(ZoneKey.from(source.tree.coords), 1).filter { it !== player && activeChopper(it, source) }.take(10).count()
                if (random.of(100) < 60 + 2 * others && space) bonus(player, product.item, 1, source)
            }
            val leaf = LEAVES[source.productType.id] ?: return@onEvent
            if (!store.owns(player, ForestryContainers.kit) && player.inv.physicalCount(ATTACHMENT) == 0) return@onEvent
            if (random.of(4) != 0) return@onEvent
            val double = has(player, ATTACHMENT)
            if (bonus(player, leaf, if (double) 2 else 1, source) && double) consume(player, ATTACHMENT)
        }
    }
    private fun activeChopper(player: Player, source: SkillingProductSource.Woodcutting): Boolean {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || player.coords.level != source.tree.coords.level || player.coords.chebyshevDistance(source.tree.coords) > 6 || player.actionDelay < player.currentMapClock) return false
        val target = when (val interaction = player.interaction) {
            is InteractionLocOp -> if (interaction.op in setOf(InteractionOp.Op1, InteractionOp.Op3)) interaction.target else null
            is InteractionLocT -> if (interaction.objType?.isContentType("content.woodcutting_axe") == true) interaction.target else null
            else -> null
        }
        return target?.id == source.tree.id && target.coords == source.tree.coords
    }
    private fun has(player: Player, item: String) = player.inv.physicalCount(item) > 0 || store.owns(player, ForestryContainers.kit) && store.count(player, ForestryContainers.kit, item.asRSCM()) > 0
    private fun consume(player: Player, item: String): Boolean {
        if (player.inv.physicalCount(item) > 0) return player.invDel(player.inv, item, 1).success
        if (!store.owns(player, ForestryContainers.kit) || store.count(player, ForestryContainers.kit, item.asRSCM()) == 0) return false
        store.update(player, ForestryContainers.kit, item.asRSCM(), -1); return true
    }
    private fun bonus(player: Player, item: String, count: Int, source: SkillingProductSource.Woodcutting) = player.awardSkillingProduct(bus, SkillingProduct(player, "stat.woodcutting", item, count, grantsExperience = false, source = source, isBonus = true)) == SkillingAwardResult.Success
    private data class Prepared(val clock: Int, val id: Int, val coords: CoordGrid, val space: Boolean)
    private companion object {
        val PREPARED = AttributeKey<Prepared>()
        const val OFFERING = "obj.nature_offerings"
        const val ATTACHMENT = "obj.forestry_secateurs_attachment"
        val LEAVES by lazy { listOf("oak", "willow", "maple", "yew", "magic").associate { "obj.${it}_logs".asRSCM() to "obj.leaves_$it" } }
    }
}
