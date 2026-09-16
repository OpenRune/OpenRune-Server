package org.rsmod.server.app

import org.rsmod.module.ExtendedModule
import org.rsmod.server.app.modules.GameModule
import org.rsmod.server.app.modules.ParserModule
import org.rsmod.server.app.modules.ServiceModule

object GameServerModule : ExtendedModule() {
    override fun bind() {
        install(GameModule)
        install(ParserModule)
        install(ServiceModule)
    }
}
