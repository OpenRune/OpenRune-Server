package org.alter.game.model.inv.objtx

public class TransactionCancellation(public val err: TransactionResult.Err) :
    IllegalStateException()
