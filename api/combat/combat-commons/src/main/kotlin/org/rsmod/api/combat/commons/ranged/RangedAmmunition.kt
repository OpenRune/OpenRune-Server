package org.rsmod.api.combat.commons.ranged

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.util.Wearpos
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.back
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.WornUnequipOp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.obj.Obj
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

public object RangedAmmunition {
    /** The default chance for standard ammunition to drop on the ground instead of disappearing. */
    public const val DEFAULT_AMMO_DROP_RATE: Int = 5

    /** The obj spawn duration when an ammunition is dropped on the ground after being fired. */
    public const val DEFAULT_AMMO_DROP_DURATION: Int = 200

    /**
     * Verifies that [weapon] can use [ammo] as valid ammunition and sends an appropriate error
     * message to the [player] if it cannot.
     *
     * **Note:** This function only performs validation and messaging. It does **not** remove
     * ammunition or attempt to drop it - use [detractAmmo] and [attemptAmmoDrop] for that behavior.
     *
     * @return `true` if the [ammo] is valid for the given [weapon], or if the [weapon] does not
     *   require specific ammunition
     */
    public fun attemptAmmoUsage(
        player: Player,
        weapon: ItemServerType,
        ammo: ItemServerType?,
    ): Boolean {
        val crossbow = weapon.isCategoryType("category.crossbow")
        if (crossbow) {
            if (ammo == null) {
                player.mes("There is no ammo left in your quiver.")
                return false
            }

            val ammoValidation = validateBolts(weapon, ammo)
            return when (ammoValidation) {
                is Validation.Invalid.IncorrectAmmo -> {
                    player.mes("You can't use that ammo with your crossbow.")
                    false
                }
                is Validation.Invalid.BoneWeaponIncorrectAmmo -> {
                    player.mes("You can't use that ammo with your bone crossbow.")
                    false
                }
                is Validation.Invalid.ExpectedBoneWeapon -> {
                    player.mes("Bone bolts are only usable with a bone crossbow.")
                    false
                }
                is Validation.Invalid.LevelTooHigh -> {
                    player.mes("Your crossbow isn't powerful enough for those bolts.")
                    false
                }
                is Validation.Valid -> true
            }
        }

        val bow = weapon.isCategoryType("category.bow")
        if (bow) {
            if (ammo == null) {
                player.mes("There is no ammo left in your quiver.")
                return false
            }

            val ammoValidation = validateArrows(weapon, ammo)
            return when (ammoValidation) {
                is Validation.Invalid.Ammo -> {
                    player.mes("You can't use that ammo with your bow.")
                    false
                }
                is Validation.Invalid.LevelTooHigh -> {
                    player.mes("Your bow isn't powerful enough for those arrows.")
                    false
                }
                is Validation.Valid -> true
            }
        }

        val ballista = weapon.isCategoryType("category.ballista")
        if (ballista) {
            if (ammo == null) {
                player.mes("There are no javelins in your quiver.")
                return false
            }

            val ammoValidation = validateJavelins(weapon, ammo)
            return when (ammoValidation) {
                is Validation.Invalid -> {
                    player.mes("You can't use that ammo with your ballista.")
                    false
                }
                is Validation.Valid -> true
            }
        }

        return true
    }

    public fun conserveAmmo(player: Player, random: GameRandom): Boolean {
        val cape = getOrNull(player.back)
        if (cape != null) {
            val recoveryRate = cape.paramOrNull(params.ammo_recovery_rate) ?: return false

            val body = getOrNull(player.torso)
            if (body != null && body.param(params.metallic_interference)) {
                return false
            }

            return recoveryRate > random.of(maxExclusive = 100)
        }
        return false
    }

    public fun detractAmmo(
        player: Player,
        wearpos: Wearpos,
        wornType: ItemServerType,
        detract: Int,
        eventBus: EventBus,
    ) {
        val startObj = player.worn[wearpos.slot]
        check(startObj.isType(wornType)) {
            "Expected worn obj to match `wornType`: wearpos=$wearpos, obj=$startObj, type=$wornType"
        }

        val oldCount = startObj.count
        check(oldCount >= detract) { "Unexpected low worn count: $oldCount (expected=$detract)" }

        player.worn[wearpos.slot] = startObj.copy(count = oldCount - detract)

        val outOfAmmo = oldCount - detract == 0
        if (outOfAmmo) {
            player.worn[wearpos.slot] = null

            // Official behavior: manually unequipping the quiver slot triggers an appearance
            // rebuild, but running out of ammo does not.
            val rebuildAppearance = wearpos == Wearpos.RightHand
            WornUnequipOp.notifyWornUnequip(
                player = player,
                wearpos = wearpos,
                objType = wornType,
                eventBus = eventBus,
                rebuildAppearance = rebuildAppearance,
            )
        }
    }

    public fun attemptAmmoDrop(
        player: Player,
        delay: Int,
        ammoType: ItemServerType,
        ammoCount: Int,
        dropCoord: CoordGrid,
        dropDuration: Int,
        collision: CollisionFlagMap,
        worldQueues: WorldQueueList,
        objRepo: ObjRepository,
    ) {
        // Note: This might not be the "official" behavior - ideally, this check would happen
        // after the `delay` has passed. However, collision flags are unlikely to change in
        // that short time frame, and performing the check here prevents adding unnecessary
        // entries to the world queue. This is a micro-optimization, but practically free
        // and safe.
        if (collision.isWalkBlocked(dropCoord)) {
            return
        }

        val obj = Obj.fromOwner(player, dropCoord, ammoType, ammoCount)
        worldQueues.add(delay) { objRepo.add(obj, dropDuration) }
    }

    public val defualtArrows: CategoryType = CategoryType("category.arrows".asRSCM(RSCMType.CATEGORY))

    public fun validateArrows(weapon: ItemServerType, ammo: ItemServerType): Validation {

        val requiredAmmo = weapon.paramOrNull(params.required_ammo) ?: defualtArrows

        // Dragon arrows have a separate category from standard arrows, but any bow that accepts
        // regular arrows can also use dragon arrows, provided the `levelrequire` threshold is met.
        val isAlternativeAmmo =
            requiredAmmo.isType("category.arrows") && ammo.isCategoryType("category.dragon_arrow")

        if (!ammo.isCategory(requiredAmmo) && !isAlternativeAmmo) {
            return Validation.Invalid.IncorrectAmmo
        }

        if (ammo.param(params.levelrequire) > weapon.param(params.levelrequire)) {
            return Validation.Invalid.LevelTooHigh
        }

        return Validation.Valid
    }

    public fun validateBolts(weapon: ItemServerType, ammo: ItemServerType): Validation {
        val requiredAmmo = weapon.paramOrNull(params.required_ammo) ?: CategoryType("category.crossbow_bolt".asRSCM(RSCMType.CATEGORY))

        if (!ammo.isCategory(requiredAmmo)) {
            return if (weapon.param(params.bone_weapon) != 0) {
                Validation.Invalid.BoneWeaponIncorrectAmmo
            } else {
                Validation.Invalid.IncorrectAmmo
            }
        }

        if (ammo.param(params.bone_weapon) != 0 && weapon.param(params.bone_weapon) == 0) {
            return Validation.Invalid.ExpectedBoneWeapon
        }

        if (ammo.param(params.levelrequire) > weapon.param(params.levelrequire)) {
            return Validation.Invalid.LevelTooHigh
        }

        return Validation.Valid
    }

    public fun validateJavelins(weapon: ItemServerType, ammo: ItemServerType): Validation {
        val requiredAmmo = weapon.paramOrNull(params.required_ammo) ?: CategoryType("category.javelin".asRSCM(RSCMType.CATEGORY))
        return if (!ammo.isCategory(requiredAmmo)) {
            Validation.Invalid.IncorrectAmmo
        } else {
            return Validation.Valid
        }
    }

    public sealed class Validation {
        public data object Valid : Validation()

        public sealed class Invalid : Validation() {
            public data object LevelTooHigh : Invalid()

            public sealed class Ammo : Invalid()

            public data object IncorrectAmmo : Ammo()

            public data object BoneWeaponIncorrectAmmo : Ammo()

            public data object ExpectedBoneWeapon : Ammo()
        }
    }
}
