package org.rsmod.api.game.process

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

public class PluginScriptBootGate {
    private val ready = AtomicBoolean(false)
    private val latch = CountDownLatch(1)

    public fun markReady() {
        if (ready.compareAndSet(false, true)) {
            latch.countDown()
        }
    }

    public fun awaitReady() {
        if (ready.get()) {
            return
        }
        check(latch.await(5, TimeUnit.MINUTES)) {
            "Timed out waiting for plugin scripts before network/game Startup"
        }
    }

    public fun isReady(): Boolean = ready.get()
}
