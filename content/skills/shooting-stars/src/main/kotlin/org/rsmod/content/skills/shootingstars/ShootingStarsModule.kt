package org.rsmod.content.skills.shootingstars

import org.rsmod.content.skills.shootingstars.shops.StardustShopOperations
import org.rsmod.plugin.module.PluginModule

class ShootingStarsModule : PluginModule() {
    override fun bind() {
        bindInstance<StardustShopOperations>()
    }
}
