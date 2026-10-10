package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow

private val PICKPOCKET_OPS = setOf("Pickpocket", "Steal-from")
private const val XP_SCALE = 10.0

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

internal fun NpcServerType.pickpocketSlot(): Int? =
    (1..5).firstOrNull { actions.getOpOrNull(it - 1) in PICKPOCKET_OPS }
