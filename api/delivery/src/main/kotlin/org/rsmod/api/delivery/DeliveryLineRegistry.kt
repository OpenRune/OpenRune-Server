package org.rsmod.api.delivery

import jakarta.inject.Singleton

@Singleton
public class DeliveryLineRegistry {
    private val lines: MutableMap<String, DeliveryLine> = HashMap()

    public fun register(line: DeliveryLine) {
        val existing = lines[line.id]
        if (existing === line) {
            return
        }
        check(existing == null) {
            "Delivery line id is already registered: id=${line.id}, existing=$existing, new=$line"
        }
        lines[line.id] = line
    }

    public fun find(id: String): DeliveryLine? = lines[id]

    public fun isRegistered(line: DeliveryLine): Boolean = lines[line.id] === line
}
