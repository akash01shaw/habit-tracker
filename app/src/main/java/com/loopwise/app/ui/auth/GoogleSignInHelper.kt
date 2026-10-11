package com.loopwise.app.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.loopwise.app.BuildConfig

/**
 * Launches the system "Sign in with Google" sheet via Credential Manager and returns
 * the Google ID token, or null if the user cancelled / no credential was available.
 * That token is handed to Supabase Auth's IDToken provider (see AuthRepository).
 */
suspend fun requestGoogleIdToken(context: Context): String? {
    val option = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    return runCatching {
        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            null
        }
    }.getOrNull()
}
