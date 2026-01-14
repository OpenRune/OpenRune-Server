package org.alter.combat

import org.alter.game.model.item.Item
import org.alter.game.model.item.isAnyType
import org.alter.game.model.item.isType

public object EquipmentChecks {
    public fun isSmokeStaff(obj: Item?): Boolean =
        obj.isAnyType("items.smoke_battlestaff", "items.mystic_smoke_battlestaff", "items.twinflame_staff")

    public fun isSoulreaperAxe(obj: Item?): Boolean = obj.isType("items.soulreaper")

    public fun isTumekensShadow(obj: Item?): Boolean = obj.isType("items.tumekens_shadow")

    public fun isTwistedBow(obj: Item?): Boolean = obj.isType("items.twisted_bow")

    public fun isDragonHunterCrossbow(obj: Item?): Boolean =
        obj.isAnyType(
            "items.dragonhunter_xbow",
            "items.dragonhunter_xbow_vorkath",
            "items.dragonhunter_xbow_kbd",
        )

    public fun isCrystalBow(obj: Item?): Boolean =
        obj.isAnyType(
            "items.crystal_bow",
            "items.bow_of_faerdhinen",
            "items.bow_of_faerdhinen_infinite",
            "items.bow_of_faerdhinen_infinite_ithell",
            "items.bow_of_faerdhinen_infinite_iorwerth",
            "items.bow_of_faerdhinen_infinite_trahaearn",
            "items.bow_of_faerdhinen_infinite_cadarn",
            "items.bow_of_faerdhinen_infinite_crwys",
            "items.bow_of_faerdhinen_infinite_meilyr",
            "items.bow_of_faerdhinen_infinite_amlodd",
        )

    public fun isCrystalHelm(obj: Item?): Boolean =
        obj.isAnyType(
            "items.crystal_helmet_hefin",
            "items.crystal_helmet_ithell",
            "items.crystal_helmet_iorwerth",
            "items.crystal_helmet_trahaearn",
            "items.crystal_helmet_cadarn",
            "items.crystal_helmet_crwys",
            "items.crystal_helmet",
            "items.crystal_helmet_amlodd",
        )

    public fun isCrystalBody(obj: Item?): Boolean =
        obj.isAnyType(
            "items.crystal_chestplate_hefin",
            "items.crystal_chestplate_ithell",
            "items.crystal_chestplate_iorwerth",
            "items.crystal_chestplate_trahaearn",
            "items.crystal_chestplate_cadarn",
            "items.crystal_chestplate_crwys",
            "items.crystal_chestplate",
            "items.crystal_chestplate_amlodd",
        )

    public fun isCrystalLegs(obj: Item?): Boolean =
        obj.isAnyType(
            "items.crystal_platelegs_hefin",
            "items.crystal_platelegs_ithell",
            "items.crystal_platelegs_iorwerth",
            "items.crystal_platelegs_trahaearn",
            "items.crystal_platelegs_cadarn",
            "items.crystal_platelegs_crwys",
            "items.crystal_platelegs",
            "items.crystal_platelegs_amlodd",
        )

    public fun isObsidianSet(helm: Item?, top: Item?, legs: Item?): Boolean =
        helm.isType("items.obsidian_helmet") &&
                top.isType("items.obsidian_platebody") &&
                legs.isType("items.obsidian_platelegs")

    public fun isVirtusMask(obj: Item?): Boolean =
        obj.isAnyType("items.virtus_mask", "items.virtus_mask_ornament")

    public fun isVirtusRobeTop(obj: Item?): Boolean =
        obj.isAnyType("items.virtus_top", "items.virtus_top_ornament")

    public fun isVirtusRobeBottom(obj: Item?): Boolean =
        obj.isAnyType("items.virtus_legs", "items.virtus_legs_ornament")

    public fun isVoidMeleeHelm(obj: Item?): Boolean =
        obj.isAnyType(
            "items.game_pest_melee_helm",
            "items.game_pest_melee_helm_trouver",
            "items.league_3_void_melee_helm",
            "items.league_3_void_melee_helm_trouver",
        )

    public fun isVoidRangerHelm(obj: Item?): Boolean =
        obj.isAnyType(
            "items.game_pest_archer_helm",
            "items.game_pest_archer_helm_trouver",
            "items.league_3_void_range_helm",
            "items.league_3_void_range_helm_trouver",
        )

    public fun isVoidMageHelm(obj: Item?): Boolean =
        obj.isAnyType(
            "items.game_pest_mage_helm",
            "items.game_pest_mage_helm_trouver",
            "items.league_3_void_mage_helm",
            "items.league_3_void_mage_helm_trouver",
        )

    public fun isVoidTop(obj: Item?): Boolean = isRegularVoidTop(obj) || isEliteVoidTop(obj)

    public fun isRegularVoidTop(obj: Item?): Boolean =
        obj.isAnyType(
            "items.pest_void_knight_top",
            "items.pest_void_knight_top_trouver",
            "items.league_3_void_knight_top",
            "items.league_3_void_knight_top_trouver",
        )

    public fun isEliteVoidTop(obj: Item?): Boolean =
        obj.isAnyType(
            "items.elite_void_knight_top",
            "items.elite_void_knight_top_trouver",
            "items.league_3_void_knight_top_elite",
            "items.league_3_void_knight_top_elite_trouver",
        )

    public fun isVoidRobe(obj: Item?): Boolean = isRegularVoidRobe(obj) || isEliteVoidRobe(obj)

    public fun isRegularVoidRobe(obj: Item?): Boolean =
        obj.isAnyType(
            "items.pest_void_knight_robes",
            "items.pest_void_knight_robes_trouver",
            "items.league_3_void_knight_robes",
            "items.league_3_void_knight_robes_trouver",
        )

    public fun isEliteVoidRobe(obj: Item?): Boolean =
        obj.isAnyType(
            "items.elite_void_knight_robes",
            "items.elite_void_knight_robes_trouver",
            "items.league_3_void_knight_robes_elite",
            "items.league_3_void_knight_robes_elite_trouver",
        )

    public fun isVoidGloves(obj: Item?): Boolean =
        obj.isAnyType(
            "items.pest_void_knight_gloves",
            "items.pest_void_knight_gloves_trouver",
            "items.league_3_void_knight_gloves",
            "items.league_3_void_knight_gloves_trouver",
        )

    public fun isDharokSet(helm: Item?, top: Item?, legs: Item?, weapon: Item?): Boolean =
        helm.isAnyType(
            "items.barrows_dharok_head_100",
            "items.barrows_dharok_head_75",
            "items.barrows_dharok_head_50",
            "items.barrows_dharok_head_25",
        ) &&
                top.isAnyType(
                    "items.barrows_dharok_body_100",
                    "items.barrows_dharok_body_75",
                    "items.barrows_dharok_body_50",
                    "items.barrows_dharok_body_25",
                ) &&
                legs.isAnyType(
                    "items.barrows_dharok_legs_100",
                    "items.barrows_dharok_legs_75",
                    "items.barrows_dharok_legs_50",
                    "items.barrows_dharok_legs_25",
                ) &&
                weapon.isAnyType(
                    "items.barrows_dharok_weapon_100",
                    "items.barrows_dharok_weapon_75",
                    "items.barrows_dharok_weapon_50",
                    "items.barrows_dharok_weapon_25",
                )

    public fun isToragSet(helm: Item?, top: Item?, legs: Item?, weapon: Item?): Boolean =
        helm.isAnyType(
            "items.barrows_torag_head_100",
            "items.barrows_torag_head_75",
            "items.barrows_torag_head_50",
            "items.barrows_torag_head_25",
        ) &&
                top.isAnyType(
                    "items.barrows_torag_body_100",
                    "items.barrows_torag_body_75",
                    "items.barrows_torag_body_50",
                    "items.barrows_torag_body_25",
                ) &&
                legs.isAnyType(
                    "items.barrows_torag_legs_100",
                    "items.barrows_torag_legs_75",
                    "items.barrows_torag_legs_50",
                    "items.barrows_torag_legs_25",
                ) &&
                weapon.isAnyType(
                    "items.barrows_torag_weapon_100",
                    "items.barrows_torag_weapon_75",
                    "items.barrows_torag_weapon_50",
                    "items.barrows_torag_weapon_25",
                )

    public fun isAhrimSet(helm: Item?, top: Item?, legs: Item?, weapon: Item?): Boolean =
        helm.isAnyType(
            "items.barrows_ahrim_head_100",
            "items.barrows_ahrim_head_75",
            "items.barrows_ahrim_head_50",
            "items.barrows_ahrim_head_25",
        ) &&
                top.isAnyType(
                    "items.barrows_ahrim_body_100",
                    "items.barrows_ahrim_body_75",
                    "items.barrows_ahrim_body_50",
                    "items.barrows_ahrim_body_25",
                ) &&
                legs.isAnyType(
                    "items.barrows_ahrim_legs_100",
                    "items.barrows_ahrim_legs_75",
                    "items.barrows_ahrim_legs_50",
                    "items.barrows_ahrim_legs_25",
                ) &&
                weapon.isAnyType(
                    "items.barrows_ahrim_weapon_100",
                    "items.barrows_ahrim_weapon_75",
                    "items.barrows_ahrim_weapon_50",
                    "items.barrows_ahrim_weapon_25",
                )

    public fun isJusticiarSet(helm: Item?, top: Item?, legs: Item?): Boolean =
        helm.isType("items.justiciar_faceguard") &&
                top.isType("items.justiciar_chestguard") &&
                legs.isType("items.justiciar_leg_guards")
}
