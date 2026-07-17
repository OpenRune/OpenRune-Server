package org.rsmod.content.interfaces.depositbox.configs

object DepositBoxConstants {

    // Animation played when depositing.
    const val OPEN_SEQ = "seq.human_leverdown"

    // The deposit box modal.
    const val INTERFACE_MAIN = "interface.bank_depositbox"

    // The deposit box's own inventory grid (clicked to deposit).
    const val COMP_ITEMS = "component.bank_depositbox:inventory"

    // "Deposit inventory" and "Deposit worn items" buttons.
    const val COMP_DEPOSIT_INV = "component.bank_depositbox:deposit_inv"
    const val COMP_DEPOSIT_WORN = "component.bank_depositbox:deposit_worn"

    // "Hide deposit worn items" checkbox in the top-left menu dropdown.
    const val COMP_DEPOSITWORN_TOGGLE = "component.bank_depositbox:depositworn_toggle"

    // Quantity selection buttons (1 / 5 / 10 / X / All).
    const val COMP_QUANTITY_1 = "component.bank_depositbox:1"
    const val COMP_QUANTITY_5 = "component.bank_depositbox:5"
    const val COMP_QUANTITY_10 = "component.bank_depositbox:10"
    const val COMP_QUANTITY_X = "component.bank_depositbox:x"
    const val COMP_QUANTITY_ALL = "component.bank_depositbox:all"

    //TODO: Inventory slot-lock settings button is not implemented.
    const val COM_LOCK_MENU = "component.bank_depositbox:lock_menu"

    // Wearpos of each equipment slot shown in the box; the per-slot component is "slot<wearpos>".
    val WORN_WEAPOS_SLOTS = intArrayOf(0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13)

    fun wornComponent(wearpos: Int): String = "component.bank_depositbox:slot$wearpos"

    // Inventory interfaces we swap to "disable" the inventory when the deposit box interface is open.
    const val INVENTORY_INTERFACE = "interface.inventory"
    const val INVENTORY_DISABLED = "interface.inventory_noops"
    const val INVENTORY_MAIN_TARGET = "component.toplevel_osrs_stretch:side3"
}
