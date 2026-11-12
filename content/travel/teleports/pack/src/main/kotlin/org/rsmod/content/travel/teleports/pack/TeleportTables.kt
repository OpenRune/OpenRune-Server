package org.rsmod.content.travel.teleports.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Where teleports go, and the items that take the player there.
 *
 * `teleport_destination` is shared by every travel network: each row is one stop on a named
 * network (`glider`, `charter`, `minigame`, ...), ordered the way the network lists them.
 * For items the order is the order of the item's sub-menu and worn ops, so a destination's
 * ordinal is the index the client sends back when that entry is clicked.
 *
 * `teleport_item` is one row per family of items sharing a network and a way of spending charges:
 * - a charge chain (`objs` from most to fewest charges) that steps down one obj per teleport, into
 *   `uncharged` or to dust when there is none;
 * - `unlimited` items that never change;
 * - items whose charges live in `charge_varbit`.
 *
 * Coordinates come from the OSRS wiki teleport tables; destinations that need a quest or unlock in
 * OSRS are open to everyone here (see docs/quirks.md).
 */
object TeleportTables {
    const val DEST_NETWORK = 0
    const val DEST_ORDINAL = 1
    const val DEST_NAME = 2
    const val DEST_COORDS = 3
    const val DEST_WILDERNESS = 4

    const val ITEM_OBJS = 0
    const val ITEM_NETWORK = 1
    const val ITEM_LABEL = 2
    const val ITEM_TELEPORT_TYPE = 3
    const val ITEM_UNCHARGED = 4
    const val ITEM_UNLIMITED = 5
    const val ITEM_CHARGE_VARBIT = 6
    const val ITEM_MAX_CHARGES = 7
    const val ITEM_DEPART_MESSAGE = 8
    const val ITEM_ARRIVE_MESSAGE = 9

    class Destination(
        val name: String,
        val x: Int,
        val z: Int,
        val level: Int = 0,
        val wilderness: Boolean = false,
    )

    class Item(
        val key: String,
        val network: String,
        val label: String,
        val objs: List<String>,
        val teleportType: String = STANDARD,
        val uncharged: String? = null,
        val unlimited: Boolean = false,
        val chargeVarbit: String? = null,
        val maxCharges: Int? = null,
        val departMessage: String? = null,
        val arriveMessage: String? = null,
    )

    private const val STANDARD = "Standard"
    private const val LEVEL_30 = "MemberLevel30"

    val NETWORKS: Map<String, List<Destination>> =
        linkedMapOf(
            "memoirs" to
                listOf(
                    Destination("Lunch by the Lancalliums", 1713, 3612),
                    Destination("The Fisher's Flute", 1804, 3748),
                    Destination("History and Hearsay", 1478, 3576),
                    Destination("Jewellery of Jubilation", 1545, 3761),
                    Destination("A Dark Disposition", 1681, 3747),
                ),
            "drakans_medallion" to
                listOf(
                    Destination("Ver Sinhaza", 3649, 3230),
                    Destination("Darkmeyer", 3592, 3337),
                    Destination("Slepe", 3808, 9700, level = 1),
                    Destination("Castle Drakan", 3166, 7711),
                ),
            "ardougne_cloak_easy" to listOf(Destination("Kandarin Monastery", 2607, 3221)),
            "ardougne_cloak" to
                listOf(
                    Destination("Kandarin Monastery", 2607, 3221),
                    Destination("Ardougne Farm", 2665, 3376),
                ),
            "royal_seed_pod" to listOf(Destination("Grand Tree", 2465, 3495)),
            "ectophial" to listOf(Destination("Ectofuntus", 3659, 3522)),
            "karamja_gloves_3" to listOf(Destination("Gem Mine", 2840, 9387)),
            "karamja_gloves_4" to
                listOf(
                    Destination("Gem Mine", 2840, 9387),
                    Destination("Slayer Master", 2869, 2982, level = 1),
                ),
            "desert_amulet" to listOf(Destination("Nardah", 3420, 2917)),
            "desert_amulet_4" to
                listOf(
                    Destination("Nardah", 3426, 2927),
                    Destination("Kalphite cave", 3322, 3122),
                ),
            "morytania_legs" to listOf(Destination("Ectofuntus Pit", 3683, 9888)),
            "morytania_legs_3" to
                listOf(
                    Destination("Ectofuntus Pit", 3683, 9888),
                    Destination("Burgh de Rott", 3482, 3230),
                ),
            "fremennik_sea_boots" to listOf(Destination("Rellekka", 2643, 3675)),
            "kandarin_headgear" to listOf(Destination("Sherlock", 2730, 3420)),
            "wilderness_sword" to
                listOf(Destination("Fountain of Rune", 3377, 3890, wilderness = true)),
            "western_banner" to listOf(Destination("Piscatoris Fishing Colony", 2332, 3681)),
            "explorers_ring" to listOf(Destination("Cabbage patch", 3052, 3291)),
            "radas_blessing" to listOf(Destination("Kourend Woodland", 1554, 3461)),
            "radas_blessing_3" to
                listOf(
                    Destination("Kourend Woodland", 1554, 3461),
                    Destination("Mount Karuulm", 1310, 3796),
                ),
            "wilderness_obelisk" to
                listOf(
                    Destination("Level 13", 3156, 3620, wilderness = true),
                    Destination("Level 19", 3227, 3667, wilderness = true),
                    Destination("Level 27", 3035, 3732, wilderness = true),
                    Destination("Level 35", 3106, 3794, wilderness = true),
                    Destination("Level 44", 2980, 3866, wilderness = true),
                    Destination("Level 50", 3307, 3916, wilderness = true),
                ),
            "glider" to
                listOf(
                    Destination("Ta Quir Priw", 2465, 3501, level = 3),
                    Destination("Gandius", 2971, 2968),
                    Destination("Kar-Hewo", 3284, 3210),
                    Destination("Lemanto Andra", 3321, 3429),
                    Destination("Sindarpos", 2850, 3498),
                    Destination("Lemantolly Undri", 2550, 2970),
                    Destination("Ookookolly Undri", 2711, 2803),
                ),
            "charter" to
                listOf(
                    Destination("Port Sarim", 3038, 3192),
                    Destination("Brimhaven", 2760, 3238),
                    Destination("Catherby", 2792, 3414),
                    Destination("Mos Le'Harmless", 3671, 2931),
                    Destination("Musa Point", 2954, 3158),
                    Destination("Port Khazard", 2674, 3144),
                    Destination("Port Phasmatys", 3702, 3503),
                    Destination("Shipyard", 3001, 3032),
                    Destination("Port Tyras", 2142, 3122),
                    Destination("Corsair Cove", 2589, 2851),
                    Destination("Prifddinas", 2157, 3330),
                    Destination("Port Piscarilius", 1808, 3679),
                    Destination("Land's End", 1496, 3403),
                    Destination("Civitas illa Fortis", 1743, 3136),
                    Destination("Aldarin", 1455, 2968),
                    Destination("Sunset Coast", 1514, 2971),
                    Destination("The Pandemonium", 3064, 2999),
                    Destination("The Summer Shore", 3189, 2369),
                    Destination("Red Rock", 2805, 2502),
                    Destination("Deepfin Point", 1943, 2757),
                    Destination("Port Roberts", 1871, 3309),
                ),
            "carpet_shantay" to
                listOf(
                    Destination("Shantay Pass", 3311, 3109),
                    Destination("Uzer", 3467, 3110),
                    Destination("Bedabin Camp", 3182, 3043),
                    Destination("Pollnivneach", 3350, 3001),
                ),
            "carpet_pollnivneach" to
                listOf(
                    Destination("Pollnivneach", 3348, 2942),
                    Destination("Nardah", 3401, 2918),
                    Destination("Sophanem", 3287, 2813),
                    Destination("Menaphos", 3243, 2813),
                ),
            "lovakengj_minecart" to
                listOf(
                    Destination("Arceuus", 1670, 3833),
                    Destination("Farming Guild", 1218, 3737),
                    Destination("Hosidius South", 1808, 3479),
                    Destination("Hosidius West", 1655, 3543),
                    Destination("Kingstown", 1699, 3660),
                    Destination("Kourend Woodland", 1572, 3466),
                    Destination("Lovakengj", 1518, 3733),
                    Destination("Mount Quidamortem", 1255, 3548),
                    Destination("Northern Tundras", 1648, 3931),
                    Destination("Port Piscarilius", 1761, 3710),
                    Destination("Shayzien East", 1590, 3620),
                    Destination("Shayzien West", 1415, 3577),
                ),
            "keldagrim_cart" to
                listOf(
                    Destination("Keldagrim", 2909, 10174),
                    Destination("Grand Exchange", 3141, 3504),
                    Destination("Ice Mountain", 2995, 9835),
                    Destination("White Wolf Mountain", 2876, 9868),
                ),
            "minigame" to
                listOf(
                    Destination("Barbarian Assault", 2531, 3577),
                    Destination("Blast Furnace", 2934, 10184),
                    Destination("Burthorpe Games Room", 2899, 3553),
                    Destination("Bounty Hunter", 3140, 3626),
                    Destination("Castle Wars", 2442, 3093),
                    Destination("Clan Wars", 3150, 3635),
                    Destination("Fishing Trawler", 2658, 3157),
                    Destination("Giants' Foundry", 3361, 3149),
                    Destination("Guardians of the Rift", 3614, 9477),
                    Destination("Last Man Standing", 3150, 3635),
                    Destination("Mage Training Arena", 3363, 3295),
                    Destination("Mastering Mixology", 1388, 2918),
                    Destination("Nightmare Zone", 2609, 3114),
                    Destination("Pest Control", 2658, 2663),
                    Destination("Rat Pits", 2561, 3319),
                    Destination("Shades of Mort'ton", 3506, 3304),
                    Destination("Sorceress's Garden", 3319, 3140),
                    Destination("Soul Wars", 2018, 5929),
                    Destination("Tithe Farm", 1792, 3501),
                    Destination("Trouble Brewing", 3801, 3015),
                    Destination("TzHaar Fight Pit", 2399, 5177),
                ),
            "camulet" to
                listOf(
                    Destination("Enakhra's Temple", 3105, 9315, level = 2),
                    Destination("Enakhra's Temple Entrance", 3191, 2924),
                ),
            "grand_seed_pod" to listOf(Destination("Grand Tree", 2465, 3495)),
        )

    val ITEMS: List<Item> =
        listOf(
            Item(
                key = "memoirs",
                network = "memoirs",
                label = "book",
                objs = listOf("obj.veos_kharedsts_memoirs"),
                chargeVarbit = "varbit.veos_memoir_charges",
                maxCharges = 100,
            ),
            Item(
                key = "book_of_the_dead",
                network = "memoirs",
                label = "book",
                objs = listOf("obj.book_of_the_dead"),
                chargeVarbit = "varbit.veos_memoir_charges",
                maxCharges = 250,
            ),
            Item(
                key = "drakans_medallion",
                network = "drakans_medallion",
                label = "medallion",
                objs = listOf("obj.drakans_medallion"),
                unlimited = true,
            ),
            Item(
                key = "ardougne_cloak_easy",
                network = "ardougne_cloak_easy",
                label = "cloak",
                objs = listOf("obj.ardy_cape_easy"),
                unlimited = true,
            ),
            Item(
                key = "ardougne_cloak",
                network = "ardougne_cloak",
                label = "cloak",
                objs = listOf("obj.ardy_cape_medium", "obj.ardy_cape_hard", "obj.ardy_cape_elite"),
                unlimited = true,
            ),
            Item(
                key = "royal_seed_pod",
                network = "royal_seed_pod",
                label = "seed pod",
                objs = listOf("obj.mm2_royal_seed_pod"),
                unlimited = true,
                teleportType = LEVEL_30,
                arriveMessage = "The Royal seed pod teleports you to the Grand Tree.",
            ),
            Item(
                key = "ectophial",
                network = "ectophial",
                label = "ectophial",
                objs = listOf("obj.ectophial"),
                unlimited = true,
                departMessage = "You empty the ectoplasm onto the ground around your feet...",
                arriveMessage = "...and the world changes around you.",
            ),
            diary("karamja_gloves_3", "gloves", "obj.atjun_gloves_hard"),
            diary("karamja_gloves_4", "gloves", "obj.atjun_gloves_elite"),
            diary("desert_amulet", "amulet", "obj.desert_amulet_medium", "obj.desert_amulet_hard"),
            diary("desert_amulet_4", "amulet", "obj.desert_amulet_elite"),
            diary("morytania_legs", "legs", "obj.morytania_legs_easy", "obj.morytania_legs_medium"),
            diary(
                "morytania_legs_3",
                "legs",
                "obj.morytania_legs_hard",
                "obj.morytania_legs_elite",
            ),
            diary(
                "fremennik_sea_boots",
                "boots",
                "obj.fremennik_boots_easy",
                "obj.fremennik_boots_medium",
                "obj.fremennik_boots_hard",
                "obj.fremennik_boots_elite",
            ),
            diary(
                "kandarin_headgear",
                "headgear",
                "obj.seers_headband_hard",
                "obj.seers_headband_elite",
            ),
            diary(
                "wilderness_sword",
                "sword",
                "obj.wilderness_sword_hard",
                "obj.wilderness_sword_elite",
            ),
            diary(
                "western_banner",
                "banner",
                "obj.western_banner_hard",
                "obj.western_banner_elite",
            ),
            diary(
                "explorers_ring",
                "ring",
                "obj.lumbridge_ring_medium",
                "obj.lumbridge_ring_hard",
                "obj.lumbridge_ring_elite",
            ),
            diary(
                "radas_blessing",
                "blessing",
                "obj.zeah_blessing_easy",
                "obj.zeah_blessing_medium",
            ),
            diary(
                "radas_blessing_3",
                "blessing",
                "obj.zeah_blessing_hard",
                "obj.zeah_blessing_elite",
            ),
            Item(
                key = "camulet",
                network = "camulet",
                label = "camulet",
                objs = listOf("obj.camulet"),
                unlimited = true,
            ),
            Item(
                key = "grand_seed_pod",
                network = "grand_seed_pod",
                label = "seed pod",
                objs = listOf("obj.aluft_seed_pod"),
                teleportType = LEVEL_30,
            ),
        )

    /** An achievement diary reward whose teleports, like every tier here, have no daily limit. */
    private fun diary(network: String, label: String, vararg objs: String) =
        Item(
            key = network,
            network = network,
            label = label,
            objs = objs.toList(),
            unlimited = true,
        )

    fun destinationRow(network: String, ordinal: Int): String =
        "dbrow.teleport_dest_${network}_$ordinal"

    fun itemRow(key: String): String = "dbrow.teleport_item_$key"

    fun destinations() =
        dbTable("dbtable.teleport_destination", serverOnly = true) {
            column("network", DEST_NETWORK, VarType.STRING)
            column("ordinal", DEST_ORDINAL, VarType.INT)
            column("name", DEST_NAME, VarType.STRING)
            column("coords", DEST_COORDS, VarType.COORDGRID)
            column("wilderness", DEST_WILDERNESS, VarType.BOOLEAN)

            for ((network, stops) in NETWORKS) {
                stops.forEachIndexed { ordinal, stop ->
                    row(destinationRow(network, ordinal)) {
                        column(DEST_NETWORK, network)
                        column(DEST_ORDINAL, ordinal)
                        column(DEST_NAME, stop.name)
                        columnCoord(DEST_COORDS, CoordGrid(stop.x, stop.z, stop.level))
                        column(DEST_WILDERNESS, stop.wilderness)
                    }
                }
            }
        }

    fun items() =
        dbTable("dbtable.teleport_item", serverOnly = true) {
            column("objs", ITEM_OBJS, VarType.OBJ)
            column("network", ITEM_NETWORK, VarType.STRING)
            column("label", ITEM_LABEL, VarType.STRING)
            column("teleport_type", ITEM_TELEPORT_TYPE, VarType.STRING)
            column("uncharged", ITEM_UNCHARGED, VarType.OBJ)
            column("unlimited", ITEM_UNLIMITED, VarType.BOOLEAN)
            column("charge_varbit", ITEM_CHARGE_VARBIT, VarType.INT)
            column("max_charges", ITEM_MAX_CHARGES, VarType.INT)
            column("depart_message", ITEM_DEPART_MESSAGE, VarType.STRING)
            column("arrive_message", ITEM_ARRIVE_MESSAGE, VarType.STRING)

            for (item in ITEMS) {
                row(itemRow(item.key)) {
                    columnRSCM(ITEM_OBJS, *item.objs.toTypedArray())
                    column(ITEM_NETWORK, item.network)
                    column(ITEM_LABEL, item.label)
                    column(ITEM_TELEPORT_TYPE, item.teleportType)
                    item.uncharged?.let { columnRSCM(ITEM_UNCHARGED, it) }
                    column(ITEM_UNLIMITED, item.unlimited)
                    item.chargeVarbit?.let { columnRSCM(ITEM_CHARGE_VARBIT, it) }
                    item.maxCharges?.let { column(ITEM_MAX_CHARGES, it) }
                    item.departMessage?.let { column(ITEM_DEPART_MESSAGE, it) }
                    item.arriveMessage?.let { column(ITEM_ARRIVE_MESSAGE, it) }
                }
            }
        }
}
