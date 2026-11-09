package org.rsmod.content.other.grandexchange

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.content.other.grandexchange.persistence.GeOfferPipeline
import org.rsmod.plugin.module.PluginModule

class GrandExchangeModule : PluginModule() {
    override fun bind() {
        addSetBinding<CharacterDataStage.Pipeline>(GeOfferPipeline::class.java)
    }
}
