package org.rsmod.content.other.toolbelt

object ToolbeltUi {
    const val INTERFACE = "interface.toolbelt"
    const val CONTENT = "component.toolbelt:content"
    const val SLOT_COUNT = 28
    const val ITEM_COM_BASE = 300
    const val LOCK_COM_BASE = 400
    const val BONE_CRUSHER_SLOT = 27

    fun slotFromComsub(comsub: Int): Int? = when (comsub) {
        in ITEM_COM_BASE until (ITEM_COM_BASE + SLOT_COUNT) -> comsub - ITEM_COM_BASE
        in LOCK_COM_BASE until (LOCK_COM_BASE + SLOT_COUNT) -> comsub - LOCK_COM_BASE
        in 0 until SLOT_COUNT -> comsub
        else -> null
    }
}
