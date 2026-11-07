package org.rsmod.content.other.pets

import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.droptable.NpcKillExtraDrops
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.content.other.pets.cats.CatDeathHook
import org.rsmod.content.other.pets.cats.CatScript
import org.rsmod.content.other.pets.dogs.DogScript
import org.rsmod.plugin.module.PluginModule

class PetsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerPostTickHook>(PetScript::class.java)
        addSetBinding<PlayerPostTickHook>(CatScript::class.java)
        addSetBinding<PlayerPostTickHook>(DogScript::class.java)
        addSetBinding<PlayerDeathHook>(CatDeathHook::class.java)
        addSetBinding<NpcKillExtraDrops>(BossPetRoll::class.java)
    }
}
