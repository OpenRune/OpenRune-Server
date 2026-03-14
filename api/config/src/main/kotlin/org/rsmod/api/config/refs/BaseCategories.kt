@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.category.CategoryReferences

typealias categories = BaseCategories

object BaseCategories : CategoryReferences() {
    val staff = category("staff")
    val throwing_weapon = category("throwing_weapon")
    val spear = category("spear")
    val crossbow = category("crossbow")
    val rack_bolts = category("rack_bolts")
    val arrows = category("arrows")
    val crossbow_bolt = category("crossbow_bolt")
    val bow = category("bow")
    val halberd = category("halberd")
    val ogre_arrows = category("ogre_arrows")
    val chargebow = category("chargebow")
    val rune = category("rune")
    val vampyres = category("vampyres")
    val arrows_training = category("arrows_training")
    val ballista = category("ballista")
    val chinchompa = category("chinchompa")
    val kebbit_bolts = category("kebbit_bolts")
    val javelin = category("javelin")
    val dragon_arrow = category("dragon_arrow")
    val dinhs_bulwark = category("dinhs_bulwark")
    val atlatl_dart = category("atlatl_dart")

    val attacktype_stab = category("attacktype_stab")
    val attacktype_slash = category("attacktype_slash")
    val attacktype_crush = category("attacktype_crush")
}
