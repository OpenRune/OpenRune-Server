package org.rsmod.content.interfaces.prayer.tab

import com.google.inject.Provider
import org.rsmod.plugin.module.PluginModule

class PrayerModule : PluginModule() {
    override fun bind() {
        bindProvider(PrayerRepositoryProvider::class.java)
    }
}

private class PrayerRepositoryProvider : Provider<PrayerRepository> {
    override fun get(): PrayerRepository {
        val repo = PrayerRepository()
        repo.load()
        return repo
    }
}
