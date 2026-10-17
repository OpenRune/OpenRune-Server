package org.rsmod.api.specials

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.specials.instant.InstantSpecialAttack
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.inv.InvObj

public class SpecialAttackRegistry @Inject constructor(private val weapons: SpecialAttackWeapons) {
    private val specials = hashMapOf<Int, SpecialAttack>()

    public operator fun get(obj: InvObj): SpecialAttack? = specials[obj.id]

    /** The authoritative eligibility check for the live `::specialbank` weapon collection. */
    public fun isRegisteredItem(id: Int): Boolean = id in specials

    /** Returns a snapshot of every item ID whose special is currently registered. */
    public fun registeredItemIds(): Set<Int> = specials.keys.toSet()

    public fun add(obj: String, spec: InstantSpecialAttack): Result.Add {
        val id = obj.asRSCM(RSCMType.OBJ)

        if (id in specials) {
            return Result.Add.AlreadyAdded
        }
        val energy = weapons.getSpecialEnergy(id) ?: return Result.Add.SpecialEnergyNotMapped
        val special = SpecialAttack.Instant(energy, spec)
        specials[id] = special
        return Result.Add.Success
    }

    public fun add(obj: String, spec: MeleeSpecialAttack): Result.Add {
        val id = obj.asRSCM(RSCMType.OBJ)

        if (id in specials) {
            return Result.Add.AlreadyAdded
        }
        val energy = weapons.getSpecialEnergy(id) ?: return Result.Add.SpecialEnergyNotMapped
        val special = SpecialAttack.Melee(energy, spec)
        specials[id] = special
        return Result.Add.Success
    }

    /** Registers a normal-energy melee special for a cache-native item without an RSCM alias. */
    public fun add(obj: Int, energyInHundreds: Int, spec: MeleeSpecialAttack): Result.Add {
        require(energyInHundreds in 10..SpecialAttackEnergy.MAX_ENERGY)
        if (obj in specials) {
            return Result.Add.AlreadyAdded
        }
        specials[obj] = SpecialAttack.Melee(energyInHundreds, spec)
        return Result.Add.Success
    }

    public fun add(obj: String, spec: RangedSpecialAttack): Result.Add {
        val id = obj.asRSCM(RSCMType.OBJ)

        if (id in specials) {
            return Result.Add.AlreadyAdded
        }
        val energy = weapons.getSpecialEnergy(id) ?: return Result.Add.SpecialEnergyNotMapped
        val special = SpecialAttack.Ranged(energy, spec)
        specials[id] = special
        return Result.Add.Success
    }

    /** Registers a normal-energy ranged special for a cache-native item without an RSCM alias. */
    public fun add(obj: Int, energyInHundreds: Int, spec: RangedSpecialAttack): Result.Add {
        require(energyInHundreds in 10..SpecialAttackEnergy.MAX_ENERGY)
        if (obj in specials) {
            return Result.Add.AlreadyAdded
        }
        specials[obj] = SpecialAttack.Ranged(energyInHundreds, spec)
        return Result.Add.Success
    }

    public fun add(obj: String, spec: MagicSpecialAttack): Result.Add {
        val id = obj.asRSCM(RSCMType.OBJ)

        if (id in specials) {
            return Result.Add.AlreadyAdded
        }
        val energy = weapons.getSpecialEnergy(id) ?: return Result.Add.SpecialEnergyNotMapped
        val special = SpecialAttack.Magic(energy, spec)
        specials[id] = special
        return Result.Add.Success
    }

    /** Registers a normal-energy magic special for a cache-native item without an RSCM alias. */
    public fun add(obj: Int, energyInHundreds: Int, spec: MagicSpecialAttack): Result.Add {
        require(energyInHundreds in 10..SpecialAttackEnergy.MAX_ENERGY)
        if (obj in specials) {
            return Result.Add.AlreadyAdded
        }
        specials[obj] = SpecialAttack.Magic(energyInHundreds, spec)
        return Result.Add.Success
    }

    public class Result {
        public sealed class Add {
            public data object Success : Add()

            public sealed class Failure : Add()

            public data object AlreadyAdded : Failure()

            public data object SpecialEnergyNotMapped : Failure()
        }
    }
}
