package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import org.rsmod.api.table.thieving.ThievingChestConfigRow
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow

private val PICKPOCKET_OPS = setOf("Pickpocket", "Steal-from")
private const val XP_SCALE = 10.0
private const val TRAP_DAMAGE_DIVISOR = 100
private val UNCOUNTABLE_SUFFIXES = setOf("coal", "ore", "fish", "ray")

internal val ThievingPickpocketRow.experience: Double
    get() = xp / XP_SCALE

internal val ThievingStallRow.experience: Double
    get() = xp / XP_SCALE

internal val ThievingPickpocketRow.stunDamage: IntRange
    get() = stunDamageMin..stunDamageMax

internal val ThievingPickpocketRow.pouchObj: String?
    get() = coinPouch?.internalName

internal val ThievingPickpocketRow.pouchCoins: IntRange
    get() = (pouchCoinsMin ?: 0)..(pouchCoinsMax ?: 0)

internal fun ThievingPickpocketRow.npcTypes(): List<NpcServerType> {
    val ids = npcs.mapTo(HashSet()) { it.id }
    val cats = categories.toHashSet()
    return ServerCacheManager.getNpcs().values.filter { it.id in ids || it.category in cats }
}

internal fun pocketOwnerName(visible: String, lowercase: Boolean, properName: Boolean): String {
    val properNoun = visible.drop(1).any { it.isUpperCase() }
    val name = if (lowercase && !properNoun) visible.lowercase() else visible
    return if (properName || name.contains(" the ")) name else "the $name"
}

internal fun ThievingPickpocketRow.pickpocketTypes(): List<NpcServerType> =
    npcTypes().filter { it.hasPickpocketOp(op) }

internal fun NpcServerType.isPickpocketable(): Boolean = (1..5).any { hasPickpocketOp(it) }

private fun NpcServerType.hasPickpocketOp(op: Int): Boolean =
    actions.getOpOrNull(op - 1) in PICKPOCKET_OPS

internal enum class ChestMethod {
    Trapped,
    Locked,
    Picklock,
}

internal val ThievingChestConfigRow.experience: Double
    get() = xp / XP_SCALE

internal val ThievingChestConfigRow.chestMethod: ChestMethod
    get() = ChestMethod.entries[method]

internal fun trapDamage(hitpoints: Int, scale: Int, base: Int): Int =
    (hitpoints * scale + base) / TRAP_DAMAGE_DIVISOR

internal fun lootNoun(name: String, count: Int): String {
    val lower = name.lowercase()
    val uncountable = lower.endsWith("s") || UNCOUNTABLE_SUFFIXES.any { lower.endsWith(it) }
    return if (count > 1 && !uncountable) "${lower}s" else lower
}
