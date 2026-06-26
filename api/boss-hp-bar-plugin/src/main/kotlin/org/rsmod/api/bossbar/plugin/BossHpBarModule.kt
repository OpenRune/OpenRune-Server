package org.rsmod.api.bossbar.plugin

import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.plugin.module.PluginModule

public class BossHpBarModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDamageContributor>(BossHpBarDamageContributor::class.java)
    }
}
