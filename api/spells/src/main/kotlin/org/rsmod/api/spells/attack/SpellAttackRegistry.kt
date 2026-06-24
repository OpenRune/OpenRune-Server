package org.rsmod.api.spells.attack

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.magic.MagicSpell

public class SpellAttackRegistry {
    private val attacks = hashMapOf<Int, SpellAttack>()

    public operator fun get(spell: String): SpellAttack? = attacks[spell.asRSCM(RSCMType.OBJ)]

    public operator fun contains(spell: MagicSpell): Boolean = spell.obj.id in attacks

    public operator fun contains(spell: ItemServerType): Boolean = spell.id in attacks

    public fun spellIds(): Set<Int> = attacks.keys.toSet()

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
