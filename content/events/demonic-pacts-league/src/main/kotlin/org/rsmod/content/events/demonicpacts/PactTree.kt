package org.rsmod.content.events.demonicpacts

import org.rsmod.game.entity.Player

sealed interface PactPurchase {
    data object Bought : PactPurchase

    data object AlreadyOwned : PactPurchase

    data object NoPoints : PactPurchase

    data object NotConnected : PactPurchase
}

object PactTree {
    fun canReach(player: Player, node: PactNode): Boolean {
        if (node.id == DemonicPactsData.root.id) {
            return true
        }
        val linked = DemonicPactsData.neighbours[node.id] ?: return false
        return linked.any { id ->
            val other = DemonicPactsData.nodesById[id.uppercase()]
            other != null && player.ownsPact(other)
        }
    }

    fun purchasable(player: Player): List<PactNode> =
        DemonicPactsData.nodes.filter { !player.ownsPact(it) && canReach(player, it) }

    fun buy(player: Player, node: PactNode): PactPurchase {
        if (player.ownsPact(node)) {
            return PactPurchase.AlreadyOwned
        }
        if (player.pactPointsAvailable() <= 0) {
            return PactPurchase.NoPoints
        }
        if (!canReach(player, node)) {
            return PactPurchase.NotConnected
        }
        player.setPactOwned(node, true)
        return PactPurchase.Bought
    }

    fun resetsRemaining(player: Player): Int =
        (DemonicPactsData.settings.maxPactResets - player.pactResetsUsed).coerceAtLeast(0)

    fun reset(player: Player): Boolean {
        if (resetsRemaining(player) <= 0) {
            return false
        }
        player.clearAllPacts()
        player.pactResetsUsed += 1
        return true
    }
}

class PactEffects(
    val accuracyPercent: Int,
    val meleeDamagePercent: Int,
    val rangedDamagePercent: Int,
    val magicDamagePercent: Int,
    val maxHitDistancePercent: Int,
    val meleeMinHit: Int,
    val thorns: Int,
    val thornsDouble: Boolean,
    val reflect: Boolean,
    val specOnMeleeHit: Int,
    val specFromDistance: Int,
    val prayerRegenTicks: Int,
    val prayerPenetration: Int,
) {
    companion object {
        val NONE: PactEffects = PactEffects(0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, 0, 0)
    }
}

fun Player.pactEffects(): PactEffects {
    if (!isDemonicPactsLeague) {
        return PactEffects.NONE
    }
    var accuracy = 0
    var melee = 0
    var ranged = 0
    var magic = 0
    var maxHitDistance = 0
    var meleeMinHit = 0
    var thorns = 0
    var thornsDouble = false
    var reflect = false
    var specOnMelee = 0
    var specDistance = 0
    var prayerRegen = 0
    var penetration = 0
    for (node in DemonicPactsData.nodes) {
        if (!node.isImplemented || !ownsPact(node)) {
            continue
        }
        when (node.effect) {
            "accuracy" -> accuracy += node.value
            "damage" ->
                when (node.style) {
                    "melee" -> melee += node.value
                    "ranged" -> ranged += node.value
                    "magic" -> magic += node.value
                }
            "max_hit_distance" -> maxHitDistance += node.value
            "melee_min_hit" -> meleeMinHit += node.value
            "thorns" -> thorns += node.value
            "thorns_double" -> thornsDouble = true
            "reflect" -> reflect = true
            "spec_on_melee" -> specOnMelee += node.value
            "spec_distance" -> specDistance += node.value
            "prayer_regen" -> prayerRegen = node.value
            "prayer_penetration" -> penetration += node.value
        }
    }
    return PactEffects(
        accuracyPercent = accuracy,
        meleeDamagePercent = melee,
        rangedDamagePercent = ranged,
        magicDamagePercent = magic,
        maxHitDistancePercent = maxHitDistance,
        meleeMinHit = meleeMinHit,
        thorns = thorns,
        thornsDouble = thornsDouble,
        reflect = reflect,
        specOnMeleeHit = specOnMelee,
        specFromDistance = specDistance,
        prayerRegenTicks = prayerRegen,
        prayerPenetration = penetration.coerceAtMost(100),
    )
}
