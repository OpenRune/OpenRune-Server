package org.rsmod.api.type.refs.synth

import jakarta.inject.Inject
import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.api.type.refs.resolver.TypeReferenceResolver
import org.rsmod.api.type.refs.resolver.TypeReferenceResult
import org.rsmod.api.type.refs.resolver.TypeReferenceResult.FullSuccess
import org.rsmod.api.type.refs.resolver.TypeReferenceResult.ImplicitNameNotFound
import org.rsmod.api.type.refs.resolver.err
import org.rsmod.api.type.refs.resolver.ok
import org.rsmod.api.type.symbols.name.NameMapping
import org.rsmod.game.type.TypeResolver
import org.rsmod.game.type.synth.SynthType

public class SynthReferenceResolver @Inject constructor(private val nameMapping: NameMapping) :
    TypeReferenceResolver<SynthType> {
    private val names: Map<String, Int>
        get() = nameMapping.synths

    override fun resolve(refs: TypeReferences<SynthType>): List<TypeReferenceResult> =
        refs.cache.map { it.resolve() }

    private fun SynthType.resolve(): TypeReferenceResult {
        val internalId = names[internalName] ?: return err(ImplicitNameNotFound(internalName))
        TypeResolver[this] = internalId
        return ok(FullSuccess)
    }
}
