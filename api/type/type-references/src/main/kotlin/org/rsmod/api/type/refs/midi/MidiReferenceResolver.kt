package org.rsmod.api.type.refs.midi

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
import org.rsmod.game.type.midi.MidiType

public class MidiReferenceResolver @Inject constructor(private val nameMapping: NameMapping) :
    TypeReferenceResolver<MidiType> {
    private val names: Map<String, Int>
        get() = nameMapping.midis

    override fun resolve(refs: TypeReferences<MidiType>): List<TypeReferenceResult> =
        refs.cache.map { it.resolve() }

    private fun MidiType.resolve(): TypeReferenceResult {
        val internalId = names[internalName] ?: return err(ImplicitNameNotFound(internalName))
        TypeResolver[this] = internalId
        return ok(FullSuccess)
    }
}
