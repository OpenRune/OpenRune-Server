@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias interfaces = BaseInterfaces

object BaseInterfaces : InterfaceReferences() {
    val fade_overlay = inter("fade_overlay")

    val bank_main = inter("bankmain")
    val bank_side = inter("bankside")
    val bankpin_settings = inter("bankpin_settings")

    val toplevel = inter("toplevel")
    val toplevel_osrs_stretch = inter("toplevel_osrs_stretch")
    val toplevel_pre_eoc = inter("toplevel_pre_eoc")

    val buff_bar = inter("buff_bar")
    val stat_boosts_hud = inter("stat_boosts_hud")
    val pvp_icons = inter("pvp_icons")
    val ehc_worldhop = inter("ehc_worldhop")
    val chatbox = inter("chatbox")
    val popout = inter("popout")
    val pm_chat = inter("pm_chat")
    val orbs = inter("orbs")
    val xp_drops = inter("xp_drops")
    val stats = inter("stats")
    val side_journal = inter("side_journal")
    val questlist = inter("questlist")
    val inventory = inter("inventory")
    val wornitems = inter("wornitems")
    val side_channels = inter("side_channels")
    val settings_side = inter("settings_side")
    val prayerbook = inter("prayerbook")
    val magic_spellbook = inter("magic_spellbook")
    val friends = inter("friends")
    val account = inter("account")
    val logout = inter("logout")
    val emote = inter("emote")
    val music = inter("music")
    val chatchannel_current = inter("chatchannel_current")
    val worldswitcher = inter("worldswitcher")
    val combat_interface = inter("combat_interface")
    val hpbar_hud = inter("hpbar_hud")

    val account_summary_sidepanel = inter("account_summary_sidepanel")
    val area_task = inter("area_task")

    val chat_right = inter("chat_right")
    val chat_left = inter("chat_left")
    val chatmenu = inter("chatmenu")
    val messagebox = inter("messagebox")
    val obj_dialogue = inter("objectbox")
    val double_obj_dialogue = inter("objectbox_double")
    val destroy_obj_dialogue = inter("confirmdestroy")
    val menu = inter("menu")

    val popupoverlay = inter("popupoverlay")
    val ge_collection_box = inter("ge_collect")
    val ca_overview = inter("ca_overview")
    val collection = inter("collection")
    val bond_main = inter("bond_main")
    val poh_options = inter("poh_options")
    val settings = inter("settings")
}
