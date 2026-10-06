package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import org.rsmod.api.registry.account.AccountRegistry

public class PlayerLoginProcess
@Inject
constructor(
    private val accountRegistry: AccountRegistry,
    private val worldTypeSwitches: WorldTypeSwitchService,
) {
    public fun process() {
        accountRegistry.handleLogins()
        worldTypeSwitches.processPendingFailures()
    }
}
