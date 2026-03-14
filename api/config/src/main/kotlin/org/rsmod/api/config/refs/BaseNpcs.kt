@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.npc.NpcReferences

typealias npcs = BaseNpcs

object BaseNpcs : NpcReferences() {
    val man = npc("man")
    val man2 = npc("man2")
    val man3 = npc("man3")
    val woman = npc("woman")
    val woman2 = npc("woman2")
    val woman3 = npc("woman3")
    val man_indoor = npc("man_indoor")
    val uri_emote_1 = npc("trail_master_uri")
    val uri_emote_2 = npc("uri_emote")
    val diary_emote_npc = npc("diary_emote_npc")
    val corp_beast = npc("corp_beast")
    val imp = npc("imp")
    val farming_tools_leprechaun = npc("farming_tools_leprechaun")
    val rod_fishing_spot_1527 = npc("0_50_50_freshfish")
    val fishing_spot_1530 = npc("0_50_49_saltfish")
}
