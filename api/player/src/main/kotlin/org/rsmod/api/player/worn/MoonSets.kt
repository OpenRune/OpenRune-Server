package org.rsmod.api.player.worn

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

public enum class MoonSet(
    private val helm: Collection<String>,
    private val helmBroken: String,
    private val body: Collection<String>,
    private val bodyBroken: String,
    private val legs: Collection<String>,
    private val legsBroken: String,
    private val weapon: Collection<String>,
) {
    Blood(
        helm = listOf("obj.blood_moon_helm", "obj.blood_moon_helm_degraded", "obj.br_blood_moon_helm"),
        helmBroken = "obj.blood_moon_helm_broken",
        body =
            listOf(
                "obj.blood_moon_chestplate",
                "obj.blood_moon_chestplate_degraded",
                "obj.br_blood_moon_chestplate",
            ),
        bodyBroken = "obj.blood_moon_chestplate_broken",
        legs =
            listOf(
                "obj.blood_moon_tassets",
                "obj.blood_moon_tassets_degraded",
                "obj.br_blood_moon_tassets",
            ),
        legsBroken = "obj.blood_moon_tassets_broken",
        weapon = listOf("obj.dual_macuahuitl", "obj.br_dual_macuahuitl"),
    ),
    Blue(
        helm = listOf("obj.frost_moon_helm", "obj.frost_moon_helm_degraded", "obj.br_frost_moon_helm"),
        helmBroken = "obj.frost_moon_helm_broken",
        body =
            listOf(
                "obj.frost_moon_chestplate",
                "obj.frost_moon_chestplate_degraded",
                "obj.br_frost_moon_chestplate",
            ),
        bodyBroken = "obj.frost_moon_chestplate_broken",
        legs =
            listOf(
                "obj.frost_moon_tassets",
                "obj.frost_moon_tassets_degraded",
                "obj.br_frost_moon_tassets",
            ),
        legsBroken = "obj.frost_moon_tassets_broken",
        weapon = listOf("obj.frostmoon_spear", "obj.br_frostmoon_spear"),
    ),
    Eclipse(
        helm =
            listOf(
                "obj.eclipse_moon_helm",
                "obj.eclipse_moon_helm_degraded",
                "obj.br_eclipse_moon_helm",
            ),
        helmBroken = "obj.eclipse_moon_helm_broken",
        body =
            listOf(
                "obj.eclipse_moon_chestplate",
                "obj.eclipse_moon_chestplate_degraded",
                "obj.br_eclipse_moon_chestplate",
            ),
        bodyBroken = "obj.eclipse_moon_chestplate_broken",
        legs =
            listOf(
                "obj.eclipse_moon_tassets",
                "obj.eclipse_moon_tassets_degraded",
                "obj.br_eclipse_moon_tassets",
            ),
        legsBroken = "obj.eclipse_moon_tassets_broken",
        weapon = listOf("obj.eclipse_atlatl", "obj.br_eclipse_atlatl"),
    );

    private val helmIds = helm.toIds()
    private val bodyIds = body.toIds()
    private val legsIds = legs.toIds()
    private val weaponIds = weapon.toIds()
    private val helmBrokenId = helmBroken.asRSCM(RSCMType.OBJ)
    private val bodyBrokenId = bodyBroken.asRSCM(RSCMType.OBJ)
    private val legsBrokenId = legsBroken.asRSCM(RSCMType.OBJ)

    public fun isWeapon(obj: InvObj?): Boolean = obj != null && obj.id in weaponIds

    /**
     * Set effects keep working while armour pieces are broken, so a broken piece is accepted here.
     */
    public fun wearsSetEffect(player: Player): Boolean =
        isWeapon(player.righthand) &&
            (player.hat.matches(helmIds, helmBrokenId, allowBroken = true)) &&
            (player.torso.matches(bodyIds, bodyBrokenId, allowBroken = true)) &&
            (player.legs.matches(legsIds, legsBrokenId, allowBroken = true))

    /** Special attacks need every armour piece to be in a usable (not broken) state. */
    public fun wearsFunctionalSet(player: Player): Boolean =
        isWeapon(player.righthand) &&
            (player.hat.matches(helmIds, helmBrokenId, allowBroken = false)) &&
            (player.torso.matches(bodyIds, bodyBrokenId, allowBroken = false)) &&
            (player.legs.matches(legsIds, legsBrokenId, allowBroken = false))

    private fun InvObj?.matches(ids: Set<Int>, brokenId: Int, allowBroken: Boolean): Boolean {
        if (this == null) return false
        return id in ids || (allowBroken && id == brokenId)
    }

}

private fun Collection<String>.toIds(): Set<Int> = mapTo(HashSet()) { it.asRSCM(RSCMType.OBJ) }
