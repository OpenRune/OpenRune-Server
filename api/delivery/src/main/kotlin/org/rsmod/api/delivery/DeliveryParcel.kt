package org.rsmod.api.delivery

public class DeliveryParcel(
    public val payload: Int,
    public val amount: Int,
    public val remainingTicks: Int,
) {
    override fun toString(): String =
        "DeliveryParcel(payload=$payload, amount=$amount, remainingTicks=$remainingTicks)"
}
