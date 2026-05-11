package org.rsmod.api.net.login

import dev.or2.login.model.OpenRuneLoginOutcome
import net.rsprot.protocol.loginprot.outgoing.LoginResponse

public fun LoginResponse.toOpenRuneLoginOutcome(): OpenRuneLoginOutcome =
    when (this) {
        is LoginResponse.Ok ->
            OpenRuneLoginOutcome.Ok(
                slot = index,
                staffModLevel = staffModLevel,
                playerMod = playerMod,
                member = member,
                accountHash = accountHash,
                userId = userId,
                userHash = userHash,
                authenticatorKind = authenticatorResponse::class.simpleName ?: "AuthenticatorResponse",
            )
        LoginResponse.InvalidUsernameOrPassword -> OpenRuneLoginOutcome.Denied.InvalidCredentials
        LoginResponse.Banned -> OpenRuneLoginOutcome.Denied.Banned
        LoginResponse.IPBlocked -> OpenRuneLoginOutcome.Denied.InvalidCredentials
        LoginResponse.Locked -> OpenRuneLoginOutcome.Denied.Locked
        LoginResponse.ServerFull -> OpenRuneLoginOutcome.Denied.ServerFull
        LoginResponse.Duplicate -> OpenRuneLoginOutcome.Denied.Duplicate
        LoginResponse.Timeout -> OpenRuneLoginOutcome.Denied.Timeout
        LoginResponse.UpdateInProgress -> OpenRuneLoginOutcome.Denied.UpdateInProgress
        LoginResponse.LoginServerOffline -> OpenRuneLoginOutcome.Denied.LoginServerOffline
        LoginResponse.LoginServerNoReply -> OpenRuneLoginOutcome.Denied.LoginServerNoReply
        LoginResponse.LoginServerLoadError -> OpenRuneLoginOutcome.Denied.LoginServerLoadError
        LoginResponse.UnknownReplyFromLoginServer -> OpenRuneLoginOutcome.Denied.UnknownReplyFromLoginServer
        LoginResponse.InvalidAuthenticatorCode -> OpenRuneLoginOutcome.Denied.InvalidAuthenticatorCode
        LoginResponse.Authenticator -> OpenRuneLoginOutcome.Denied.AuthenticatorRequired
        LoginResponse.InvalidSave -> OpenRuneLoginOutcome.Denied.InvalidSave
        LoginResponse.ConnectFail -> OpenRuneLoginOutcome.Denied.ConnectFail
        is LoginResponse.DisallowedByScript ->
            OpenRuneLoginOutcome.Denied.DisallowedByScript(
                line1 = line1,
                line2 = line2,
                line3 = line3,
            )
        else ->
            OpenRuneLoginOutcome.Denied.Unmapped(
                rsprotKind = this::class.simpleName ?: "LoginResponse",
            )
    }

public fun OpenRuneLoginOutcome.Denied.toLoginResponse(): LoginResponse =
    when (this) {
        OpenRuneLoginOutcome.Denied.InvalidCredentials -> LoginResponse.InvalidUsernameOrPassword
        OpenRuneLoginOutcome.Denied.Banned -> LoginResponse.Banned
        OpenRuneLoginOutcome.Denied.Locked -> LoginResponse.Locked
        OpenRuneLoginOutcome.Denied.ServerFull -> LoginResponse.ServerFull
        OpenRuneLoginOutcome.Denied.Duplicate -> LoginResponse.Duplicate
        OpenRuneLoginOutcome.Denied.Timeout -> LoginResponse.Timeout
        OpenRuneLoginOutcome.Denied.UpdateInProgress -> LoginResponse.UpdateInProgress
        OpenRuneLoginOutcome.Denied.LoginServerOffline -> LoginResponse.LoginServerOffline
        OpenRuneLoginOutcome.Denied.LoginServerNoReply -> LoginResponse.LoginServerNoReply
        OpenRuneLoginOutcome.Denied.LoginServerLoadError -> LoginResponse.LoginServerLoadError
        OpenRuneLoginOutcome.Denied.UnknownReplyFromLoginServer -> LoginResponse.UnknownReplyFromLoginServer
        OpenRuneLoginOutcome.Denied.InvalidAuthenticatorCode -> LoginResponse.InvalidAuthenticatorCode
        OpenRuneLoginOutcome.Denied.AuthenticatorRequired -> LoginResponse.Authenticator
        OpenRuneLoginOutcome.Denied.InvalidSave -> LoginResponse.InvalidSave
        OpenRuneLoginOutcome.Denied.ConnectFail -> LoginResponse.ConnectFail
        is OpenRuneLoginOutcome.Denied.DisallowedByScript ->
            LoginResponse.DisallowedByScript(line1, line2, line3)
        is OpenRuneLoginOutcome.Denied.Unmapped -> LoginResponse.UnknownReplyFromLoginServer
    }
