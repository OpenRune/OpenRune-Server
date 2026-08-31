package org.rsmod.content.skills.fishing.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object FishingConfig {
    const val M_ID = 0
    const val M_TOOL = 1
    const val M_BAIT = 2
    const val M_ANIM = 3
    const val M_ARTICLE = 4
    const val M_MSG = 5
    const val M_ALT_TOOL = 6
    const val M_FALLBACK = 7

    fun fishingMethod() = dbTable("dbtable.fishing_method", serverOnly = true) {
        column("method_id", M_ID, VarType.INT)
        column("tool", M_TOOL, VarType.STRING)
        column("bait", M_BAIT, VarType.STRING)
        column("anim", M_ANIM, VarType.STRING)
        column("article", M_ARTICLE, VarType.STRING)
        column("msg", M_MSG, VarType.STRING)
        column("alt_tool", M_ALT_TOOL, VarType.STRING)
        column("fallback", M_FALLBACK, VarType.INT)

        fun method(
            name: String,
            id: Int,
            tool: String?,
            bait: String?,
            anim: String,
            article: String,
            msg: String,
            altTool: String? = null,
            fallback: Int = -1,
        ) = row(name) {
            column(M_ID, id)
            if (tool != null) column(M_TOOL, tool)
            if (bait != null) column(M_BAIT, bait)
            column(M_ANIM, anim)
            column(M_ARTICLE, article)
            column(M_MSG, msg)
            if (altTool != null) column(M_ALT_TOOL, altTool)
            column(M_FALLBACK, fallback)
        }

        method("dbrow.fishing_method_net", 0, "obj.net", null, "seq.human_smallnet", "some", "You need a small fishing net to fish here.")
        method("dbrow.fishing_method_bait", 1, "obj.fishing_rod", "obj.fishing_bait", "seq.human_fishing_casting", "a", "You need a fishing rod to fish here.")
        method("dbrow.fishing_method_lure", 2, "obj.fly_fishing_rod", "obj.feather", "seq.human_fishing_casting", "a", "You need a fly fishing rod to lure fish here.", altTool = "obj.fishingrod_pearl_fly")
        method("dbrow.fishing_method_cage", 3, "obj.lobster_pot", null, "seq.human_lobster", "a", "You need a lobster pot to catch these fish.")
        method("dbrow.fishing_method_harpoon", 4, "obj.harpoon", null, "seq.human_harpoon", "a", "You need a harpoon to catch these fish.", fallback = 10)
        method("dbrow.fishing_method_big_net", 5, "obj.big_net", null, "seq.human_largenet", "a", "You need a big fishing net to catch these fish.")
        method("dbrow.fishing_method_oily_rod", 6, "obj.oily_fishing_rod", "obj.fishing_bait", "seq.human_fishing_casting", "a", "You need an oily fishing rod to fish here.", altTool = "obj.fishingrod_pearl_oily")
        method("dbrow.fishing_method_sandworm", 7, "obj.fishing_rod", "obj.piscarilius_sandworms", "seq.human_fishing_casting", "a", "You need a fishing rod to fish here.", altTool = "obj.fishingrod_pearl")
        method("dbrow.fishing_method_dark_crab_cage", 8, "obj.lobster_pot", "obj.wilderness_fishing_bait", "seq.human_lobster", "a", "You need a lobster pot to catch these fish.")
        method("dbrow.fishing_method_barbarian", 9, "obj.brut_fishing_rod", "obj.fishing_bait", "seq.human_fishing_casting", "a", "You need a barbarian rod to fish here.", altTool = "obj.fishingrod_pearl_brut")
        method("dbrow.fishing_method_barehand", 10, null, null, "seq.human_harpoon", "a", "You need to learn how to fish with your bare hands first.")
    }

    const val S_ID = 0
    const val S_CONTENT = 1
    const val S_OP1 = 2
    const val S_OP3 = 3

    fun fishingSpotDef() = dbTable("dbtable.fishing_spot_def", serverOnly = true) {
        column("spot_id", S_ID, VarType.INT)
        column("content", S_CONTENT, VarType.STRING)
        column("op1", S_OP1, VarType.INT)
        column("op3", S_OP3, VarType.INT)

        fun spot(name: String, id: Int, content: String, op1: Int, op3: Int = -1) = row(name) {
            column(S_ID, id)
            column(S_CONTENT, content)
            column(S_OP1, op1)
            column(S_OP3, op3)
        }

        spot("dbrow.fishing_spot_net_bait", 0, "content.net_bait_fishing_spot", 0, 1)
        spot("dbrow.fishing_spot_lure_bait", 1, "content.lure_bait_fishing_spot", 2, 1)
        spot("dbrow.fishing_spot_cage_harpoon", 2, "content.cage_harpoon_fishing_spot", 3, 4)
        spot("dbrow.fishing_spot_big_net_harpoon", 3, "content.big_net_harpoon_fishing_spot", 5, 4)
        spot("dbrow.fishing_spot_monkfish", 4, "content.monkfish_fishing_spot", 0)
        spot("dbrow.fishing_spot_slimy_eel", 5, "content.slimy_eel_fishing_spot", 1)
        spot("dbrow.fishing_spot_cave_eel", 6, "content.cave_eel_fishing_spot", 1)
        spot("dbrow.fishing_spot_lava_eel", 7, "content.lava_eel_fishing_spot", 6)
        spot("dbrow.fishing_spot_anglerfish", 8, "content.anglerfish_fishing_spot", 7)
        spot("dbrow.fishing_spot_dark_crab", 9, "content.dark_crab_fishing_spot", 8)
        spot("dbrow.fishing_spot_infernal_eel", 10, "content.infernal_eel_fishing_spot", 6)
        spot("dbrow.fishing_spot_karambwanji", 11, "content.karambwanji_fishing_spot", 0)
        spot("dbrow.fishing_spot_barbarian", 12, "content.barbarian_fishing_spot", 9)
        spot("dbrow.fishing_spot_minnow", 13, "content.minnow_fishing_spot", 0)
    }
}
