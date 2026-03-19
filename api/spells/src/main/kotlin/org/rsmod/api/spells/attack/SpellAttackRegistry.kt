package org.rsmod.api.spells.attack

import dev.openrune.types.ItemServerType

public class SpellAttackRegistry {
    private val attacks = hashMapOf<Int, SpellAttack>()

    public operator fun get(spell: ItemServerType): SpellAttack? = attacks[spell.id]

    public fun add(spell: ItemServerType, attack: SpellAttack): Result.Add {
        if (spell.id in attacks) {
            return Result.Add.AlreadyAdded
        }
        attacks[spell.id] = attack
        return Result.Add.Success
    }

    public class Result {
        public sealed class Add {
            public data object Success : Add()

            public sealed class Failure : Add()

            public data object AlreadyAdded : Failure()
        }
    }
}
