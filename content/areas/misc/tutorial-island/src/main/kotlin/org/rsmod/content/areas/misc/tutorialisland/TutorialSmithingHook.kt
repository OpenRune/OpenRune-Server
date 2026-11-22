package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.content.skills.smithing.SmithingRestrictHook
import org.rsmod.content.skills.smithing.util.SmithingProductMeta
import org.rsmod.game.entity.Player

internal class TutorialSmithingHook @Inject constructor() : SmithingRestrictHook {
    override fun restrict(player: Player, product: SmithingProductMeta): String? {
        if (!player.inTutorial || product.product.internalName == "obj.bronze_dagger") {
            return null
        }
        return "You cannot make this on Tutorial Island."
    }
}
