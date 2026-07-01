package org.rsmod.api.totp

import org.rsmod.api.totp.google.GoogleTotp
import org.rsmod.api.totp.laravel.LaravelSettingsLoader
import org.rsmod.api.totp.laravel.TwoFactorSecretResolver
import org.rsmod.module.ExtendedModule

public object TotpModule : ExtendedModule() {
    override fun bind() {
        bindBaseInstance<Totp>(GoogleTotp::class.java)
        bindInstance<LaravelSettingsLoader>()
        bindInstance<TwoFactorSecretResolver>()
    }
}
