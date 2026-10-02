package org.rsmod.content.raids.toa.lobby

internal object LobbyLocs {
    const val SACK = "loc.toa_sack_full"
    const val SHROUD_CHEST = "loc.toa_lobby_cape_chest"
    const val SCOREBOARD = "loc.toa_scoreboard"
}

internal object LobbyNpcs {
    const val BANK_CAMEL = "npc.toa_bank_camel"
}

internal object LobbyObjs {
    const val CAMULET = "obj.camulet"
    const val NEEDLE = "obj.needle"
    const val GRAIN = "obj.toa_grain"
}

internal object LobbySack {
    const val STAT = "stat.thieving"
    const val LOW = 1
    const val HIGH = 10
    const val CAMEL_TITLE = "Bank Camel"
}

internal class ShroudReward(val obj: String, val completions: Int, val option: String)

internal object LobbyShrouds {
    val REWARDS =
        listOf(
            ShroudReward("obj.icthlarins_shroud_1", 100, "Take Icthlarin's shroud (tier 1)."),
            ShroudReward("obj.icthlarins_shroud_2", 500, "Take Icthlarin's shroud (tier 2)."),
            ShroudReward("obj.icthlarins_shroud_3", 1000, "Take Icthlarin's shroud (tier 3)."),
            ShroudReward("obj.icthlarins_shroud_4", 1500, "Take Icthlarin's shroud (tier 4)."),
            ShroudReward("obj.icthlarins_shroud_5", 2000, "Take Icthlarin's shroud (tier 5)."),
            ShroudReward("obj.icthlarins_hood", 2000, "Take Icthlarin's hood (tier 5)."),
        )
    const val MAX_CHOICES = 5
    const val MORE = "More..."
    const val TAKEN = "You take a mysterious shroud from the chest."
    const val NONE_UNLOCKED = "There doesn't seem to be anything inside."
    const val NO_SPACE = "You don't have enough inventory space."
}

internal object LobbyScoreboard {
    const val INTERFACE = "interface.toa_scoreboard"
    const val TAB_VARBIT = "varbit.toa_scoreboard_tab"
    val TABS =
        listOf(
            "component.toa_scoreboard:normal_tab",
            "component.toa_scoreboard:story_tab",
            "component.toa_scoreboard:hard_tab",
        )
    val MODES = listOf("Normal", "Entry", "Expert")
    const val ATTEMPTS = "component.toa_scoreboard:data_a_p"
    const val COMPLETIONS = "component.toa_scoreboard:data_c_p"
    const val DEATHS = "component.toa_scoreboard:data_d_p"
    const val NO_TIME = "-"
    const val CENTIS_PER_TICK = 60
    const val CENTIS_PER_SECOND = 100

    fun challenge(size: Int): String = "component.toa_scoreboard:data_${row(size)}_p_r"

    fun overall(size: Int): String = "component.toa_scoreboard:data_${row(size)}_p_o"

    private fun row(size: Int): String = if (size == 1) "solo" else "${size}man"
}

internal object LobbyCavity {
    val LOCS =
        listOf(
            "loc.toa_lobby_wall02_cavity_pickaxe_empty01",
            "loc.toa_lobby_wall02_cavity_pickaxe_iron01",
            "loc.toa_lobby_wall02_cavity_pickaxe_steel01",
            "loc.toa_lobby_wall02_cavity_pickaxe_black01",
            "loc.toa_lobby_wall02_cavity_pickaxe_mithril01",
            "loc.toa_lobby_wall02_cavity_pickaxe_adamant01",
            "loc.toa_lobby_wall02_cavity_pickaxe_rune01",
            "loc.toa_lobby_wall02_cavity_pickaxe_gilded01",
            "loc.toa_lobby_wall02_cavity_pickaxe_dragon01",
            "loc.toa_lobby_wall02_cavity_pickaxe_dragon02",
            "loc.toa_lobby_wall02_cavity_pickaxe_dragon03",
            "loc.toa_lobby_wall02_cavity_pickaxe_3rdage01",
            "loc.toa_lobby_wall02_cavity_pickaxe_infernal01",
            "loc.toa_lobby_wall02_cavity_pickaxe_crystal01",
            "loc.toa_lobby_wall02_cavity_pickaxe_trailblazer01",
            "loc.toa_lobby_wall02_cavity_pickaxe_leagues",
            "loc.toa_lobby_wall02_cavity_pickaxe_trailblazer_reloaded",
        )
    const val STORED = "varbit.toa_pickaxe_stored"
    const val BRONZE = "obj.bronze_pickaxe"
    const val ANIM = "seq.human_pickuptable"
    const val TAKEN = "You take the %s."
    const val PLACED = "You place down the %s."
    const val ALREADY_HELD = "You already have a pickaxe."
    const val NO_SPACE = "You don't have any space in your inventory."
    const val NOTHING_TO_DEPOSIT = "You don't have anything to deposit."
    const val ALREADY_STORED = "There's already a pickaxe stored here."
    const val NOTHING_INTERESTING = "Nothing interesting happens."
    const val BRONZE_REFUSED = "There's no point storing a bronze pickaxe here. Het will provide."
}

internal object ToaPickaxeStorage {
    private val PICKAXES =
        listOf(
            "obj.iron_pickaxe",
            "obj.steel_pickaxe",
            "obj.black_pickaxe",
            "obj.mithril_pickaxe",
            "obj.adamant_pickaxe",
            "obj.rune_pickaxe",
            "obj.trail_gilded_pickaxe",
            "obj.dragon_pickaxe",
            "obj.dragon_pickaxe_pretty",
            "obj.zalcano_pickaxe",
            "obj.infernal_pickaxe",
            "obj.infernal_pickaxe_empty",
            "obj.3a_pickaxe",
            "obj.crystal_pickaxe",
            "obj.crystal_pickaxe_inactive",
            "obj.league_trailblazer_pickaxe",
            "obj.trailblazer_pickaxe",
            "obj.trailblazer_pickaxe_empty",
            "obj.trailblazer_pickaxe_no_infernal",
            "obj.trailblazer_reloaded_pickaxe",
            "obj.trailblazer_reloaded_pickaxe_empty",
            "obj.trailblazer_reloaded_pickaxe_no_infernal",
        )

    fun obj(value: Int): String? = PICKAXES.getOrNull(value - 1)

    fun value(obj: String): Int = PICKAXES.indexOf(obj) + 1
}
