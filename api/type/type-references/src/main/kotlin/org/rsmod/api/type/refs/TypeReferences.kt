package org.rsmod.api.type.refs

public abstract class TypeReferences<T>(internal val type: Class<T>) {
    internal val cache = mutableListOf<T>()
}
