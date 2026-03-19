package org.rsmod.api.core

import org.rsmod.api.account.AccountModule
import org.rsmod.api.area.checker.AreaCheckerModule
import org.rsmod.api.core.module.EntityHitModule
import org.rsmod.api.core.module.GameMapModule
import org.rsmod.api.core.module.PlayerModule
import org.rsmod.api.core.module.RealmModule
import org.rsmod.api.core.module.RegistryModule
import org.rsmod.api.core.module.StatModModule
import org.rsmod.api.db.DatabaseModule
import org.rsmod.api.game.process.GameCycle
import org.rsmod.api.hunt.HuntModule
import org.rsmod.api.market.MarketModule
import org.rsmod.api.pw.hash.PasswordHashModule
import org.rsmod.api.random.RandomModule
import org.rsmod.api.route.RouteModule
import org.rsmod.api.server.config.ServerConfigModule
import org.rsmod.api.totp.TotpModule
import org.rsmod.api.utils.logging.ExceptionHandlerModule
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.module.ExtendedModule

public object CoreModule : ExtendedModule() {
    override fun bind() {
        install(AccountModule)
        install(AreaCheckerModule)
        install(DatabaseModule)
        install(EntityHitModule)
        install(ExceptionHandlerModule)
        install(GameMapModule)
        install(HuntModule)
        install(MarketModule)
        install(PlayerModule)
        install(PasswordHashModule)
        install(RandomModule)
        install(RealmModule)
        install(RegistryModule)
        install(RouteModule)
        install(ServerConfigModule)
        install(StatModModule)
        install(TotpModule)
        bindInstance<GameCycle>()
        bindInstance<WorldQueueList>()
    }
}
