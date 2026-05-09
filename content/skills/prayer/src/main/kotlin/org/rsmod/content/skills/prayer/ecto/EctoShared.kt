package org.rsmod.content.skills.prayer.ecto

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.prayer.PrayerEctofuntusBonemealRow

internal data class EctoRecipe(
    val id: Int,
    val bones: String,
    val bonemeal: String,
    val bonesName: String,
    val xp: Double,
    val prayerLevel: Int = 1,
)

internal val ECTO_RECIPES: List<EctoRecipe> by lazy { PrayerEctofuntusBonemealRow.all().mapIndexed { index, row ->
    EctoRecipe(
        id = index + 1, bones = row.bone.internalName,
        bonemeal = row.bonemeal.internalName,
        bonesName = row.bone.name,
        xp = row.worshipXp.toDouble(),
        prayerLevel = row.requiredLevel
    )}
}

internal fun findEctoRecipe(id: Int): EctoRecipe? = ECTO_RECIPES.firstOrNull { it.id == id }

internal var ProtectedAccess.ectoGrinderStatus by intVarBit("varbit.ahoy_grinder_status")
internal var ProtectedAccess.ectoGrinderRecipe by intVarBit("varbit.ecto_grinder_recipe")
var ProtectedAccess.ectoTokens by intVarBit("varbit.ecto_tokens")
