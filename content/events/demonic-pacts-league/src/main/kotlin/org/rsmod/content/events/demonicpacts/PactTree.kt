package org.rsmod.content.events.demonicpacts

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

sealed interface PactPurchase {
    data object Bought : PactPurchase

    data object AlreadyOwned : PactPurchase

    data object NoPoints : PactPurchase

    data object NotConnected : PactPurchase
}

sealed interface PactApply {
    data class Applied(val count: Int) : PactApply

    data class Rejected(val reason: String) : PactApply
}

object PactTree {
    fun canReach(player: Player, node: PactNode): Boolean = canReach(node) { player.ownsPact(it) }

    private fun canReach(node: PactNode, owned: (PactNode) -> Boolean): Boolean {
        if (node.bit == DemonicPactsData.root.bit) {
            return true
        }
        val linked = DemonicPactsData.neighbours[node.id] ?: return false
        return linked.any { id ->
            val other = DemonicPactsData.nodesById[id.uppercase()]
            other != null && owned(other)
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
        player.onPactsChanged()
        return PactPurchase.Bought
    }

    /**
     * Applies a batch of nodes chosen in the pact tree interface. The client lets a node be picked
     * when it touches an owned node or one picked earlier in the same batch, so the batch is
     * checked in that order; nothing is applied unless every node is valid.
     */
    fun apply(player: Player, bits: IntArray): PactApply {
        val chosen = LinkedHashSet<PactNode>()
        for (bit in bits) {
            val node =
                DemonicPactsData.nodesByBit[bit]
                    ?: return PactApply.Rejected("That pact does not exist.")
            if (player.ownsPact(node) || node in chosen) {
                return PactApply.Rejected("You already hold one of those pacts.")
            }
            val reachable = canReach(node) { player.ownsPact(it) || it in chosen }
            if (!reachable) {
                return PactApply.Rejected("Those pacts are not connected to ones you hold.")
            }
            chosen += node
        }
        if (chosen.isEmpty()) {
            return PactApply.Rejected("No pacts were selected.")
        }
        if (chosen.size > player.pactPointsAvailable()) {
            return PactApply.Rejected("You do not have enough pact points for those pacts.")
        }
        for (node in chosen) {
            player.setPactOwned(node, true)
        }
        player.onPactsChanged()
        return PactApply.Applied(chosen.size)
    }

    fun resetsRemaining(player: Player): Int = player.pactResetsAvailable

    fun reset(player: Player): Boolean {
        if (player.pactResetsAvailable <= 0) {
            return false
        }
        player.clearAllPacts()
        player.pactResetsAvailable -= 1
        player.onPactsChanged()
        return true
    }
}

/** Keeps the client's pact counters and per-effect totals in step with the pacts held. */
fun Player.onPactsChanged() {
    syncLeagueClientVars()
    syncPactEffectVarbits()
}

fun Player.syncPactEffectVarbits() {
    val totals = HashMap<Int, Int>()
    for (node in ownedPacts()) {
        totals.merge(node.effect, node.value, Int::plus)
    }
    for (effect in DemonicPactsData.effectVarbits) {
        val value = (totals[effect.id] ?: 0).coerceIn(0, effect.max)
        if (vars[effect.varbit] != value) {
            VarPlayerIntMapSetter.set(this, effect.varbit, value)
        }
    }
}

class PactEffects(
    val accuracyPercent: Int,
    val meleeDamagePercent: Int,
    val rangedDamagePercent: Int,
    val magicDamagePercent: Int,
    val meleeMaxHitDistancePercent: Int,
    val meleeMinHit: Int,
    val thorns: Int,
    val thornsDouble: Boolean,
    val reflect: Boolean,
    val specOnMeleeHit: Int,
    val specFromDistance: Int,
    val prayerRegen: Boolean,
    val prayerPenetration: Int,
    val defenceBoost: Int,
) {
    companion object {
        val NONE: PactEffects = PactEffects(0, 0, 0, 0, 0, 0, 0, false, false, 0, 0, false, 0, 0)
    }
}

/** Accuracy every style-damage pact also grants, per its description. */
private const val DAMAGE_NODE_ACCURACY = 10

/** Special attack energy is stored in tenths of a percent. */
private const val SPEC_UNITS_PER_PERCENT = 10

private const val SPEC_FROM_DISTANCE_PERCENT = 2

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
    var prayerRegen = false
    var penetration = 0
    var defence = 0
    for (node in DemonicPactsData.nodes) {
        if (!ownsPact(node)) {
            continue
        }
        when (node.effect) {
            PactEffectIds.ALL_STYLE_ACCURACY -> accuracy += node.value
            PactEffectIds.MELEE_DAMAGE -> {
                melee += node.value
                accuracy += DAMAGE_NODE_ACCURACY
            }
            PactEffectIds.RANGED_DAMAGE -> {
                ranged += node.value
                accuracy += DAMAGE_NODE_ACCURACY
            }
            PactEffectIds.MAGIC_DAMAGE -> {
                magic += node.value
                accuracy += DAMAGE_NODE_ACCURACY
            }
            PactEffectIds.MELEE_MAX_HIT_DISTANCE -> maxHitDistance += node.value
            PactEffectIds.MELEE_MIN_HIT_DISTANCE -> meleeMinHit += node.value
            PactEffectIds.THORNS -> thorns += node.value
            PactEffectIds.THORNS_DOUBLE_HIT -> thornsDouble = true
            PactEffectIds.SHIELD_REFLECT -> reflect = true
            PactEffectIds.SPEC_ON_MELEE_HIT -> specOnMelee += node.value * SPEC_UNITS_PER_PERCENT
            PactEffectIds.SPEC_FROM_DISTANCE ->
                specDistance = SPEC_FROM_DISTANCE_PERCENT * SPEC_UNITS_PER_PERCENT
            PactEffectIds.PRAYER_RESTORE_NO_OVERHEAD -> prayerRegen = true
            PactEffectIds.PRAYER_PEN_ALL -> penetration += node.value
            PactEffectIds.DEFENCE_BOOST -> defence += node.value
        }
    }
    return PactEffects(
        accuracyPercent = accuracy,
        meleeDamagePercent = melee,
        rangedDamagePercent = ranged,
        magicDamagePercent = magic,
        meleeMaxHitDistancePercent = maxHitDistance,
        meleeMinHit = meleeMinHit,
        thorns = thorns,
        thornsDouble = thornsDouble,
        reflect = reflect,
        specOnMeleeHit = specOnMelee,
        specFromDistance = specDistance,
        prayerRegen = prayerRegen,
        prayerPenetration = penetration.coerceAtMost(100),
        defenceBoost = defence,
    )
}
