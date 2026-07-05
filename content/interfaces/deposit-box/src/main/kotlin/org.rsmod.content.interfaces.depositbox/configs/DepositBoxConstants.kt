@file:Suppress("ConstPropertyName")

package org.rsmod.content.interfaces.depositbox.configs

internal typealias deposit_constants = DepositBoxConstants

object DepositBoxConstants {
    // Content group tagged on deposit box locs.
    const val content_group = "content.bank_deposit_box"

    // Animation played when depositing.
    const val open_seq = "seq.human_leverdown"

    // The deposit box modal.
    const val interface_main = "interface.bank_depositbox"

    // The deposit box's own inventory grid (clicked to deposit).
    const val comp_items = "component.bank_depositbox:inventory"

    // "Deposit inventory" and "Deposit worn items" buttons.
    const val comp_deposit_inv = "component.bank_depositbox:deposit_inv"
    const val comp_deposit_worn = "component.bank_depositbox:deposit_worn"

    // "Hide deposit worn items" checkbox in the top-left menu dropdown.
    const val comp_depositworn_toggle = "component.bank_depositbox:depositworn_toggle"

    // Quantity selection buttons (1 / 5 / 10 / X / All).
    const val comp_quantity_1 = "component.bank_depositbox:1"
    const val comp_quantity_5 = "component.bank_depositbox:5"
    const val comp_quantity_10 = "component.bank_depositbox:10"
    const val comp_quantity_x = "component.bank_depositbox:x"
    const val comp_quantity_all = "component.bank_depositbox:all"

    //TODO: Inventory slot-lock settings button is not implemented.
    const val comp_lock_menu = "component.bank_depositbox:lock_menu"

    // Wearpos of each equipment slot shown in the box; the per-slot component is "slot<wearpos>".
    val worn_wearpos_slots = intArrayOf(0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13)

    fun wornComponent(wearpos: Int): String = "component.bank_depositbox:slot$wearpos"

    // Inventory interfaces we swap to "disable" the inventory when the deposit box interface is open.
    const val inventory_interface = "interface.inventory"
    const val inventory_disabled = "interface.inventory_noops"
    const val inventory_main_target = "component.toplevel_osrs_stretch:side3"
}
