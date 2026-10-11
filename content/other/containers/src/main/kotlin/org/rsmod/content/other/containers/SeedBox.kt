package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.content.skills.farming.FarmingSeedContainer
import org.rsmod.game.entity.Player

internal class SeedBox @Inject constructor(private val store: ContainerStore) : FarmingSeedContainer {
    override fun recognises(item: Int) = item in forms.map { it.asRSCM() }
    override fun seeds(player: Player): List<Int> {
        val contents = store.contents(player, spec).filterValues { it > 0 }.keys.map { it.toInt() }
        return (player.attr[ORDER].orEmpty().filter { it in contents } + contents).distinct()
    }
    override fun count(player: Player, seed: Int) = store.count(player, spec, seed)
    override fun consume(player: Player, seed: Int, amount: Int): Boolean {
        if (amount <= 0 || !owns(player) || count(player, seed) < amount) return false
        store.update(player, spec, seed, -amount)
        return true
    }
    fun owns(player: Player) = forms.any { store.owns(player, ContainerCatalog.byId[it.asRSCM()]!!) }
    fun first(player: Player, seed: Int) { val order = seeds(player); if (seed in order) player.attr[ORDER] = listOf(seed) + order.filter { it != seed } }
    companion object {
        val forms = listOf("obj.seed_box", "obj.seed_box_open")
        val spec get() = ContainerCatalog.byId[forms.first().asRSCM()]!!
        val ORDER = AttributeKey<List<Int>>(persistenceKey = "seed_box_order")
    }
}
