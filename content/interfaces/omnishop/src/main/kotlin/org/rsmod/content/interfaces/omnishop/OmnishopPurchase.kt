package org.rsmod.content.interfaces.omnishop

import org.rsmod.api.player.protect.ProtectedAccess

/** Special native shop rewards can own their currency and account-unlock transaction. */
public interface OmnishopPurchase {
    public fun handles(shop: Int, stock: Int): Boolean
    public fun buy(access: ProtectedAccess, stock: Int, requested: Int)
}
