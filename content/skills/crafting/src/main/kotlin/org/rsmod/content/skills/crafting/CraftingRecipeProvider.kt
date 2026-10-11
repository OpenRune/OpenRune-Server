package org.rsmod.content.skills.crafting

import org.rsmod.api.player.protect.ProtectedAccess

/** Adds recipes whose successful output also changes storage owned by another content module. */
interface CraftingRecipeProvider {
    val products: List<CraftingProduct>
    suspend fun create(access: ProtectedAccess, product: CraftingProduct)
}
