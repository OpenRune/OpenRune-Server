package org.rsmod.content.interfaces.bank.configs

import dev.openrune.queue

internal typealias bank_queues = BankQueues

object BankQueues {
    val bank_compress = queue("bank_compress")
}
