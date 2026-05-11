package org.rsmod.api.net.rsprot

import com.google.inject.Provider
import com.google.inject.Scopes
import com.google.inject.TypeLiteral
import jakarta.inject.Inject
import net.rsprot.protocol.api.NetworkService
import org.rsmod.api.net.central.embed.CentralEmbeddedLifecycle
import org.rsmod.api.net.central.logging.CentralActivityLogWriter
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule
import org.rsmod.server.services.Service

class NetworkModule : PluginModule() {
    override fun bind() {
        bindInstance<CentralEmbeddedLifecycle>()
        bindInstance<CentralActivityLogWriter>()
        bind(object : TypeLiteral<NetworkService<Player>>() {})
            .toProvider(NetworkServiceProvider::class.java)
            .`in`(Scopes.SINGLETON)
        addSetBinding<Service>(RspService::class.java)
    }

    private class NetworkServiceProvider @Inject constructor(private val factory: NetworkFactory) :
        Provider<NetworkService<Player>> {
        override fun get(): NetworkService<Player> = factory.build()
    }
}
