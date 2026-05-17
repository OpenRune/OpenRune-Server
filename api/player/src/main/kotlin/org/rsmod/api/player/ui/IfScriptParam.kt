package org.rsmod.api.player.ui

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class IfScriptParam(val type: Char)
