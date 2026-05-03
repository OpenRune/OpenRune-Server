package org.rsmod.api.spells.attack

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType

public class SpellAttackRegistry {
    private val attacks = hashMapOf<Int, SpellAttack>()

    public operator fun get(spell: String): SpellAttack? = attacks[spell.asRSCM(RSCMType.OBJ)]

    public fun add(spell: String, attack: SpellAttack): Result.Add {
        val id = spell.asRSCM(RSCMType.OBJ)
        if (id in attacks) {
            return Result.Add.AlreadyAdded
        }
        attacks[id] = attack
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
