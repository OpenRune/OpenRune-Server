@file:Suppress("unused", "SpellCheckingInspection")

package org.rsmod.content.areas.city.lumbridge.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.map.CoordGrid

typealias lumbridge_npcs = LumbridgeNpcs

object LumbridgeNpcs : NpcReferences() {
    val barfy_bill = npc("canoeing_bill")
    val banker = npc("deadman_banker_blue_south")
    val banker_tutor = npc("aide_tutor_banker")
    val shop_keeper = npc("generalshopkeeper1")
    val shop_assistant = npc("generalassistant1")
    val gee = npc("lumbridge_guide2_man")
    val donie = npc("lumbridge_guide2_woman")
    val hans = npc("hans")
    val bartender = npc("ram_bartender")
    val arthur_the_clue_hunter = npc("aide_tutor_clues")
    val prayer_tutor = npc("aide_tutor_prayer")
    val hatius_lumbridge_diary = npc("hatius_lumbridge_diary")
    val bob = npc("bob")
    val woodsman_tutor = npc("aide_tutor_woodsman")
    val smithing_apprentice = npc("aide_tutor_smithing_apprentice")
    val father_aereck = npc("father_aereck")
    val cook = npc("cook")
    val perdu = npc("lost_property_merchant_standard")
    val guide = npc("lumbridge_guide")
    val doomsayer = npc("cws_doomsayer")
    val abigaila = npc("tob_spectator_misthalin")
    val count_check = npc("count_check")
    val veos = npc("veos_lumbridge")
    val adventurer_jon = npc("ap_guide_parent")
    val hewey = npc("mistmyst_hewey")
    val fishing_tutor = npc("aide_tutor_fishing")
    val millie = npc("millie_the_miller")
}

internal object LumbridgeNpcEditor : NpcEditor() {
    init {
        edit(lumbridge_npcs.shop_keeper) { moveRestrict = indoors }

        edit(lumbridge_npcs.shop_assistant) { moveRestrict = indoors }

        edit(lumbridge_npcs.banker) { contentGroup = content.banker }

        edit(lumbridge_npcs.banker_tutor) { contentGroup = content.banker_tutor }

        edit(lumbridge_npcs.prayer_tutor) { moveRestrict = indoors }

        edit(lumbridge_npcs.father_aereck) { moveRestrict = indoors }

        edit(lumbridge_npcs.hans) {
            defaultMode = patrol
            patrol1 = patrol(CoordGrid(0, 50, 50, 7, 33), 0)
            patrol2 = patrol(CoordGrid(0, 50, 50, 11, 30), 0)
            patrol3 = patrol(CoordGrid(0, 50, 50, 19, 30), 0)
            patrol4 = patrol(CoordGrid(0, 50, 50, 19, 22), 10)
            patrol5 = patrol(CoordGrid(0, 50, 50, 21, 22), 0)
            patrol6 = patrol(CoordGrid(0, 50, 50, 21, 12), 0)
            patrol7 = patrol(CoordGrid(0, 50, 50, 18, 9), 0)
            patrol8 = patrol(CoordGrid(0, 50, 50, 14, 5), 0)
            patrol9 = patrol(CoordGrid(0, 50, 50, 2, 5), 0)
            patrol10 = patrol(CoordGrid(0, 50, 50, 2, 32), 0)
            maxRange = 40
        }

        edit(lumbridge_npcs.cook) { moveRestrict = indoors }

        edit(lumbridge_npcs.perdu) {
            respawnDir = west
            wanderRange = 0
        }

        edit(lumbridge_npcs.guide) {
            respawnDir = west
            wanderRange = 0
        }

        edit(lumbridge_npcs.doomsayer) {
            respawnDir = east
            wanderRange = 0
        }

        edit(lumbridge_npcs.abigaila) {
            respawnDir = south
            wanderRange = 0
        }

        edit(lumbridge_npcs.count_check) {
            respawnDir = east
            wanderRange = 0
        }

        edit(lumbridge_npcs.arthur_the_clue_hunter) {
            respawnDir = north
            wanderRange = 0
            timer = 20
        }

        edit(lumbridge_npcs.bartender) {
            respawnDir = west
            wanderRange = 0
        }

        edit(lumbridge_npcs.smithing_apprentice) { moveRestrict = indoors }

        edit(lumbridge_npcs.veos) {
            respawnDir = south
            wanderRange = 0
        }

        edit(lumbridge_npcs.adventurer_jon) {
            respawnDir = south
            wanderRange = 0
        }

        edit(lumbridge_npcs.hewey) {
            respawnDir = east
            wanderRange = 0
        }

        edit(lumbridge_npcs.fishing_tutor) {
            respawnDir = east
            wanderRange = 0
        }

        edit(lumbridge_npcs.bob) {
            moveRestrict = indoors
            param[ShopParams.shop_sell_percentage] = 1000
            param[ShopParams.shop_buy_percentage] = 600
            param[ShopParams.shop_change_percentage] = 20
        }

        edit(lumbridge_npcs.millie) { wanderRange = 1 }
    }
}
