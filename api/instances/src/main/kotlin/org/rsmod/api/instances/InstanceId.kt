package org.rsmod.api.instances

@JvmInline
value class InstanceId(val value: Long) {
    override fun toString(): String = value.toString()
}
