package org.rsmod.api.player.worn

import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

public object EquipmentChecks {
    public fun isSmokeStaff(obj: InvObj?): Boolean =
        obj.isAnyType("obj.smoke_battlestaff", "obj.mystic_smoke_battlestaff", "obj.twinflame_staff")

    public fun isSoulreaperAxe(obj: InvObj?): Boolean = obj.isType("obj.soulreaper")

    public fun isTumekensShadow(obj: InvObj?): Boolean = obj.isType("obj.tumekens_shadow")

    public fun isTwistedBow(obj: InvObj?): Boolean = obj.isType("obj.twisted_bow")

    public fun isDragonHunterCrossbow(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.dragonhunter_xbow",
            "obj.dragonhunter_xbow_vorkath",
            "obj.dragonhunter_xbow_kbd",
        )

    public fun isCrystalBow(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.crystal_bow",
            "obj.bow_of_faerdhinen",
            "obj.bow_of_faerdhinen_infinite",
            "obj.bow_of_faerdhinen_infinite_ithell",
            "obj.bow_of_faerdhinen_infinite_iorwerth",
            "obj.bow_of_faerdhinen_infinite_trahaearn",
            "obj.bow_of_faerdhinen_infinite_cadarn",
            "obj.bow_of_faerdhinen_infinite_crwys",
            "obj.bow_of_faerdhinen_infinite_meilyr",
            "obj.bow_of_faerdhinen_infinite_amlodd",
        )

    public fun isCrystalHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.crystal_helmet_hefin",
            "obj.crystal_helmet_ithell",
            "obj.crystal_helmet_iorwerth",
            "obj.crystal_helmet_trahaearn",
            "obj.crystal_helmet_cadarn",
            "obj.crystal_helmet_crwys",
            "obj.crystal_helmet",
            "obj.crystal_helmet_amlodd",
        )

    public fun isCrystalBody(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.crystal_chestplate_hefin",
            "obj.crystal_chestplate_ithell",
            "obj.crystal_chestplate_iorwerth",
            "obj.crystal_chestplate_trahaearn",
            "obj.crystal_chestplate_cadarn",
            "obj.crystal_chestplate_crwys",
            "obj.crystal_chestplate",
            "obj.crystal_chestplate_amlodd",
        )

    public fun isCrystalLegs(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.crystal_platelegs_hefin",
            "obj.crystal_platelegs_ithell",
            "obj.crystal_platelegs_iorwerth",
            "obj.crystal_platelegs_trahaearn",
            "obj.crystal_platelegs_cadarn",
            "obj.crystal_platelegs_crwys",
            "obj.crystal_platelegs",
            "obj.crystal_platelegs_amlodd",
        )

    public fun isObsidianSet(helm: InvObj?, top: InvObj?, legs: InvObj?): Boolean =
        helm.isType("obj.obsidian_helmet") &&
            top.isType("obj.obsidian_platebody") &&
            legs.isType("obj.obsidian_platelegs")

    public fun isVirtusMask(obj: InvObj?): Boolean =
        obj.isAnyType("obj.virtus_mask", "obj.virtus_mask_ornament")

    public fun isVirtusRobeTop(obj: InvObj?): Boolean =
        obj.isAnyType("obj.virtus_top", "obj.virtus_top_ornament")

    public fun isVirtusRobeBottom(obj: InvObj?): Boolean =
        obj.isAnyType("obj.virtus_legs", "obj.virtus_legs_ornament")

    public fun isVoidMeleeHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.game_pest_melee_helm",
            "obj.game_pest_melee_helm_trouver",
            "obj.league_3_void_melee_helm",
            "obj.league_3_void_melee_helm_trouver",
        )

    public fun isVoidRangerHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.game_pest_archer_helm",
            "obj.game_pest_archer_helm_trouver",
            "obj.league_3_void_range_helm",
            "obj.league_3_void_range_helm_trouver",
        )

    public fun isVoidMageHelm(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.game_pest_mage_helm",
            "obj.game_pest_mage_helm_trouver",
            "obj.league_3_void_mage_helm",
            "obj.league_3_void_mage_helm_trouver",
        )

    public fun isVoidTop(obj: InvObj?): Boolean = isRegularVoidTop(obj) || isEliteVoidTop(obj)

    public fun isRegularVoidTop(obj: InvObj?): Boolean =
        obj.isAnyType("obj.pest_void_knight_top", "obj.pest_void_knight_top_trouver", "obj.league_3_void_knight_top", "obj.league_3_void_knight_top_trouver")

    public fun isEliteVoidTop(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.elite_void_knight_top",
            "obj.elite_void_knight_top_trouver",
            "obj.league_3_void_knight_top_elite",
            "obj.league_3_void_knight_top_elite_trouver",
        )

    public fun isVoidRobe(obj: InvObj?): Boolean = isRegularVoidRobe(obj) || isEliteVoidRobe(obj)

    public fun isRegularVoidRobe(obj: InvObj?): Boolean =
        obj.isAnyType("obj.pest_void_knight_robes", "obj.pest_void_knight_robes_trouver", "obj.league_3_void_knight_robes", "obj.league_3_void_knight_robes_trouver")

    public fun isEliteVoidRobe(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.elite_void_knight_robes",
            "obj.elite_void_knight_robes_trouver",
            "obj.league_3_void_knight_robes_elite",
            "obj.league_3_void_knight_robes_elite_trouver",
        )

    public fun isVoidGloves(obj: InvObj?): Boolean =
        obj.isAnyType(
            "obj.pest_void_knight_gloves",
            "obj.pest_void_knight_gloves_trouver",
            "obj.league_3_void_knight_gloves",
            "obj.league_3_void_knight_gloves_trouver",
        )

    public fun isDharokSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            "obj.barrows_dharok_head_100",
            "obj.barrows_dharok_head_75",
            "obj.barrows_dharok_head_50",
            "obj.barrows_dharok_head_25",
        ) &&
            top.isAnyType(
                "obj.barrows_dharok_body_100",
                "obj.barrows_dharok_body_75",
                "obj.barrows_dharok_body_50",
                "obj.barrows_dharok_body_25",
            ) &&
            legs.isAnyType(
                "obj.barrows_dharok_legs_100",
                "obj.barrows_dharok_legs_75",
                "obj.barrows_dharok_legs_50",
                "obj.barrows_dharok_legs_25",
            ) &&
            weapon.isAnyType(
                "obj.barrows_dharok_weapon_100",
                "obj.barrows_dharok_weapon_75",
                "obj.barrows_dharok_weapon_50",
                "obj.barrows_dharok_weapon_25",
            )

    public fun isToragSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            "obj.barrows_torag_head_100",
            "obj.barrows_torag_head_75",
            "obj.barrows_torag_head_50",
            "obj.barrows_torag_head_25",
        ) &&
            top.isAnyType(
                "obj.barrows_torag_body_100",
                "obj.barrows_torag_body_75",
                "obj.barrows_torag_body_50",
                "obj.barrows_torag_body_25",
            ) &&
            legs.isAnyType(
                "obj.barrows_torag_legs_100",
                "obj.barrows_torag_legs_75",
                "obj.barrows_torag_legs_50",
                "obj.barrows_torag_legs_25",
            ) &&
            weapon.isAnyType(
                "obj.barrows_torag_weapon_100",
                "obj.barrows_torag_weapon_75",
                "obj.barrows_torag_weapon_50",
                "obj.barrows_torag_weapon_25",
            )

    public fun isAhrimSet(helm: InvObj?, top: InvObj?, legs: InvObj?, weapon: InvObj?): Boolean =
        helm.isAnyType(
            "obj.barrows_ahrim_head_100",
            "obj.barrows_ahrim_head_75",
            "obj.barrows_ahrim_head_50",
            "obj.barrows_ahrim_head_25",
        ) &&
            top.isAnyType(
                "obj.barrows_ahrim_body_100",
                "obj.barrows_ahrim_body_75",
                "obj.barrows_ahrim_body_50",
                "obj.barrows_ahrim_body_25",
            ) &&
            legs.isAnyType(
                "obj.barrows_ahrim_legs_100",
                "obj.barrows_ahrim_legs_75",
                "obj.barrows_ahrim_legs_50",
                "obj.barrows_ahrim_legs_25",
            ) &&
            weapon.isAnyType(
                "obj.barrows_ahrim_weapon_100",
                "obj.barrows_ahrim_weapon_75",
                "obj.barrows_ahrim_weapon_50",
                "obj.barrows_ahrim_weapon_25",
            )

    public fun isJusticiarSet(helm: InvObj?, top: InvObj?, legs: InvObj?): Boolean =
        helm.isType("obj.justiciar_faceguard") &&
            top.isType("obj.justiciar_chestguard") &&
            legs.isType("obj.justiciar_leg_guards")
}
