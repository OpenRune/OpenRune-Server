package org.rsmod.api.type.refs.resolver

import org.rsmod.api.type.refs.TypeReferences

public fun interface TypeReferenceResolver<T> {
    public fun resolve(refs: TypeReferences<T>): List<TypeReferenceResult>
}
