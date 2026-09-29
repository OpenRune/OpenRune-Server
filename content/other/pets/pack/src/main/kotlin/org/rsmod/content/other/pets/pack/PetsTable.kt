package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PetsTable {
    const val PET = 0
    const val NAME = 1
    const val CATEGORY = 2
    const val OBJ = 3
    const val NPC = 4
    const val BASE = 5
    const val RUNS = 6
    const val MAIN_DROP = 7
    const val UNLOCK = 8
    const val ITEM_USE = 9
    const val EMOTES = 10

    const val BOSS = 0
    const val SKILLING = 1
    const val OTHER = 2

    private const val LIL_ZIK_MORPHS = "varbit.pet_lil_zik_morphs"
    private const val OLMLET_MORPHS = "varbit.pet_olmlet_morphs"
    private const val MUPHIN_FORMS = "varbit.pet_muphin_forms"
    private const val FUNKY_LOG = "varbit.pet_beaver_funky_log"
    private val GULL_EMOTES = listOf("seq.npc_gryphon_pet_attack01")

    private class Form(
        val obj: String,
        val npc: String,
        val runs: Boolean,
        val unlock: String?,
        val itemUse: Boolean?,
        val emotes: List<String>,
    )

    private fun form(
        obj: String,
        npc: String,
        runs: Boolean = false,
        unlock: String? = null,
        itemUse: Boolean? = null,
        emotes: List<String> = emptyList(),
    ) = Form(obj, npc, runs, unlock, itemUse, emotes)

    fun pets() = dbTable("dbtable.pets", serverOnly = true) {
        column("pet", PET, VarType.STRING)
        column("name", NAME, VarType.STRING)
        column("category", CATEGORY, VarType.INT)
        column("obj", OBJ, VarType.OBJ)
        column("npc", NPC, VarType.NPC)
        column("base", BASE, VarType.BOOLEAN)
        column("runs", RUNS, VarType.BOOLEAN)
        column("main_drop", MAIN_DROP, VarType.BOOLEAN)
        column("unlock", UNLOCK, VarType.INT)
        column("item_use", ITEM_USE, VarType.BOOLEAN)
        column("emotes", EMOTES, VarType.SEQ)

        fun pet(name: String, category: Int, vararg forms: Form, mainDrop: Boolean = false, itemUse: Boolean = false) {
            val key = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
            forms.forEachIndexed { index, form ->
                row("dbrow.pet_${form.obj.removePrefix("obj.")}") {
                    column(PET, key)
                    column(NAME, name)
                    column(CATEGORY, category)
                    columnRSCM(OBJ, form.obj)
                    columnRSCM(NPC, form.npc)
                    column(BASE, index == 0)
                    column(RUNS, form.runs)
                    column(MAIN_DROP, mainDrop)
                    if (form.unlock != null) {
                        columnRSCM(UNLOCK, form.unlock)
                    }
                    column(ITEM_USE, form.itemUse ?: itemUse)
                    if (form.emotes.isNotEmpty()) {
                        columnRSCM(EMOTES, form.emotes)
                    }
                }
            }
        }

        pet("Abyssal orphan", BOSS, form("obj.abyssalsire_pet", "npc.abyssalsire_pet"), mainDrop = true)
        pet("Aggy", BOSS, form("obj.madangelpet", "npc.mad_angel_pet"))
        pet(
            "Baby Mole",
            BOSS,
            form("obj.molepet", "npc.mole_pet"),
            form("obj.molepet_naked", "npc.mole_pet_naked"),
            itemUse = true,
        )
        pet("Baron", BOSS, form("obj.dukesucelluspet", "npc.duke_sucellus_pet"))
        pet(
            "Bran",
            BOSS,
            form("obj.rtbrandapet", "npc.rtbranda_pet"),
            form("obj.rteldricpet", "npc.rteldric_pet"),
        )
        pet("Beef", BOSS, form("obj.cowbosspet", "npc.cowboss_pet"))
        pet(
            "Butch",
            BOSS,
            form(
                "obj.vardorvispet",
                "npc.vardorvis_pet",
                emotes =
                    listOf(
                        "seq.npc_vardorvis_01_melee_01",
                        "seq.npc_vardorvis_01_spawn_01",
                        "seq.npc_vardorvis_01_entangle_start",
                        "seq.npc_vardorvis_01_dash_01",
                        "seq.npc_vardorvis_01_death_01_pet",
                    ),
            ),
        )
        pet(
            "Callisto cub",
            BOSS,
            form("obj.callisto_pet", "npc.callistopet"),
            form("obj.callisto_pet_legacy", "npc.callistopet_legacy"),
        )
        pet("Dom", BOSS, form("obj.dompet", "npc.dom_pet"))
        pet(
            "Gull",
            BOSS,
            form("obj.gryphonbosspet", "npc.gryphonboss_pet", emotes = GULL_EMOTES),
            form(
                "obj.gryphonbosspet_adult",
                "npc.gryphonboss_pet_adult",
                unlock = "varbit.pet_gull_gulliver",
                emotes = GULL_EMOTES,
            ),
        )
        pet("Hellpuppy", BOSS, form("obj.hell_pet", "npc.hellpet"))
        pet("Huberte", BOSS, form("obj.hueypet", "npc.huey_pet"))
        pet(
            "Ikkle Hydra",
            BOSS,
            form("obj.hydrapet", "npc.hydra_pet"),
            form("obj.hydrapet_electric", "npc.hydra_pet_electric"),
            form("obj.hydrapet_fire", "npc.hydra_pet_fire"),
            form("obj.hydrapet_extinguished", "npc.hydra_pet_extinguished"),
        )
        pet(
            "Jal-Nib-Rek",
            BOSS,
            form("obj.infernopet", "npc.inferno_pet"),
            form("obj.infernopet_zuk", "npc.zuk_pet"),
        )
        pet(
            "Kalphite Princess",
            BOSS,
            form("obj.kqpet_walking", "npc.kq_pet_walking"),
            form("obj.kqpet_flying", "npc.kq_pet_flying"),
        )
        pet(
            "Lil' Zik",
            BOSS,
            form("obj.verzikpet", "npc.verzik_pet"),
            form("obj.maidenpet", "npc.verzik_pet_maiden", unlock = LIL_ZIK_MORPHS),
            form("obj.bloatpet", "npc.verzik_pet_bloat", unlock = LIL_ZIK_MORPHS),
            form("obj.nylocaspet", "npc.verzik_pet_nylocas", unlock = LIL_ZIK_MORPHS),
            form("obj.sotetsegpet", "npc.verzik_pet_sotetseg", unlock = LIL_ZIK_MORPHS),
            form("obj.xarpuspet", "npc.verzik_pet_xarpus", unlock = LIL_ZIK_MORPHS),
            itemUse = true,
        )
        pet("Lil'viathan", BOSS, form("obj.leviathanpet", "npc.leviathan_pet"))
        pet(
            "Little Nightmare",
            BOSS,
            form("obj.nightmarepet", "npc.nightmare_pet"),
            form(
                "obj.nightmarepet_parasite",
                "npc.nightmare_pet_parasite",
                unlock = "varbit.nightmare_pet_parasite_unlocked",
            ),
            itemUse = true,
        )
        pet("Maggot marquess", BOSS, form("obj.maggotkingpet", "npc.maggot_king_pet"))
        pet("Moxi", BOSS, form("obj.amoxliatlpet", "npc.amoxliatl_pet"))
        pet(
            "Muphin",
            BOSS,
            form("obj.muspahpet", "npc.muspah_pet"),
            form("obj.muspahpet_melee", "npc.muspah_pet_melee", unlock = MUPHIN_FORMS),
            form("obj.muspahpet_shielded", "npc.muspah_pet_shielded", unlock = MUPHIN_FORMS),
            itemUse = true,
        )
        pet("Nexling", BOSS, form("obj.nexpet", "npc.nex_pet", runs = true))
        pet(
            "Nid",
            BOSS,
            form("obj.araxxorpet", "npc.araxxor_pet"),
            form("obj.araxxorpet_cute", "npc.araxxor_pet_cute", unlock = "varbit.pet_nid_rax"),
            itemUse = true,
        )
        pet(
            "Noon",
            BOSS,
            form("obj.dawnpet", "npc.dawn_pet"),
            form("obj.duskpet", "npc.dusk_pet"),
        )
        pet(
            "Olmlet",
            BOSS,
            form("obj.olmpet", "npc.raids_olm_pet"),
            form("obj.dogadilepet", "npc.dogadile_pet", unlock = OLMLET_MORPHS),
            form("obj.tektonpet", "npc.tekton_pet", unlock = OLMLET_MORPHS),
            form("obj.tektonenragedpet", "npc.tekton_enraged_pet", unlock = OLMLET_MORPHS),
            form("obj.vanguardpet", "npc.vanguard_pet", unlock = OLMLET_MORPHS),
            form("obj.vasapet", "npc.vasa_pet", unlock = OLMLET_MORPHS),
            form("obj.vespulapet", "npc.vespula_pet", unlock = OLMLET_MORPHS),
            form("obj.vespulaflyingpet", "npc.vespula_flying_pet", unlock = OLMLET_MORPHS),
            itemUse = true,
        )
        pet("Pet chaos elemental", BOSS, form("obj.chaoselepet", "npc.chaos_elemental_pet"))
        pet("Pet Dagannoth Prime", BOSS, form("obj.primepet", "npc.prime_pet"))
        pet("Pet Dagannoth Rex", BOSS, form("obj.rexpet", "npc.rex_pet"))
        pet("Pet Dagannoth Supreme", BOSS, form("obj.supremepet", "npc.supreme_pet"))
        pet(
            "Pet dark core",
            BOSS,
            form("obj.corepet", "npc.core_pet"),
            form("obj.corppet", "npc.corp_pet"),
        )
        pet("Pet General Graardor", BOSS, form("obj.bandospet", "npc.bandos_pet"))
        pet("Pet K'ril Tsutsaroth", BOSS, form("obj.zamorakpet", "npc.zamorak_pet"))
        pet("Pet Kraken", BOSS, form("obj.krakenpet", "npc.kraken_pet"))
        pet("Pet Kree'arra", BOSS, form("obj.armadylpet", "npc.armadyl_pet"))
        pet(
            "Pet Smoke Devil",
            BOSS,
            form("obj.smokepet", "npc.smoke_pet"),
            form("obj.smokepet_old", "npc.smoke_pet_old"),
        )
        pet(
            "Pet Snakeling",
            BOSS,
            form("obj.snakepet", "npc.snake_pet_green"),
            form("obj.snakepet_blue", "npc.snake_pet_blue"),
            form("obj.snakepet_orange", "npc.snake_pet_orange"),
        )
        pet("Pet Zilyana", BOSS, form("obj.saradominpet", "npc.saradomin_pet"))
        pet(
            "Phoenix",
            BOSS,
            form("obj.phoenixpet", "npc.phoenix_pet"),
            form("obj.phoenixpet_blue", "npc.phoenix_pet_blue", unlock = "varbit.pet_phoenix_blue"),
            form("obj.phoenixpet_green", "npc.phoenix_pet_green", unlock = "varbit.pet_phoenix_green"),
            form("obj.phoenixpet_purple", "npc.phoenix_pet_purple", unlock = "varbit.pet_phoenix_purple"),
            form("obj.phoenixpet_white", "npc.phoenix_pet_white", unlock = "varbit.pet_phoenix_white"),
        )
        pet("Prince Black Dragon", BOSS, form("obj.kbdpet", "npc.kbd_pet"))
        pet("Scorpia's offspring", BOSS, form("obj.scorpia_pet", "npc.scorpiapet"))
        pet("Scurry", BOSS, form("obj.scurriuspet", "npc.scurrius_pet"))
        pet("Skotos", BOSS, form("obj.skotizopet", "npc.skotizo_pet"))
        pet("Smolcano", BOSS, form("obj.zalcanopet", "npc.zalcano_pet"))
        pet("Smol Heredit", BOSS, form("obj.solhereditpet", "npc.solheredit_pet"), mainDrop = true)
        pet(
            "Sraracha",
            BOSS,
            form("obj.sarachnispet", "npc.sarachnispet"),
            form("obj.sarachnispet_blue", "npc.sarachnispet_blue", unlock = "varbit.sarachnispet_blue_transmog"),
            form("obj.sarachnispet_orange", "npc.sarachnispet_orange", unlock = "varbit.sarachnispet_orange_transmog"),
        )
        pet("Tiny tempor", BOSS, form("obj.temporosspet", "npc.tempoross_pet"))
        pet(
            "Tumeken's guardian",
            BOSS,
            form("obj.wardenpet_tumeken", "npc.warden_pet_tumeken"),
            form("obj.wardenpet_elidinis", "npc.warden_pet_elidinis"),
            form(
                "obj.wardenpet_tumeken_destroyed",
                "npc.warden_pet_tumeken_destroyed",
                unlock = "varbit.toa_unlocked_pet_morph_destroyed_tumeken",
            ),
            form(
                "obj.wardenpet_elidinis_destroyed",
                "npc.warden_pet_elidinis_destroyed",
                unlock = "varbit.toa_unlocked_pet_morph_destroyed_elidinis",
            ),
            form("obj.wardenpet_akkha", "npc.warden_pet_akkha", unlock = "varbit.toa_unlocked_pet_morph_akkha"),
            form("obj.wardenpet_baba", "npc.warden_pet_baba", runs = true, unlock = "varbit.toa_unlocked_pet_morph_baba"),
            form("obj.wardenpet_kephri", "npc.warden_pet_kephri", unlock = "varbit.toa_unlocked_pet_morph_kephri"),
            form(
                "obj.wardenpet_zebak",
                "npc.warden_pet_zebak",
                unlock = "varbit.toa_unlocked_pet_morph_zebak",
                itemUse = false,
            ),
            itemUse = true,
        )
        pet(
            "TzRek-Jad",
            BOSS,
            form("obj.jad_pet", "npc.jadpet"),
            form("obj.jad_pet_inferno", "npc.jadpet_inferno"),
        )
        pet(
            "Venenatis spiderling",
            BOSS,
            form("obj.venenatis_pet", "npc.venenatispet"),
            form("obj.venenatis_pet_legacy", "npc.venenatispet_legacy"),
        )
        pet(
            "Vet'ion Jr.",
            BOSS,
            form("obj.vetion_pet", "npc.vetionpet"),
            form("obj.vetion_pet2", "npc.vetionpet_2"),
            form("obj.vetion_pet_legacy", "npc.vetionpet_legacy"),
            form("obj.vetion_pet2_legacy", "npc.vetionpet_2_legacy"),
        )
        pet("Vorki", BOSS, form("obj.vorkathpet", "npc.vorkath_pet"))
        pet(
            "Wisp",
            BOSS,
            form(
                "obj.whispererpet",
                "npc.whisperer_pet",
                emotes =
                    listOf(
                        "seq.npc_whisperer_01_teleport_02_pet",
                        "seq.npc_whisperer_01_attack_screech_01_end_pet",
                        "seq.npc_whisperer_01_death_02_pet",
                    ),
            ),
        )
        pet(
            "Yami",
            BOSS,
            form(
                "obj.yamapet",
                "npc.yama_pet",
                emotes =
                    listOf(
                        "seq.npc_yama01_stomp01",
                        "seq.npc_yama01_melee01",
                        "seq.npc_yama01_magic01",
                        "seq.npc_yama01_portal01",
                    ),
            ),
        )
        pet(
            "Youngllef",
            BOSS,
            form("obj.gauntletpet", "npc.gauntlet_pet"),
            form("obj.gauntletpet_corrupt", "npc.gauntlet_pet_corrupt"),
        )
        pet(
            "Baby chinchompa",
            SKILLING,
            form("obj.skillpethunter_grey", "npc.skillpet_hunter_grey"),
            form("obj.skillpethunter_red", "npc.skillpet_hunter_red"),
            form("obj.skillpethunter_black", "npc.skillpet_hunter_black"),
            form("obj.skillpethunter_gold", "npc.skillpet_hunter_gold"),
        )
        pet(
            "Beaver",
            SKILLING,
            form("obj.skillpetwc", "npc.skillpetwc"),
            form("obj.skillpet_wc_oak", "npc.skillpet_wc_oak", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_willow", "npc.skillpet_wc_willow", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_maple", "npc.skillpet_wc_maple", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_yew", "npc.skillpet_wc_yew", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_magic", "npc.skillpet_wc_magic", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_redwood", "npc.skillpet_wc_redwood", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_teak", "npc.skillpet_wc_teak", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_mahogany", "npc.skillpet_wc_mahogany", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_arctic", "npc.skillpet_wc_arctic", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_camphor", "npc.skillpet_wc_camphor", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_ironwood", "npc.skillpet_wc_ironwood", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_jatoba", "npc.skillpet_wc_jatoba", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_rosewood", "npc.skillpet_wc_rosewood", unlock = FUNKY_LOG),
            form("obj.skillpet_wc_fox", "npc.skillpet_wc_fox", unlock = "varbit.pet_beaver_fox"),
            form("obj.skillpet_wc_pheasant", "npc.skillpet_wc_pheasant", unlock = "varbit.pet_beaver_pheasant"),
        )
        pet(
            "Giant Squirrel",
            SKILLING,
            form("obj.skillpetagility", "npc.skillpet_agility"),
            form("obj.skillpetagility_dark", "npc.skillpet_agility_dark", unlock = "varbit.hallowed_skillpet_dark_unlocked"),
            form("obj.skillpetagility_bone", "npc.skillpet_agility_bone", unlock = "varbit.agility_skillpet_bone_unlocked"),
        )
        pet(
            "Heron",
            SKILLING,
            form("obj.skillpetfish", "npc.skillpet_fish"),
            form("obj.skillpetfish_tempoross", "npc.skillpet_fish_tempoross"),
        )
        pet(
            "Rift guardian",
            SKILLING,
            form("obj.skillpetrunecrafting_fire", "npc.skillpet_runecrafting_fire"),
            form("obj.skillpetrunecrafting_air", "npc.skillpet_runecrafting_air"),
            form("obj.skillpetrunecrafting_mind", "npc.skillpet_runecrafting_mind"),
            form("obj.skillpetrunecrafting_water", "npc.skillpet_runecrafting_water"),
            form("obj.skillpetrunecrafting_earth", "npc.skillpet_runecrafting_earth"),
            form("obj.skillpetrunecrafting_body", "npc.skillpet_runecrafting_body"),
            form("obj.skillpetrunecrafting_cosmic", "npc.skillpet_runecrafting_cosmic"),
            form("obj.skillpetrunecrafting_chaos", "npc.skillpet_runecrafting_chaos"),
            form("obj.skillpetrunecrafting_nature", "npc.skillpet_runecrafting_nature"),
            form("obj.skillpetrunecrafting_law", "npc.skillpet_runecrafting_law"),
            form("obj.skillpetrunecrafting_death", "npc.skillpet_runecrafting_death"),
            form("obj.skillpetrunecrafting_soul", "npc.skillpet_runecrafting_soul"),
            form("obj.skillpetrunecrafting_astral", "npc.skillpet_runecrafting_astral"),
            form("obj.skillpetrunecrafting_blood", "npc.skillpet_runecrafting_blood"),
            form("obj.skillpetrunecrafting_wrath", "npc.skillpet_runecrafting_wrath"),
            form(
                "obj.skillpetrunecrafting_gotr",
                "npc.skillpet_runecrafting_gotr",
                unlock = "varbit.skillpet_runecrafting_gotr_unlocked",
            ),
            itemUse = true,
        )
        pet(
            "Rock golem",
            SKILLING,
            form("obj.skillpetmining", "npc.skillpet_mining_default"),
            form("obj.skillpetmining_tin", "npc.skillpet_mining_tin"),
            form("obj.skillpetmining_copper", "npc.skillpet_mining_copper"),
            form("obj.skillpetmining_iron", "npc.skillpet_mining_iron"),
            form("obj.skillpetmining_blurite", "npc.skillpet_mining_blurite"),
            form("obj.skillpetmining_silver", "npc.skillpet_mining_silver"),
            form("obj.skillpetmining_coal", "npc.skillpet_mining_coal"),
            form("obj.skillpetmining_gold", "npc.skillpet_mining_gold"),
            form("obj.skillpetmining_mithril", "npc.skillpet_mining_mithril"),
            form("obj.skillpetmining_granite", "npc.skillpet_mining_granite"),
            form("obj.skillpetmining_adamantite", "npc.skillpet_mining_adamantite"),
            form("obj.skillpetmining_runite", "npc.skillpet_mining_runite"),
            form("obj.skillpetmining_amethyst", "npc.skillpet_mining_amethyst"),
            form("obj.skillpetmining_lovakite", "npc.skillpet_mining_lovakite"),
            form("obj.skillpetmining_elemental", "npc.skillpet_mining_elemental"),
            form("obj.skillpetmining_daeyalt", "npc.skillpet_mining_daeyalt"),
            form("obj.skillpetmining_lead", "npc.skillpet_mining_lead"),
            form("obj.skillpetmining_rubium", "npc.skillpet_mining_rubium"),
            form("obj.skillpetmining_nickel", "npc.skillpet_mining_nickel"),
            itemUse = true,
        )
        pet(
            "Rocky",
            SKILLING,
            form("obj.skillpetthieving", "npc.skillpet_thieving"),
            form("obj.skillpetthieving_panda", "npc.skillpet_thieving_panda"),
            form("obj.skillpetthieving_tanuki", "npc.skillpet_thieving_tanuki"),
            itemUse = true,
        )
        pet("Soup", SKILLING, form("obj.skillpetsailing", "npc.skillpet_sailing"))
        pet(
            "Tangleroot",
            SKILLING,
            form("obj.skillpetfarming", "npc.skillpet_farming"),
            form("obj.skillpetfarming_crystal", "npc.skillpet_farming_crystal"),
            form("obj.skillpetfarming_dragon", "npc.skillpet_farming_dragon"),
            form("obj.skillpetfarming_herb", "npc.skillpet_farming_herb"),
            form("obj.skillpetfarming_lily", "npc.skillpet_farming_lily"),
            form("obj.skillpetfarming_redwood", "npc.skillpet_farming_redwood"),
            itemUse = true,
        )
        pet("Abyssal protector", OTHER, form("obj.abyssalpet", "npc.abyssal_pet"))
        pet("Bloodhound", OTHER, form("obj.bloodhound_pet", "npc.bloodhoundpet"))
        pet("Chompy chick", OTHER, form("obj.chompybird_pet", "npc.chompy_bird_pet"))
        pet("Herbi", OTHER, form("obj.herbiboarpet", "npc.herbiboar_pet"))
        pet(
            "Lil' Creator",
            OTHER,
            form("obj.soulwarspet_blue", "npc.soulwars_pet_blue"),
            form("obj.soulwarspet_red", "npc.soulwars_pet_red"),
        )
        pet("Pet Penance Queen", OTHER, form("obj.penancepet", "npc.penance_pet"))
        pet("Quetzin", OTHER, form("obj.quetzalpet", "npc.quetzal_pet"))
        pet("Mr McGroot", OTHER, form("obj.goatpitpet", "npc.goat_pit_pet"))
    }
}
