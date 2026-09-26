package org.rsmod.content.other.pets

import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.PlayerDeathHook
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
        addSetBinding<NpcDeathDropHook>(PetDropHook::class.java)
        addSetBinding<NpcDeathKillHook>(PetNpcDropHook::class.java)
        addSetBinding<PlayerDeathHook>(CatDeathHook::class.java)
    }
}
