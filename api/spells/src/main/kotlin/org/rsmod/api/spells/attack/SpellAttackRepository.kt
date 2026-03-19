package org.rsmod.api.spells.attack

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject

public class SpellAttackRepository @Inject constructor(private val registry: SpellAttackRegistry) {
    public fun register(spell: ItemServerType, attack: SpellAttack) {
        val result = registry.add(spell, attack)
        assertValidResult(spell, result)
    }

    private fun assertValidResult(spell: ItemServerType, result: SpellAttackRegistry.Result.Add) {
        when (result) {
            SpellAttackRegistry.Result.Add.AlreadyAdded -> error("Spell already mapped: $spell")
            SpellAttackRegistry.Result.Add.Success -> {
                /* no-op */
            }
        }
    }
}
