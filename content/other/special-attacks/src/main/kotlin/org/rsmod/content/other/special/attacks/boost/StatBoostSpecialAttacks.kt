package org.rsmod.content.other.special.attacks.boost

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository

class StatBoostSpecialAttacks @Inject constructor(private val worldRepo: WorldRepository) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant("obj.dragon_axe", ::lumberUpRed)
        registerInstant("obj.trailblazer_axe_no_infernal", ::lumberUpRed)
        registerInstant("obj.trailblazer_reloaded_axe_no_infernal", ::lumberUpRed)
        registerInstant("obj.dragon_axe_2h", ::lumberUpRed)
        registerInstant("obj.league_trailblazer_axe", ::lumberUpRed)
        registerInstant("obj.3a_axe", ::lumberUpSilver)
        registerInstant("obj.3a_axe_2h", ::lumberUpSilver)
        registerInstant("obj.infernal_axe", ::lumberUpRed)
        registerInstant("obj.infernal_axe_empty", ::lumberUpRed)
        registerInstant("obj.trailblazer_axe", ::lumberUpRed)
        registerInstant("obj.trailblazer_axe_empty", ::lumberUpRed)
        registerInstant("obj.trailblazer_reloaded_axe", ::lumberUpRed)
        registerInstant("obj.trailblazer_reloaded_axe_empty", ::lumberUpRed)
        registerInstant("obj.crystal_axe", ::lumberUpSilver)
        registerInstant("obj.crystal_axe_inactive", ::lumberUpSilver)
        registerInstant("obj.crystal_axe_2h", ::lumberUpSilver)
        registerInstant("obj.crystal_axe_2h_inactive", ::lumberUpSilver)

        registerInstant("obj.dragon_harpoon", ::fishstabberDragonHarpoon)
        registerInstant("obj.trailblazer_harpoon_no_infernal", ::fishstabberDragonHarpoonOr)
        registerInstant("obj.trailblazer_reloaded_harpoon_no_infernal", ::fishstabberDragonHarpoonOr)
        registerInstant("obj.league_trailblazer_harpoon", ::fishstabberDragonHarpoonOr)
        registerInstant("obj.infernal_harpoon", ::fishstabberInfernalHarpoon)
        registerInstant("obj.infernal_harpoon_empty", ::fishstabberInfernalHarpoon)
        registerInstant("obj.trailblazer_harpoon", ::fishstabberInfernalHarpoonOr)
        registerInstant("obj.trailblazer_harpoon_empty", ::fishstabberInfernalHarpoonOr)
        registerInstant("obj.trailblazer_reloaded_harpoon", ::fishstabberInfernalHarpoonOr)
        registerInstant("obj.trailblazer_reloaded_harpoon_empty", ::fishstabberInfernalHarpoonOr)
        registerInstant("obj.crystal_harpoon", ::fishstabberCrystalHarpoon)
        registerInstant("obj.crystal_harpoon_inactive", ::fishstabberCrystalHarpoon)

        registerInstant("obj.dragon_pickaxe", ::rockKnockerDragonPickaxe)
        registerInstant("obj.zalcano_pickaxe", ::rockKnockerDragonPickaxeOrZalcano)
        registerInstant("obj.trailblazer_pickaxe_no_infernal", ::rockKnockerDragonPickaxeOrTrailblazer)
        registerInstant("obj.trailblazer_reloaded_pickaxe_no_infernal", ::rockKnockerDragonPickaxeOrTrailblazer)
        registerInstant("obj.league_trailblazer_pickaxe", ::rockKnockerDragonPickaxeOrTrailblazer)
        registerInstant("obj.dragon_pickaxe_pretty", ::rockKnockerDragonPickaxeUpgraded)
        registerInstant("obj.infernal_pickaxe", ::rockKnockerInfernalPickaxe)
        registerInstant("obj.infernal_pickaxe_empty", ::rockKnockerInfernalPickaxe)
        registerInstant("obj.trailblazer_pickaxe", ::rockKnockerInfernalPickaxeOr)
        registerInstant("obj.trailblazer_pickaxe_empty", ::rockKnockerInfernalPickaxeOr)
        registerInstant("obj.trailblazer_reloaded_pickaxe", ::rockKnockerInfernalPickaxeOr)
        registerInstant("obj.trailblazer_reloaded_pickaxe_empty", ::rockKnockerInfernalPickaxeOr)
        registerInstant("obj.3a_pickaxe", ::rockKnockerThirdAgePickaxe)
        registerInstant("obj.crystal_pickaxe", ::rockKnockerCrystalPickaxe)
        registerInstant("obj.crystal_pickaxe_inactive", ::rockKnockerCrystalPickaxe)

        registerInstant("obj.excalibur", ::sanctuary)
    }

    private fun sanctuary(access: ProtectedAccess): Boolean {
        access.statBoost("stat.defence", constant = 8, percent = 0)
        access.anim("seq.sanctuary")
        return true
    }

    private fun lumberUpRed(access: ProtectedAccess): Boolean {
        return access.lumberUp("spotanim.dragon_smallaxe_swoosh_spotanim")
    }

    private fun lumberUpSilver(access: ProtectedAccess): Boolean {
        return access.lumberUp("spotanim.crystal_smallaxe_swoosh_spotanim")
    }

    private fun ProtectedAccess.lumberUp(spot: String): Boolean {
        statBoost("stat.woodcutting", constant = 3, percent = 0)
        say("Chop chop!")
        anim("seq.dragon_smallaxe_anim")
        spotanim(spot, height = 96, slot = constants.spotanim_slot_combat)
        soundArea(worldRepo, coords, "synth.clobber", radius = 1)
        return true
    }

    private fun fishstabberDragonHarpoon(access: ProtectedAccess): Boolean {
        return access.fishstabber(
            "seq.fishstabber",
            "spotanim.sp_attackglow_red",
        )
    }

    private fun fishstabberDragonHarpoonOr(access: ProtectedAccess): Boolean {
        // Uses the same seq as Infernal harpoon (or).
        return access.fishstabber(
            "seq.fishstabber_trailblazer",
            "spotanim.sp_attackglow_red",
        )
    }

    private fun fishstabberInfernalHarpoon(access: ProtectedAccess): Boolean {
        return access.fishstabber(
            "seq.fishstabber_infernal",
            "spotanim.sp_attackglow_red",
        )
    }

    private fun fishstabberInfernalHarpoonOr(access: ProtectedAccess): Boolean {
        return access.fishstabber(
            "seq.fishstabber_trailblazer",
            "spotanim.sp_attackglow_red",
        )
    }

    private fun fishstabberCrystalHarpoon(access: ProtectedAccess): Boolean {
        return access.fishstabber(
            "seq.fishstabber_crystal",
            "spotanim.sp_attackglow_crystal",
        )
    }

    private fun ProtectedAccess.fishstabber(seq: String, spot: String): Boolean {
        statBoost("stat.fishing", constant = 3, percent = 0)
        say("Here fishy fishies!")
        anim(seq)
        spotanim(spot)
        soundArea(worldRepo, coords, "synth.rampage", radius = 1)
        return true
    }

    private fun rockKnockerDragonPickaxe(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker")
    }

    private fun rockKnockerDragonPickaxeOrTrailblazer(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_trailblazer")
    }

    private fun rockKnockerDragonPickaxeOrZalcano(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_zalcano")
    }

    private fun rockKnockerDragonPickaxeUpgraded(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_pretty")
    }

    private fun rockKnockerInfernalPickaxe(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_infernal")
    }

    private fun rockKnockerInfernalPickaxeOr(access: ProtectedAccess): Boolean {
        // Uses same seq as Dragon pickaxe (or).
        return access.rockKnocker("seq.rockknocker_trailblazer")
    }

    private fun rockKnockerThirdAgePickaxe(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_3a")
    }

    private fun rockKnockerCrystalPickaxe(access: ProtectedAccess): Boolean {
        return access.rockKnocker("seq.rockknocker_crystal")
    }

    private fun ProtectedAccess.rockKnocker(seq: String): Boolean {
        statBoost("stat.mining", constant = 3, percent = 0)
        say("Smashing!")
        anim(seq)
        soundArea(worldRepo, coords, "synth.found_gem", radius = 1)
        return true
    }
}
