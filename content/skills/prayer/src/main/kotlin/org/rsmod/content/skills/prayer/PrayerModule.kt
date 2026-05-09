package org.rsmod.content.skills.prayer

import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.content.skills.prayer.items.ashsanctifier.AshSanctifierNpcDropHook
import org.rsmod.content.skills.prayer.items.bonecrusher.BonecrusherNpcDropHook
import org.rsmod.content.skills.prayer.items.EctoplasmatorNpcKillHook
import org.rsmod.plugin.module.PluginModule

public class PrayerModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathDropHook>(BonecrusherNpcDropHook::class.java)
        addSetBinding<NpcDeathDropHook>(AshSanctifierNpcDropHook::class.java)
        addSetBinding<NpcDeathKillHook>(EctoplasmatorNpcKillHook::class.java)
    }
}
