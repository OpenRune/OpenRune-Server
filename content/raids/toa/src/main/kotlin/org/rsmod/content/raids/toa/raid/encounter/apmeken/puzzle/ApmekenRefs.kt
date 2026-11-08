package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

internal object ApmekenNpcs {
    const val BRAWLER_SMALL = "npc.toa_path_apmeken_baboon_melee_1"
    const val BRAWLER_LARGE = "npc.toa_path_apmeken_baboon_melee_2"
    const val THROWER_SMALL = "npc.toa_path_apmeken_baboon_ranged_1"
    const val THROWER_LARGE = "npc.toa_path_apmeken_baboon_ranged_2"
    const val MAGE_SMALL = "npc.toa_path_apmeken_baboon_magic_1"
    const val MAGE_LARGE = "npc.toa_path_apmeken_baboon_magic_2"
    const val SHAMAN = "npc.toa_path_apmeken_baboon_shaman"
    const val VOLATILE = "npc.toa_path_apmeken_baboon_zombie"
    const val CURSED = "npc.toa_path_apmeken_baboon_cursed"
    const val THRALL = "npc.toa_path_apmeken_baboon_thrall"
    const val EXPLOSION = "npc.spotanim_zebak_jugbreak_npc"

    val ALL =
        listOf(
            BRAWLER_SMALL,
            BRAWLER_LARGE,
            THROWER_SMALL,
            THROWER_LARGE,
            MAGE_SMALL,
            MAGE_LARGE,
            SHAMAN,
            VOLATILE,
            CURSED,
            THRALL,
        )
}

internal object ApmekenLocs {
    const val VENOM_POOL = "loc.toa_path_apmeken_venom_pool"
    const val PILLAR = "loc.toa_path_apmeken_pillar"
    const val PILLAR_SHAKING = "loc.toa_path_apmeken_pillar_shaking"
    const val HAMMERS = "loc.toa_path_apmeken_hammers"
    const val POTIONS = "loc.toa_path_apmeken_potions"
    const val BARRIER = "loc.toa_path_barrier"
    const val BARRIER_OPEN = "loc.invisible_type8_nonblocking"
}

internal object ApmekenObjs {
    const val POTION = "obj.toa_path_apmeken_potion"
    const val HAMMER = "obj.hammer"
    const val DRAGON_WARHAMMER = "obj.dragon_warhammer"
}

internal object ApmekenSeqs {
    const val PICKUP = "seq.human_pickupfloor"
    const val REPAIR = "seq.human_poh_build"
    const val POUR = "seq.farming_ingredient_sprinkle"
    const val MELEE = "seq.npc_mandrill_attack_melee"
    const val RANGED = "seq.npc_mandrill_attack_ranged_02"
    const val MAGIC = "seq.npc_mandrill_attack_magic"
    const val SUMMON = "seq.npc_mandrill_attack_magic_02"
    const val EXPLODE = "seq.npc_mandrill_explode"
}

internal object ApmekenSpots {
    const val GAIN_SIGHT = "spotanim.toa_path_apmeken_gain_sight"
    const val LOSE_SIGHT = "spotanim.toa_path_apmeken_lose_sight"
    const val WARNING_NORTH = "spotanim.toa_path_apmeken_sight_warning"
    const val WARNING_SOUTH = "spotanim.toa_path_apmeken_sight_warning02"
    const val CORRUPTED = "spotanim.toa_path_apmeken_gain_cursed"
    const val GAS = "spotanim.toa_path_apmeken_gas"
    const val RUBBLE = "spotanim.toa_path_apmeken_rubble"
    const val RANGED_SMALL = "spotanim.toa_baboon_ranged_travel_1"
    const val RANGED_LARGE = "spotanim.toa_baboon_ranged_travel_2"
    const val MAGIC = "spotanim.toa_baboon_magic_travel_1"
    const val SUMMON = "spotanim.nex_summon"
    const val EXPLODE_CARRIER = "spotanim.spotanim_mandrill_explode"
    const val EXPLODE_GROUND = "spotanim.fireblast_impact"
}

internal object ApmekenSynths {
    const val SIGHT = 6574
    const val RESOLVED = 2655
    const val CRATE = 2582
    const val REPAIR = 938
    const val POUR = 2401
    const val POUR_TAIL = 6577
    const val ROOF_COLLAPSE = 6583
    const val ROOF_COLLAPSE_TAIL = 6573
    const val VENT_FAILURE = 6569
    const val CORRUPTION_FAILURE = 6570
    const val VENOM_POOL = 3308
    const val VENOM_HIT = 2285
    const val THRALL_APPEAR = 6579
}

internal object ApmekenMidis {
    const val PATH = "midi.toa_path_apmeken"
}

internal object ApmekenVarbits {
    fun sight(slot: Int) = "varbit.toa_client_primary$slot"
}

internal object ApmekenMessages {
    const val SIGHT_GRANTED = "<col=3366ff>You have been granted Apmeken's Sight."
    const val SIGHT_LOST = "<col=3366ff>You no longer have Apmeken's Sight."
    const val ROOF_ISSUE = "<col=a53fff>You sense an issue with the roof supports."
    const val VENT_ISSUE = "<col=a53fff>You sense some strange fumes coming from holes in the floor."
    const val OTHER_ISSUE = "<col=0000b2>You sense an issue somewhere in the room."
    const val CORRUPTION_ISSUE =
        "<col=a53fff>You sense Amascut's corruption beginning to take hold."
    const val CORRUPTION_SUCCESS =
        "<col=3366ff>Apmeken's Sight guides you into neutralising Amascut's corruption."
    const val CORRUPTION_FAILURE =
        "<col=ff3045>Your group is overwhelmed by Amascut's corruption!"
    const val CORRUPTION_WRONG =
        "<col=ff3045>As there was nothing to neutralise, you hurt yourself in process."
    const val ROOF_REPAIRED = "<col=229628>You repair the damaged roof support."
    const val VENT_NEUTRALISED = "<col=229628>You neutralise the fumes coming from the hole."
    const val ROOF_SUCCESS = "<col=3366ff>Apmeken's Sight guides you into repairing the roof supports."
    const val VENT_SUCCESS = "<col=3366ff>Apmeken's Sight guides you into neutralising some dangerous fumes."
    const val ROOF_FAILURE = "<col=ff3045>Damaged roof supports cause some debris to fall on you!"
    const val VENT_FAILURE = "<col=ff3045>The fumes filling the room suddenly ignite!"
    const val ROOF_WRONG =
        "<col=ff3045>Some debris falls on you! The roof support clearly didn't need repairing."
    const val VENT_WRONG =
        "<col=ff3045>Some toxic fumes errupt out of the hole! There was clearly nothing to neutralise."
    const val ROOF_DONE = "This roof support has already been repaired."
    const val VENT_DONE = "This vent has already been neutralised."
    const val NEED_HAMMER = "You need a hammer to repair this roof support."
    const val TAKE_HAMMER = "You take a hammer."
    const val TAKE_POTIONS = "You take some potions."
    const val NO_SPACE = "You need more space in your inventory to do that."
    const val NOTHING_HAPPENS = "You pour the potion on the floor, but nothing happens."
    const val VENOMED = "You have been poisoned by venom!"

    fun sightGrantedTo(name: String) = "<col=3366ff>$name has been granted Apmeken's Sight."

    fun corruptionCured(name: String) = "<col=229628>You neutralise $name's corruption."

    fun corruptionCuredBy(name: String) = "<col=229628>$name has neutralised your corruption."
}
