package org.rsmod.api.delivery

import org.rsmod.plugin.module.PluginModule

public class DeliveryModule : PluginModule() {
    override fun bind() {
        bindInstance<DeliveryClock>()
        bindInstance<DeliveryLineRegistry>()
        bindInstance<DeliveryManager>()
    }
}
