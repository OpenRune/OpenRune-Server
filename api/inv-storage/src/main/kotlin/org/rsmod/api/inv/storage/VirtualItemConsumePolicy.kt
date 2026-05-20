package org.rsmod.api.inv.storage

/** How [PlayerItemStorage] combines physical inventory and virtual storage when deleting items. */
public enum class VirtualItemConsumePolicy {
    /** Use inventory slots first, then virtual storage (default coal bag smelting). */
    InventoryFirst,
    /** Use virtual storage first, then inventory slots. */
    StorageFirst,
}
