package org.rsmod.content.skills.runecrafting

import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.content.skills.runecrafting.essencepouch.EssencePouchDeathHook
import org.rsmod.content.skills.runecrafting.essencepouch.EssencePouchStorageHook
import org.rsmod.plugin.module.PluginModule

public class RunecraftingModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerItemStorageHook>(EssencePouchStorageHook::class.java)
        addSetBinding<PlayerDeathDropHook>(EssencePouchDeathHook::class.java)
    }
}
