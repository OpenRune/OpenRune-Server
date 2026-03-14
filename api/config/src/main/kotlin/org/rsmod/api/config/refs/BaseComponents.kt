@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias components = BaseComponents

object BaseComponents : ComponentReferences() {
    val mainmodal = component("toplevel_osrs_stretch:mainmodal")
    val sidemodal = component("toplevel_osrs_stretch:sidemodal")

    val hp_hud_container = component("hpbar_hud:container")
    val hp_hud_hp = component("hpbar_hud:hp")

    val toplevel_target_ehc_listener = component("toplevel_osrs_stretch:ehc_listener")
    val toplevel_target_buff_bar = component("toplevel_osrs_stretch:buff_bar")
    val toplevel_target_stat_boosts_hud = component("toplevel_osrs_stretch:stat_boosts_hud")
    val toplevel_target_popout = component("toplevel_osrs_stretch:popout")
    val toplevel_target_side0 = component("toplevel_osrs_stretch:side0")
    val toplevel_target_side1 = component("toplevel_osrs_stretch:side1")
    val toplevel_target_side2 = component("toplevel_osrs_stretch:side2")
    val toplevel_target_side3 = component("toplevel_osrs_stretch:side3")
    val toplevel_target_side4 = component("toplevel_osrs_stretch:side4")
    val toplevel_target_side5 = component("toplevel_osrs_stretch:side5")
    val toplevel_target_side6 = component("toplevel_osrs_stretch:side6")
    val toplevel_target_side7 = component("toplevel_osrs_stretch:side7")
    val toplevel_target_side8 = component("toplevel_osrs_stretch:side8")
    val toplevel_target_side9 = component("toplevel_osrs_stretch:side9")
    val toplevel_target_side10 = component("toplevel_osrs_stretch:side10")
    val toplevel_target_side11 = component("toplevel_osrs_stretch:side11")
    val toplevel_target_side12 = component("toplevel_osrs_stretch:side12")
    val toplevel_target_side13 = component("toplevel_osrs_stretch:side13")
    val toplevel_target_orbs = component("toplevel_osrs_stretch:orbs")
    val toplevel_target_chat_container = component("toplevel_osrs_stretch:chat_container")
    val toplevel_target_pvp_icons = component("toplevel_osrs_stretch:pvp_icons")
    val toplevel_target_xp_drops = component("toplevel_osrs_stretch:xp_drops")
    val toplevel_target_pm_container = component("toplevel_osrs_stretch:pm_container")
    val toplevel_target_hpbar_hud = component("toplevel_osrs_stretch:hpbar_hud")
    val toplevel_target_floater = component("toplevel_osrs_stretch:floater")
    val toplevel_target_overlay_atmosphere = component("toplevel_osrs_stretch:overlay_atmosphere")
    val toplevel_target_maincrm = component("toplevel_osrs_stretch:maincrm")
    val toplevel_target_sidecrm = component("toplevel_osrs_stretch:sidecrm")
    val toplevel_target_overlay_hud = component("toplevel_osrs_stretch:overlay_hud")
    val toplevel_target_zeah = component("toplevel_osrs_stretch:zeah")
    val toplevel_target_helper_content = component("toplevel_osrs_stretch:helper_content")

    val chatbox_chatmodal = component("chatbox:chatmodal")

    val chat_right_head = component("chat_right:head")
    val chat_right_name = component("chat_right:name")
    val chat_right_pbutton = component("chat_right:continue")
    val chat_right_text = component("chat_right:text")

    val chat_left_head = component("chat_left:head")
    val chat_left_name = component("chat_left:name")
    val chat_left_pbutton = component("chat_left:continue")
    val chat_left_text = component("chat_left:text")

    val chatmenu_pbutton = component("chatmenu:options")

    val messagebox_text = component("messagebox:text")
    val messagebox_pbutton = component("messagebox:continue")

    val objectbox_pbutton = component("objectbox:universe")
    val objectbox_text = component("objectbox:text")
    val objectbox_item = component("objectbox:item")

    val objectbox_double_pbutton = component("objectbox_double:pausebutton")
    val objectbox_double_text = component("objectbox_double:text")
    val objectbox_double_model1 = component("objectbox_double:model1")
    val objectbox_doublee_model2 = component("objectbox_double:model2")

    val confirmdestroy_pbutton = component("confirmdestroy:universe")

    val menu_list = component("menu:lj_layer1")

    val inv_items = component("inventory:items")

    val combat_tab_title = component("combat_interface:title")
    val combat_tab_category = component("combat_interface:category")

    val fade_overlay_message = component("fade_overlay:message")

    val music_now_playing_text = component("music:now_playing_text")
}
