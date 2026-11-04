package org.rsmod.api.delivery

/**
 * An arrival can be partially refused by store capacity; callers must handle its accepted and
 * refused amounts.
 */
public class DeliveryArrival(
    public val payload: Int,
    public val amount: Int,
    public val stored: Int,
    public val discarded: Int,
    public val storedTotal: Int,
) {
    public val overflowed: Boolean
        get() = discarded > 0

    override fun toString(): String =
        "DeliveryArrival(payload=$payload, amount=$amount, stored=$stored, " +
            "discarded=$discarded, storedTotal=$storedTotal)"
}
