package org.rsmod.content.interfaces.depositbox.configs

object DepositBoxConfig {
    // Behavior when the clicked item also exists in other inventory slots. This can probably be extracted into a toml at some point.
    // true  = deposit from the topmost matching slot (left-to-right, top-to-bottom). This is the accurate behavior.
    // false = deposit from the exact slot that was clicked.
    const val deposit_top_to_bottom: Boolean = true
}
