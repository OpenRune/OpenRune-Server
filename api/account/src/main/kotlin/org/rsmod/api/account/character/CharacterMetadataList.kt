package org.rsmod.api.account.character

import dev.or2.central.account.AccountData

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
public data class CharacterMetadataList(
    val accountData: AccountData,
    val transformers: MutableList<CharacterDataTransformer<*>>,
) : List<CharacterDataTransformer<*>> by transformers {
    public val characterId: Int
        get() = accountData.characterData.characterId

    public fun <T : CharacterDataStage.Segment> add(
        applier: CharacterDataStage.Applier<T>,
        segment: T,
    ) {
        val transformer = CharacterDataTransformer(applier, segment)
        transformers.add(transformer)
    }
}
